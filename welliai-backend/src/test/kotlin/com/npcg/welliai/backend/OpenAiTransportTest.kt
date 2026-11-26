package com.npcg.welliai.backend

import com.npcg.welliai.backend.ai.KtorOpenAiTransport
import com.npcg.welliai.backend.ai.OpenAiConnectionException
import com.npcg.welliai.backend.ai.OpenAiCredentialsException
import com.npcg.welliai.backend.ai.OpenAiRateLimitedException
import com.npcg.welliai.backend.api.OpenAiResponse
import com.npcg.welliai.backend.config.BackendConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OpenAiTransportTest {
    @Test
    fun sendsBearerCredentialToResponsesEndpointAndParsesResponse() = kotlinx.coroutines.test.runTest {
        var requestUrl = ""
        var authorization = ""
        val client = mockClient { request ->
            requestUrl = request.url.toString()
            authorization = request.headers[HttpHeaders.Authorization].orEmpty()
            respond(
                ByteReadChannel("""{"status":"completed","output":[]}"""),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        try {
            val response = KtorOpenAiTransport(testConfig(), client).createResponse(buildJsonObject { put("model", "test-model") })
            assertEquals("https://api.openai.com/v1/responses", requestUrl)
            assertEquals("Bearer local-test-key", authorization)
            assertEquals("completed", response.status)
        } finally {
            client.close()
        }
    }

    @Test
    fun mapsProviderAuthenticationAndRateLimitErrorsWithoutExposingBodies() = kotlinx.coroutines.test.runTest {
        val unauthorized = mockClient {
            respond(ByteReadChannel("private provider detail"), HttpStatusCode.Unauthorized)
        }
        val rateLimited = mockClient {
            respond(ByteReadChannel("private provider detail"), HttpStatusCode.TooManyRequests)
        }
        try {
            assertFailsWith<OpenAiCredentialsException> {
                KtorOpenAiTransport(testConfig(), unauthorized).createResponse(emptyJson())
            }
            assertFailsWith<OpenAiRateLimitedException> {
                KtorOpenAiTransport(testConfig(), rateLimited).createResponse(emptyJson())
            }
        } finally {
            unauthorized.close()
            rateLimited.close()
        }
    }

    @Test
    fun mapsConnectionErrorsToSafeFailure() = kotlinx.coroutines.test.runTest {
        val client = mockClient { throw IOException("private provider detail") }
        try {
            assertFailsWith<OpenAiConnectionException> {
                KtorOpenAiTransport(testConfig(), client).createResponse(emptyJson())
            }
        } finally {
            client.close()
        }
    }

    private fun mockClient(handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> io.ktor.client.request.HttpResponseData) =
        HttpClient(MockEngine(handler)) {
            expectSuccess = false
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

    private fun emptyJson() = buildJsonObject { }

    private fun testConfig() = BackendConfig("local-test-key", "test-model", "127.0.0.1", 8080)
}
