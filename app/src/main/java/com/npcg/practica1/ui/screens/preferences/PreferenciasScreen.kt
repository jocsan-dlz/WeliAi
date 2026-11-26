package com.npcg.practica1.ui.screens.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.npcg.practica1.domain.model.PresupuestosAlimentarios
import com.npcg.practica1.domain.model.RestriccionesAlimentarias
import com.npcg.practica1.domain.model.TiposAlimentacion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferenciasScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PreferenciasViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val preferences = state.preferencias
    var customRestriction by remember { mutableStateOf("") }
    var customAllergy by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Preferencias alimentarias") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.cargando -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }
            else -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PreferenceSection(title = "Tipo de alimentación") {
                    ChoiceChips(
                        options = TiposAlimentacion.all,
                        selected = preferences.tipoAlimentacion,
                        enabled = !state.guardando,
                        onSelect = { viewModel.onPreferenciasChange(preferences.copy(tipoAlimentacion = it)) }
                    )
                }

                PreferenceSection(title = "Presupuesto") {
                    Text("Cero gasto aprovecha tu inventario; Ahorro prioriza opciones económicas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ChoiceChips(
                        options = PresupuestosAlimentarios.all,
                        selected = preferences.presupuesto,
                        enabled = !state.guardando,
                        onSelect = { viewModel.onPreferenciasChange(preferences.copy(presupuesto = it)) }
                    )
                }

                PreferenceSection(title = "Restricciones") {
                    Text(
                        if (preferences.restricciones.isEmpty()) "Sin restricciones seleccionadas" else "Puedes elegir varias o añadir otra.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ChoiceChips(
                        options = RestriccionesAlimentarias.sugeridas,
                        selectedValues = preferences.restricciones,
                        enabled = !state.guardando,
                        onToggle = { selected ->
                            val next = if (selected in preferences.restricciones) {
                                preferences.restricciones - selected
                            } else preferences.restricciones + selected
                            viewModel.onPreferenciasChange(preferences.copy(restricciones = next))
                        }
                    )
                    AddPreferenceItem(
                        value = customRestriction,
                        onValueChange = { customRestriction = it },
                        label = "Otra restricción",
                        enabled = !state.guardando,
                        onAdd = {
                            val cleaned = customRestriction.trim()
                            if (cleaned.isNotEmpty()) {
                                viewModel.onPreferenciasChange(preferences.copy(restricciones = addUnique(preferences.restricciones, cleaned)))
                                customRestriction = ""
                            }
                        }
                    )
                    RemovableItems(preferences.restricciones, !state.guardando) { item ->
                        viewModel.onPreferenciasChange(preferences.copy(restricciones = preferences.restricciones - item))
                    }
                }

                PreferenceSection(title = "Alergias") {
                    Text("Registra alimentos que deseas excluir de las recetas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AddPreferenceItem(
                        value = customAllergy,
                        onValueChange = { customAllergy = it },
                        label = "Agregar alergia",
                        enabled = !state.guardando,
                        onAdd = {
                            val cleaned = customAllergy.trim()
                            if (cleaned.isNotEmpty()) {
                                viewModel.onPreferenciasChange(preferences.copy(alergias = addUnique(preferences.alergias, cleaned)))
                                customAllergy = ""
                            }
                        }
                    )
                    RemovableItems(preferences.alergias, !state.guardando) { item ->
                        viewModel.onPreferenciasChange(preferences.copy(alergias = preferences.alergias - item))
                    }
                }

                PreferenceSection(title = "Porciones habituales") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        IconButton(
                            onClick = { viewModel.onPreferenciasChange(preferences.copy(porcionesHabituales = (preferences.porcionesHabituales - 1).coerceAtLeast(1))) },
                            enabled = !state.guardando && preferences.porcionesHabituales > 1
                        ) { Icon(Icons.Default.Remove, contentDescription = "Restar porción") }
                        Text("${preferences.porcionesHabituales}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        IconButton(
                            onClick = { viewModel.onPreferenciasChange(preferences.copy(porcionesHabituales = (preferences.porcionesHabituales + 1).coerceAtMost(20))) },
                            enabled = !state.guardando && preferences.porcionesHabituales < 20
                        ) { Icon(Icons.Default.Add, contentDescription = "Sumar porción") }
                        Text("porciones", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (state.guardado) Text("Preferencias guardadas.", color = MaterialTheme.colorScheme.primary)
                Button(
                    onClick = viewModel::guardar,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.guardando
                ) {
                    if (state.guardando) CircularProgressIndicator() else Icon(Icons.Default.Save, contentDescription = null)
                    Text(if (state.guardando) " Guardando…" else " Guardar preferencias")
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun PreferenceSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                content()
            }
        )
    }
}

@Composable
private fun ChoiceChips(
    options: List<String>,
    selected: String,
    enabled: Boolean,
    onSelect: (String) -> Unit
) = ChoiceChips(options, listOf(selected), enabled) { onSelect(it) }

@Composable
private fun ChoiceChips(
    options: List<String>,
    selectedValues: List<String>,
    enabled: Boolean,
    onToggle: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { option ->
                    FilterChip(
                        selected = option in selectedValues,
                        onClick = { onToggle(option) },
                        enabled = enabled,
                        label = { Text(option) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddPreferenceItem(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    onAdd: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )
        IconButton(onClick = onAdd, enabled = enabled && value.isNotBlank()) {
            Icon(Icons.Default.Add, contentDescription = "Agregar")
        }
    }
}

@Composable
private fun RemovableItems(items: List<String>, enabled: Boolean, onRemove: (String) -> Unit) {
    items.forEach { item ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("• $item", modifier = Modifier.weight(1f))
            IconButton(onClick = { onRemove(item) }, enabled = enabled) {
                Icon(Icons.Default.Remove, contentDescription = "Quitar $item")
            }
        }
    }
}

private fun addUnique(items: List<String>, value: String): List<String> =
    if (items.any { it.equals(value, ignoreCase = true) }) items else items + value
