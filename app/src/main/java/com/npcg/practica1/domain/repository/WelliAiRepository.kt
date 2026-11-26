package com.npcg.practica1.domain.repository

import com.npcg.practica1.domain.model.AiRecipeResult
import com.npcg.practica1.domain.model.ContextoGeneracionReceta
import com.npcg.practica1.domain.model.WelliChatMessage

interface WelliAiRepository {
    suspend fun generateRecipe(context: ContextoGeneracionReceta): Result<AiRecipeResult>
    suspend fun sendMessage(history: List<WelliChatMessage>): Result<String>
}
