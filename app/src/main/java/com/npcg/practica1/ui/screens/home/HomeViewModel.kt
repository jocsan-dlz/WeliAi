package com.npcg.practica1.ui.screens.home

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.npcg.practica1.R
import com.npcg.practica1.domain.model.IngredienteInventario
import com.npcg.practica1.domain.repository.InventarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BudgetOption(@StringRes val titleRes: Int) {
    GRATIS(R.string.budget_free),
    AHORRO(R.string.budget_savings),
    GOURMET(R.string.budget_gourmet)
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val inventarioRepository: InventarioRepository
) : ViewModel() {

    var selectedBudget by mutableStateOf(BudgetOption.GRATIS)
        private set

    var selectedTab by mutableStateOf(0)
        private set

    var isFavoriteRecipe by mutableStateOf(false)
        private set

    private val _inventoryItems = MutableStateFlow<List<IngredienteInventario>>(emptyList())
    val inventoryItems: StateFlow<List<IngredienteInventario>> = _inventoryItems.asStateFlow()
    private val _inventoryError = MutableStateFlow<String?>(null)
    val inventoryError: StateFlow<String?> = _inventoryError.asStateFlow()

    init {
        viewModelScope.launch {
            inventarioRepository.observarIngredientes()
                .catch { error -> _inventoryError.value = error.message ?: "No se pudo cargar el inventario." }
                .collect { items ->
                    _inventoryItems.value = items
                    _inventoryError.value = null
                }
        }
    }

    fun onBudgetSelected(budget: BudgetOption) {
        selectedBudget = budget
    }

    fun onTabSelected(index: Int) {
        selectedTab = index
    }

    fun toggleFavorite() {
        isFavoriteRecipe = !isFavoriteRecipe
    }
}
