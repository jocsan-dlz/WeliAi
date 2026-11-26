package com.npcg.practica1.domain.usecase

import com.npcg.practica1.domain.model.AiInputException
import com.npcg.practica1.domain.model.AiRecipeResult
import com.npcg.practica1.domain.model.ContextoGeneracionReceta
import com.npcg.practica1.domain.model.IngredienteInventario
import com.npcg.practica1.domain.model.PresupuestosAlimentarios
import com.npcg.practica1.domain.repository.WelliAiRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class GenerateRecipeUseCase @Inject constructor(
    private val contextUseCase: GetRecipeContextUseCase,
    private val aiRepository: WelliAiRepository
) {
    suspend operator fun invoke(
        selectedIngredients: List<IngredienteInventario>,
        budget: String
    ): Result<AiRecipeResult> = try {
        require(budget in PresupuestosAlimentarios.all) { throw AiInputException("Elige un presupuesto válido.") }
        if (selectedIngredients.isEmpty()) throw AiInputException("Selecciona al menos un ingrediente de tu inventario.")
        val savedContext = contextUseCase().getOrElse { throw it }
        val savedIds = savedContext.ingredientesDisponibles.map { it.id }.toSet()
        if (selectedIngredients.any { it.id !in savedIds }) throw AiInputException("Actualiza el inventario e inténtalo otra vez.")
        val context = ContextoGeneracionReceta(
            ingredientesDisponibles = selectedIngredients.distinctBy { it.id },
            preferencias = savedContext.preferencias.copy(presupuesto = budget)
        )
        aiRepository.generateRecipe(context)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }
}
