package com.npcg.practica1.ui.screens.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.npcg.practica1.domain.model.ChatRole
import com.npcg.practica1.domain.model.WelliChatMessage
import com.npcg.practica1.domain.usecase.SendWelliChatMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WelliChatUiState(
    val messages: List<WelliChatMessage> = emptyList(),
    val draft: String = "",
    val sending: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class WelliChatViewModel @Inject constructor(
    private val sendMessage: SendWelliChatMessageUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(WelliChatUiState())
    val state: StateFlow<WelliChatUiState> = _state.asStateFlow()
    private var nextId = 1L

    fun onDraftChange(value: String) {
        if (!_state.value.sending) _state.value = _state.value.copy(draft = value.take(MAX_MESSAGE_LENGTH + 1), error = null)
    }

    fun send() {
        val current = _state.value
        val userText = current.draft.trim()
        if (current.sending || userText.isBlank()) return
        if (userText.length > MAX_MESSAGE_LENGTH) {
            _state.value = current.copy(error = "El mensaje debe tener 2,000 caracteres o menos.")
            return
        }

        val userMessage = WelliChatMessage(nextId++, ChatRole.USER, userText)
        val recentContext = current.messages.takeLast(MAX_CONTEXT_MESSAGES).toMutableList()
        while (recentContext.isNotEmpty() && recentContext.sumOf { it.text.length } + userText.length > MAX_CONTEXT_CHARS) {
            repeat(minOf(2, recentContext.size)) { recentContext.removeAt(0) }
        }
        val requestHistory = recentContext + userMessage
        _state.value = current.copy(messages = current.messages + userMessage, draft = "", sending = true, error = null)
        viewModelScope.launch {
            try {
                sendMessage(requestHistory).fold(
                    onSuccess = { answer ->
                        val assistantMessage = WelliChatMessage(nextId++, ChatRole.ASSISTANT, answer)
                        _state.value = _state.value.copy(
                            messages = (_state.value.messages + assistantMessage).takeLast(MAX_VISIBLE_MESSAGES),
                            sending = false
                        )
                    },
                    onFailure = { error -> restoreFailedMessage(userMessage, error.message) }
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                restoreFailedMessage(userMessage, error.message)
            }
        }
    }

    fun startNewConversation() {
        if (!_state.value.sending) _state.value = WelliChatUiState()
    }

    private fun restoreFailedMessage(message: WelliChatMessage, error: String?) {
        _state.value = _state.value.copy(
            messages = _state.value.messages.filterNot { it.id == message.id },
            draft = message.text,
            sending = false,
            error = error ?: "No se pudo enviar el mensaje. Inténtalo otra vez."
        )
    }

    private companion object {
        const val MAX_MESSAGE_LENGTH = 2_000
        const val MAX_CONTEXT_MESSAGES = 10
        const val MAX_VISIBLE_MESSAGES = 12
        const val MAX_CONTEXT_CHARS = 8_000
    }
}
