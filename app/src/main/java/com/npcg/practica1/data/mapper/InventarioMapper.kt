package com.npcg.practica1.data.mapper

import com.npcg.practica1.data.local.entity.IngredienteInventarioEntity
import com.npcg.practica1.domain.model.IngredienteInventario

fun IngredienteInventarioEntity.toDomain() = IngredienteInventario(
    id = id_ingrediente_inventario,
    nombre = nombre,
    cantidad = cantidad,
    unidad = unidad,
    categoria = categoria
)

fun IngredienteInventario.toEntity(userId: Int, normalizedName: String, registeredAt: Long) =
    IngredienteInventarioEntity(
        id_ingrediente_inventario = id,
        id_usuario = userId,
        nombre = nombre,
        nombre_normalizado = normalizedName,
        cantidad = cantidad,
        unidad = unidad,
        categoria = categoria,
        fecha_registro = registeredAt
    )
