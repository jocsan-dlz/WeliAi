package com.npcg.welliai.backend.api

import com.npcg.welliai.backend.ai.InvalidModelResponseException
import com.npcg.welliai.backend.ai.OpenAiConnectionException
import com.npcg.welliai.backend.ai.OpenAiCredentialsException
import com.npcg.welliai.backend.ai.OpenAiModelUnavailableException
import com.npcg.welliai.backend.ai.OpenAiRateLimitedException
import com.npcg.welliai.backend.ai.OpenAiRefusalException
import com.npcg.welliai.backend.ai.OpenAiTimeoutException
import com.npcg.welliai.backend.ai.WelliAiAssistant
import com.npcg.welliai.backend.config.BackendConfig
import com.npcg.welliai.backend.security.InMemoryRateLimiter
import com.npcg.welliai.backend.security.InvalidRequestException
import com.npcg.welliai.backend.security.MAX_REQUEST_BODY_BYTES
import com.npcg.welliai.backend.security.validateChatRequest
import com.npcg.welliai.backend.security.validateRecipeRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveStream
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.ByteArrayOutputStream

class PayloadTooLargeException : RuntimeException()
class InvalidJsonRequestException : RuntimeException()
class UnsupportedRequestContentTypeException : RuntimeException()

fun Application.welliModule(
    assistant: WelliAiAssistant,
    rateLimiter: InMemoryRateLimiter = InMemoryRateLimiter(),
    json: Json = Json { ignoreUnknownKeys = false; explicitNulls = true }
) {
    install(ContentNegotiation) { json(json) }
    install(StatusPages) {
        exception<InvalidRequestException> { call, cause -> call.respondError(HttpStatusCode.BadRequest, cause.code, cause.message.orEmpty()) }
        exception<UnsupportedRequestContentTypeException> { call, _ -> call.respondError(HttpStatusCode.UnsupportedMediaType, "unsupported_media_type", "Envía el cuerpo como JSON.") }
        exception<PayloadTooLargeException> { call, _ -> call.respondError(HttpStatusCode.PayloadTooLarge, "request_too_large", "La solicitud supera el tamaño permitido.") }
        exception<InvalidJsonRequestException> { call, _ -> call.respondError(HttpStatusCode.BadRequest, "invalid_json", "No se pudo leer el JSON enviado.") }
        exception<OpenAiRateLimitedException> { call, _ -> call.respondError(HttpStatusCode.TooManyRequests, "provider_rate_limited", "El servicio de IA está ocupado. Inténtalo más tarde.") }
        exception<OpenAiTimeoutException> { call, _ -> call.respondError(HttpStatusCode.GatewayTimeout, "provider_timeout", "La solicitud tardó demasiado. Inténtalo otra vez.") }
        exception<OpenAiModelUnavailableException> { call, _ -> call.respondError(HttpStatusCode.ServiceUnavailable, "model_unavailable", "El modelo configurado no está disponible para esta cuenta.") }
        exception<OpenAiCredentialsException> { call, _ -> call.respondError(HttpStatusCode.ServiceUnavailable, "provider_configuration", "La configuración del servicio de IA necesita revisión.") }
        exception<OpenAiConnectionException> { call, _ -> call.respondError(HttpStatusCode.BadGateway, "provider_unavailable", "No se pudo conectar con el servicio de IA.") }
        exception<OpenAiRefusalException> { call, _ -> call.respondError(HttpStatusCode.BadGateway, "response_refused", "WelliAI no pudo preparar una respuesta para esa solicitud.") }
        exception<InvalidModelResponseException> { call, _ -> call.respondError(HttpStatusCode.BadGateway, "invalid_provider_response", "El servicio devolvió una respuesta incompleta. Inténtalo otra vez.") }
        exception<Throwable> { call, cause ->
            if (cause is CancellationException) throw cause
            System.err.println("WelliAI request failed: category=${cause::class.simpleName}")
            call.respondError(HttpStatusCode.InternalServerError, "internal_error", "Ocurrió un error. Inténtalo más tarde.")
        }
    }

    routing {
        get("/health/live") { call.respond(mapOf("status" to "ok")) }
        route("/api/v1/ai") {
            post("/recipes/generate") {
                if (!rateLimiter.tryAcquire()) {
                    call.respondError(HttpStatusCode.TooManyRequests, "rate_limited", "Has enviado varias solicitudes. Espera un momento e inténtalo otra vez.")
                    return@post
                }
                val request = call.receiveJson<RecipeGenerationRequest>(json)
                validateRecipeRequest(request)
                call.respond(assistant.generateRecipe(request))
            }
            post("/chat") {
                if (!rateLimiter.tryAcquire()) {
                    call.respondError(HttpStatusCode.TooManyRequests, "rate_limited", "Has enviado varias solicitudes. Espera un momento e inténtalo otra vez.")
                    return@post
                }
                val request = call.receiveJson<ChatRequest>(json)
                validateChatRequest(request)
                call.respond(assistant.chat(request))
            }
        }
    }
}

private suspend inline fun <reified T> ApplicationCall.receiveJson(json: Json): T {
    if (!request.contentType().match(ContentType.Application.Json)) throw UnsupportedRequestContentTypeException()
    val bytes = receiveStream().use { stream ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(4_096)
        var total = 0
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) break
            total += count
            if (total > MAX_REQUEST_BODY_BYTES) throw PayloadTooLargeException()
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    }
    return try {
        json.decodeFromString<T>(bytes.decodeToString())
    } catch (_: SerializationException) {
        throw InvalidJsonRequestException()
    }
}

private suspend fun ApplicationCall.respondError(status: HttpStatusCode, code: String, message: String) {
    respond(status, ApiErrorBody(ApiError(code, message)))
}
