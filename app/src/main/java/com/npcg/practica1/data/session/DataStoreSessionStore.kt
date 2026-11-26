package com.npcg.practica1.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DataStoreSessionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : com.npcg.practica1.domain.repository.SessionStore {
    override suspend fun saveUserId(userId: Int) {
        require(userId > 0) { "El identificador de usuario debe ser válido" }
        dataStore.edit { preferences -> preferences[USER_ID_KEY] = userId }
    }

    override suspend fun getUserId(): Int? =
        dataStore.data.first()[USER_ID_KEY]

    override suspend fun clear() {
        dataStore.edit { preferences -> preferences.remove(USER_ID_KEY) }
    }

    private companion object {
        val USER_ID_KEY = intPreferencesKey("authenticated_user_id")
    }
}
