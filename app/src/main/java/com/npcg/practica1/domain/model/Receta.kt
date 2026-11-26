package com.npcg.practica1.domain.model

/**
 * Modelo de Dominio que representa una Receta en la lógica de negocio de WelliAI V6.
 * Independiente de la capa de persistencia (Room).
 */
data class Receta(
    val idReceta: Int = 0,
    val idUsuario: Int,
    val nombre: String,
    val descripcion: String?,
    val instrucciones: String?,
    val tiempoPreparacion: Int,
    val presupuesto: Double,
    val fechaCreacion: String
)
