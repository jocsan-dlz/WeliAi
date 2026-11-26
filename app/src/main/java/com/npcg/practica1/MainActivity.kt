package com.npcg.practica1

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.*
import com.npcg.practica1.ui.screens.auth.LoginScreen
import com.npcg.practica1.ui.screens.home.HomeScreen
import com.npcg.practica1.ui.theme.WelliTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentScreen by remember { mutableStateOf("login") }

            WelliTheme {
                when (currentScreen) {
                    "login" -> {
                        LoginScreen(
                            onLoginSuccess = {
                                currentScreen = "home"
                            },
                            onCreateAccountClick = {
                                Toast.makeText(
                                    this,
                                    "Sección de registro de cuenta",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                    "home" -> {
                        HomeScreen(
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
                            }
                        )
                    }
                }
            }
        }
    }
}
