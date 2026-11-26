package com.npcg.practica1.data.mapper

import com.npcg.practica1.data.local.entity.IngredienteEntity
import com.npcg.practica1.domain.model.Ingrediente

/**
 * Mappers para convertir entre IngredienteEntity (Room) y Ingrediente (Domain).
 */
fun IngredienteEntity.toDomain(): Ingrediente {
    return Ingrediente(
        idIngrediente = id_ingrediente,
        nombre = nombre,
        categoria = categoria,
        unidadMedida = unidad_medida
    )
}

fun Ingrediente.toEntity(): IngredienteEntity {
    return IngredienteEntity(
        id_ingrediente = idIngrediente,
        nombre = nombre,
        categoria = categoria,
        unidad_medida = unidadMedida
    )
}
