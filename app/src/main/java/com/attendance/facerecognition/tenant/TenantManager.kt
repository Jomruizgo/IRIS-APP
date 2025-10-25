package com.attendance.facerecognition.tenant

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.tenantDataStore: DataStore<Preferences> by preferencesDataStore(name = "tenant")

/**
 * Gestiona la información del tenant (empresa) actual
 * Necesario para multi-tenancy en la sincronización con la nube
 */
class TenantManager(private val context: Context) {

    companion object {
        private val TENANT_CODE = stringPreferencesKey("tenant_code")
        private val TENANT_NAME = stringPreferencesKey("tenant_name")
        private val SERVER_URL = stringPreferencesKey("server_url")
    }

    val tenantCode: Flow<String?> = context.tenantDataStore.data
        .map { preferences -> preferences[TENANT_CODE] }

    val tenantName: Flow<String?> = context.tenantDataStore.data
        .map { preferences -> preferences[TENANT_NAME] }

    val serverUrl: Flow<String?> = context.tenantDataStore.data
        .map { preferences -> preferences[SERVER_URL] }

    /**
     * Obtiene el código de tenant actual (suspending)
     */
    suspend fun getTenantCode(): String? {
        return tenantCode.first()
    }

    /**
     * Obtiene el nombre del tenant actual (suspending)
     */
    suspend fun getTenantName(): String? {
        return tenantName.first()
    }

    /**
     * Obtiene la URL del servidor (suspending)
     */
    suspend fun getServerUrl(): String? {
        return serverUrl.first()
    }

    /**
     * Configura el tenant actual
     */
    suspend fun setTenant(code: String, name: String, serverUrl: String) {
        context.tenantDataStore.edit { preferences ->
            preferences[TENANT_CODE] = code
            preferences[TENANT_NAME] = name
            preferences[SERVER_URL] = serverUrl
        }
    }

    /**
     * Configura solo el código de tenant
     */
    suspend fun setTenantCode(code: String) {
        context.tenantDataStore.edit { preferences ->
            if (code.isEmpty()) {
                preferences.remove(TENANT_CODE)
            } else {
                preferences[TENANT_CODE] = code
            }
        }
    }

    /**
     * Verifica si hay un tenant configurado
     */
    suspend fun hasTenant(): Boolean {
        return getTenantCode() != null
    }

    /**
     * Limpia la configuración del tenant
     * ADVERTENCIA: Esto debería hacerse solo si no hay datos locales
     */
    suspend fun clearTenant() {
        context.tenantDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Obtiene toda la configuración del tenant
     */
    suspend fun getTenantInfo(): TenantInfo? {
        val code = getTenantCode() ?: return null
        val name = getTenantName() ?: return null
        val url = getServerUrl() ?: return null

        return TenantInfo(code, name, url)
    }
}

/**
 * Información completa del tenant
 */
data class TenantInfo(
    val code: String,
    val name: String,
    val serverUrl: String
)
