package com.npcg.practica1.domain.model

/**
 * Modelo de Dominio que representa a un Usuario en la lógica de negocio de WelliAI V6.
 * El hash de la contraseña NO se incluye aquí para proteger la seguridad del dominio y la UI.
 */
data class Usuario(
    val idUsuario: Int = 0,
    val nombre: String,
    val correo: String,
    val fechaRegistro: String,
    val estado: Boolean = true
)
