package com.npcg.welliai.backend.ai

import com.npcg.welliai.backend.api.OpenAiResponse
import com.npcg.welliai.backend.config.BackendConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonObject
import java.io.IOException

interface OpenAiTransport {
    suspend fun createResponse(requestBody: JsonObject): OpenAiResponse
}

class KtorOpenAiTransport(
    private val config: BackendConfig,
    private val client: HttpClient
) : OpenAiTransport {
    override suspend fun createResponse(requestBody: JsonObject): OpenAiResponse {
        val response = try {
            client.post("https://api.openai.com/v1/responses") {
                bearerAuth(config.apiKey)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
                timeout {
                    requestTimeoutMillis = OPENAI_TIMEOUT_MILLIS
                    connectTimeoutMillis = OPENAI_CONNECT_TIMEOUT_MILLIS
                    socketTimeoutMillis = OPENAI_TIMEOUT_MILLIS
                }
            }
        } catch (error: HttpRequestTimeoutException) {
            throw OpenAiTimeoutException()
        } catch (error: IOException) {
            throw OpenAiConnectionException()
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (error: Exception) {
            throw OpenAiConnectionException()
        }

        if (!response.status.isSuccess()) {
            when (response.status) {
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> throw OpenAiCredentialsException()
                HttpStatusCode.TooManyRequests -> throw OpenAiRateLimitedException()
                HttpStatusCode.NotFound, HttpStatusCode.BadRequest -> throw OpenAiModelUnavailableException()
                HttpStatusCode.RequestTimeout, HttpStatusCode.GatewayTimeout -> throw OpenAiTimeoutException()
                else -> throw OpenAiConnectionException()
            }
        }
        return try {
            response.body<OpenAiResponse>()
        } catch (_: Exception) {
            throw InvalidModelResponseException()
        }
    }

    companion object {
        const val OPENAI_TIMEOUT_MILLIS = 45_000L
        const val OPENAI_CONNECT_TIMEOUT_MILLIS = 10_000L
    }
}
