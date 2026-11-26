package com.npcg.practica1.data.remote

import com.google.gson.Gson
import com.npcg.practica1.domain.model.AiBackendException
import com.npcg.practica1.domain.model.AiRecipeCard
import com.npcg.practica1.domain.model.AiRecipeIngredient
import com.npcg.practica1.domain.model.AiRecipeResult
import com.npcg.practica1.domain.model.AiRecipeSubstitution
import com.npcg.practica1.domain.model.ChatRole
import com.npcg.practica1.domain.model.ContextoGeneracionReceta
import com.npcg.practica1.domain.model.WelliChatMessage
import com.npcg.practica1.domain.repository.WelliAiRepository
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class WelliAiRepositoryImpl @Inject constructor(
    private val api: WelliAiApiService,
    private val gson: Gson
) : WelliAiRepository {
    override suspend fun generateRecipe(context: ContextoGeneracionReceta): Result<AiRecipeResult> = capture {
        require(context.ingredientesDisponibles.isNotEmpty()) { "Tu inventario no tiene ingredientes seleccionados." }
        val dto = RecipeGenerationRequestDto(
            ingredients = context.ingredientesDisponibles.map { item ->
                IngredientInputDto(item.nombre, item.cantidad, item.unidad, item.categoria)
            },
            budget = context.preferencias.presupuesto,
            foodType = context.preferencias.tipoAlimentacion,
            restrictions = context.preferencias.restricciones,
            allergies = context.preferencias.alergias,
            servings = context.preferencias.porcionesHabituales
        )
        val response = api.generateRecipe(dto)
        validateRecipeResponse(response)
        response.toDomain()
    }

    override suspend fun sendMessage(history: List<WelliChatMessage>): Result<String> = capture {
        val request = ChatRequestDto(history.map { ChatMessageDto(it.role.apiValue, it.text.trim()) })
        val response = api.sendChat(request)
        response.message.trim().takeIf(String::isNotBlank)
            ?: throw AiBackendException("WelliAI devolvió una respuesta vacía.")
    }

    private fun validateRecipeResponse(response: RecipeGenerationResponseDto) {
        if (!response.canGenerate) {
            require(response.recipe == null && response.message.isNotBlank()) {
                "No se pudo validar la respuesta del generador."
            }
            return
        }
        val recipe = response.recipe ?: throw AiBackendException("La respuesta de la receta está incompleta.")
        if (response.message.isBlank() || recipe.name.isBlank() || recipe.description.isBlank() ||
            recipe.ingredients.isEmpty() || recipe.instructions.isEmpty() ||
            recipe.preparationTimeMinutes !in 1..720 || recipe.servings !in 1..20 ||
            recipe.ingredients.any { it.name.isBlank() || it.quantity.isBlank() || it.unit.isBlank() || it.source !in setOf("available", "additional") } ||
            recipe.instructions.any(String::isBlank)
        ) throw AiBackendException("La respuesta de la receta está incompleta.")
    }

    private fun RecipeGenerationResponseDto.toDomain() = AiRecipeResult(
        canGenerate = canGenerate,
        message = message,
        recipe = recipe?.let { card ->
            AiRecipeCard(
                name = card.name,
                description = card.description,
                ingredients = card.ingredients.map { AiRecipeIngredient(it.name, it.quantity, it.unit, it.source == "available") },
                instructions = card.instructions,
                preparationTimeMinutes = card.preparationTimeMinutes,
                servings = card.servings,
                estimatedCostMxn = card.estimatedCostMxn?.takeIf { it.isFinite() && it >= 0.0 },
                availableIngredients = card.availableIngredients,
                additionalIngredients = card.additionalIngredients,
                substitutions = card.substitutions.map { AiRecipeSubstitution(it.ingredient, it.replacement, it.reason) }
            )
        }
    )

    private suspend fun <T> capture(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: HttpException) {
        Result.failure(error.toBackendException())
    } catch (error: IOException) {
        Result.failure(AiBackendException("No se pudo conectar con WelliAI. Comprueba que el backend esté ejecutándose."))
    } catch (error: Exception) {
        Result.failure(error)
    }

    private fun HttpException.toBackendException(): AiBackendException {
        val parsed = try {
            response()?.errorBody()?.string()?.let { gson.fromJson(it, BackendErrorEnvelopeDto::class.java)?.error }
        } catch (_: Exception) {
            null
        }
        val fallback = when (code()) {
            400, 413 -> "Revisa los ingredientes y preferencias e inténtalo otra vez."
            429 -> "Has enviado varias solicitudes. Espera un momento."
            502, 503, 504 -> "El servicio de IA no está disponible ahora."
            else -> "No se pudo completar la solicitud. Inténtalo otra vez."
        }
        return AiBackendException(parsed?.message?.takeIf(String::isNotBlank) ?: fallback)
    }
}
