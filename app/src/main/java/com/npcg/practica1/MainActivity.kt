package com.npcg.practica1

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.npcg.practica1.ui.screens.auth.AuthUiState
import com.npcg.practica1.ui.screens.auth.AuthError
import com.npcg.practica1.ui.screens.auth.AuthViewModel
import com.npcg.practica1.ui.screens.auth.LoginScreen
import com.npcg.practica1.ui.screens.auth.RegisterScreen
import com.npcg.practica1.ui.screens.home.HomeScreen
import com.npcg.practica1.ui.theme.WelliTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val authViewModel: AuthViewModel = hiltViewModel()
            val authState by authViewModel.state.collectAsState()
            var showRegistration by remember { mutableStateOf(false) }

            BackHandler(
                enabled = showRegistration && authState == AuthUiState.SignedOut
            ) {
                showRegistration = false
            }

            WelliTheme {
                when (val state = authState) {
                    AuthUiState.Loading -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    AuthUiState.SignedOut -> {
                        if (showRegistration) {
                            RegisterScreen(
                                onRegistrationSuccess = {
                                    showRegistration = false
                                    Toast.makeText(
                                        this,
                                        R.string.registration_success,
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                onBackToLogin = { showRegistration = false }
                            )
                        } else {
                            LoginScreen(
                                onLoginSuccess = authViewModel::restoreSession,
                                onCreateAccountClick = { showRegistration = true }
                            )
                        }
                    }
                    is AuthUiState.SignedIn -> {
                        HomeScreen(
                            userName = state.user.nombre,
                            onCreateRecipeClick = {
                                Toast.makeText(
                                    this,
                                    "Generador IA de recetas activado",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onInventoryClick = {
                                Toast.makeText(
                                    this,
                                    "Accediendo a inventario completo",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onFavoritesClick = {
                                Toast.makeText(
                                    this,
                                    "Accediendo a recetas favoritas",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onProfileClick = {
                                Toast.makeText(
                                    this,
                                    "Accediendo a perfil de usuario",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onLogout = authViewModel::logout
                        )
                    }
                    is AuthUiState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(
                                    if (state.reason == AuthError.LOGOUT) {
                                        R.string.error_logout
                                    } else {
                                        R.string.error_session
                                    }
                                ),
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(
                                onClick = if (state.reason == AuthError.LOGOUT) {
                                    authViewModel::logout
                                } else {
                                    authViewModel::restoreSession
                                }
                            ) {
                                Text(stringResource(R.string.btn_retry))
                            }
                        }
                    }
                }
            }
        }
    }
}
