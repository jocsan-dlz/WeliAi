package com.npcg.practica1.domain.repository

import com.npcg.practica1.domain.model.Receta
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de Repositorio para la gestión de recetas en la capa de Dominio.
 */
interface RecetaRepository {
    suspend fun guardarReceta(receta: Receta): Result<Long>
    suspend fun obtenerTodasLasRecetas(): Result<List<Receta>>
    fun getRecetasFlow(): Flow<List<Receta>>
    suspend fun obtenerRecetasPorUsuario(idUsuario: Int): Result<List<Receta>>
    fun getRecetasPorUsuarioFlow(idUsuario: Int): Flow<List<Receta>>
    suspend fun obtenerRecetaPorId(id: Int): Result<Receta?>
    suspend fun actualizarReceta(receta: Receta): Result<Unit>
    suspend fun eliminarReceta(receta: Receta): Result<Unit>
}
