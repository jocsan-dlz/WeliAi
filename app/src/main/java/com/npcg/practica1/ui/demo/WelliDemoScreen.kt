package com.npcg.practica1.ui.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.npcg.practica1.ui.components.WelliButton
import com.npcg.practica1.ui.components.WelliLogo
import com.npcg.practica1.ui.theme.WelliTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelliDemoScreen() {
    var isDarkTheme by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    WelliTheme(darkTheme = isDarkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Selector Tema Claro / Oscuro
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isDarkTheme) "Modo Oscuro 🌙" else "Modo Claro ☀️",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { isDarkTheme = it }
                    )
                }

                HorizontalDivider()

                // 1. LOGOTIPO OFICIAL REUTILIZABLE
                WelliLogo(size = 70.dp)

                Text(
                    text = "Asistente de Cocina Inteligente",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. PALETA DE COLORES
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Paleta de Colores", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ColorBox(MaterialTheme.colorScheme.primary, "Primario")
                            ColorBox(MaterialTheme.colorScheme.secondary, "Secundario")
                            ColorBox(MaterialTheme.colorScheme.tertiary, "Terciario")
                        }
                    }
                }

                // 3. CAMPOS DE TEXTO
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("¿Qué receta deseas buscar hoy?") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                // 4. BOTONES REUTILIZABLES
                WelliButton(
                    text = "Buscar Recetas con WelliAI",
                    onClick = {}
                )

                WelliButton(
                    text = "Explorar Categorías",
                    onClick = {},
                    isSecondary = true
                )
            }
        }
    }
}

@Composable
private fun RowScope.ColorBox(color: Color, label: String) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .background(color, shape = MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
