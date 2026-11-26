package com.npcg.practica1.data.repository

import com.npcg.practica1.data.local.dao.RecetaIngredienteDao
import com.npcg.practica1.data.mapper.toDomain
import com.npcg.practica1.data.mapper.toEntity
import com.npcg.practica1.domain.model.RecetaIngrediente
import com.npcg.practica1.domain.repository.RecetaIngredienteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * Implementación del repositorio de RecetaIngrediente utilizando Room DAO y Mappers.
 */
class RecetaIngredienteRepositoryImpl @Inject constructor(
    private val recetaIngredienteDao: RecetaIngredienteDao
) : RecetaIngredienteRepository {

    override suspend fun agregarIngredienteAReceta(recetaIngrediente: RecetaIngrediente): Result<Long> {
        return try {
            val id = recetaIngredienteDao.insertRecetaIngrediente(recetaIngrediente.toEntity())
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun obtenerIngredientesDeReceta(idReceta: Int): Result<List<RecetaIngrediente>> {
        return try {
            val list = recetaIngredienteDao.getIngredientesByReceta(idReceta).map { it.toDomain() }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getIngredientesDeRecetaFlow(idReceta: Int): Flow<List<RecetaIngrediente>> {
        return recetaIngredienteDao.getIngredientesByRecetaFlow(idReceta).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun actualizarCantidadIngrediente(recetaIngrediente: RecetaIngrediente): Result<Unit> {
        return try {
            if (recetaIngredienteDao.updateRecetaIngrediente(recetaIngrediente.toEntity()) == 0) {
                return Result.failure(NoSuchElementException("La relación receta-ingrediente no existe"))
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun eliminarIngredienteDeReceta(recetaIngrediente: RecetaIngrediente): Result<Unit> {
        return try {
            if (recetaIngredienteDao.deleteRecetaIngrediente(recetaIngrediente.toEntity()) == 0) {
                return Result.failure(NoSuchElementException("La relación receta-ingrediente no existe"))
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun eliminarTodosLosIngredientesDeReceta(idReceta: Int): Result<Unit> {
        return try {
            recetaIngredienteDao.deleteIngredientesByReceta(idReceta)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
