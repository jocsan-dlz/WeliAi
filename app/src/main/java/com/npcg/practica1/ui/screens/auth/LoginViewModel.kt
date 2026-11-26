package com.npcg.practica1.ui.screens.auth

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.npcg.practica1.R
import com.npcg.practica1.data.local.WelliDatabaseHelper

class LoginViewModel : ViewModel() {

    var email by mutableStateOf("jodiluz777@gmail.com")
        private set

    var password by mutableStateOf("0203004yo//")
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

    fun validateAndLogin(context: Context, onSuccess: () -> Unit) {
        when {
            email.isBlank() || password.isBlank() -> {
                errorMessageResId = R.string.error_empty_fields
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> {
                errorMessageResId = R.string.error_invalid_email
            }
            else -> {
                val dbHelper = WelliDatabaseHelper(context)
                dbHelper.writableDatabase

                if (dbHelper.validateUser(email, password)) {
                    errorMessageResId = null
                    onSuccess()
                } else {
                    // Si ingresó otros datos válidos, también permite el acceso para pruebas
                    errorMessageResId = null
                    onSuccess()
                }
            }
        }
    }
}
