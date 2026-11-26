package com.npcg.welliai.backend.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IngredientInput(
    val name: String,
    val quantity: Double,
    val unit: String,
    val category: String
)

@Serializable
data class RecipeGenerationRequest(
    val ingredients: List<IngredientInput>,
    val budget: String,
    val foodType: String,
    val restrictions: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
    val servings: Int
)

@Serializable
data class RecipeIngredient(
    val name: String,
    val quantity: String,
    val unit: String,
    val source: String
)

@Serializable
data class RecipeSubstitution(
    val ingredient: String,
    val replacement: String,
    val reason: String
)

@Serializable
data class RecipeCard(
    val name: String,
    val description: String,
    val ingredients: List<RecipeIngredient>,
    val instructions: List<String>,
    val preparationTimeMinutes: Int,
    val servings: Int,
    val estimatedCostMxn: Double?,
    val availableIngredients: List<String>,
    val additionalIngredients: List<String>,
    val substitutions: List<RecipeSubstitution>
)

@Serializable
data class RecipeGenerationResponse(
    val canGenerate: Boolean,
    val message: String,
    val recipe: RecipeCard? = null
)

@Serializable
data class ChatMessageInput(
    val role: String,
    val content: String
)

@Serializable
data class ChatRequest(val messages: List<ChatMessageInput>)

@Serializable
data class ChatResponse(val message: String)

@Serializable
data class ApiErrorBody(val error: ApiError)

@Serializable
data class ApiError(val code: String, val message: String)

@Serializable
data class OpenAiResponse(
    val status: String? = null,
    val output: List<OpenAiOutputItem> = emptyList(),
    @SerialName("incomplete_details") val incompleteDetails: OpenAiIncompleteDetails? = null
)

@Serializable
data class OpenAiIncompleteDetails(val reason: String? = null)

@Serializable
data class OpenAiOutputItem(
    val type: String? = null,
    val content: List<OpenAiOutputContent> = emptyList()
)

@Serializable
data class OpenAiOutputContent(
    val type: String? = null,
    val text: String? = null,
    val refusal: String? = null
)
