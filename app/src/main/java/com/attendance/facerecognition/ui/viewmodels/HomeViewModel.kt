package com.attendance.facerecognition.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.facerecognition.auth.SessionManager
import com.attendance.facerecognition.data.local.database.AppDatabase
import com.attendance.facerecognition.data.local.entities.DeviceRegistration
import com.attendance.facerecognition.device.DeviceManager
import com.attendance.facerecognition.network.ConnectivityObserver
import com.attendance.facerecognition.tenant.TenantManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val sessionManager = SessionManager(application)
    private val connectivityObserver = ConnectivityObserver(application)
    private val deviceManager = DeviceManager(application)
    private val tenantManager = TenantManager(application)

    private val _currentUsername = MutableStateFlow<String?>(null)
    val currentUsername: StateFlow<String?> = _currentUsername.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _canManageEmployees = MutableStateFlow(false)
    val canManageEmployees: StateFlow<Boolean> = _canManageEmployees.asStateFlow()

    private val _canViewReports = MutableStateFlow(false)
    val canViewReports: StateFlow<Boolean> = _canViewReports.asStateFlow()

    private val _employeeCount = MutableStateFlow(0)
    val employeeCount: StateFlow<Int> = _employeeCount.asStateFlow()

    private val _pendingRecordsCount = MutableStateFlow(0)
    val pendingRecordsCount: StateFlow<Int> = _pendingRecordsCount.asStateFlow()

    private val _networkStatus = MutableStateFlow(ConnectivityObserver.Status.UNAVAILABLE)
    val networkStatus: StateFlow<ConnectivityObserver.Status> = _networkStatus.asStateFlow()

    private val _deviceRegistration = MutableStateFlow<DeviceRegistration?>(null)
    val deviceRegistration: StateFlow<DeviceRegistration?> = _deviceRegistration.asStateFlow()

    private val _tenantCode = MutableStateFlow<String?>(null)
    val tenantCode: StateFlow<String?> = _tenantCode.asStateFlow()

    init {
        loadUserInfo()
        loadStatistics()
        observeNetworkStatus()
        observeDeviceRegistration()
        observeTenantCode()
    }

    /**
     * Carga la información del usuario actual y sus permisos
     */
    private fun loadUserInfo() {
        viewModelScope.launch {
            sessionManager.isLoggedIn.collect { loggedIn ->
                _isLoggedIn.value = loggedIn

                // Recargar permisos cuando cambia el estado de login
                if (loggedIn) {
                    // Recargar rol del usuario actual
                    loadUserPermissions()
                } else {
                    _canManageEmployees.value = false
                    _canViewReports.value = false
                }
            }
        }

        viewModelScope.launch {
            sessionManager.currentUserFullName.collect { fullName ->
                _currentUsername.value = fullName
            }
        }
    }

    /**
     * Carga los permisos basados en el rol del usuario
     */
    private suspend fun loadUserPermissions() {
        _canManageEmployees.value = sessionManager.canManageEmployees()
        _canViewReports.value = sessionManager.canViewReports()
    }

    /**
     * Carga las estadísticas de la base de datos
     */
    private fun loadStatistics() {
        viewModelScope.launch {
            database.employeeDao().getAllActiveEmployees().collect { employees ->
                _employeeCount.value = employees.size
            }
        }

        viewModelScope.launch {
            // Observar contador de registros pendientes de aprobación
            database.pendingAttendanceDao().getPendingCount().collect { count ->
                _pendingRecordsCount.value = count
            }
        }
    }

    /**
     * Observa el estado de conectividad de red
     */
    private fun observeNetworkStatus() {
        viewModelScope.launch {
            connectivityObserver.networkStatus.collect { status ->
                _networkStatus.value = status
            }
        }
    }

    /**
     * Observa el registro del dispositivo para obtener info de sincronización
     */
    private fun observeDeviceRegistration() {
        viewModelScope.launch {
            deviceManager.observeDeviceRegistration().collect { device ->
                _deviceRegistration.value = device
            }
        }
    }

    /**
     * Observa el código de tenant configurado
     */
    private fun observeTenantCode() {
        viewModelScope.launch {
            tenantManager.tenantCode.collect { code: String? ->
                _tenantCode.value = code
            }
        }
    }

    /**
     * Cierra sesión
     */
    fun logout() {
        viewModelScope.launch {
            sessionManager.logout()
        }
    }

    /**
     * Actualiza la actividad del usuario (para timeout)
     */
    fun updateActivity() {
        viewModelScope.launch {
            sessionManager.updateLastActivity()
        }
    }

    /**
     * Verifica si la sesión ha expirado
     */
    suspend fun isSessionExpired(): Boolean {
        return sessionManager.isSessionExpired()
    }
}
