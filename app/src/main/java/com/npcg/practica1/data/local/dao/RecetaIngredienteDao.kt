package com.npcg.practica1.data.local.dao

import androidx.room.*
import com.npcg.practica1.data.local.entity.RecetaIngredienteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecetaIngredienteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecetaIngrediente(item: RecetaIngredienteEntity): Long

    @Update
    suspend fun updateRecetaIngrediente(item: RecetaIngredienteEntity)

    @Delete
    suspend fun deleteRecetaIngrediente(item: RecetaIngredienteEntity)

    @Query("SELECT * FROM receta_ingrediente WHERE id_receta = :recetaId")
    fun getIngredientesByRecetaFlow(recetaId: Int): Flow<List<RecetaIngredienteEntity>>

    @Query("SELECT * FROM receta_ingrediente WHERE id_receta = :recetaId")
    suspend fun getIngredientesByReceta(recetaId: Int): List<RecetaIngredienteEntity>

    @Query("DELETE FROM receta_ingrediente WHERE id_receta = :recetaId")
    suspend fun deleteIngredientesByReceta(recetaId: Int)
}
