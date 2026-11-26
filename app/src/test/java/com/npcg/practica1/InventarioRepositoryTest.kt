package com.npcg.practica1

import com.npcg.practica1.data.local.dao.InventarioDao
import com.npcg.practica1.data.local.dao.UsuarioDao
import com.npcg.practica1.data.local.entity.IngredienteInventarioEntity
import com.npcg.practica1.data.local.entity.UsuarioEntity
import com.npcg.practica1.data.repository.InventarioRepositoryImpl
import com.npcg.practica1.data.util.normalizedLabel
import com.npcg.practica1.domain.model.IngredienteDuplicadoException
import com.npcg.practica1.domain.model.IngredienteInventario
import com.npcg.practica1.domain.repository.SessionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InventarioRepositoryTest {
    private val dao = FakeInventarioDao()
    private val users = FakeUsuarioDao(
        1 to activeUser(1),
        2 to activeUser(2)
    )
    private val session = FakeSessionStore(1)
    private val repository = InventarioRepositoryImpl(dao, users, session)

    @Test
    fun addsIngredientWithActiveOwnerAndNormalizesDuplicateNames() = runBlocking {
        val result = repository.agregarIngrediente(ingredient(name = "  Lechuga   romana "))

        assertTrue(result.isSuccess)
        val row = dao.rows.single()
        assertEquals(1, row.id_usuario)
        assertEquals("Lechuga romana", row.nombre)
        assertEquals("lechuga romana", row.nombre_normalizado)
        assertTrue(repository.agregarIngrediente(ingredient(name = "LECHUGA ROMANA")).exceptionOrNull() is IngredienteDuplicadoException)
        assertEquals(1, dao.rows.size)
    }

    @Test
    fun updatesAndDeletesOnlyRowsOwnedByCurrentSession() = runBlocking {
        val id = repository.agregarIngrediente(ingredient()).getOrThrow()
        val changed = ingredient(id = id, quantity = 3.5, name = "Tomate saladet")
        assertTrue(repository.actualizarIngrediente(changed).isSuccess)
        assertEquals(3.5, dao.rows.single().cantidad, 0.0)

        session.setUser(2)
        assertTrue(repository.actualizarIngrediente(changed.copy(nombre = "Robado")).isFailure)
        assertEquals("Tomate saladet", dao.rows.single().nombre)
        assertTrue(repository.eliminarIngrediente(id).isFailure)
        assertEquals(1, dao.rows.size)

        session.setUser(1)
        assertTrue(repository.eliminarIngrediente(id).isSuccess)
        assertTrue(dao.rows.isEmpty())
    }

    @Test
    fun queriesAreScopedAndSupportSearchAndCategory() = runBlocking {
        repository.agregarIngrediente(ingredient(name = "Manzana", category = "Frutas")).getOrThrow()
        repository.agregarIngrediente(ingredient(name = "Tomate", category = "Verduras")).getOrThrow()
        dao.seed(ingredientRow(id = 20, userId = 2, name = "Manzana gala", category = "Frutas"))

        assertEquals(listOf("Manzana"), repository.observarIngredientes(" manzana ", "Frutas").first().map { it.nombre })
        session.setUser(2)
        assertEquals(listOf("Manzana gala"), repository.observarIngredientes("manzana").first().map { it.nombre })
    }

    @Test
    fun rejectsBlankNameInvalidQuantityAndInvalidUnit() = runBlocking {
        assertFalse(repository.agregarIngrediente(ingredient(name = "  ")).isSuccess)
        assertFalse(repository.agregarIngrediente(ingredient(quantity = 0.0)).isSuccess)
        assertFalse(repository.agregarIngrediente(ingredient(quantity = Double.NaN)).isSuccess)
        assertFalse(repository.agregarIngrediente(ingredient(unit = "caja inventada")).isSuccess)
        assertTrue(dao.rows.isEmpty())
    }

    @Test
    fun missingOrDisabledSessionCannotWriteInventory() = runBlocking {
        session.setUser(null)
        assertTrue(repository.agregarIngrediente(ingredient()).isFailure)
        session.setUser(2)
        users.rows[2] = activeUser(2).copy(estado = false)
        assertTrue(repository.agregarIngrediente(ingredient()).isFailure)
        assertTrue(dao.rows.isEmpty())
    }

    private fun ingredient(
        id: Long = 0,
        name: String = "Tomate",
        quantity: Double = 2.0,
        unit: String = "pieza",
        category: String = "Verduras"
    ) = IngredienteInventario(id, name, quantity, unit, category)

    private fun ingredientRow(id: Long, userId: Int, name: String, category: String) =
        IngredienteInventarioEntity(id, userId, name, normalizedLabel(name), 1.0, "pieza", category, 1L)
}

