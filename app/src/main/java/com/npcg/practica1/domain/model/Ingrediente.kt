package com.npcg.practica1.domain.model

/**
 * Modelo de Dominio que representa un Ingrediente disponible en WelliAI V6.
 * Independiente de la capa de persistencia (Room).
 */
data class Ingrediente(
    val idIngrediente: Int = 0,
    val nombre: String,
    val categoria: String,
    val unidadMedida: String
)
