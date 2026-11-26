package com.npcg.practica1.ui.screens.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.npcg.practica1.R
import com.npcg.practica1.domain.model.AccountDisabledException
import com.npcg.practica1.domain.model.InvalidCredentialsException
import com.npcg.practica1.domain.repository.SessionStore
import com.npcg.practica1.domain.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val usuarioRepository: UsuarioRepository,
    private val sessionStore: SessionStore
) : ViewModel() {
    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var isPasswordVisible by mutableStateOf(false)
        private set
    var errorMessageResId by mutableStateOf<Int?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set

    fun onEmailChange(newEmail: String) {
        email = newEmail
        errorMessageResId = null
    }

    fun onPasswordChange(newPassword: String) {
        password = newPassword
        errorMessageResId = null
    }

    fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
    }

    fun validateAndLogin(onSuccess: () -> Unit) {
        if (isLoading) return
        when {
            email.isBlank() || password.isBlank() -> errorMessageResId = R.string.error_empty_fields
            !EMAIL_PATTERN.matches(email.trim()) ->
                errorMessageResId = R.string.error_invalid_email
            else -> {
                isLoading = true
                viewModelScope.launch {
                    try {
                        val result = withContext(Dispatchers.IO) {
                            usuarioRepository.iniciarSesion(email.trim(), password)
                        }
                        val user = result.getOrElse { throw it }
                        withContext(Dispatchers.IO) {
                            sessionStore.saveUserId(user.idUsuario)
                        }
                        password = ""
                        errorMessageResId = null
                        onSuccess()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: InvalidCredentialsException) {
                        errorMessageResId = R.string.error_invalid_credentials
                    } catch (e: AccountDisabledException) {
                        errorMessageResId = R.string.error_account_disabled
                    } catch (e: Exception) {
                        errorMessageResId = R.string.error_database
                    } finally {
                        isLoading = false
                    }
                }
            }
        }
    }

    private companion object {
        val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
