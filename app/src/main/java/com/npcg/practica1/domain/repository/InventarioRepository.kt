package com.npcg.practica1.domain.repository

import com.npcg.practica1.domain.model.IngredienteInventario
import kotlinx.coroutines.flow.Flow

interface InventarioRepository {
    fun observarIngredientes(busqueda: String = "", categoria: String? = null): Flow<List<IngredienteInventario>>
    suspend fun agregarIngrediente(ingrediente: IngredienteInventario): Result<Long>
    suspend fun actualizarIngrediente(ingrediente: IngredienteInventario): Result<Unit>
    suspend fun eliminarIngrediente(id: Long): Result<Unit>
}
