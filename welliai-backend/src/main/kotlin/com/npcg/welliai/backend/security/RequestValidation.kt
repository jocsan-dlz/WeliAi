package com.npcg.welliai.backend.security

import com.npcg.welliai.backend.api.ChatRequest
import com.npcg.welliai.backend.api.RecipeGenerationRequest
import java.text.Normalizer
import java.util.Locale

const val MAX_REQUEST_BODY_BYTES = 16 * 1024
const val MAX_CHAT_MESSAGES = 12
const val MAX_CHAT_MESSAGE_CHARS = 2_000
const val MAX_CHAT_TOTAL_CHARS = 8_000
const val MAX_RECIPE_INGREDIENTS = 50

class InvalidRequestException(val code: String, message: String) : IllegalArgumentException(message)

fun validateRecipeRequest(request: RecipeGenerationRequest) {
    if (request.ingredients.isEmpty()) throw InvalidRequestException("empty_inventory", "Agrega o selecciona ingredientes antes de generar una receta.")
    if (request.ingredients.size > MAX_RECIPE_INGREDIENTS) throw InvalidRequestException("too_many_ingredients", "Selecciona como máximo $MAX_RECIPE_INGREDIENTS ingredientes.")
    if (request.ingredients.any { it.name.isBlank() || it.name.length > 80 || it.unit.isBlank() || it.unit.length > 24 || it.category.length > 48 || !it.quantity.isFinite() || it.quantity <= 0.0 }) {
        throw InvalidRequestException("invalid_ingredient", "Revisa el nombre, cantidad, unidad y categoría de los ingredientes.")
    }
    if (request.budget !in setOf("Cero gasto", "Ahorro", "Gourmet")) throw InvalidRequestException("invalid_budget", "Selecciona un presupuesto válido.")
    if (request.foodType !in setOf("Tradicional", "Saludable", "Vegetariana", "Vegana", "Otra")) throw InvalidRequestException("invalid_food_type", "Selecciona un tipo de alimentación válido.")
    if (request.servings !in 1..20) throw InvalidRequestException("invalid_servings", "Las porciones deben estar entre 1 y 20.")
    if ((request.restrictions + request.allergies).any { it.isBlank() || it.length > 60 } || request.restrictions.size > 20 || request.allergies.size > 20) {
        throw InvalidRequestException("invalid_preferences", "Revisa tus restricciones y alergias.")
    }
}

fun validateChatRequest(request: ChatRequest) {
    if (request.messages.isEmpty() || request.messages.size > MAX_CHAT_MESSAGES) {
        throw InvalidRequestException("invalid_history", "Envía entre 1 y $MAX_CHAT_MESSAGES mensajes recientes.")
    }
    val total = request.messages.sumOf { it.content.length }
    if (total > MAX_CHAT_TOTAL_CHARS) throw InvalidRequestException("history_too_long", "El historial es demasiado largo. Inicia una conversación nueva.")
    if (request.messages.any { it.content.isBlank() || it.content.length > MAX_CHAT_MESSAGE_CHARS || it.role !in setOf("user", "assistant") }) {
        throw InvalidRequestException("invalid_message", "Los mensajes deben tener texto y un rol válido.")
    }
    if (request.messages.last().role != "user") throw InvalidRequestException("invalid_last_message", "El último mensaje debe ser una pregunta tuya.")
    if (request.messages.zipWithNext().any { (first, second) -> first.role == second.role }) {
        throw InvalidRequestException("invalid_history_order", "El historial debe alternar entre usuario y WelliAI.")
    }
}

fun normalizedIngredientName(value: String): String = Normalizer.normalize(
    value.trim().replace(Regex("\\s+"), " "),
    Normalizer.Form.NFD
).replace(Regex("\\p{Mn}+"), "").lowercase(Locale.ROOT)
