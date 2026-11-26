package com.npcg.practica1

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.npcg.practica1.data.session.DataStoreSessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class DataStoreSessionStoreTest {
    @Test
    fun userIdIsPersistedInTheDataStoreFile() = runBlocking {
        val directory = Files.createTempDirectory("welli-session").toFile()
        val file = File(directory, "session.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val sessionStore = DataStoreSessionStore(createStore(scope, file))
            sessionStore.saveUserId(42)

            assertTrue(file.exists())
            assertEquals(42, sessionStore.getUserId())
        } finally {
            scope.cancel()
            directory.deleteRecursively()
        }
    }

    @Test
    fun clearRemovesPersistedUserId() = runBlocking {
        val sessionStore = DataStoreSessionStore(InMemoryPreferencesDataStore())
        sessionStore.saveUserId(42)

        sessionStore.clear()

        assertNull(sessionStore.getUserId())
    }

    private fun createStore(scope: CoroutineScope, file: File): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { file }
        )
}

private class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val preferences = MutableStateFlow(emptyPreferences())
    override val data: Flow<Preferences> = preferences

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences
    ): Preferences {
        val updated = transform(preferences.value)
        preferences.value = updated
        return updated
    }
}
