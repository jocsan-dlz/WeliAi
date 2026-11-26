package com.npcg.practica1

import com.npcg.practica1.data.local.dao.UsuarioDao
import com.npcg.practica1.data.local.entity.UsuarioEntity
import com.npcg.practica1.data.repository.UsuarioRepositoryImpl
import com.npcg.practica1.domain.repository.SessionStore
import com.npcg.practica1.domain.model.AccountDisabledException
import com.npcg.practica1.domain.model.EmailAlreadyRegisteredException
import com.npcg.practica1.domain.model.InvalidCredentialsException
import com.npcg.practica1.domain.model.Usuario
import com.npcg.practica1.domain.model.WeakPasswordException
import com.npcg.practica1.ui.screens.auth.RegisterViewModel
import com.npcg.practica1.ui.screens.auth.LoginViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsuarioRepositoryAuthTest {
    private val dao = InMemoryUsuarioDao()
    private val repository = UsuarioRepositoryImpl(dao)

    @Test
    fun registrationStoresOnlyPasswordHashAndRejectsDuplicateEmail() = runBlocking {
        val created = repository.registrarUsuario(user(), "StrongPass123")

        assertTrue(created.isSuccess)
        val saved = dao.rows.values.single()
        assertTrue(saved.contrasena_hash.startsWith("pbkdf2:600000:"))
        assertFalse(saved.contrasena_hash.contains("StrongPass123"))
        assertTrue(repository.registrarUsuario(user(correo = "USER@example.com"), "StrongPass123").exceptionOrNull()
            is EmailAlreadyRegisteredException)
    }

    @Test
    fun registrationRejectsEmptyFieldsInvalidEmailAndWeakPassword() = runBlocking {
        assertTrue(repository.registrarUsuario(user(nombre = " "), "StrongPass123").isFailure)
        assertTrue(repository.registrarUsuario(user(correo = "not-an-email"), "StrongPass123").isFailure)
        assertTrue(repository.registrarUsuario(user(), "short").exceptionOrNull() is WeakPasswordException)
    }

    @Test
    fun loginAcceptsMatchingPasswordAndRejectsWrongOrMissingCredentials() = runBlocking {
        repository.registrarUsuario(user(), "StrongPass123")

        assertTrue(repository.iniciarSesion("user@example.com", "StrongPass123").isSuccess)
        assertTrue(repository.iniciarSesion("user@example.com", "WrongPass123").exceptionOrNull()
            is InvalidCredentialsException)
        assertTrue(repository.iniciarSesion("missing@example.com", "StrongPass123").exceptionOrNull()
            is InvalidCredentialsException)
    }

    @Test
    fun disabledAccountCannotLogin() = runBlocking {
        repository.registrarUsuario(user(), "StrongPass123")
        dao.rows[1] = dao.rows.getValue(1).copy(estado = false)

        assertTrue(repository.iniciarSesion("user@example.com", "StrongPass123").exceptionOrNull()
            is AccountDisabledException)
    }

    @Test
    fun databaseErrorsAreReturnedAsFailures() = runBlocking {
        dao.failure = IllegalStateException("database unavailable")

        val result = repository.iniciarSesion("user@example.com", "StrongPass123")

        assertTrue(result.isFailure)
        assertEquals("database unavailable", result.exceptionOrNull()?.message)
    }

    @Test
    fun registrationViewModelRejectsEmptyFieldsInvalidEmailAndMismatchedPasswords() {
        val viewModel = RegisterViewModel(repository)
        viewModel.register {}
        assertEquals(R.string.error_empty_fields, viewModel.errorMessageResId)

        viewModel.onNameChange("User")
        viewModel.onEmailChange("invalid")
        viewModel.onPasswordChange("StrongPass123")
        viewModel.onConfirmPasswordChange("StrongPass123")
        viewModel.register {}
        assertEquals(R.string.error_invalid_email, viewModel.errorMessageResId)

        viewModel.onEmailChange("user@example.com")
        viewModel.onConfirmPasswordChange("Different123")
        viewModel.register {}
        assertEquals(R.string.error_password_mismatch, viewModel.errorMessageResId)
    }

    @Test
    fun loginViewModelRejectsEmptyAndInvalidEmailWithoutRepositoryCall() {
        val viewModel = LoginViewModel(repository, NoOpSessionStore)
        viewModel.validateAndLogin {}
        assertEquals(R.string.error_empty_fields, viewModel.errorMessageResId)

        viewModel.onEmailChange("invalid")
        viewModel.onPasswordChange("Secret123")
        viewModel.validateAndLogin {}
        assertEquals(R.string.error_invalid_email, viewModel.errorMessageResId)
    }

    private object NoOpSessionStore : SessionStore {
        override suspend fun saveUserId(userId: Int) = Unit
        override suspend fun getUserId(): Int? = null
        override fun observeUserId(): Flow<Int?> = flowOf(null)
        override suspend fun clear() = Unit
    }

    private fun user(
        nombre: String = "Test User",
        correo: String = "user@example.com"
    ) = Usuario(
        nombre = nombre,
        correo = correo,
        fechaRegistro = "2026-11-26"
    )
}

private class InMemoryUsuarioDao : UsuarioDao {
    val rows = linkedMapOf<Int, UsuarioEntity>()
    var failure: Exception? = null
    private var nextId = 1

    private fun failIfRequested() {
        failure?.let { throw it }
    }

    override suspend fun insertUsuario(usuario: UsuarioEntity): Long {
        failIfRequested()
        val id = nextId++
        rows[id] = usuario.copy(id_usuario = id)
        return id.toLong()
    }

    override suspend fun updateUsuario(usuario: UsuarioEntity): Int {
        failIfRequested()
        if (!rows.containsKey(usuario.id_usuario)) return 0
        rows[usuario.id_usuario] = usuario
        return 1
    }

    override suspend fun deleteUsuario(usuario: UsuarioEntity): Int {
        failIfRequested()
        return if (rows.remove(usuario.id_usuario) != null) 1 else 0
    }

    override suspend fun getUsuarioById(id: Int): UsuarioEntity? {
        failIfRequested()
        return rows[id]
    }

    override suspend fun getUsuarioByCorreo(correo: String): UsuarioEntity? {
        failIfRequested()
        return rows.values.firstOrNull { it.correo == correo }
    }

    override fun getUsuarioByIdFlow(id: Int): Flow<UsuarioEntity?> = flowOf(rows[id])

    override fun getAllUsuarios(): Flow<List<UsuarioEntity>> = flowOf(rows.values.toList())
}
