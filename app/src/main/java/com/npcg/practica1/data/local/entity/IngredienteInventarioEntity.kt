package com.npcg.practica1.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventario_ingrediente",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id_usuario"],
            childColumns = ["id_usuario"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["id_usuario"]),
        Index(value = ["id_usuario", "categoria"]),
        Index(value = ["id_usuario", "nombre_normalizado"], unique = true)
    ]
)
data class IngredienteInventarioEntity(
    @PrimaryKey(autoGenerate = true)
    val id_ingrediente_inventario: Long = 0,
    val id_usuario: Int,
    val nombre: String,
    val nombre_normalizado: String,
    val cantidad: Double,
    val unidad: String,
    val categoria: String,
    val fecha_registro: Long
)
