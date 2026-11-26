package com.npcg.practica1.ui.screens.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.npcg.practica1.R
import com.npcg.practica1.domain.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val usuarioRepository: UsuarioRepository
) : ViewModel() {
    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var isPasswordVisible by mutableStateOf(false)
        private set
    var errorMessageResId by mutableStateOf<Int?>(null)
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
        when {
            email.isBlank() || password.isBlank() -> errorMessageResId = R.string.error_empty_fields
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                errorMessageResId = R.string.error_invalid_email
            else -> viewModelScope.launch {
                val result = try {
                    usuarioRepository.iniciarSesion(email.trim(), password)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Result.failure(e)
                }
                result.fold(
                    onSuccess = {
                        password = ""
                        errorMessageResId = null
                        onSuccess()
                    },
                    onFailure = { errorMessageResId = R.string.error_auth_failed }
                )
            }
        }
    }
}
