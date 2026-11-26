package com.npcg.practica1.ui.screens.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.npcg.practica1.domain.model.IngredienteInventario
import com.npcg.practica1.domain.model.InventarioOpciones

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InventarioViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var editor by remember { mutableStateOf<IngredienteInventario?>(null) }
    var deleting by remember { mutableStateOf<IngredienteInventario?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Mi inventario") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editor = IngredienteInventario(
                    nombre = "",
                    cantidad = 1.0,
                    unidad = InventarioOpciones.unidades.first(),
                    categoria = InventarioOpciones.categorias.last()
                )
            }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar ingrediente")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.busqueda,
                onValueChange = viewModel::onBusquedaChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar ingrediente") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            ChoiceDropdown(
                label = "Categoría",
                value = state.categoria ?: "Todas las categorías",
                options = listOf("Todas las categorías") + InventarioOpciones.categorias,
                onSelect = { viewModel.onCategoriaChange(if (it == "Todas las categorías") null else it) }
            )
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            state.mensaje?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium) }
            when {
                state.cargando && state.ingredientes.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.ingredientes.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Aún no tienes ingredientes. Agrega el primero con +.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.ingredientes, key = { it.id }) { ingredient ->
                        IngredientCard(
                            ingredient = ingredient,
                            onEdit = { editor = ingredient },
                            onDelete = { deleting = ingredient }
                        )
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }

    editor?.let { ingredient ->
        IngredientEditorDialog(
            initial = ingredient,
            saving = state.guardando,
            error = state.error,
            onDismiss = { if (!state.guardando) { editor = null; viewModel.clearMessages() } },
            onSave = { edited ->
                if (edited.id == 0L) viewModel.agregar(edited) { editor = null }
                else viewModel.actualizar(edited) { editor = null }
            }
        )
    }

    deleting?.let { ingredient ->
        AlertDialog(
            onDismissRequest = { if (!state.guardando) deleting = null },
            title = { Text("Eliminar ingrediente") },
            text = { Text("¿Quieres eliminar ${ingredient.nombre} de tu inventario?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.eliminar(ingredient.id) { deleting = null } },
                    enabled = !state.guardando
                ) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }, enabled = !state.guardando) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun IngredientCard(
    ingredient: IngredienteInventario,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(ingredient.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${ingredient.cantidad.formatQuantity()} ${ingredient.unidad}", color = MaterialTheme.colorScheme.primary)
                Text(ingredient.categoria, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Editar ${ingredient.nombre}") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Eliminar ${ingredient.nombre}") }
        }
    }
}

@Composable
private fun IngredientEditorDialog(
    initial: IngredienteInventario,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (IngredienteInventario) -> Unit
) {
    var name by remember(initial.id) { mutableStateOf(initial.nombre) }
    var quantity by remember(initial.id) { mutableStateOf(initial.cantidad.formatQuantity()) }
    var unit by remember(initial.id) { mutableStateOf(initial.unidad) }
    var category by remember(initial.id) { mutableStateOf(initial.categoria) }
    val parsedQuantity = quantity.toDoubleOrNull()
    val validation = when {
        name.isBlank() -> "El nombre es obligatorio."
        parsedQuantity == null || !parsedQuantity.isFinite() || parsedQuantity <= 0.0 -> "Ingresa una cantidad mayor que cero."
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "Agregar ingrediente" else "Editar ingrediente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true, enabled = !saving)
                OutlinedTextField(
                    quantity,
                    { quantity = it.filter { char -> char.isDigit() || char == '.' || char == ',' }.replace(',', '.') },
                    label = { Text("Cantidad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    enabled = !saving
                )
                ChoiceDropdown("Unidad", unit, InventarioOpciones.unidades, { unit = it }, enabled = !saving)
                ChoiceDropdown("Categoría", category, InventarioOpciones.categorias, { category = it }, enabled = !saving)
                (error ?: validation)?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(initial.copy(nombre = name, cantidad = parsedQuantity ?: 0.0, unidad = unit, categoria = category)) },
                enabled = !saving && validation == null
            ) { if (saving) CircularProgressIndicator(Modifier.size(18.dp)) else Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancelar") } }
    )
}

@Composable
private fun ChoiceDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
        Box(Modifier.matchParentSize().clickable(enabled = enabled) { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}

private fun Double.formatQuantity(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()
