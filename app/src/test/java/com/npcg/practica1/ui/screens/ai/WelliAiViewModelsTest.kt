package com.npcg.practica1.ui.screens.ai

import com.npcg.practica1.domain.model.AiRecipeResult
import com.npcg.practica1.domain.model.ChatRole
import com.npcg.practica1.domain.model.ContextoGeneracionReceta
import com.npcg.practica1.domain.model.IngredienteInventario
import com.npcg.practica1.domain.model.PreferenciasAlimentarias
import com.npcg.practica1.domain.repository.InventarioRepository
import com.npcg.practica1.domain.repository.PreferenciasAlimentariasRepository
import com.npcg.practica1.domain.repository.WelliAiRepository
import com.npcg.practica1.domain.usecase.GenerateRecipeUseCase
import com.npcg.practica1.domain.usecase.GetRecipeContextUseCase
import com.npcg.practica1.domain.usecase.SendWelliChatMessageUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WelliAiViewModelsTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
    private val ingredients = listOf(
        IngredienteInventario(1, "Frijol", 500.0, "g", "Legumbres"),
        IngredienteInventario(2, "Tomate", 3.0, "pieza", "Verduras")
    )
    private val preferences = PreferenciasAlimentarias(
        tipoAlimentacion = "Vegetariana",
        restricciones = listOf("Sin gluten"),
        alergias = listOf("Cacahuate"),
        porcionesHabituales = 4
    )

    @Before fun setMainDispatcher() = Dispatchers.setMain(dispatcher)
    @After fun resetMainDispatcher() = Dispatchers.resetMain()

    @Test fun recipeGeneratorRejectsEmptySelectionAndSendsSelectedInventoryContext() = runTest(dispatcher.scheduler) {
        val ai = FakeWelliAiRepository()
        val vm = RecipeGeneratorViewModel(
            GetRecipeContextUseCase(FakeInventoryRepository(ingredients), FakePreferencesRepository(preferences)),
            GenerateRecipeUseCase(
                GetRecipeContextUseCase(FakeInventoryRepository(ingredients), FakePreferencesRepository(preferences)),
                ai
            )
        )
        advanceUntilIdle()
        assertEquals(2, vm.state.value.ingredients.size)

        vm.generate()
        assertTrue(vm.state.value.generation is RecipeGenerationState.Error)
        assertEquals(0, ai.recipeRequests)

        vm.toggleIngredient(1)
        vm.onBudgetSelected("Ahorro")
        vm.generate()
        vm.generate()
        advanceUntilIdle()

        assertEquals(1, ai.recipeRequests)
        assertEquals(listOf(ingredients.first()), ai.lastContext?.ingredientesDisponibles)
        assertEquals("Ahorro", ai.lastContext?.preferencias?.presupuesto)
        assertEquals(500.0, ai.lastContext?.ingredientesDisponibles?.single()?.cantidad)
        assertEquals("g", ai.lastContext?.ingredientesDisponibles?.single()?.unidad)
        assertEquals("Legumbres", ai.lastContext?.ingredientesDisponibles?.single()?.categoria)
        assertEquals("Vegetariana", ai.lastContext?.preferencias?.tipoAlimentacion)
        assertEquals(listOf("Sin gluten"), ai.lastContext?.preferencias?.restricciones)
        assertEquals(listOf("Cacahuate"), ai.lastContext?.preferencias?.alergias)
        assertEquals(4, ai.lastContext?.preferencias?.porcionesHabituales)
        assertTrue(vm.state.value.generation is RecipeGenerationState.Success)
    }

    @Test fun chatBlocksBlankMessagesKeepsRecentTurnsAndCanReset() = runTest(dispatcher.scheduler) {
        val ai = FakeWelliAiRepository()
        val vm = WelliChatViewModel(SendWelliChatMessageUseCase(ai))

        vm.onDraftChange("   ")
        vm.send()
        advanceUntilIdle()
        assertEquals(0, ai.chatRequests)

        vm.onDraftChange("¿Cómo preparo frijoles?")
        vm.send()
        vm.send()
        advanceUntilIdle()
        assertEquals(1, ai.chatRequests)
        assertEquals(listOf(ChatRole.USER), ai.lastHistory?.map { it.role })
        assertEquals(listOf(ChatRole.USER, ChatRole.ASSISTANT), vm.state.value.messages.map { it.role })

        vm.startNewConversation()
        assertTrue(vm.state.value.messages.isEmpty())
        assertFalse(vm.state.value.sending)
    }

    @Test fun chatRequestRetainsContextWithAlternatingRoles() = runTest(dispatcher.scheduler) {
        val ai = FakeWelliAiRepository()
        val useCase = SendWelliChatMessageUseCase(ai)
        val history = listOf(
            com.npcg.practica1.domain.model.WelliChatMessage(1, ChatRole.USER, "Tengo arroz"),
            com.npcg.practica1.domain.model.WelliChatMessage(2, ChatRole.ASSISTANT, "Puedes prepararlo con verduras."),
            com.npcg.practica1.domain.model.WelliChatMessage(3, ChatRole.USER, "¿Cuánto tiempo?")
        )
        val result = useCase(history)
        assertTrue(result.isSuccess)
        assertEquals(history, ai.lastHistory)
    }

    private class FakeInventoryRepository(private val values: List<IngredienteInventario>) : InventarioRepository {
        override fun observarIngredientes(busqueda: String, categoria: String?): Flow<List<IngredienteInventario>> = flowOf(values)
        override suspend fun agregarIngrediente(ingrediente: IngredienteInventario): Result<Long> = Result.success(1L)
        override suspend fun actualizarIngrediente(ingrediente: IngredienteInventario): Result<Unit> = Result.success(Unit)
        override suspend fun eliminarIngrediente(id: Long): Result<Unit> = Result.success(Unit)
    }

    private class FakePreferencesRepository(private val preferences: PreferenciasAlimentarias) : PreferenciasAlimentariasRepository {
        override fun observarPreferencias(): Flow<PreferenciasAlimentarias> = flowOf(preferences)
        override suspend fun guardarPreferencias(preferencias: PreferenciasAlimentarias): Result<Unit> = Result.success(Unit)
    }

    private class FakeWelliAiRepository : WelliAiRepository {
        var recipeRequests = 0
        var chatRequests = 0
        var lastContext: ContextoGeneracionReceta? = null
        var lastHistory: List<com.npcg.practica1.domain.model.WelliChatMessage>? = null

        override suspend fun generateRecipe(context: ContextoGeneracionReceta): Result<AiRecipeResult> {
            recipeRequests++
            lastContext = context
            return Result.success(AiRecipeResult(false, "No hay suficientes ingredientes.", null))
        }

        override suspend fun sendMessage(history: List<com.npcg.practica1.domain.model.WelliChatMessage>): Result<String> {
            chatRequests++
            lastHistory = history
            return Result.success("Cuece a fuego medio hasta que esté suave.")
        }
    }
}
