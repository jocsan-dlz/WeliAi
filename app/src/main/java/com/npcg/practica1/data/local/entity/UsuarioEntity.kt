package com.npcg.practica1.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usuario",
    indices = [
        Index(value = ["correo"], unique = true)
    ]
)
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true)
    val id_usuario: Int = 0,
    val nombre: String,
    val correo: String,
    val contrasena_hash: String,
    val fecha_registro: String,
    val estado: Boolean = true
)
