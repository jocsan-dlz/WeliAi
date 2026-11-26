package com.npcg.practica1

import com.npcg.practica1.data.repository.UsuarioRepositoryImpl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordSecurityTest {
    @Test
    fun hashUsesPbkdf2AndUniqueSalts() {
        val first = UsuarioRepositoryImpl.hashPassword("correct horse")
        val second = UsuarioRepositoryImpl.hashPassword("correct horse")

        assertTrue(first.matches(Regex("^pbkdf2:600000:[0-9a-f]{32}:[0-9a-f]{64}$")))
        assertNotEquals(first, second)
    }

    @Test
    fun verificationAcceptsOnlyTheMatchingPassword() {
        val encoded = UsuarioRepositoryImpl.hashPassword("welli password")

        assertTrue(UsuarioRepositoryImpl.verifyPassword("welli password", encoded))
        assertFalse(UsuarioRepositoryImpl.verifyPassword("different password", encoded))
    }

    @Test
    fun verificationRejectsPlainTextAndMalformedOrWeakHashes() {
        assertFalse(UsuarioRepositoryImpl.verifyPassword("secret", "secret"))
        assertFalse(UsuarioRepositoryImpl.verifyPassword("secret", "pbkdf2:210000:xyz:xyz"))
        assertFalse(UsuarioRepositoryImpl.verifyPassword("secret", "pbkdf2:20000:${"00".repeat(16)}:${"00".repeat(32)}"))
        assertFalse(UsuarioRepositoryImpl.verifyPassword("secret", "pbkdf2:1000001:${"00".repeat(16)}:${"00".repeat(32)}"))
    }
}
