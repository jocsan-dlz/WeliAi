package com.npcg.practica1.domain.usecase

import com.npcg.practica1.domain.model.AiInputException
import com.npcg.practica1.domain.model.ChatRole
import com.npcg.practica1.domain.model.WelliChatMessage
import com.npcg.practica1.domain.repository.WelliAiRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class SendWelliChatMessageUseCase @Inject constructor(
    private val repository: WelliAiRepository
) {
    suspend operator fun invoke(history: List<WelliChatMessage>): Result<String> = try {
        if (history.isEmpty() || history.last().role != ChatRole.USER) throw AiInputException("Escribe una pregunta antes de enviarla.")
        if (history.size > MAX_MESSAGES || history.any { it.text.isBlank() || it.text.length > MAX_MESSAGE_LENGTH } ||
            history.sumOf { it.text.length } > MAX_TOTAL_CHARS
        ) {
            throw AiInputException("La conversación es demasiado larga. Inicia una nueva conversación.")
        }
        if (history.zipWithNext().any { (first, second) -> first.role == second.role }) {
            throw AiInputException("No se pudo validar el historial de la conversación.")
        }
        repository.sendMessage(history.takeLast(MAX_MESSAGES))
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }

    private companion object {
        const val MAX_MESSAGES = 12
        const val MAX_MESSAGE_LENGTH = 2_000
        const val MAX_TOTAL_CHARS = 8_000
    }
}
