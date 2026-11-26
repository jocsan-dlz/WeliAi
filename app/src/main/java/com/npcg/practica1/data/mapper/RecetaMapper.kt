package com.npcg.practica1.data.mapper

import com.npcg.practica1.data.local.entity.RecetaEntity
import com.npcg.practica1.domain.model.Receta

/**
 * Mappers para convertir entre RecetaEntity (Room) y Receta (Domain).
 */
fun RecetaEntity.toDomain(): Receta {
    return Receta(
        idReceta = id_receta,
        idUsuario = id_usuario,
        nombre = nombre,
        descripcion = descripcion,
        instrucciones = instrucciones,
        tiempoPreparacion = tiempo_preparacion,
        presupuesto = presupuesto,
        fechaCreacion = fecha_creacion
    )
}

fun Receta.toEntity(): RecetaEntity {
    return RecetaEntity(
        id_receta = idReceta,
        id_usuario = idUsuario,
        nombre = nombre,
        descripcion = descripcion,
        instrucciones = instrucciones,
        tiempo_preparacion = tiempoPreparacion,
        presupuesto = presupuesto,
        fecha_creacion = fechaCreacion
    )
}
