package com.npcg.welliai.backend

import com.npcg.welliai.backend.ai.KtorOpenAiTransport
import com.npcg.welliai.backend.ai.OpenAiWelliAssistant
import com.npcg.welliai.backend.api.welliModule
import com.npcg.welliai.backend.config.BackendConfig
import com.npcg.welliai.backend.config.BackendConfigurationException
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.serialization.json.Json

fun main() {
    val config = try {
        BackendConfig.from(System.getenv(), BackendConfig.loadLocalDotEnv())
    } catch (error: BackendConfigurationException) {
        System.err.println("WelliAI backend configuration error: ${error.message}")
        return
    } catch (error: IllegalArgumentException) {
        System.err.println("WelliAI backend configuration error: ${error.message}")
        return
    }

    val json = Json { ignoreUnknownKeys = true; explicitNulls = true }
    val client = HttpClient(CIO) {
        expectSuccess = false
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = KtorOpenAiTransport.OPENAI_TIMEOUT_MILLIS
            connectTimeoutMillis = KtorOpenAiTransport.OPENAI_CONNECT_TIMEOUT_MILLIS
            socketTimeoutMillis = KtorOpenAiTransport.OPENAI_TIMEOUT_MILLIS
        }
    }
    val assistant = OpenAiWelliAssistant(config, KtorOpenAiTransport(config, client), json)
    Runtime.getRuntime().addShutdownHook(Thread { client.close() })

    embeddedServer(Netty, host = config.host, port = config.port) {
        welliModule(assistant, json = json)
    }.start(wait = true)
}
