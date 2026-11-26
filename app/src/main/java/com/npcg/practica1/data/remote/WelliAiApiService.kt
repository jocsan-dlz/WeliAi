package com.npcg.practica1.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

interface WelliAiApiService {
    @POST("api/v1/ai/recipes/generate")
    suspend fun generateRecipe(@Body request: RecipeGenerationRequestDto): RecipeGenerationResponseDto

    @POST("api/v1/ai/chat")
    suspend fun sendChat(@Body request: ChatRequestDto): ChatResponseDto
}

data class IngredientInputDto(
    val name: String,
    val quantity: Double,
    val unit: String,
    val category: String
)

data class RecipeGenerationRequestDto(
    val ingredients: List<IngredientInputDto>,
    val budget: String,
    val foodType: String,
    val restrictions: List<String>,
    val allergies: List<String>,
    val servings: Int
)

data class RecipeIngredientDto(
    val name: String,
    val quantity: String,
    val unit: String,
    val source: String
)

data class RecipeSubstitutionDto(
    val ingredient: String,
    val replacement: String,
    val reason: String
)

data class AiRecipeCardDto(
    val name: String,
    val description: String,
    val ingredients: List<RecipeIngredientDto>,
    val instructions: List<String>,
    val preparationTimeMinutes: Int,
    val servings: Int,
    val estimatedCostMxn: Double?,
    val availableIngredients: List<String>,
    val additionalIngredients: List<String>,
    val substitutions: List<RecipeSubstitutionDto>
)

data class RecipeGenerationResponseDto(
    val canGenerate: Boolean,
    val message: String,
    val recipe: AiRecipeCardDto?
)

data class ChatMessageDto(val role: String, val content: String)
data class ChatRequestDto(val messages: List<ChatMessageDto>)
data class ChatResponseDto(val message: String)
data class BackendErrorEnvelopeDto(val error: BackendErrorDto?)
data class BackendErrorDto(val code: String?, val message: String?)
