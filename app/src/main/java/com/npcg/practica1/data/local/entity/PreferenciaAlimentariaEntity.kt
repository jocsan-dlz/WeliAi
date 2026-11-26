package com.npcg.practica1.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "preferencia_alimentaria",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id_usuario"],
            childColumns = ["id_usuario"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ]
)
data class PreferenciaAlimentariaEntity(
    @PrimaryKey
    val id_usuario: Int,
    val tipo_alimentacion: String,
    val presupuesto: String,
    val porciones: Int,
    val fecha_actualizacion: Long
)
