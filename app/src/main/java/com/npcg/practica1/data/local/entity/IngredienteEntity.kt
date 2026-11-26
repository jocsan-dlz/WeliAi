package com.npcg.practica1.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ingrediente",
    indices = [
        Index(value = ["nombre"], unique = true)
    ]
)
data class IngredienteEntity(
    @PrimaryKey(autoGenerate = true)
    val id_ingrediente: Int = 0,
    val nombre: String,
    val categoria: String,
    val unidad_medida: String
)
