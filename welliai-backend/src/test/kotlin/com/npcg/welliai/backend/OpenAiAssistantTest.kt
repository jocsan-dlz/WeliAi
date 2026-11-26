package com.npcg.welliai.backend

import com.npcg.welliai.backend.ai.OpenAiModelUnavailableException
import com.npcg.welliai.backend.ai.OpenAiTransport
import com.npcg.welliai.backend.ai.OpenAiWelliAssistant
import com.npcg.welliai.backend.api.ChatMessageInput
import com.npcg.welliai.backend.api.ChatRequest
import com.npcg.welliai.backend.api.OpenAiOutputContent
import com.npcg.welliai.backend.api.OpenAiOutputItem
import com.npcg.welliai.backend.api.OpenAiResponse
import com.npcg.welliai.backend.api.RecipeGenerationRequest
import com.npcg.welliai.backend.config.BackendConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OpenAiAssistantTest {
    @Test
    fun buildsStructuredRecipeRequestWithInventoryBudgetAndAllergies() = kotlinx.coroutines.test.runTest {
        val transport = FakeOpenAiTransport(response(recipeJson()))
        val assistant = assistant(transport)

        val result = assistant.generateRecipe(recipeRequest())

        assertTrue(result.canGenerate)
        assertEquals("Tortilla de tomate", result.recipe?.name)
        assertEquals(listOf("Tomate"), result.recipe?.availableIngredients)
        assertEquals(listOf("Huevo"), result.recipe?.additionalIngredients)
        assertNull(result.recipe?.estimatedCostMxn)
        val request = transport.lastRequest.toString()
        assertTrue(request.contains("json_schema"))
        assertTrue(request.contains("test-model"))
        assertTrue(request.contains("Nuez"))
        assertTrue(request.contains("Sin gluten"))
        assertTrue(request.contains("Cero gasto"))
        assertTrue(request.contains("store"))
        assertTrue(!request.contains("test-only-key"))
    }

    @Test
    fun rejectsMalformedOrEmptyModelOutput() = kotlinx.coroutines.test.runTest {
        assertFailsWith<com.npcg.welliai.backend.ai.InvalidModelResponseException> {
            assistant(FakeOpenAiTransport(response("not-json"))).generateRecipe(recipeRequest())
        }
        assertFailsWith<com.npcg.welliai.backend.ai.InvalidModelResponseException> {
            assistant(FakeOpenAiTransport(OpenAiResponse(status = "completed", output = emptyList())))
                .generateRecipe(recipeRequest())
        }
    }

    @Test
    fun refusesToMarkUnknownPantryItemsAsAvailable() = kotlinx.coroutines.test.runTest {
        val invalidRecipe = recipeJson().replace("\"name\":\"Tomate\",\"quantity\":\"2\",\"unit\":\"pieza\",\"source\":\"available\"", "\"name\":\"Trufa\",\"quantity\":\"2\",\"unit\":\"pieza\",\"source\":\"available\"")
        assertFailsWith<com.npcg.welliai.backend.ai.InvalidModelResponseException> {
            assistant(FakeOpenAiTransport(response(invalidRecipe))).generateRecipe(recipeRequest())
        }
    }

    @Test
    fun sendsRecentChatHistoryAndReturnsOutputText() = kotlinx.coroutines.test.runTest {
        val transport = FakeOpenAiTransport(response("¿Quieres una receta con esos ingredientes?"))
        val result = assistant(transport).chat(ChatRequest(listOf(
            ChatMessageInput("user", "Tengo arroz."),
            ChatMessageInput("assistant", "¿Qué más tienes?"),
            ChatMessageInput("user", "Tomate.")
        )))

        assertEquals("¿Quieres una receta con esos ingredientes?", result.message)
        val body = transport.lastRequest.toString()
        assertTrue(body.contains("Tengo arroz."))
        assertTrue(body.contains("¿Qué más tienes?"))
        assertTrue(body.contains("Tomate."))
    }

    @Test
    fun mapsModelUnavailableAndTimeoutAndKeepsCancellation() = kotlinx.coroutines.test.runTest {
        assertFailsWith<OpenAiModelUnavailableException> {
            assistant(FakeOpenAiTransport(failure = OpenAiModelUnavailableException()))
                .chat(ChatRequest(listOf(ChatMessageInput("user", "Hola"))))
        }
        assertFailsWith<com.npcg.welliai.backend.ai.OpenAiTimeoutException> {
            assistant(FakeOpenAiTransport(block = { withTimeout(1) { delay(50) }; response("nunca") }))
                .chat(ChatRequest(listOf(ChatMessageInput("user", "Hola"))))
        }
        assertFailsWith<CancellationException> {
            assistant(FakeOpenAiTransport(failure = CancellationException("test cancellation")))
                .chat(ChatRequest(listOf(ChatMessageInput("user", "Hola"))))
        }
    }

    private fun assistant(transport: FakeOpenAiTransport) = OpenAiWelliAssistant(
        BackendConfig("test-only-key", "test-model", "127.0.0.1", 8080),
        transport
    )

    private fun recipeRequest() = RecipeGenerationRequest(
        ingredients = listOf(com.npcg.welliai.backend.api.IngredientInput("Tomate", 2.0, "pieza", "Verduras")),
        budget = "Cero gasto",
        foodType = "Tradicional",
        restrictions = listOf("Sin gluten"),
        allergies = listOf("Nuez"),
        servings = 2
    )

    private fun recipeJson() = """
        {"canGenerate":true,"message":"Lista para preparar.","recipe":{
          "name":"Tortilla de tomate","description":"Una receta sencilla.",
          "ingredients":[
            {"name":"Tomate","quantity":"2","unit":"pieza","source":"available"},
            {"name":"Huevo","quantity":"2","unit":"pieza","source":"additional"}
          ],"instructions":["Pica el tomate.","Cocina con el huevo."],
          "preparationTimeMinutes":15,"servings":2,"estimatedCostMxn":25.0,
          "availableIngredients":["Tomate"],"additionalIngredients":["Huevo"],"substitutions":[]
        }}
    """.trimIndent()

    private fun response(text: String) = OpenAiResponse(
        status = "completed",
        output = listOf(OpenAiOutputItem(type = "message", content = listOf(OpenAiOutputContent(type = "output_text", text = text))))
    )
}

private class FakeOpenAiTransport(
    private val cannedResponse: OpenAiResponse? = null,
    private val failure: Throwable? = null,
    private val block: (suspend () -> OpenAiResponse)? = null
) : OpenAiTransport {
    var lastRequest: JsonObject = JsonObject(emptyMap())

    override suspend fun createResponse(requestBody: JsonObject): OpenAiResponse {
        lastRequest = requestBody
        failure?.let { throw it }
        block?.let { return it() }
        return cannedResponse ?: error("A test response is required")
    }
}
