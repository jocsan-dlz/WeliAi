package com.npcg.practica1.domain.repository

import com.npcg.practica1.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de Repositorio para la gestión de usuarios en la capa de Dominio.
 */
interface UsuarioRepository {
    suspend fun registrarUsuario(usuario: Usuario, contrasenaPlana: String): Result<Usuario>
    suspend fun iniciarSesion(correo: String, contrasenaPlana: String): Result<Usuario>
    suspend fun obtenerUsuarioPorId(id: Int): Result<Usuario?>
    suspend fun obtenerUsuarioPorCorreo(correo: String): Result<Usuario?>
    fun obtenerTodosLosUsuarios(): Flow<List<Usuario>>
    suspend fun actualizarUsuario(usuario: Usuario, nuevaContrasenaPlana: String? = null): Result<Unit>
    suspend fun eliminarUsuario(usuario: Usuario): Result<Unit>
}
