package com.npcg.practica1.data.local.dao

import androidx.room.*
import com.npcg.practica1.data.local.entity.IngredienteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredienteDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertIngrediente(ingrediente: IngredienteEntity): Long

    @Update
    suspend fun updateIngrediente(ingrediente: IngredienteEntity): Int

    @Delete
    suspend fun deleteIngrediente(ingrediente: IngredienteEntity): Int

    @Query("SELECT * FROM ingrediente ORDER BY nombre ASC")
    fun getAllIngredientesFlow(): Flow<List<IngredienteEntity>>

    @Query("SELECT * FROM ingrediente ORDER BY nombre ASC")
    suspend fun getAllIngredientes(): List<IngredienteEntity>

    @Query("SELECT * FROM ingrediente WHERE id_ingrediente = :id")
    suspend fun getIngredienteById(id: Int): IngredienteEntity?

    @Query("SELECT * FROM ingrediente WHERE nombre LIKE '%' || :nombre || '%' ORDER BY nombre ASC")
    fun searchIngredientesPorNombre(nombre: String): Flow<List<IngredienteEntity>>
}
