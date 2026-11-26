package com.npcg.welliai.backend

import com.npcg.welliai.backend.config.BackendConfig
import com.npcg.welliai.backend.config.BackendConfigurationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BackendConfigTest {
    @Test
    fun refusesToStartWithoutApiKeyOrModel() {
        assertFailsWith<BackendConfigurationException> {
            BackendConfig.from(mapOf("OPENAI_MODEL" to "test-model"))
        }
        assertFailsWith<BackendConfigurationException> {
            BackendConfig.from(mapOf("OPENAI_API_KEY" to "test-only-key"))
        }
    }

    @Test
    fun processEnvironmentOverridesDotEnvAndServerStaysOnLoopback() {
        val config = BackendConfig.from(
            environment = mapOf(
                "OPENAI_API_KEY" to "process-test-key",
                "OPENAI_MODEL" to "test-model",
                "PORT" to "9091"
            ),
            dotenvValues = mapOf(
                "OPENAI_API_KEY" to "dotenv-test-key",
                "OPENAI_MODEL" to "other-test-model"
            )
        )

        assertEquals("process-test-key", config.apiKey)
        assertEquals("test-model", config.model)
        assertEquals("127.0.0.1", config.host)
        assertEquals(9091, config.port)
    }

    @Test
    fun refusesRemoteHostBindingAndInvalidPort() {
        val base = mapOf("OPENAI_API_KEY" to "test-key", "OPENAI_MODEL" to "test-model")
        assertFailsWith<IllegalArgumentException> { BackendConfig.from(base + ("HOST" to "0.0.0.0")) }
        assertFailsWith<IllegalArgumentException> { BackendConfig.from(base + ("PORT" to "70000")) }
    }
}
