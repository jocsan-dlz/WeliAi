package com.npcg.practica1.data.repository

import com.npcg.practica1.data.local.dao.InventarioDao
import com.npcg.practica1.data.local.dao.UsuarioDao
import com.npcg.practica1.data.mapper.toDomain
import com.npcg.practica1.data.mapper.toEntity
import com.npcg.practica1.data.util.displayLabel
import com.npcg.practica1.data.util.normalizedLabel
import com.npcg.practica1.domain.model.IngredienteDuplicadoException
import com.npcg.practica1.domain.model.IngredienteInventario
import com.npcg.practica1.domain.model.InventarioOpciones
import com.npcg.practica1.domain.repository.InventarioRepository
import com.npcg.practica1.domain.repository.SessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class InventarioRepositoryImpl @Inject constructor(
    private val inventarioDao: InventarioDao,
    private val usuarioDao: UsuarioDao,
    private val sessionStore: SessionStore
) : InventarioRepository {
    override fun observarIngredientes(busqueda: String, categoria: String?): Flow<List<IngredienteInventario>> {
        val query = normalizedLabel(busqueda)
        return observeActiveUserId(sessionStore, usuarioDao).flatMapLatest { userId ->
            if (userId == null) flowOf(emptyList())
            else inventarioDao.observeForUser(userId, query, categoria).map { rows -> rows.map { it.toDomain() } }
        }
    }

    override suspend fun agregarIngrediente(ingrediente: IngredienteInventario): Result<Long> = capture {
        val ownerId = requireActiveUserId(sessionStore, usuarioDao)
        val clean = validateAndClean(ingrediente)
        val normalized = normalizedLabel(clean.nombre)
        if (inventarioDao.getByNormalizedName(ownerId, normalized) != null) throw IngredienteDuplicadoException()
        try {
            inventarioDao.insert(clean.toEntity(ownerId, normalized, System.currentTimeMillis()))
        } catch (error: Exception) {
            if (error.isUniqueNameConflict()) throw IngredienteDuplicadoException()
            throw error
        }
    }

    override suspend fun actualizarIngrediente(ingrediente: IngredienteInventario): Result<Unit> = capture {
        require(ingrediente.id > 0) { "Selecciona un ingrediente válido." }
        val ownerId = requireActiveUserId(sessionStore, usuarioDao)
        val clean = validateAndClean(ingrediente)
        val normalized = normalizedLabel(clean.nombre)
        val duplicate = inventarioDao.getByNormalizedName(ownerId, normalized)
        if (duplicate != null && duplicate.id_ingrediente_inventario != clean.id) throw IngredienteDuplicadoException()
        try {
            check(inventarioDao.updateOwned(
                id = clean.id,
                userId = ownerId,
                nombre = clean.nombre,
                nombreNormalizado = normalized,
                cantidad = clean.cantidad,
                unidad = clean.unidad,
                categoria = clean.categoria
            ) == 1) { "El ingrediente no existe en tu inventario." }
        } catch (error: Exception) {
            if (error.isUniqueNameConflict()) throw IngredienteDuplicadoException()
            throw error
        }
    }

    override suspend fun eliminarIngrediente(id: Long): Result<Unit> = capture {
        require(id > 0) { "Selecciona un ingrediente válido." }
        val ownerId = requireActiveUserId(sessionStore, usuarioDao)
        check(inventarioDao.deleteOwned(id, ownerId) == 1) { "El ingrediente no existe en tu inventario." }
    }

    private fun validateAndClean(ingrediente: IngredienteInventario): IngredienteInventario {
        val name = displayLabel(ingrediente.nombre)
        require(name.isNotBlank()) { "Escribe el nombre del ingrediente." }
        require(ingrediente.cantidad.isFinite() && ingrediente.cantidad > 0.0) { "La cantidad debe ser mayor que cero." }
        val unit = ingrediente.unidad.trim()
        require(unit in InventarioOpciones.unidades) { "Selecciona una unidad válida." }
        val category = ingrediente.categoria.trim()
        require(category in InventarioOpciones.categorias) { "Selecciona una categoría válida." }
        return ingrediente.copy(nombre = name, unidad = unit, categoria = category)
    }

    private suspend fun <T> capture(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }

    private fun Exception.isUniqueNameConflict(): Boolean =
        generateSequence<Throwable>(this) { it.cause }
            .any { it.message.orEmpty().contains("inventario_ingrediente.id_usuario") }
}
