package com.npcg.practica1.domain.model

data class PreferenciasAlimentarias(
    val tipoAlimentacion: String = TiposAlimentacion.TRADICIONAL,
    val presupuesto: String = PresupuestosAlimentarios.CERO_GASTO,
    val restricciones: List<String> = emptyList(),
    val alergias: List<String> = emptyList(),
    val porcionesHabituales: Int = 2
)

object TiposAlimentacion {
    const val TRADICIONAL = "Tradicional"
    const val SALUDABLE = "Saludable"
    const val VEGETARIANA = "Vegetariana"
    const val VEGANA = "Vegana"
    const val OTRA = "Otra"
    val all = listOf(TRADICIONAL, SALUDABLE, VEGETARIANA, VEGANA, OTRA)
}

object PresupuestosAlimentarios {
    const val CERO_GASTO = "Cero gasto"
    const val AHORRO = "Ahorro"
    const val GOURMET = "Gourmet"
    val all = listOf(CERO_GASTO, AHORRO, GOURMET)
}

object RestriccionesAlimentarias {
    val sugeridas = listOf("Sin lactosa", "Sin gluten", "Baja en azúcar")
}
