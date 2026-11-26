package com.npcg.practica1.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "preferencia_item",
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
        Index(value = ["id_usuario", "tipo", "nombre_normalizado"], unique = true)
    ]
)
data class PreferenciaItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id_preferencia_item: Long = 0,
    val id_usuario: Int,
    val tipo: String,
    val nombre: String,
    val nombre_normalizado: String
)
