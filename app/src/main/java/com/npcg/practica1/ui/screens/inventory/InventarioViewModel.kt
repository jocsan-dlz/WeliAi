package com.npcg.practica1.ui.screens.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.npcg.practica1.domain.model.IngredienteInventario
import com.npcg.practica1.domain.repository.InventarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InventarioUiState(
    val ingredientes: List<IngredienteInventario> = emptyList(),
    val busqueda: String = "",
    val categoria: String? = null,
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InventarioViewModel @Inject constructor(
    private val repository: InventarioRepository
) : ViewModel() {
    private val filters = MutableStateFlow(InventoryFilter())
    private val _state = MutableStateFlow(InventarioUiState())
    val state: StateFlow<InventarioUiState> = combine(
        _state,
        filters.flatMapLatest { filter ->
            repository.observarIngredientes(filter.busqueda, filter.categoria)
                .catch { error ->
                    if (error is CancellationException) throw error
                    _state.value = _state.value.copy(cargando = false, error = error.message ?: "No se pudo leer el inventario.")
                    emit(emptyList())
                }
        }
    ) { current, ingredients -> current.copy(ingredientes = ingredients, cargando = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventarioUiState())

    fun onBusquedaChange(value: String) {
        filters.value = filters.value.copy(busqueda = value)
        _state.value = _state.value.copy(busqueda = value, cargando = true, error = null)
    }

    fun onCategoriaChange(value: String?) {
        filters.value = filters.value.copy(categoria = value)
        _state.value = _state.value.copy(categoria = value, cargando = true, error = null)
    }

    fun agregar(ingrediente: IngredienteInventario, onSaved: () -> Unit = {}) = perform(onSaved) {
        repository.agregarIngrediente(ingrediente).map { Unit }
    }

    fun actualizar(ingrediente: IngredienteInventario, onSaved: () -> Unit = {}) = perform(onSaved) {
        repository.actualizarIngrediente(ingrediente)
    }

    fun eliminar(id: Long, onSaved: () -> Unit = {}) = perform(onSaved) {
        repository.eliminarIngrediente(id)
    }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, mensaje = null)
    }

    private fun perform(onSaved: () -> Unit, action: suspend () -> Result<Unit>) {
        if (_state.value.guardando) return
        viewModelScope.launch {
            _state.value = _state.value.copy(guardando = true, error = null, mensaje = null)
            val result = try {
                action()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result.failure(error)
            }
            result.fold(
                onSuccess = {
                    _state.value = _state.value.copy(guardando = false, mensaje = "Inventario actualizado.")
                    onSaved()
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        guardando = false,
                        error = error.message ?: "No se pudo guardar el ingrediente."
                    )
                }
            )
        }
    }

    private data class InventoryFilter(val busqueda: String = "", val categoria: String? = null)
}
