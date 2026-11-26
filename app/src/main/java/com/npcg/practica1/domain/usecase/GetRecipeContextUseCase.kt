package com.npcg.practica1.domain.usecase

import com.npcg.practica1.domain.model.ContextoGeneracionReceta
import com.npcg.practica1.domain.repository.InventarioRepository
import com.npcg.practica1.domain.repository.PreferenciasAlimentariasRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetRecipeContextUseCase @Inject constructor(
    private val inventarioRepository: InventarioRepository,
    private val preferenciasRepository: PreferenciasAlimentariasRepository
) {
    suspend operator fun invoke(): Result<ContextoGeneracionReceta> = try {
        val ingredients = inventarioRepository.observarIngredientes().first()
        val preferences = preferenciasRepository.observarPreferencias().first()
        Result.success(ContextoGeneracionReceta(ingredients, preferences))
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }
}
