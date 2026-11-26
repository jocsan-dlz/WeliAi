package com.npcg.practica1

import com.npcg.practica1.data.local.dao.UsuarioDao
import com.npcg.practica1.data.local.entity.UsuarioEntity
import com.npcg.practica1.data.repository.UsuarioRepositoryImpl
import com.npcg.practica1.domain.model.Usuario
import com.npcg.practica1.domain.repository.SessionStore
import com.npcg.practica1.ui.screens.auth.AuthUiState
import com.npcg.practica1.ui.screens.auth.AuthError
import com.npcg.practica1.ui.screens.auth.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun validSessionIsRecoveredAndLogoutClearsStoredId() = runTest(dispatcher) {
        val user = Usuario(
            idUsuario = 1,
            nombre = "User",
            correo = "user@example.com",
            fechaRegistro = "2026-11-26"
        )
        val dao = SessionTestUsuarioDao(
            UsuarioEntity(
                id_usuario = user.idUsuario,
                nombre = user.nombre,
                correo = user.correo,
                contrasena_hash = "unused-in-domain",
                fecha_registro = user.fechaRegistro
            )
        )
        val sessionStore = InMemorySessionStore(1)
        val viewModel = AuthViewModel(UsuarioRepositoryImpl(dao), sessionStore)

        advanceUntilIdle()
        assertEquals(AuthUiState.SignedIn(user), viewModel.state.value)

        viewModel.logout()
        advanceUntilIdle()

        assertEquals(AuthUiState.SignedOut, viewModel.state.value)
        assertNull(sessionStore.getUserId())
    }

    @Test
    fun invalidOrDisabledSavedAccountIsCleared() = runTest(dispatcher) {
        val dao = SessionTestUsuarioDao(
            UsuarioEntity(
                id_usuario = 1,
                nombre = "User",
                correo = "user@example.com",
                contrasena_hash = "unused-in-domain",
                fecha_registro = "2026-11-26",
                estado = false
            )
        )
        val sessionStore = InMemorySessionStore(1)
        val viewModel = AuthViewModel(UsuarioRepositoryImpl(dao), sessionStore)

        advanceUntilIdle()

        assertEquals(AuthUiState.SignedOut, viewModel.state.value)
        assertNull(sessionStore.getUserId())
    }

    @Test
    fun sessionForMissingUserIsCleared() = runTest(dispatcher) {
        val dao = SessionTestUsuarioDao(
            UsuarioEntity(
                id_usuario = 2,
                nombre = "Other User",
                correo = "other@example.com",
                contrasena_hash = "unused-in-domain",
                fecha_registro = "2026-11-26"
            )
        )
        val sessionStore = InMemorySessionStore(1)
        val viewModel = AuthViewModel(UsuarioRepositoryImpl(dao), sessionStore)

        advanceUntilIdle()

        assertEquals(AuthUiState.SignedOut, viewModel.state.value)
        assertNull(sessionStore.getUserId())
    }

    @Test
    fun sessionPersistenceFailureDoesNotOpenPrivateScreen() = runTest(dispatcher) {
        val dao = SessionTestUsuarioDao(
            UsuarioEntity(
                id_usuario = 1,
                nombre = "User",
                correo = "user@example.com",
                contrasena_hash = "unused-in-domain",
                fecha_registro = "2026-11-26"
            )
        )
        val viewModel = AuthViewModel(UsuarioRepositoryImpl(dao), FailingSessionStore)

        advanceUntilIdle()

        assertEquals(AuthUiState.Error(AuthError.SESSION), viewModel.state.value)
    }

    @Test
    fun logoutPersistenceFailureDoesNotRestorePrivateScreen() = runTest(dispatcher) {
        val dao = SessionTestUsuarioDao(
            UsuarioEntity(
                id_usuario = 1,
                nombre = "User",
                correo = "user@example.com",
                contrasena_hash = "unused-in-domain",
                fecha_registro = "2026-11-26"
            )
        )
        val viewModel = AuthViewModel(UsuarioRepositoryImpl(dao), FailingLogoutStore)

        advanceUntilIdle()
        assertTrue(viewModel.state.value is AuthUiState.SignedIn)

        viewModel.logout()
        advanceUntilIdle()

        assertEquals(AuthUiState.Error(AuthError.LOGOUT), viewModel.state.value)
    }
}

private class InMemorySessionStore(
    private var userId: Int?
) : SessionStore {
    override suspend fun saveUserId(userId: Int) {
        this.userId = userId
    }

    override suspend fun getUserId(): Int? = userId

    override suspend fun clear() {
        userId = null
    }
}

private class SessionTestUsuarioDao(
    private val user: UsuarioEntity
) : UsuarioDao {
    override suspend fun insertUsuario(usuario: UsuarioEntity): Long = error("Not used")
    override suspend fun updateUsuario(usuario: UsuarioEntity): Int = error("Not used")
    override suspend fun deleteUsuario(usuario: UsuarioEntity): Int = error("Not used")
    override suspend fun getUsuarioById(id: Int): UsuarioEntity? = user.takeIf { it.id_usuario == id }
    override suspend fun getUsuarioByCorreo(correo: String): UsuarioEntity? = null
    override fun getUsuarioByIdFlow(id: Int): Flow<UsuarioEntity?> = flowOf(null)
    override fun getAllUsuarios(): Flow<List<UsuarioEntity>> = flowOf(emptyList())
}

private object FailingSessionStore : SessionStore {
    override suspend fun saveUserId(userId: Int) = error("Session storage failed")
    override suspend fun getUserId(): Int? = error("Session storage failed")
    override suspend fun clear() = error("Session storage failed")
}

private object FailingLogoutStore : SessionStore {
    override suspend fun saveUserId(userId: Int) = Unit
    override suspend fun getUserId(): Int? = 1
    override suspend fun clear() = error("Session storage failed")
}
