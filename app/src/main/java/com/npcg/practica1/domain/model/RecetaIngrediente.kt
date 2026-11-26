package com.npcg.practica1.domain.model

/**
 * Modelo de Dominio que representa la relación N:M entre una Receta y sus Ingredientes en WelliAI V6.
 * Independiente de la capa de persistencia (Room).
 */
data class RecetaIngrediente(
    val idRecetaIngrediente: Int = 0,
    val idReceta: Int,
    val idIngrediente: Int,
    val cantidad: Double,
    val unidad: String
)
