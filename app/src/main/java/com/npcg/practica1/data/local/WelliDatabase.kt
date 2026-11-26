package com.npcg.practica1.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.npcg.practica1.data.local.dao.IngredienteDao
import com.npcg.practica1.data.local.dao.RecetaDao
import com.npcg.practica1.data.local.dao.RecetaIngredienteDao
import com.npcg.practica1.data.local.dao.UsuarioDao
import com.npcg.practica1.data.local.entity.IngredienteEntity
import com.npcg.practica1.data.local.entity.RecetaEntity
import com.npcg.practica1.data.local.entity.RecetaIngredienteEntity
import com.npcg.practica1.data.local.entity.UsuarioEntity

@Database(
    entities = [
        UsuarioEntity::class,
        RecetaEntity::class,
        IngredienteEntity::class,
        RecetaIngredienteEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WelliDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun recetaDao(): RecetaDao
    abstract fun ingredienteDao(): IngredienteDao
    abstract fun recetaIngredienteDao(): RecetaIngredienteDao
}
