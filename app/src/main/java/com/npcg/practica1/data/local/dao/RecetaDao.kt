package com.npcg.practica1.data.local.dao

import androidx.room.*
import com.npcg.practica1.data.local.entity.RecetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceta(receta: RecetaEntity): Long

    @Update
    suspend fun updateReceta(receta: RecetaEntity)

    @Delete
    suspend fun deleteReceta(receta: RecetaEntity)

    @Query("SELECT * FROM receta ORDER BY fecha_creacion DESC")
    fun getAllRecetasFlow(): Flow<List<RecetaEntity>>

    @Query("SELECT * FROM receta ORDER BY fecha_creacion DESC")
    suspend fun getAllRecetas(): List<RecetaEntity>

    @Query("SELECT * FROM receta WHERE id_usuario = :userId ORDER BY fecha_creacion DESC")
    fun getRecetasByUsuarioFlow(userId: Int): Flow<List<RecetaEntity>>

    @Query("SELECT * FROM receta WHERE id_usuario = :userId ORDER BY fecha_creacion DESC")
    suspend fun getRecetasByUsuario(userId: Int): List<RecetaEntity>

    @Query("SELECT * FROM receta WHERE id_receta = :id")
    suspend fun getRecetaById(id: Int): RecetaEntity?
}
