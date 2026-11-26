package com.npcg.practica1.ui.screens.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.npcg.practica1.R
import com.npcg.practica1.domain.model.EmailAlreadyRegisteredException
import com.npcg.practica1.domain.model.PasswordPolicy
import com.npcg.practica1.domain.model.Usuario
import com.npcg.practica1.domain.model.WeakPasswordException
import com.npcg.practica1.domain.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val usuarioRepository: UsuarioRepository
) : ViewModel() {
    var name by mutableStateOf("")
        private set
    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var confirmPassword by mutableStateOf("")
        private set
    var isPasswordVisible by mutableStateOf(false)
        private set
    var errorMessageResId by mutableStateOf<Int?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set

    fun onNameChange(value: String) {
        name = value
        errorMessageResId = null
    }

    fun onEmailChange(value: String) {
        email = value
        errorMessageResId = null
    }

    fun onPasswordChange(value: String) {
        password = value
        errorMessageResId = null
    }

    fun onConfirmPasswordChange(value: String) {
        confirmPassword = value
        errorMessageResId = null
    }

    fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
    }

    fun register(onSuccess: () -> Unit) {
        if (isLoading) return
        when {
            name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank() ->
                errorMessageResId = R.string.error_empty_fields
            !EMAIL_PATTERN.matches(email.trim()) ->
                errorMessageResId = R.string.error_invalid_email
            !PasswordPolicy.isStrong(password) ->
                errorMessageResId = R.string.error_weak_password
            password != confirmPassword ->
                errorMessageResId = R.string.error_password_mismatch
            else -> {
                isLoading = true
                viewModelScope.launch {
                    try {
                        val user = Usuario(
                            nombre = name.trim(),
                            correo = email.trim(),
                            fechaRegistro = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
                        )
                        val result = withContext(Dispatchers.IO) {
                            usuarioRepository.registrarUsuario(user, password)
                        }
                        result.getOrElse { throw it }
                        clearSensitiveFields()
                        errorMessageResId = null
                        onSuccess()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: EmailAlreadyRegisteredException) {
                        errorMessageResId = R.string.error_email_registered
                    } catch (e: WeakPasswordException) {
                        errorMessageResId = R.string.error_weak_password
                    } catch (e: IllegalArgumentException) {
                        errorMessageResId = R.string.error_invalid_registration
                    } catch (e: Exception) {
                        errorMessageResId = R.string.error_database
                    } finally {
                        isLoading = false
                    }
                }
            }
        }
    }

    private fun clearSensitiveFields() {
        password = ""
        confirmPassword = ""
    }

    private companion object {
        val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
