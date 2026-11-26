package com.npcg.practica1.domain.model

enum class ChatRole(val apiValue: String) {
    USER("user"), ASSISTANT("assistant")
}

data class WelliChatMessage(
    val id: Long,
    val role: ChatRole,
    val text: String
)

data class AiRecipeIngredient(
    val name: String,
    val quantity: String,
    val unit: String,
    val availableInInventory: Boolean
)

data class AiRecipeSubstitution(
    val ingredient: String,
    val replacement: String,
    val reason: String
)

data class AiRecipeCard(
    val name: String,
    val description: String,
    val ingredients: List<AiRecipeIngredient>,
    val instructions: List<String>,
    val preparationTimeMinutes: Int,
    val servings: Int,
    val estimatedCostMxn: Double?,
    val availableIngredients: List<String>,
    val additionalIngredients: List<String>,
    val substitutions: List<AiRecipeSubstitution>
)

data class AiRecipeResult(
    val canGenerate: Boolean,
    val message: String,
    val recipe: AiRecipeCard?
)

class AiInputException(message: String) : IllegalArgumentException(message)
class AiBackendException(message: String) : IllegalStateException(message)
