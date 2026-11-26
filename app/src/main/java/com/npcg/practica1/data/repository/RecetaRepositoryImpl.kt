package com.npcg.practica1.data.repository

import com.npcg.practica1.data.local.dao.RecetaDao
import com.npcg.practica1.data.mapper.toDomain
import com.npcg.practica1.data.mapper.toEntity
import com.npcg.practica1.domain.model.Receta
import com.npcg.practica1.domain.repository.RecetaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementación del repositorio de Recetas utilizando Room DAO y Mappers.
 */
class RecetaRepositoryImpl(
    private val recetaDao: RecetaDao
) : RecetaRepository {

    override suspend fun guardarReceta(receta: Receta): Result<Long> {
        return try {
            val id = recetaDao.insertReceta(receta.toEntity())
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun obtenerTodasLasRecetas(): Result<List<Receta>> {
        return try {
            val list = recetaDao.getAllRecetas().map { it.toDomain() }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getRecetasFlow(): Flow<List<Receta>> {
        return recetaDao.getAllRecetasFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun obtenerRecetasPorUsuario(idUsuario: Int): Result<List<Receta>> {
        return try {
            val list = recetaDao.getRecetasByUsuario(idUsuario).map { it.toDomain() }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getRecetasPorUsuarioFlow(idUsuario: Int): Flow<List<Receta>> {
        return recetaDao.getRecetasByUsuarioFlow(idUsuario).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun obtenerRecetaPorId(id: Int): Result<Receta?> {
        return try {
            val receta = recetaDao.getRecetaById(id)?.toDomain()
            Result.success(receta)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun actualizarReceta(receta: Receta): Result<Unit> {
        return try {
            recetaDao.updateReceta(receta.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun eliminarReceta(receta: Receta): Result<Unit> {
        return try {
            recetaDao.deleteReceta(receta.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
