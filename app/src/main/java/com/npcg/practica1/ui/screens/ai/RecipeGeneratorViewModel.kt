package com.npcg.practica1.ui.screens.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.npcg.practica1.domain.model.AiRecipeResult
import com.npcg.practica1.domain.model.ContextoGeneracionReceta
import com.npcg.practica1.domain.model.IngredienteInventario
import com.npcg.practica1.domain.model.PresupuestosAlimentarios
import com.npcg.practica1.domain.usecase.GenerateRecipeUseCase
import com.npcg.practica1.domain.usecase.GetRecipeContextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RecipeGenerationState {
    data object Idle : RecipeGenerationState
    data object Loading : RecipeGenerationState
    data class Success(val result: AiRecipeResult) : RecipeGenerationState
    data class Error(val message: String) : RecipeGenerationState
}

data class RecipeGeneratorUiState(
    val loadingContext: Boolean = true,
    val ingredients: List<IngredienteInventario> = emptyList(),
    val preferences: ContextoGeneracionReceta? = null,
    val selectedIngredientIds: Set<Long> = emptySet(),
    val budget: String = PresupuestosAlimentarios.CERO_GASTO,
    val contextError: String? = null,
    val generation: RecipeGenerationState = RecipeGenerationState.Idle
)

@HiltViewModel
class RecipeGeneratorViewModel @Inject constructor(
    private val getContext: GetRecipeContextUseCase,
    private val generateRecipe: GenerateRecipeUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(RecipeGeneratorUiState())
    val state: StateFlow<RecipeGeneratorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { loadContext() }
    }

    fun refreshContext() {
        if (_state.value.loadingContext) return
        _state.value = _state.value.copy(loadingContext = true, contextError = null)
        viewModelScope.launch { loadContext() }
    }

    private suspend fun loadContext() {
        try {
            getContext().fold(
                onSuccess = { context ->
                    val validIds = context.ingredientesDisponibles.mapTo(mutableSetOf()) { it.id }
                    _state.value = _state.value.copy(
                        loadingContext = false,
                        ingredients = context.ingredientesDisponibles,
                        preferences = context,
                        selectedIngredientIds = _state.value.selectedIngredientIds.intersect(validIds),
                        budget = context.preferencias.presupuesto,
                        contextError = null
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        loadingContext = false,
                        contextError = error.message ?: "No se pudieron cargar tus ingredientes y preferencias."
                    )
                }
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            _state.value = _state.value.copy(
                loadingContext = false,
                contextError = error.message ?: "No se pudieron cargar tus ingredientes y preferencias."
            )
        }
    }

    fun toggleIngredient(id: Long) {
        val current = _state.value
        if (current.generation == RecipeGenerationState.Loading || current.ingredients.none { it.id == id }) return
        val next = if (id in current.selectedIngredientIds) current.selectedIngredientIds - id
        else current.selectedIngredientIds + id
        _state.value = current.copy(selectedIngredientIds = next, generation = RecipeGenerationState.Idle)
    }

    fun selectAllIngredients() {
        val current = _state.value
        if (current.generation != RecipeGenerationState.Loading) {
            _state.value = current.copy(
                selectedIngredientIds = current.ingredients.mapTo(mutableSetOf()) { it.id },
                generation = RecipeGenerationState.Idle
            )
        }
    }

    fun onBudgetSelected(value: String) {
        if (value in PresupuestosAlimentarios.all && _state.value.generation != RecipeGenerationState.Loading) {
            _state.value = _state.value.copy(budget = value, generation = RecipeGenerationState.Idle)
        }
    }

    fun generate() {
        val current = _state.value
        if (current.loadingContext || current.generation == RecipeGenerationState.Loading) return
        if (current.selectedIngredientIds.isEmpty()) {
            _state.value = current.copy(generation = RecipeGenerationState.Error("Selecciona al menos un ingrediente."))
            return
        }
        val selected = current.ingredients.filter { it.id in current.selectedIngredientIds }
        _state.value = current.copy(generation = RecipeGenerationState.Loading)
        viewModelScope.launch {
            try {
                generateRecipe(selected, current.budget).fold(
                    onSuccess = { result -> _state.value = _state.value.copy(generation = RecipeGenerationState.Success(result)) },
                    onFailure = { error ->
                        _state.value = _state.value.copy(
                            generation = RecipeGenerationState.Error(error.message ?: "No se pudo generar la receta.")
                        )
                    }
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.value = _state.value.copy(
                    generation = RecipeGenerationState.Error(error.message ?: "No se pudo generar la receta.")
                )
            }
        }
    }

    fun clearResult() {
        if (_state.value.generation != RecipeGenerationState.Loading) {
            _state.value = _state.value.copy(generation = RecipeGenerationState.Idle)
        }
    }
}
