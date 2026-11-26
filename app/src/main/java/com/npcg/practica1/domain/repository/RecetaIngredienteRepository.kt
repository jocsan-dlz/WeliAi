package com.npcg.practica1.domain.repository

import com.npcg.practica1.domain.model.RecetaIngrediente
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de Repositorio para la relación Receta-Ingrediente en la capa de Dominio.
 */
interface RecetaIngredienteRepository {
    suspend fun agregarIngredienteAReceta(recetaIngrediente: RecetaIngrediente): Result<Long>
    suspend fun obtenerIngredientesDeReceta(idReceta: Int): Result<List<RecetaIngrediente>>
    fun getIngredientesDeRecetaFlow(idReceta: Int): Flow<List<RecetaIngrediente>>
    suspend fun actualizarCantidadIngrediente(recetaIngrediente: RecetaIngrediente): Result<Unit>
    suspend fun eliminarIngredienteDeReceta(recetaIngrediente: RecetaIngrediente): Result<Unit>
    suspend fun eliminarTodosLosIngredientesDeReceta(idReceta: Int): Result<Unit>
}
