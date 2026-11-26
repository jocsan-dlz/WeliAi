package com.npcg.practica1.domain.model

/** Información local preparada para el generador de recetas del siguiente sprint. */
data class ContextoGeneracionReceta(
    val ingredientesDisponibles: List<IngredienteInventario>,
    val preferencias: PreferenciasAlimentarias
)
