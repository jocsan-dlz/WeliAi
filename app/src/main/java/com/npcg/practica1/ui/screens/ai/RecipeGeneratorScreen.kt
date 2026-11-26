package com.npcg.practica1.ui.screens.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.npcg.practica1.domain.model.AiRecipeCard
import com.npcg.practica1.domain.model.PresupuestosAlimentarios

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeGeneratorScreen(
    onBack: () -> Unit,
    onChatClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecipeGeneratorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Crear receta con WelliAI") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refreshContext, enabled = !state.loadingContext) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar inventario")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Elige qué quieres aprovechar. WelliAI tendrá en cuenta tus preferencias guardadas.", style = MaterialTheme.typography.bodyLarge)
            state.contextError?.let { ErrorCard(it, onRetry = viewModel::refreshContext) }

            if (state.loadingContext) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.contextError == null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Ingredientes disponibles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        TextButton(onClick = viewModel::selectAllIngredients, enabled = state.ingredients.isNotEmpty()) {
                            Text("Seleccionar todos")
                        }
                    }
                    if (state.ingredients.isEmpty()) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Text("Tu inventario está vacío. Agrega ingredientes antes de pedir una receta.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        state.ingredients.forEach { ingredient ->
                            val selected = ingredient.id in state.selectedIngredientIds
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.toggleIngredient(ingredient.id) },
                                label = {
                                    Text("${ingredient.nombre} · ${ingredient.cantidad} ${ingredient.unidad} · ${ingredient.categoria}")
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Presupuesto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PresupuestosAlimentarios.all.forEach { option ->
                            FilterChip(
                                selected = state.budget == option,
                                onClick = { viewModel.onBudgetSelected(option) },
                                label = { Text(option) }
                            )
                        }
                    }
                }

                state.preferences?.let { context ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Preferencias aplicadas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("Alimentación: ${context.preferencias.tipoAlimentacion} · Porciones: ${context.preferencias.porcionesHabituales}")
                            if (context.preferencias.restricciones.isNotEmpty()) Text("Restricciones: ${context.preferencias.restricciones.joinToString()}")
                            if (context.preferencias.alergias.isNotEmpty()) {
                                Text("Alergias registradas: ${context.preferencias.alergias.joinToString()}", color = MaterialTheme.colorScheme.error)
                                Text("Revisa siempre etiquetas y posibles contaminaciones cruzadas antes de cocinar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                Button(
                    onClick = viewModel::generate,
                    enabled = state.ingredients.isNotEmpty() && state.selectedIngredientIds.isNotEmpty() && state.generation != RecipeGenerationState.Loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.generation == RecipeGenerationState.Loading) {
                        CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
                    } else Text("Generar receta")
                }
                when (val generation = state.generation) {
                    RecipeGenerationState.Idle -> Unit
                    RecipeGenerationState.Loading -> Text("WelliAI está preparando una propuesta…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    is RecipeGenerationState.Error -> ErrorCard(generation.message, onRetry = viewModel::generate)
                    is RecipeGenerationState.Success -> {
                        if (!generation.result.canGenerate || generation.result.recipe == null) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Text(generation.result.message, Modifier.padding(16.dp))
                            }
                        } else {
                            RecipeCard(generation.result.recipe)
                            Button(onClick = onChatClick, modifier = Modifier.fillMaxWidth()) {
                                Text("Preguntar a WelliAI")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun RecipeCard(recipe: AiRecipeCard) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(recipe.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(recipe.description)
            Text("${recipe.preparationTimeMinutes} min · ${recipe.servings} porciones", style = MaterialTheme.typography.labelLarge)
            recipe.estimatedCostMxn?.let { Text("Costo estimado: $${"%.2f".format(it)} MXN") }

            Text("Ingredientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            recipe.ingredients.forEach { ingredient ->
                Text("• ${ingredient.name}: ${ingredient.quantity} ${ingredient.unit}${if (ingredient.availableInInventory) " · en tu inventario" else " · adicional"}")
            }
            if (recipe.additionalIngredients.isNotEmpty()) {
                Text("Para conseguir", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(recipe.additionalIngredients.joinToString())
            }
            if (recipe.substitutions.isNotEmpty()) {
                Text("Sustituciones opcionales", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                recipe.substitutions.forEach { Text("${it.ingredient} → ${it.replacement}: ${it.reason}") }
            }
            Text("Preparación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            recipe.instructions.forEachIndexed { index, instruction -> Text("${index + 1}. $instruction") }
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
            TextButton(onClick = onRetry) { Text("Reintentar") }
        }
    }
}
