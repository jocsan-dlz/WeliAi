package com.npcg.practica1.ui.screens.home

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import com.npcg.practica1.R
import javax.inject.Inject

enum class BudgetOption(@StringRes val titleRes: Int) {
    GRATIS(R.string.budget_free),
    AHORRO(R.string.budget_savings),
    GOURMET(R.string.budget_gourmet)
}

data class IngredientItem(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val qtyRes: Int
)

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    var selectedBudget by mutableStateOf(BudgetOption.GRATIS)
        private set

    var selectedTab by mutableStateOf(0)
        private set

    var isFavoriteRecipe by mutableStateOf(false)
        private set

    val inventoryItems = listOf(
        IngredientItem("1", R.string.ingredient_tomato, R.string.qty_tomato),
        IngredientItem("2", R.string.ingredient_egg, R.string.qty_egg),
        IngredientItem("3", R.string.ingredient_cheese, R.string.qty_cheese),
        IngredientItem("4", R.string.ingredient_onion, R.string.qty_onion)
    )

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
