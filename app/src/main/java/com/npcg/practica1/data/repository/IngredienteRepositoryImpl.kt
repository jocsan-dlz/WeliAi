package com.npcg.practica1.data.repository

import com.npcg.practica1.data.local.dao.IngredienteDao
import com.npcg.practica1.data.mapper.toDomain
import com.npcg.practica1.data.mapper.toEntity
import com.npcg.practica1.domain.model.Ingrediente
import com.npcg.practica1.domain.repository.IngredienteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * Implementación del repositorio de Ingredientes utilizando Room DAO y Mappers.
 */
class IngredienteRepositoryImpl @Inject constructor(
    private val ingredienteDao: IngredienteDao
) : IngredienteRepository {

    override suspend fun guardarIngrediente(ingrediente: Ingrediente): Result<Long> {
        return try {
            val id = ingredienteDao.insertIngrediente(ingrediente.toEntity())
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun obtenerIngredientes(): Result<List<Ingrediente>> {
        return try {
            val list = ingredienteDao.getAllIngredientes().map { it.toDomain() }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getIngredientesFlow(): Flow<List<Ingrediente>> {
        return ingredienteDao.getAllIngredientesFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun obtenerIngredientePorId(id: Int): Result<Ingrediente?> {
        return try {
            val ingrediente = ingredienteDao.getIngredienteById(id)?.toDomain()
            Result.success(ingrediente)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun buscarIngredientesPorNombre(nombre: String): Flow<List<Ingrediente>> {
        return ingredienteDao.searchIngredientesPorNombre(nombre).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun actualizarIngrediente(ingrediente: Ingrediente): Result<Unit> {
        return try {
            if (ingredienteDao.updateIngrediente(ingrediente.toEntity()) == 0) {
                return Result.failure(NoSuchElementException("El ingrediente no existe"))
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun eliminarIngrediente(ingrediente: Ingrediente): Result<Unit> {
        return try {
            if (ingredienteDao.deleteIngrediente(ingrediente.toEntity()) == 0) {
                return Result.failure(NoSuchElementException("El ingrediente no existe"))
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
