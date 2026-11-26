package com.npcg.practica1.data.repository

import com.npcg.practica1.data.local.dao.UsuarioDao
import com.npcg.practica1.data.mapper.toDomain
import com.npcg.practica1.data.mapper.toEntity
import com.npcg.practica1.domain.model.AccountDisabledException
import com.npcg.practica1.domain.model.EmailAlreadyRegisteredException
import com.npcg.practica1.domain.model.InvalidCredentialsException
import com.npcg.practica1.domain.model.PasswordPolicy
import com.npcg.practica1.domain.model.Usuario
import com.npcg.practica1.domain.model.WeakPasswordException
import com.npcg.practica1.domain.repository.UsuarioRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject

/** Repositorio de usuarios; el hash solo se manipula en la capa de datos. */
class UsuarioRepositoryImpl @Inject constructor(
    private val usuarioDao: UsuarioDao
) : UsuarioRepository {

    override suspend fun registrarUsuario(usuario: Usuario, contrasenaPlana: String): Result<Usuario> {
        return try {
            val email = usuario.correo.trim().lowercase(Locale.ROOT)
            require(usuario.nombre.isNotBlank() && email.isNotBlank()) {
                "El nombre y el correo son obligatorios"
            }
            require(EMAIL_PATTERN.matches(email)) { "El correo electrónico no es válido" }
            if (!PasswordPolicy.isStrong(contrasenaPlana)) {
                return Result.failure(WeakPasswordException())
            }
            if (usuarioDao.getUsuarioByCorreo(email) != null) {
                return Result.failure(EmailAlreadyRegisteredException())
            }
            val hashed = hashPassword(contrasenaPlana)
            val id = usuarioDao.insertUsuario(usuario.copy(correo = email).toEntity(hashed))
            val created = usuarioDao.getUsuarioById(id.toInt())
                ?: return Result.failure(IllegalStateException("No se pudo consultar el usuario creado"))
            Result.success(created.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun iniciarSesion(correo: String, contrasenaPlana: String): Result<Usuario> {
        return try {
            val entity = usuarioDao.getUsuarioByCorreo(correo.trim())
                ?: return Result.failure(InvalidCredentialsException())
            if (!entity.estado) {
                return Result.failure(AccountDisabledException())
            }
            if (!verifyPassword(contrasenaPlana, entity.contrasena_hash)) {
                return Result.failure(InvalidCredentialsException())
            }
            Result.success(entity.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun obtenerUsuarioPorId(id: Int): Result<Usuario?> = try {
        Result.success(usuarioDao.getUsuarioById(id)?.toDomain())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun obtenerUsuarioPorCorreo(correo: String): Result<Usuario?> = try {
        Result.success(usuarioDao.getUsuarioByCorreo(correo.trim())?.toDomain())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun obtenerTodosLosUsuarios(): Flow<List<Usuario>> =
        usuarioDao.getAllUsuarios().map { rows -> rows.map { it.toDomain() } }

    override suspend fun actualizarUsuario(usuario: Usuario, nuevaContrasenaPlana: String?): Result<Unit> {
        return try {
            val current = usuarioDao.getUsuarioById(usuario.idUsuario)
                ?: return Result.failure(NoSuchElementException("El usuario no existe"))
            val hash = when {
                nuevaContrasenaPlana == null -> current.contrasena_hash
                PasswordPolicy.isStrong(nuevaContrasenaPlana) -> hashPassword(nuevaContrasenaPlana)
                else -> return Result.failure(WeakPasswordException())
            }
            if (usuarioDao.updateUsuario(usuario.toEntity(hash)) == 0) {
                return Result.failure(NoSuchElementException("El usuario no existe"))
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun eliminarUsuario(usuario: Usuario): Result<Unit> {
        return try {
            val entity = usuarioDao.getUsuarioById(usuario.idUsuario)
                ?: return Result.failure(NoSuchElementException("El usuario no existe"))
            if (usuarioDao.deleteUsuario(entity) == 0) {
                return Result.failure(NoSuchElementException("El usuario no existe"))
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val ITERATIONS = 600_000
        private const val MIN_ACCEPTED_ITERATIONS = 100_000
        private const val MAX_ACCEPTED_ITERATIONS = 1_000_000
        private const val KEY_LENGTH_BITS = 256
        private const val HASH_SIZE_BYTES = KEY_LENGTH_BITS / 8
        private const val SALT_SIZE_BYTES = 16
        private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        private val HEX_PATTERN = Regex("^[0-9a-fA-F]+$")

        internal fun hashPassword(password: String): String {
            val salt = ByteArray(SALT_SIZE_BYTES)
            val passwordChars = password.toCharArray()
            SecureRandom().nextBytes(salt)
            val keySpec = PBEKeySpec(passwordChars, salt, ITERATIONS, KEY_LENGTH_BITS)
            return try {
                val derivedKey = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(keySpec).encoded
                try {
                    "pbkdf2:$ITERATIONS:${salt.toHex()}:${derivedKey.toHex()}"
                } finally {
                    derivedKey.fill(0)
                }
            } finally {
                keySpec.clearPassword()
                passwordChars.fill('\u0000')
                salt.fill(0)
            }
        }

        internal fun verifyPassword(password: String, storedValue: String): Boolean {
            val parts = storedValue.split(':')
            if (parts.size != 4 || parts[0] != "pbkdf2") return false
            val iterations = parts[1].toIntOrNull() ?: return false
            if (iterations !in MIN_ACCEPTED_ITERATIONS..MAX_ACCEPTED_ITERATIONS) return false
            val saltHex = parts[2]
            val hashHex = parts[3]
            if (saltHex.length != SALT_SIZE_BYTES * 2 || hashHex.length != HASH_SIZE_BYTES * 2) return false
            if (!HEX_PATTERN.matches(saltHex) || !HEX_PATTERN.matches(hashHex)) return false

            val salt = saltHex.hexToBytes()
            val expectedKey = hashHex.hexToBytes()
            val passwordChars = password.toCharArray()
            val keySpec = PBEKeySpec(passwordChars, salt, iterations, KEY_LENGTH_BITS)
            return try {
                val actualKey = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(keySpec).encoded
                try {
                    MessageDigest.isEqual(actualKey, expectedKey)
                } finally {
                    actualKey.fill(0)
                }
            } finally {
                keySpec.clearPassword()
                passwordChars.fill('\u0000')
                salt.fill(0)
                expectedKey.fill(0)
            }
        }

        private fun String.hexToBytes(): ByteArray = ByteArray(length / 2) { index ->
            substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }

        private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    }
}
