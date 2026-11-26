package com.npcg.practica1.data.local.dao

import androidx.room.*
import com.npcg.practica1.data.local.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsuario(usuario: UsuarioEntity): Long

    @Update
    suspend fun updateUsuario(usuario: UsuarioEntity)

    @Delete
    suspend fun deleteUsuario(usuario: UsuarioEntity)

    @Query("SELECT * FROM usuario WHERE id_usuario = :id")
    suspend fun getUsuarioById(id: Int): UsuarioEntity?

    @Query("SELECT * FROM usuario WHERE correo = :correo LIMIT 1")
    suspend fun getUsuarioByCorreo(correo: String): UsuarioEntity?

    @Query("SELECT * FROM usuario WHERE id_usuario = :id")
    fun getUsuarioByIdFlow(id: Int): Flow<UsuarioEntity?>

    @Query("SELECT * FROM usuario")
    fun getAllUsuarios(): Flow<List<UsuarioEntity>>
}
