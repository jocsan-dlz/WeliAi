package com.npcg.welliai.backend

import com.npcg.welliai.backend.ai.OpenAiModelUnavailableException
import com.npcg.welliai.backend.ai.WelliAiAssistant
import com.npcg.welliai.backend.api.ChatMessageInput
import com.npcg.welliai.backend.api.ChatRequest
import com.npcg.welliai.backend.api.ChatResponse
import com.npcg.welliai.backend.api.IngredientInput
import com.npcg.welliai.backend.api.RecipeGenerationRequest
import com.npcg.welliai.backend.api.RecipeGenerationResponse
import com.npcg.welliai.backend.api.welliModule
import com.npcg.welliai.backend.security.InMemoryRateLimiter
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiRoutesTest {
    @Test
    fun recipeEndpointPassesValidatedPreferencesAndInventoryToService() = testApplication {
        val assistant = RecordingAssistant()
        application { welliModule(assistant) }

        val response = client.post("/api/v1/ai/recipes/generate") {
            contentType(ContentType.Application.Json)
            setBody("""
                {"ingredients":[{"name":"Tomate","quantity":2.0,"unit":"pieza","category":"Verduras"}],
                 "budget":"Cero gasto","foodType":"Vegetariana","restrictions":["Sin gluten"],
                 "allergies":["Nuez"],"servings":2}
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Tomate", assistant.recipeRequest?.ingredients?.single()?.name)
        assertEquals(listOf("Nuez"), assistant.recipeRequest?.allergies)
        assertEquals("Cero gasto", assistant.recipeRequest?.budget)
    }

    @Test
    fun chatValidatesJsonMessageAndRejectsOversizedBody() = testApplication {
        val assistant = RecordingAssistant()
        application { welliModule(assistant) }

        val empty = client.post("/api/v1/ai/chat") {
            contentType(ContentType.Application.Json)
            setBody("{\"messages\":[]}")
        }
        assertEquals(HttpStatusCode.BadRequest, empty.status)

        val oversizedJson = "{\"messages\":[{\"role\":\"user\",\"content\":\"${"x".repeat(17_000)}\"}]}"
        val oversized = client.post("/api/v1/ai/chat") {
            contentType(ContentType.Application.Json)
            setBody(oversizedJson)
        }
        assertEquals(HttpStatusCode.PayloadTooLarge, oversized.status)
    }

    @Test
    fun rateLimiterBlocksRepeatedRequestsAndLeavesHealthRouteAvailable() = testApplication {
        application { welliModule(RecordingAssistant(), InMemoryRateLimiter(maximumRequests = 1)) }

        val first = client.post("/api/v1/ai/chat") {
            contentType(ContentType.Application.Json)
            setBody("{\"messages\":[{\"role\":\"user\",\"content\":\"Hola\"}]}")
        }
        val second = client.post("/api/v1/ai/chat") {
            contentType(ContentType.Application.Json)
            setBody("{\"messages\":[{\"role\":\"user\",\"content\":\"Otra pregunta\"}]}")
        }
        val health = client.get("/health/live")

        assertEquals(HttpStatusCode.OK, first.status)
        assertEquals(HttpStatusCode.TooManyRequests, second.status)
        assertEquals(HttpStatusCode.OK, health.status)
    }

    @Test
    fun modelAndCredentialErrorsDoNotExposeProviderDetails() = testApplication {
        val assistant = object : RecordingAssistant() {
            override suspend fun chat(request: ChatRequest): ChatResponse = throw OpenAiModelUnavailableException()
        }
        application { welliModule(assistant) }

        val response = client.post("/api/v1/ai/chat") {
            contentType(ContentType.Application.Json)
            setBody("{\"messages\":[{\"role\":\"user\",\"content\":\"Hola\"}]}")
        }
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertTrue(body.contains("model_unavailable"))
        assertFalse(body.contains("test-only-key"))
    }

    @Test
    fun unsupportedContentTypeAndUserIdAreRejected() = testApplication {
        application { welliModule(RecordingAssistant()) }

        val wrongType = client.post("/api/v1/ai/chat") {
            contentType(ContentType.Text.Plain)
            setBody("hello")
        }
        val suppliedIdentity = client.post("/api/v1/ai/recipes/generate") {
            contentType(ContentType.Application.Json)
            setBody("""
                {"userId":99,"ingredients":[{"name":"Tomate","quantity":2.0,"unit":"pieza","category":"Verduras"}],
                 "budget":"Cero gasto","foodType":"Tradicional","restrictions":[],"allergies":[],"servings":2}
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.UnsupportedMediaType, wrongType.status)
        assertEquals(HttpStatusCode.BadRequest, suppliedIdentity.status)
    }
}

private open class RecordingAssistant : WelliAiAssistant {
    var recipeRequest: RecipeGenerationRequest? = null
    override suspend fun generateRecipe(request: RecipeGenerationRequest): RecipeGenerationResponse {
        recipeRequest = request
        return RecipeGenerationResponse(canGenerate = false, message = "Faltan ingredientes.")
    }

    override suspend fun chat(request: ChatRequest) = ChatResponse("Respuesta de prueba")
}
