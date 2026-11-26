package com.npcg.welliai.backend.config

import java.nio.file.Files
import java.nio.file.Path

data class BackendConfig(
    val apiKey: String,
    val model: String,
    val host: String,
    val port: Int
) {
    companion object {
        fun from(
            environment: Map<String, String>,
            dotenvValues: Map<String, String> = emptyMap()
        ): BackendConfig {
            fun value(name: String): String? = environment[name]?.takeIf(String::isNotBlank)
                ?: dotenvValues[name]?.takeIf(String::isNotBlank)

            val apiKey = value("OPENAI_API_KEY")
                ?: throw BackendConfigurationException("Falta configurar OPENAI_API_KEY.")
            val model = value("OPENAI_MODEL")
                ?: throw BackendConfigurationException("Falta configurar OPENAI_MODEL.")
            require(apiKey.length <= 512) { "OPENAI_API_KEY tiene un formato no válido." }
            require(model.length <= 128 && model.matches(Regex("[A-Za-z0-9][A-Za-z0-9._:/-]*"))) {
                "OPENAI_MODEL tiene un formato no válido."
            }
            val host = value("HOST") ?: "127.0.0.1"
            require(host == "127.0.0.1" || host == "localhost" || host == "::1") {
                "El backend de este sprint solo puede escuchar en loopback."
            }
            val port = value("PORT")?.toIntOrNull() ?: 8080
            require(port in 1..65535) { "PORT debe estar entre 1 y 65535." }
            return BackendConfig(apiKey, model, host, port)
        }

        fun loadLocalDotEnv(path: Path = Path.of(".env")): Map<String, String> {
            if (!Files.isRegularFile(path)) return emptyMap()
            return Files.readAllLines(path)
                .map(String::trim)
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .mapNotNull { line ->
                    val separator = line.indexOf('=')
                    if (separator <= 0) return@mapNotNull null
                    val key = line.substring(0, separator).trim()
                    val value = line.substring(separator + 1).trim().removeSurrounding("\"").removeSurrounding("'")
                    key.takeIf { it.matches(Regex("[A-Z][A-Z0-9_]*")) }?.let { it to value }
                }
                .toMap()
        }
    }
}

class BackendConfigurationException(message: String) : IllegalStateException(message)
