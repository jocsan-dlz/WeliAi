package com.npcg.practica1.data.mapper

import com.npcg.practica1.data.local.entity.UsuarioEntity
import com.npcg.practica1.domain.model.Usuario

/**
 * Mappers para convertir entre UsuarioEntity (Room) y Usuario (Domain).
 */
fun UsuarioEntity.toDomain(): Usuario {
    return Usuario(
        idUsuario = id_usuario,
        nombre = nombre,
        correo = correo,
        fechaRegistro = fecha_registro,
        estado = estado
    )
}

fun Usuario.toEntity(contrasenaHash: String): UsuarioEntity {
    return UsuarioEntity(
        id_usuario = idUsuario,
        nombre = nombre,
        correo = correo,
        contrasena_hash = contrasenaHash,
        fecha_registro = fechaRegistro,
        estado = estado
    )
}
