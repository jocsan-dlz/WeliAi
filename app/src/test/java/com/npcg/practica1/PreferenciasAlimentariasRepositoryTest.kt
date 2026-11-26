package com.npcg.practica1

import com.npcg.practica1.data.local.dao.PreferenciasAlimentariasDao
import com.npcg.practica1.data.local.dao.UsuarioDao
import com.npcg.practica1.data.local.entity.PreferenciaAlimentariaEntity
import com.npcg.practica1.data.local.entity.PreferenciaItemEntity
import com.npcg.practica1.data.local.entity.UsuarioEntity
import com.npcg.practica1.data.repository.PreferenciasAlimentariasRepositoryImpl
import com.npcg.practica1.domain.model.PreferenciasAlimentarias
import com.npcg.practica1.domain.model.PresupuestosAlimentarios
import com.npcg.practica1.domain.model.TiposAlimentacion
import com.npcg.practica1.domain.repository.SessionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferenciasAlimentariasRepositoryTest {
    private val users = PreferencesFakeUsuarioDao(listOf(prefTestUser(1), prefTestUser(2)))
    private val store = PreferencesFakeSessionStore(1)
    private val dao = PreferencesFakeDao()
    private val repository = PreferenciasAlimentariasRepositoryImpl(dao, users, store)

    @Test
    fun savesAndReloadsFoodPreferencesIncludingSeparateAllergies() = runBlocking {
        val preferences = PreferenciasAlimentarias(
            tipoAlimentacion = TiposAlimentacion.VEGETARIANA,
            presupuesto = PresupuestosAlimentarios.AHORRO,
            restricciones = listOf("Sin gluten"),
            alergias = listOf("Nuez", "Maní"),
            porcionesHabituales = 4
        )
        assertTrue(repository.guardarPreferencias(preferences).isSuccess)

        val restartedRepository = PreferenciasAlimentariasRepositoryImpl(dao, users, store)
        assertEquals(preferences, restartedRepository.observarPreferencias().first())
        assertEquals(setOf("restriccion", "alergia"), dao.items.map { it.tipo }.toSet())
        assertEquals(2, dao.items.count { it.tipo == "alergia" })
    }

    @Test
    fun preferencesStaySeparatedByAuthenticatedUser() = runBlocking {
        repository.guardarPreferencias(PreferenciasAlimentarias(alergias = listOf("Almendra"))).getOrThrow()
        store.setUser(2)

        assertEquals(PreferenciasAlimentarias(), repository.observarPreferencias().first())
        repository.guardarPreferencias(PreferenciasAlimentarias(tipoAlimentacion = TiposAlimentacion.VEGANA)).getOrThrow()
        store.setUser(1)
        assertEquals(listOf("Almendra"), repository.observarPreferencias().first().alergias)
    }

    @Test
    fun validatesServingsAndDeduplicatesNamesWithoutMixingTypes() = runBlocking {
        assertTrue(repository.guardarPreferencias(PreferenciasAlimentarias(porcionesHabituales = 0)).isFailure)
        assertTrue(repository.guardarPreferencias(PreferenciasAlimentarias(porcionesHabituales = 21)).isFailure)

        repository.guardarPreferencias(
            PreferenciasAlimentarias(
                restricciones = listOf("SIN gluten", "Sin   gluten"),
                alergias = listOf("sin gluten")
            )
        ).getOrThrow()
        assertEquals(2, dao.items.size)
        assertEquals(1, dao.items.count { it.tipo == "restriccion" })
        assertEquals(1, dao.items.count { it.tipo == "alergia" })
    }
}

private class PreferencesFakeDao : PreferenciasAlimentariasDao {
    private val preferences = MutableStateFlow<Map<Int, PreferenciaAlimentariaEntity>>(emptyMap())
    private val itemRows = MutableStateFlow<List<PreferenciaItemEntity>>(emptyList())
    val items get() = itemRows.value

    override fun observeForUser(userId: Int): Flow<PreferenciaAlimentariaEntity?> = preferences.combine(itemRows) { rows, _ -> rows[userId] }
    override fun observeItemsForUser(userId: Int): Flow<List<PreferenciaItemEntity>> = itemRows.combine(preferences) { rows, _ -> rows.filter { it.id_usuario == userId } }
    override suspend fun upsert(preferencias: PreferenciaAlimentariaEntity) {
        preferences.value = preferences.value + (preferencias.id_usuario to preferencias)
    }
    override suspend fun deleteItemsForUser(userId: Int) { itemRows.value = itemRows.value.filterNot { it.id_usuario == userId } }
    override suspend fun insertItems(items: List<PreferenciaItemEntity>) { itemRows.value = itemRows.value + items }
    override suspend fun saveForUser(preferencias: PreferenciaAlimentariaEntity, items: List<PreferenciaItemEntity>) {
        upsert(preferencias)
        deleteItemsForUser(preferencias.id_usuario)
        insertItems(items)
    }
}

private class PreferencesFakeUsuarioDao(private val rows: List<UsuarioEntity>) : UsuarioDao {
    override suspend fun insertUsuario(usuario: UsuarioEntity): Long = error("Not used")
    override suspend fun updateUsuario(usuario: UsuarioEntity): Int = error("Not used")
    override suspend fun deleteUsuario(usuario: UsuarioEntity): Int = error("Not used")
    override suspend fun getUsuarioById(id: Int): UsuarioEntity? = rows.firstOrNull { it.id_usuario == id }
    override suspend fun getUsuarioByCorreo(correo: String): UsuarioEntity? = null
    override fun getUsuarioByIdFlow(id: Int): Flow<UsuarioEntity?> = MutableStateFlow(null)
    override fun getAllUsuarios(): Flow<List<UsuarioEntity>> = MutableStateFlow(rows)
}

private class PreferencesFakeSessionStore(initialId: Int?) : SessionStore {
    private val id = MutableStateFlow(initialId)
    fun setUser(userId: Int?) { id.value = userId }
    override suspend fun saveUserId(userId: Int) { id.value = userId }
    override suspend fun getUserId(): Int? = id.value
    override fun observeUserId(): Flow<Int?> = id
    override suspend fun clear() { id.value = null }
}

private fun prefTestUser(id: Int) = UsuarioEntity(
    id_usuario = id,
    nombre = "User $id",
    correo = "pref$id@example.com",
    contrasena_hash = "test-hash",
    fecha_registro = "2026-11-26"
)
