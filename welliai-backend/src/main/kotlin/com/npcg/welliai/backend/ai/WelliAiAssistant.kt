package com.npcg.welliai.backend.ai

import com.npcg.welliai.backend.api.ChatRequest
import com.npcg.welliai.backend.api.ChatResponse
import com.npcg.welliai.backend.api.OpenAiResponse
import com.npcg.welliai.backend.api.RecipeCard
import com.npcg.welliai.backend.api.RecipeGenerationRequest
import com.npcg.welliai.backend.api.RecipeGenerationResponse
import com.npcg.welliai.backend.api.RecipeIngredient
import com.npcg.welliai.backend.api.RecipeSubstitution
import com.npcg.welliai.backend.config.BackendConfig
import com.npcg.welliai.backend.security.normalizedIngredientName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

interface WelliAiAssistant {
    suspend fun generateRecipe(request: RecipeGenerationRequest): RecipeGenerationResponse
    suspend fun chat(request: ChatRequest): ChatResponse
}

class OpenAiWelliAssistant(
    private val config: BackendConfig,
    private val transport: OpenAiTransport,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : WelliAiAssistant {
    override suspend fun generateRecipe(request: RecipeGenerationRequest): RecipeGenerationResponse {
        val prompt = buildRecipePrompt(request)
        val response = execute(buildResponseRequest(
            instructions = RECIPE_INSTRUCTIONS,
            input = JsonPrimitive(prompt),
            maxOutputTokens = RECIPE_MAX_OUTPUT_TOKENS,
            structuredRecipe = true
        ))
        val text = response.outputTextOrThrow()
        val parsed = try {
            json.decodeFromString<RecipeGenerationResponse>(text)
        } catch (_: Exception) {
            throw InvalidModelResponseException()
        }
        return validateAndSanitizeRecipe(parsed, request)
    }

    override suspend fun chat(request: ChatRequest): ChatResponse {
        val input = buildJsonArray {
            request.messages.forEach { message ->
                add(buildJsonObject {
                    put("role", JsonPrimitive(message.role))
                    put("content", JsonPrimitive(message.content))
                })
            }
        }
        val response = execute(buildResponseRequest(
            instructions = CHAT_INSTRUCTIONS,
            input = input,
            maxOutputTokens = CHAT_MAX_OUTPUT_TOKENS,
            structuredRecipe = false
        ))
        val text = response.outputTextOrThrow().trim()
        if (text.isBlank() || text.length > MAX_CHAT_RESPONSE_CHARS) throw InvalidModelResponseException()
        return ChatResponse(text)
    }

    private suspend fun execute(requestBody: JsonObject): OpenAiResponse = try {
        withTimeout(OPENAI_REQUEST_TIMEOUT_MILLIS) { transport.createResponse(requestBody) }
    } catch (_: TimeoutCancellationException) {
        throw OpenAiTimeoutException()
    } catch (error: CancellationException) {
        throw error
    }

    private fun validateAndSanitizeRecipe(
        response: RecipeGenerationResponse,
        request: RecipeGenerationRequest
    ): RecipeGenerationResponse {
        if (!response.canGenerate) {
            if (response.recipe != null || response.message.isBlank()) throw InvalidModelResponseException()
            return response.copy(message = response.message.take(MAX_RECIPE_MESSAGE_CHARS))
        }
        val recipe = response.recipe ?: throw InvalidModelResponseException()
        if (response.message.isBlank() || recipe.name.isBlank() || recipe.name.length > 120 || recipe.description.isBlank() ||
            recipe.ingredients.isEmpty() || recipe.ingredients.size > MAX_RECIPE_INGREDIENTS ||
            recipe.instructions.isEmpty() || recipe.instructions.size > MAX_RECIPE_STEPS ||
            recipe.preparationTimeMinutes !in 1..720 || recipe.servings != request.servings ||
            recipe.substitutions.size > MAX_RECIPE_INGREDIENTS
        ) throw InvalidModelResponseException()

        val availableNames = request.ingredients.map { normalizedIngredientName(it.name) }.toSet()
        val validIngredients = recipe.ingredients.all { ingredient ->
            ingredient.name.isNotBlank() && ingredient.name.length <= 120 && ingredient.quantity.isNotBlank() &&
                ingredient.quantity.length <= 40 && ingredient.unit.isNotBlank() && ingredient.unit.length <= 24 &&
                ingredient.source in setOf("available", "additional") &&
                (ingredient.source != "available" || normalizedIngredientName(ingredient.name) in availableNames)
        }
        if (!validIngredients || recipe.instructions.any { it.isBlank() || it.length > 600 }) throw InvalidModelResponseException()
        if (recipe.substitutions.any { it.ingredient.isBlank() || it.replacement.isBlank() || it.reason.isBlank() }) {
            throw InvalidModelResponseException()
        }

        val available = recipe.ingredients.filter { it.source == "available" }.map { it.name }.distinctBy(::normalizedIngredientName)
        val additional = recipe.ingredients.filter { it.source == "additional" }.map { it.name }.distinctBy(::normalizedIngredientName)
        val safeRecipe = recipe.copy(
            estimatedCostMxn = null,
            availableIngredients = available,
            additionalIngredients = additional
        )
        return response.copy(message = response.message.take(MAX_RECIPE_MESSAGE_CHARS), recipe = safeRecipe)
    }

    private fun OpenAiResponse.outputTextOrThrow(): String {
        if (output.any { item -> item.content.any { !it.refusal.isNullOrBlank() } }) throw OpenAiRefusalException()
        if (status != "completed") throw InvalidModelResponseException()
        val text = output.asSequence()
            .filter { it.type == "message" }
            .flatMap { it.content.asSequence() }
            .filter { it.type == "output_text" }
            .mapNotNull { it.text?.takeIf(String::isNotBlank) }
            .joinToString("\n")
        if (text.isBlank()) throw InvalidModelResponseException()
        return text
    }

    private fun buildRecipePrompt(request: RecipeGenerationRequest): String = buildString {
        appendLine("Genera una receta siguiendo estrictamente las preferencias proporcionadas.")
        appendLine("Datos locales confirmados (JSON):")
        appendLine(json.encodeToString(request))
        appendLine("No inventes ingredientes disponibles. Marca cada ingrediente de la receta como available solo si aparece en ingredients del JSON; los demás deben ser additional.")
        appendLine("Si los ingredientes no alcanzan, responde canGenerate=false, recipe=null y explica qué hace falta en message.")
        appendLine("Si budget es Cero gasto, usa solamente los ingredientes confirmados; si no es viable, explica por qué.")
        appendLine("No inventes precios. estimatedCostMxn debe ser null porque no recibiste precios verificables.")
        appendLine("Respeta allergies y restrictions. Nunca asegures que una preparación es libre de alérgenos ni que evita trazas.")
        appendLine("Usa servings exactamente como la cantidad solicitada. Devuelve el JSON del esquema sin texto adicional.")
    }

    private fun buildResponseRequest(
        instructions: String,
        input: kotlinx.serialization.json.JsonElement,
        maxOutputTokens: Int,
        structuredRecipe: Boolean
    ): JsonObject = buildJsonObject {
        put("model", JsonPrimitive(config.model))
        put("instructions", JsonPrimitive(instructions))
        put("input", input)
        put("max_output_tokens", JsonPrimitive(maxOutputTokens))
        put("store", JsonPrimitive(false))
        if (structuredRecipe) {
            put("text", buildJsonObject {
                put("format", buildJsonObject {
                    put("type", JsonPrimitive("json_schema"))
                    put("name", JsonPrimitive("welliai_recipe"))
                    put("strict", JsonPrimitive(true))
                    put("schema", recipeJsonSchema)
                })
            })
        }
    }

    private companion object {
        const val RECIPE_MAX_OUTPUT_TOKENS = 1_500
        const val CHAT_MAX_OUTPUT_TOKENS = 600
        const val MAX_CHAT_RESPONSE_CHARS = 6_000
        const val MAX_RECIPE_MESSAGE_CHARS = 500
        const val MAX_RECIPE_INGREDIENTS = 40
        const val MAX_RECIPE_STEPS = 30
        const val OPENAI_REQUEST_TIMEOUT_MILLIS = 45_000L

        const val RECIPE_INSTRUCTIONS = """
            Eres WelliAI, un asistente de cocina amable, práctico, organizado y creativo. Responde en español sencillo.
            Aprovecha los ingredientes disponibles, reduce desperdicios y adapta cada receta al tipo de alimentación, presupuesto, restricciones y alergias indicados.
            Distingue con precisión ingredientes disponibles y adicionales. No inventes lo que la persona tiene ni precios.
            Trata las alergias con especial cuidado, pero nunca diagnostiques ni afirmes que un platillo está libre de alérgenos o trazas.
            Si no hay suficientes ingredientes, devuelve canGenerate=false, recipe=null y explica qué hace falta.
            Devuelve únicamente el objeto que cumple el esquema solicitado.
        """

        const val CHAT_INSTRUCTIONS = """
            Eres WelliAI, un asistente inteligente de cocina amable, práctico, organizado y creativo. Responde en español natural y sencillo.
            Ayuda a preparar alimentos, aprovechar ingredientes, reducir desperdicios, cocinar con poco presupuesto, sustituir ingredientes y aprender técnicas.
            Explica procedimientos en pasos claros. No inventes qué ingredientes tiene la persona si no lo dijo.
            Si se mencionan alergias, recomienda verificar etiquetas y contaminación cruzada; nunca afirmes que un alimento es totalmente libre de alérgenos.
            No des diagnósticos médicos ni inventes precios exactos.
        """

        val recipeJsonSchema = Json.parseToJsonElement(
            """
            {
              "type":"object",
              "properties":{
                "canGenerate":{"type":"boolean"},
                "message":{"type":"string"},
                "recipe":{"type":["object","null"],"properties":{
                  "name":{"type":"string"},
                  "description":{"type":"string"},
                  "ingredients":{"type":"array","items":{"type":"object","properties":{
                    "name":{"type":"string"},
                    "quantity":{"type":"string"},
                    "unit":{"type":"string"},
                    "source":{"type":"string","enum":["available","additional"]}
                  },"required":["name","quantity","unit","source"],"additionalProperties":false}},
                  "instructions":{"type":"array","items":{"type":"string"}},
                  "preparationTimeMinutes":{"type":"integer"},
                  "servings":{"type":"integer"},
                  "estimatedCostMxn":{"type":["number","null"]},
                  "availableIngredients":{"type":"array","items":{"type":"string"}},
                  "additionalIngredients":{"type":"array","items":{"type":"string"}},
                  "substitutions":{"type":"array","items":{"type":"object","properties":{
                    "ingredient":{"type":"string"},
                    "replacement":{"type":"string"},
                    "reason":{"type":"string"}
                  },"required":["ingredient","replacement","reason"],"additionalProperties":false}}
                },"required":["name","description","ingredients","instructions","preparationTimeMinutes","servings","estimatedCostMxn","availableIngredients","additionalIngredients","substitutions"],"additionalProperties":false}
              },
              "required":["canGenerate","message","recipe"],
              "additionalProperties":false
            }
            """.trimIndent()
        ).jsonObject
    }
}
