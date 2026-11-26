package com.npcg.practica1.domain.repository

import com.npcg.practica1.domain.model.Ingrediente
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de Repositorio para la gestión de ingredientes en la capa de Dominio.
 */
interface IngredienteRepository {
    suspend fun guardarIngrediente(ingrediente: Ingrediente): Result<Long>
    suspend fun obtenerIngredientes(): Result<List<Ingrediente>>
    fun getIngredientesFlow(): Flow<List<Ingrediente>>
    suspend fun obtenerIngredientePorId(id: Int): Result<Ingrediente?>
    fun buscarIngredientesPorNombre(nombre: String): Flow<List<Ingrediente>>
    suspend fun actualizarIngrediente(ingrediente: Ingrediente): Result<Unit>
    suspend fun eliminarIngrediente(ingrediente: Ingrediente): Result<Unit>
}
