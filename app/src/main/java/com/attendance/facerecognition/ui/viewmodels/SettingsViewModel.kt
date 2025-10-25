package com.attendance.facerecognition.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.facerecognition.network.RetrofitClient
import com.attendance.facerecognition.settings.DataRetentionManager
import com.attendance.facerecognition.settings.SettingsManager
import com.attendance.facerecognition.tenant.TenantManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val retentionManager = DataRetentionManager(application)
    private val settingsManager = SettingsManager(application)
    private val tenantManager = TenantManager(application)

    val attendanceRetentionDays: Flow<Int> = retentionManager.attendanceRetentionDays
    val auditRetentionDays: Flow<Int> = retentionManager.auditRetentionDays
    val serverUrl: Flow<String> = settingsManager.getServerUrlFlow()
    val tenantCode: Flow<String> = tenantManager.tenantCode.map { it ?: "" }

    private val _cleanupResult = MutableStateFlow<DataRetentionManager.CleanupResult?>(null)
    val cleanupResult: StateFlow<DataRetentionManager.CleanupResult?> = _cleanupResult.asStateFlow()

    /**
     * Configura la retención de registros de asistencia
     */
    fun setAttendanceRetention(days: Int) {
        viewModelScope.launch {
            retentionManager.setAttendanceRetentionDays(days)
        }
    }

    /**
     * Configura la retención de registros de auditoría
     */
    fun setAuditRetention(days: Int) {
        viewModelScope.launch {
            retentionManager.setAuditRetentionDays(days)
        }
    }

    /**
     * Ejecuta limpieza de registros antiguos (solo sincronizados)
     */
    fun cleanOldRecords() {
        viewModelScope.launch {
            val result = retentionManager.cleanOldRecords()
            _cleanupResult.value = result
        }
    }

    /**
     * Limpia el resultado de la limpieza
     */
    fun clearCleanupResult() {
        _cleanupResult.value = null
    }

    /**
     * Actualiza la URL del servidor
     */
    fun updateServerUrl(url: String) {
        viewModelScope.launch {
            // Validar URL básica
            var cleanUrl = url.trim()
            if (cleanUrl.isNotEmpty() && !cleanUrl.endsWith("/")) {
                cleanUrl += "/"
            }

            settingsManager.setServerUrl(cleanUrl)
            // Invalidar cliente Retrofit para que use la nueva URL
            RetrofitClient.invalidate()
        }
    }

    /**
     * Resetea la URL del servidor al valor por defecto
     */
    fun resetServerUrl() {
        viewModelScope.launch {
            settingsManager.resetServerUrl()
            RetrofitClient.invalidate()
        }
    }

    /**
     * Actualiza el código de tenant
     */
    fun updateTenantCode(code: String) {
        viewModelScope.launch {
            tenantManager.setTenantCode(code.trim().uppercase())
        }
    }

    /**
     * Limpia el código de tenant
     */
    fun clearTenantCode() {
        viewModelScope.launch {
            tenantManager.setTenantCode("")
        }
    }
}
