package com.npcg.practica1.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "receta",
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
        Index(value = ["id_usuario"])
    ]
)
data class RecetaEntity(
    @PrimaryKey(autoGenerate = true)
    val id_receta: Int = 0,
    val id_usuario: Int,
    val nombre: String,
    val descripcion: String?,
    val instrucciones: String?,
    val tiempo_preparacion: Int,
    val presupuesto: Double,
    val fecha_creacion: String
)
