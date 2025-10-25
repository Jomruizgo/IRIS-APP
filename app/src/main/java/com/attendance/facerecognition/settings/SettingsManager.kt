package com.attendance.facerecognition.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Manager para configuraciones de la aplicación
 * Usa DataStore para almacenamiento persistente
 */
class SettingsManager(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

        private val SERVER_URL = stringPreferencesKey("server_url")
        private const val DEFAULT_SERVER_URL = "https://api.iris-attendance.com/"
    }

    /**
     * Obtiene la URL del servidor configurada
     */
    suspend fun getServerUrl(): String {
        return context.dataStore.data.map { preferences ->
            preferences[SERVER_URL] ?: DEFAULT_SERVER_URL
        }.first()
    }

    /**
     * Obtiene la URL del servidor como Flow (reactivo)
     */
    fun getServerUrlFlow(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[SERVER_URL] ?: DEFAULT_SERVER_URL
        }
    }

    /**
     * Actualiza la URL del servidor
     */
    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[SERVER_URL] = url
        }
    }

    /**
     * Resetea la URL del servidor al valor por defecto
     */
    suspend fun resetServerUrl() {
        context.dataStore.edit { preferences ->
            preferences.remove(SERVER_URL)
        }
    }
}
