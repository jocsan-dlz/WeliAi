package com.npcg.practica1.data.repository

import com.npcg.practica1.data.local.dao.UsuarioDao
import com.npcg.practica1.data.mapper.toDomain
import com.npcg.practica1.data.mapper.toEntity
import com.npcg.practica1.domain.model.Usuario
import com.npcg.practica1.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Implementación del repositorio de Usuarios utilizando Room DAO y PBKDF2-HMAC-SHA256 con Salt.
 */
class UsuarioRepositoryImpl(
    private val usuarioDao: UsuarioDao
) : UsuarioRepository {

    override suspend fun registrarUsuario(usuario: Usuario, contrasenaPlana: String): Result<Usuario> {
        return try {
            val exist = usuarioDao.getUsuarioByCorreo(usuario.correo)
            if (exist != null) {
                return Result.failure(IllegalArgumentException("El correo electrónico ya está registrado"))
            }
            val hashed = hashPassword(contrasenaPlana)
            val entity = usuario.toEntity(contrasenaHash = hashed)
            val id = usuarioDao.insertUsuario(entity)
            val created = usuarioDao.getUsuarioById(id.toInt())?.toDomain()
                ?: return Result.failure(IllegalStateException("Error al consultar el usuario recién creado"))
            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun iniciarSesion(correo: String, contrasenaPlana: String): Result<Usuario> {
        return try {
            val userEntity = usuarioDao.getUsuarioByCorreo(correo.trim())
                ?: return Result.failure(IllegalArgumentException("Usuario o contraseña incorrectos"))

            if (!userEntity.estado) {
                return Result.failure(IllegalStateException("La cuenta de usuario se encuentra inactiva"))
            }

            if (verifyPassword(contrasenaPlana, userEntity.contrasena_hash)) {
                Result.success(userEntity.toDomain())
            } else {
                Result.failure(IllegalArgumentException("Usuario o contraseña incorrectos"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun obtenerUsuarioPorId(id: Int): Result<Usuario?> {
        return try {
            val user = usuarioDao.getUsuarioById(id)?.toDomain()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun obtenerUsuarioPorCorreo(correo: String): Result<Usuario?> {
        return try {
            val user = usuarioDao.getUsuarioByCorreo(correo.trim())?.toDomain()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun obtenerTodosLosUsuarios(): Flow<List<Usuario>> {
        return usuarioDao.getAllUsuarios().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun actualizarUsuario(usuario: Usuario, nuevaContrasenaPlana: String?): Result<Unit> {
        return try {
            val currentEntity = usuarioDao.getUsuarioById(usuario.idUsuario)
                ?: return Result.failure(IllegalArgumentException("El usuario no existe"))

            val hashToUse = if (!nuevaContrasenaPlana.isNullOrBlank()) {
                hashPassword(nuevaContrasenaPlana)
            } else {
                currentEntity.contrasena_hash
            }

            val updatedEntity = usuario.toEntity(contrasenaHash = hashToUse)
            usuarioDao.updateUsuario(updatedEntity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun eliminarUsuario(usuario: Usuario): Result<Unit> {
        return try {
            val entity = usuarioDao.getUsuarioById(usuario.idUsuario)
                ?: return Result.failure(IllegalArgumentException("El usuario no existe"))
            usuarioDao.deleteUsuario(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val ITERATIONS = 20000
        private const val KEY_LENGTH = 256
        private const val SALT_SIZE = 16

        fun hashPassword(password: String): String {
            val random = SecureRandom()
            val salt = ByteArray(SALT_SIZE)
            random.nextBytes(salt)

            val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            val hash = factory.generateSecret(spec).encoded
            spec.clearPassword()

            val saltHex = salt.joinToString("") { "%02x".format(it) }
            val hashHex = hash.joinToString("") { "%02x".format(it) }

            return "pbkdf2:$ITERATIONS:$saltHex:$hashHex"
        }

        fun verifyPassword(password: String, storedValue: String): Boolean {
            if (!storedValue.startsWith("pbkdf2:")) {
                // Se rechaza completamente cualquier intento de comparación en texto plano o hash legado no seguro
                return false
            }

            val parts = storedValue.split(":")
            if (parts.size != 4) return false

            val iterations = parts[1].toIntOrNull() ?: return false
            if (iterations < 1000 || iterations > 100000) return false

            val saltHex = parts[2]
            val storedHashHex = parts[3]

            val salt = hexToBytes(saltHex)
            val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH)
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            val computedHash = factory.generateSecret(spec).encoded
            spec.clearPassword()

            val computedHashHex = computedHash.joinToString("") { "%02x".format(it) }

            return MessageDigest.isEqual(computedHashHex.toByteArray(), storedHashHex.toByteArray())
        }

        private fun hexToBytes(hex: String): ByteArray {
            val result = ByteArray(hex.length / 2)
            for (i in result.indices) {
                val index = i * 2
                result[i] = hex.substring(index, index + 2).toInt(16).toByte()
            }
            return result
        }
    }
}
