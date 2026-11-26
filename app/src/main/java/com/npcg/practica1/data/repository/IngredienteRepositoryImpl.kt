package com.npcg.practica1.data.repository

import com.npcg.practica1.data.local.dao.IngredienteDao
import com.npcg.practica1.data.mapper.toDomain
import com.npcg.practica1.data.mapper.toEntity
import com.npcg.practica1.domain.model.Ingrediente
import com.npcg.practica1.domain.repository.IngredienteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementación del repositorio de Ingredientes utilizando Room DAO y Mappers.
 */
class IngredienteRepositoryImpl(
    private val ingredienteDao: IngredienteDao
) : IngredienteRepository {

    override suspend fun guardarIngrediente(ingrediente: Ingrediente): Long {
        return ingredienteDao.insertIngrediente(ingrediente.toEntity())
    }

    override suspend fun obtenerIngredientes(): List<Ingrediente> {
        return ingredienteDao.getAllIngredientes().map { it.toDomain() }
    }

    override fun getIngredientesFlow(): Flow<List<Ingrediente>> {
        return ingredienteDao.getAllIngredientesFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun obtenerIngredientePorId(id: Int): Ingrediente? {
        return ingredienteDao.getIngredienteById(id)?.toDomain()
    }

    override fun buscarIngredientesPorNombre(nombre: String): Flow<List<Ingrediente>> {
        return ingredienteDao.searchIngredientesPorNombre(nombre).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun actualizarIngrediente(ingrediente: Ingrediente): Boolean {
        return try {
            ingredienteDao.updateIngrediente(ingrediente.toEntity())
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun eliminarIngrediente(ingrediente: Ingrediente): Boolean {
        return try {
            ingredienteDao.deleteIngrediente(ingrediente.toEntity())
            true
        } catch (e: Exception) {
            false
        }
    }
}
