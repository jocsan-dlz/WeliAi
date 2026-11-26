package com.npcg.practica1.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.npcg.practica1.domain.model.Usuario
import com.npcg.practica1.domain.repository.SessionStore
import com.npcg.practica1.domain.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthUiState {
    data object Loading : AuthUiState
    data object SignedOut : AuthUiState
    data class SignedIn(val user: Usuario) : AuthUiState
    data class Error(val reason: AuthError) : AuthUiState
}

enum class AuthError {
    SESSION,
    LOGOUT
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val usuarioRepository: UsuarioRepository,
    private val sessionStore: SessionStore
) : ViewModel() {
    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()
    private var operationInProgress = false

    init {
        restoreSession()
    }

    fun restoreSession() {
        if (operationInProgress) return
        operationInProgress = true
        _state.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val userId = sessionStore.getUserId()
                if (userId == null) {
                    _state.value = AuthUiState.SignedOut
                    return@launch
                }
                val result = usuarioRepository.obtenerUsuarioPorId(userId)
                val user = result.getOrElse { throw it }
                if (user == null || !user.estado) {
                    sessionStore.clear()
                    _state.value = AuthUiState.SignedOut
                } else {
                    _state.value = AuthUiState.SignedIn(user)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.value = AuthUiState.Error(AuthError.SESSION)
            } finally {
                operationInProgress = false
            }
        }
    }

    fun logout() {
        if (operationInProgress) return
        operationInProgress = true
        _state.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                sessionStore.clear()
                _state.value = AuthUiState.SignedOut
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.value = AuthUiState.Error(AuthError.LOGOUT)
            } finally {
                operationInProgress = false
            }
        }
    }
}