private class FakeInventarioDao : InventarioDao {
    val rows = mutableListOf<IngredienteInventarioEntity>()
    private val updates = MutableStateFlow(0)
    private var nextId = 1L

    fun seed(row: IngredienteInventarioEntity) { rows += row; updates.value++ }

    override suspend fun insert(ingrediente: IngredienteInventarioEntity): Long {
        if (rows.any { it.id_usuario == ingrediente.id_usuario && it.nombre_normalizado == ingrediente.nombre_normalizado }) {
            error("UNIQUE constraint failed: inventario_ingrediente.id_usuario, inventario_ingrediente.nombre_normalizado")
        }
        val id = nextId++
        rows += ingrediente.copy(id_ingrediente_inventario = id)
        updates.value++
        return id
    }

    override suspend fun getByNormalizedName(userId: Int, nombreNormalizado: String) =
        rows.firstOrNull { it.id_usuario == userId && it.nombre_normalizado == nombreNormalizado }

    override suspend fun updateOwned(id: Long, userId: Int, nombre: String, nombreNormalizado: String, cantidad: Double, unidad: String, categoria: String): Int {
        val index = rows.indexOfFirst { it.id_ingrediente_inventario == id && it.id_usuario == userId }
        if (index < 0 || rows.any { it.id_usuario == userId && it.id_ingrediente_inventario != id && it.nombre_normalizado == nombreNormalizado }) return 0
        rows[index] = rows[index].copy(nombre = nombre, nombre_normalizado = nombreNormalizado, cantidad = cantidad, unidad = unidad, categoria = categoria)
        updates.value++
        return 1
    }

    override suspend fun deleteOwned(id: Long, userId: Int): Int {
        val removed = rows.removeAll { it.id_ingrediente_inventario == id && it.id_usuario == userId }
        if (removed) updates.value++
        return if (removed) 1 else 0
    }

    override fun observeForUser(userId: Int, query: String, category: String?): Flow<List<IngredienteInventarioEntity>> =
        updates.map {
            rows.filter { row ->
                row.id_usuario == userId && (query.isEmpty() || row.nombre_normalizado.contains(query)) &&
                    (category == null || row.categoria == category)
            }.sortedBy { row -> row.nombre_normalizado }
        }
}

private class FakeUsuarioDao(val rows: MutableMap<Int, UsuarioEntity>) : UsuarioDao {
    constructor(vararg users: Pair<Int, UsuarioEntity>) : this(users.toMap().toMutableMap())
    override suspend fun insertUsuario(usuario: UsuarioEntity): Long = error("Not used")
    override suspend fun updateUsuario(usuario: UsuarioEntity): Int = error("Not used")
    override suspend fun deleteUsuario(usuario: UsuarioEntity): Int = error("Not used")
    override suspend fun getUsuarioById(id: Int): UsuarioEntity? = rows[id]
    override suspend fun getUsuarioByCorreo(correo: String): UsuarioEntity? = null
    override fun getUsuarioByIdFlow(id: Int): Flow<UsuarioEntity?> = MutableStateFlow(rows[id])
    override fun getAllUsuarios(): Flow<List<UsuarioEntity>> = MutableStateFlow(rows.values.toList())
}

private class FakeSessionStore(initialUserId: Int?) : SessionStore {
    private val userId = MutableStateFlow(initialUserId)
    fun setUser(value: Int?) { userId.value = value }
    override suspend fun saveUserId(userId: Int) { this.userId.value = userId }
    override suspend fun getUserId(): Int? = userId.value
    override fun observeUserId(): Flow<Int?> = userId
    override suspend fun clear() { userId.value = null }
}

private fun activeUser(id: Int) = UsuarioEntity(
    id_usuario = id,
    nombre = "User $id",
    correo = "user$id@example.com",
    contrasena_hash = "test-hash",
    fecha_registro = "2026-11-26"
)
