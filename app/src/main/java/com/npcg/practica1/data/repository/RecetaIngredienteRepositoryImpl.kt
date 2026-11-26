package com.npcg.practica1.data.repository

import com.npcg.practica1.data.local.dao.RecetaIngredienteDao
import com.npcg.practica1.data.mapper.toDomain
import com.npcg.practica1.data.mapper.toEntity
import com.npcg.practica1.domain.model.RecetaIngrediente
import com.npcg.practica1.domain.repository.RecetaIngredienteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementación del repositorio de RecetaIngrediente utilizando Room DAO y Mappers.
 */
class RecetaIngredienteRepositoryImpl(
    private val recetaIngredienteDao: RecetaIngredienteDao
) : RecetaIngredienteRepository {

    override suspend fun agregarIngredienteAReceta(recetaIngrediente: RecetaIngrediente): Long {
        return recetaIngredienteDao.insertRecetaIngrediente(recetaIngrediente.toEntity())
    }

    override suspend fun obtenerIngredientesDeReceta(idReceta: Int): List<RecetaIngrediente> {
        return recetaIngredienteDao.getIngredientesByReceta(idReceta).map { it.toDomain() }
    }

    override fun getIngredientesDeRecetaFlow(idReceta: Int): Flow<List<RecetaIngrediente>> {
        return recetaIngredienteDao.getIngredientesByRecetaFlow(idReceta).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun actualizarCantidadIngrediente(recetaIngrediente: RecetaIngrediente): Boolean {
        return try {
            recetaIngredienteDao.updateRecetaIngrediente(recetaIngrediente.toEntity())
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun eliminarIngredienteDeReceta(recetaIngrediente: RecetaIngrediente): Boolean {
        return try {
            recetaIngredienteDao.deleteRecetaIngrediente(recetaIngrediente.toEntity())
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun eliminarTodosLosIngredientesDeReceta(idReceta: Int): Boolean {
        return try {
            recetaIngredienteDao.deleteIngredientesByReceta(idReceta)
            true
        } catch (e: Exception) {
            false
        }
    }
}
