package com.npcg.practica1.domain.model

data class IngredienteInventario(
    val id: Long = 0,
    val nombre: String,
    val cantidad: Double,
    val unidad: String,
    val categoria: String
)

object InventarioOpciones {
    val categorias = listOf(
        "Frutas", "Verduras", "Carnes", "Lácteos", "Cereales y granos",
        "Legumbres", "Condimentos", "Otros"
    )
    val unidades = listOf("pieza", "g", "kg", "ml", "l", "taza", "paquete")
}

class IngredienteDuplicadoException : IllegalArgumentException("Ese ingrediente ya está en tu inventario.")
class SesionNoValidaException : IllegalStateException("La sesión no está activa. Inicia sesión para continuar.")
