package com.npcg.practica1.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "receta_ingrediente",
    foreignKeys = [
        ForeignKey(
            entity = RecetaEntity::class,
            parentColumns = ["id_receta"],
            childColumns = ["id_receta"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = IngredienteEntity::class,
            parentColumns = ["id_ingrediente"],
            childColumns = ["id_ingrediente"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["id_receta", "id_ingrediente"], unique = true),
        Index(value = ["id_ingrediente"])
    ]
)
data class RecetaIngredienteEntity(
    @PrimaryKey(autoGenerate = true)
    val id_receta_ingrediente: Int = 0,
    val id_receta: Int,
    val id_ingrediente: Int,
    val cantidad: Double,
    val unidad: String
)
