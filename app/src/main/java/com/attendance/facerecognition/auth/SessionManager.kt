package com.attendance.facerecognition.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.attendance.facerecognition.data.local.entities.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

/**
 * Gestiona la sesión del usuario autenticado
 */
class SessionManager(private val context: Context) {

    companion object {
        private val USER_ID = longPreferencesKey("user_id")
        private val USERNAME = stringPreferencesKey("username")
        private val FULL_NAME = stringPreferencesKey("full_name")
        private val ROLE = stringPreferencesKey("role")
        private val LAST_ACTIVITY = longPreferencesKey("last_activity")

        const val SESSION_TIMEOUT_MS = 30 * 60 * 1000L // 30 minutos
    }

    val isLoggedIn: Flow<Boolean> = context.sessionDataStore.data
        .map { preferences ->
            preferences[USER_ID] != null
        }

    val currentUserId: Flow<Long?> = context.sessionDataStore.data
        .map { preferences ->
            preferences[USER_ID]
        }

    val currentUsername: Flow<String?> = context.sessionDataStore.data
        .map { preferences ->
            preferences[USERNAME]
        }

    val currentUserRole: Flow<UserRole?> = context.sessionDataStore.data
        .map { preferences ->
            preferences[ROLE]?.let { UserRole.valueOf(it) }
        }

    val currentUserFullName: Flow<String?> = context.sessionDataStore.data
        .map { preferences ->
            preferences[FULL_NAME]
        }

    /**
     * Inicia sesión guardando los datos del usuario
     */
    suspend fun login(userId: Long, username: String, fullName: String, role: UserRole) {
        context.sessionDataStore.edit { preferences ->
            preferences[USER_ID] = userId
            preferences[USERNAME] = username
            preferences[FULL_NAME] = fullName
            preferences[ROLE] = role.name
            preferences[LAST_ACTIVITY] = System.currentTimeMillis()
        }
    }

    /**
     * Cierra sesión eliminando todos los datos
     */
    suspend fun logout() {
        context.sessionDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Actualiza el timestamp de última actividad
     */
    suspend fun updateLastActivity() {
        context.sessionDataStore.edit { preferences ->
            preferences[LAST_ACTIVITY] = System.currentTimeMillis()
        }
    }

    /**
     * Verifica si la sesión ha expirado
     */
    suspend fun isSessionExpired(): Boolean {
        val lastActivity = context.sessionDataStore.data.first()[LAST_ACTIVITY] ?: return true
        val now = System.currentTimeMillis()
        return (now - lastActivity) > SESSION_TIMEOUT_MS
    }

    /**
     * Verifica si el usuario actual tiene un rol específico
     */
    suspend fun hasRole(role: UserRole): Boolean {
        val currentRole = currentUserRole.first() ?: return false
        return currentRole == role
    }

    /**
     * Verifica si el usuario es ADMIN
     */
    suspend fun isAdmin(): Boolean {
        return hasRole(UserRole.ADMIN)
    }

    /**
     * Verifica si el usuario puede gestionar empleados (ADMIN solamente)
     */
    suspend fun canManageEmployees(): Boolean {
        return isAdmin()
    }

    /**
     * Verifica si el usuario puede ver reportes (ADMIN o SUPERVISOR)
     */
    suspend fun canViewReports(): Boolean {
        val role = currentUserRole.first() ?: return false
        return role == UserRole.ADMIN || role == UserRole.SUPERVISOR
    }
}
