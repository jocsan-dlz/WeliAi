package com.npcg.practica1.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.npcg.practica1.data.local.entity.PreferenciaAlimentariaEntity
import com.npcg.practica1.data.local.entity.PreferenciaItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PreferenciasAlimentariasDao {
    @Query("SELECT * FROM preferencia_alimentaria WHERE id_usuario = :userId LIMIT 1")
    fun observeForUser(userId: Int): Flow<PreferenciaAlimentariaEntity?>

    @Query("SELECT * FROM preferencia_item WHERE id_usuario = :userId ORDER BY nombre_normalizado COLLATE NOCASE ASC")
    fun observeItemsForUser(userId: Int): Flow<List<PreferenciaItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preferencias: PreferenciaAlimentariaEntity)

    @Query("DELETE FROM preferencia_item WHERE id_usuario = :userId")
    suspend fun deleteItemsForUser(userId: Int)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItems(items: List<PreferenciaItemEntity>)

    @Transaction
    suspend fun saveForUser(preferencias: PreferenciaAlimentariaEntity, items: List<PreferenciaItemEntity>) {
        upsert(preferencias)
        deleteItemsForUser(preferencias.id_usuario)
        if (items.isNotEmpty()) insertItems(items)
    }
}
