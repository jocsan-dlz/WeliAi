package com.npcg.practica1.ui.screens.preferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.npcg.practica1.domain.model.PreferenciasAlimentarias
import com.npcg.practica1.domain.repository.PreferenciasAlimentariasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PreferenciasUiState(
    val preferencias: PreferenciasAlimentarias = PreferenciasAlimentarias(),
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val guardado: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PreferenciasViewModel @Inject constructor(
    private val repository: PreferenciasAlimentariasRepository
) : ViewModel() {
    private val _state = MutableStateFlow(PreferenciasUiState())
    val state: StateFlow<PreferenciasUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.observarPreferencias().collect { preferences ->
                    _state.value = _state.value.copy(preferencias = preferences, cargando = false)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.value = _state.value.copy(cargando = false, error = error.message ?: "No se pudieron leer tus preferencias.")
            }
        }
    }

    fun onPreferenciasChange(value: PreferenciasAlimentarias) {
        _state.value = _state.value.copy(preferencias = value, guardado = false, error = null)
    }

    fun guardar() {
        if (_state.value.guardando) return
        viewModelScope.launch {
            _state.value = _state.value.copy(guardando = true, guardado = false, error = null)
            val result = try {
                repository.guardarPreferencias(_state.value.preferencias)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result.failure(error)
            }
            result.fold(
                onSuccess = { _state.value = _state.value.copy(guardando = false, guardado = true) },
                onFailure = { error ->
                    _state.value = _state.value.copy(guardando = false, error = error.message ?: "No se pudieron guardar tus preferencias.")
                }
            )
        }
    }
}
