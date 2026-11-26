package com.npcg.practica1.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.npcg.practica1.data.local.entity.IngredienteInventarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventarioDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(ingrediente: IngredienteInventarioEntity): Long

    @Query("SELECT * FROM inventario_ingrediente WHERE id_usuario = :userId AND nombre_normalizado = :nombreNormalizado LIMIT 1")
    suspend fun getByNormalizedName(userId: Int, nombreNormalizado: String): IngredienteInventarioEntity?

    @Query("UPDATE inventario_ingrediente SET nombre = :nombre, nombre_normalizado = :nombreNormalizado, cantidad = :cantidad, unidad = :unidad, categoria = :categoria WHERE id_ingrediente_inventario = :id AND id_usuario = :userId")
    suspend fun updateOwned(id: Long, userId: Int, nombre: String, nombreNormalizado: String, cantidad: Double, unidad: String, categoria: String): Int

    @Query("DELETE FROM inventario_ingrediente WHERE id_ingrediente_inventario = :id AND id_usuario = :userId")
    suspend fun deleteOwned(id: Long, userId: Int): Int

    @Query("SELECT * FROM inventario_ingrediente WHERE id_usuario = :userId AND (:query = '' OR nombre_normalizado LIKE '%' || :query || '%') AND (:category IS NULL OR categoria = :category) ORDER BY nombre_normalizado COLLATE NOCASE ASC")
    fun observeForUser(userId: Int, query: String, category: String?): Flow<List<IngredienteInventarioEntity>>
}
