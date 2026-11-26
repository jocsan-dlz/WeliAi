package com.npcg.welliai.backend

import com.npcg.welliai.backend.api.ChatMessageInput
import com.npcg.welliai.backend.api.ChatRequest
import com.npcg.welliai.backend.api.IngredientInput
import com.npcg.welliai.backend.api.RecipeGenerationRequest
import com.npcg.welliai.backend.security.InvalidRequestException
import com.npcg.welliai.backend.security.validateChatRequest
import com.npcg.welliai.backend.security.validateRecipeRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RequestValidationTest {
    @Test
    fun acceptsRecipeContextAndRejectsInvalidIngredientAndPortions() {
        validateRecipeRequest(recipeRequest())
        assertFailsWith<InvalidRequestException> {
            validateRecipeRequest(recipeRequest().copy(ingredients = listOf(IngredientInput("Tomate", 0.0, "pieza", "Verduras"))))
        }
        assertFailsWith<InvalidRequestException> { validateRecipeRequest(recipeRequest().copy(servings = 0)) }
        assertFailsWith<InvalidRequestException> { validateRecipeRequest(recipeRequest().copy(allergies = listOf(" "))) }
    }

    @Test
    fun validatesChatLengthRoleOrderAndEmptyMessages() {
        validateChatRequest(ChatRequest(listOf(ChatMessageInput("user", "¿Cómo hiervo arroz?"))))
        assertEquals("invalid_message", assertFailsWith<InvalidRequestException> {
            validateChatRequest(ChatRequest(listOf(ChatMessageInput("user", " "))))
        }.code)
        assertEquals("history_too_long", assertFailsWith<InvalidRequestException> {
            validateChatRequest(ChatRequest(listOf(ChatMessageInput("user", "x".repeat(8_001)))))
        }.code)
        assertEquals("invalid_last_message", assertFailsWith<InvalidRequestException> {
            validateChatRequest(ChatRequest(listOf(ChatMessageInput("assistant", "Hola"))))
        }.code)
    }

    private fun recipeRequest() = RecipeGenerationRequest(
        ingredients = listOf(IngredientInput("Tomate", 2.0, "pieza", "Verduras")),
        budget = "Cero gasto",
        foodType = "Tradicional",
        restrictions = listOf("Sin gluten"),
        allergies = listOf("Nuez"),
        servings = 2
    )
}
