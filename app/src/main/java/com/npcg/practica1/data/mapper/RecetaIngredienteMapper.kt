package com.npcg.practica1.data.mapper

import com.npcg.practica1.data.local.entity.RecetaIngredienteEntity
import com.npcg.practica1.domain.model.RecetaIngrediente

/**
 * Mappers para convertir entre RecetaIngredienteEntity (Room) y RecetaIngrediente (Domain).
 */
fun RecetaIngredienteEntity.toDomain(): RecetaIngrediente {
    return RecetaIngrediente(
        idRecetaIngrediente = id_receta_ingrediente,
        idReceta = id_receta,
        idIngrediente = id_ingrediente,
        cantidad = cantidad,
        unidad = unidad
    )
}

fun RecetaIngrediente.toEntity(): RecetaIngredienteEntity {
    return RecetaIngredienteEntity(
        id_receta_ingrediente = idRecetaIngrediente,
        id_receta = idReceta,
        id_ingrediente = idIngrediente,
        cantidad = cantidad,
        unidad = unidad
    )
}
