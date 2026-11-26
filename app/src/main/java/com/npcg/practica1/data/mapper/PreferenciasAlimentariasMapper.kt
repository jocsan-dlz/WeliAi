package com.npcg.practica1.data.mapper

import com.npcg.practica1.data.local.entity.PreferenciaAlimentariaEntity
import com.npcg.practica1.data.local.entity.PreferenciaItemEntity
import com.npcg.practica1.domain.model.PresupuestosAlimentarios
import com.npcg.practica1.domain.model.PreferenciasAlimentarias
import com.npcg.practica1.domain.model.TiposAlimentacion

private const val ITEM_RESTRICCION = "restriccion"
private const val ITEM_ALERGIA = "alergia"

fun toPreferenciasDomain(
    preference: PreferenciaAlimentariaEntity?,
    items: List<PreferenciaItemEntity>
): PreferenciasAlimentarias = PreferenciasAlimentarias(
    tipoAlimentacion = preference?.tipo_alimentacion ?: TiposAlimentacion.TRADICIONAL,
    presupuesto = preference?.presupuesto ?: PresupuestosAlimentarios.CERO_GASTO,
    restricciones = items.filter { it.tipo == ITEM_RESTRICCION }.map { it.nombre },
    alergias = items.filter { it.tipo == ITEM_ALERGIA }.map { it.nombre },
    porcionesHabituales = preference?.porciones ?: 2
)

fun PreferenciasAlimentarias.toEntity(userId: Int, updatedAt: Long) =
    PreferenciaAlimentariaEntity(
        id_usuario = userId,
        tipo_alimentacion = tipoAlimentacion,
        presupuesto = presupuesto,
        porciones = porcionesHabituales,
        fecha_actualizacion = updatedAt
    )

fun PreferenciasAlimentarias.toItems(userId: Int, normalize: (String) -> String): List<PreferenciaItemEntity> =
    (restricciones.map { it.trim() to ITEM_RESTRICCION } + alergias.map { it.trim() to ITEM_ALERGIA })
        .map { (name, type) ->
            PreferenciaItemEntity(
                id_usuario = userId,
                tipo = type,
                nombre = name,
                nombre_normalizado = normalize(name)
            )
        }
