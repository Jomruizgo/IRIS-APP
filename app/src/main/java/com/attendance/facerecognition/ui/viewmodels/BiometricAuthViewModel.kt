package com.attendance.facerecognition.ui.viewmodels

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.facerecognition.biometric.BiometricKeyManager
import com.attendance.facerecognition.data.local.database.AppDatabase
import com.attendance.facerecognition.data.local.entities.AttendanceAudit
import com.attendance.facerecognition.data.local.entities.AttendanceRecord
import com.attendance.facerecognition.data.local.entities.AttendanceType
import com.attendance.facerecognition.data.local.entities.AuditAction
import com.attendance.facerecognition.data.local.entities.Employee
import com.attendance.facerecognition.data.repository.AttendanceAuditRepository
import com.attendance.facerecognition.data.repository.AttendanceRepository
import com.attendance.facerecognition.data.repository.EmployeeRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BiometricAuthViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val employeeRepository = EmployeeRepository(database.employeeDao())
    private val attendanceRepository = AttendanceRepository(database.attendanceDao())
    private val auditRepository = AttendanceAuditRepository(database.attendanceAuditDao())
    private val biometricKeyManager = BiometricKeyManager(application)
    private val gson = Gson()

    private val _enteredId = MutableStateFlow("")
    val enteredId: StateFlow<String> = _enteredId.asStateFlow()

    private val _uiState = MutableStateFlow<BiometricAuthState>(BiometricAuthState.Idle)
    val uiState: StateFlow<BiometricAuthState> = _uiState.asStateFlow()

    private var currentEmployee: Employee? = null

    /**
     * Agrega un dígito al ID ingresado
     */
    fun addDigit(digit: Int) {
        if (_enteredId.value.length < 10) { // Límite de 10 dígitos
            _enteredId.value += digit.toString()
        }
    }

    /**
     * Elimina el último dígito
     */
    fun removeLastDigit() {
        if (_enteredId.value.isNotEmpty()) {
            _enteredId.value = _enteredId.value.dropLast(1)
        }
    }

    /**
     * Autentica con huella digital
     */
    fun authenticateWithFingerprint(activity: FragmentActivity, attendanceType: AttendanceType) {
        val employeeId = _enteredId.value

        if (employeeId.isEmpty()) {
            _uiState.value = BiometricAuthState.Error("Ingresa tu ID de empleado")
            return
        }

        viewModelScope.launch {
            try {
                // Buscar empleado por ID
                val employee = employeeRepository.getEmployeeByEmployeeId(employeeId)

                if (employee == null) {
                    _uiState.value = BiometricAuthState.Error("Empleado no encontrado con ID: $employeeId")
                    return@launch
                }

                // Verificar que el empleado tenga huella habilitada
                if (!employee.hasFingerprintEnabled) {
                    _uiState.value = BiometricAuthState.Error(
                        "${employee.fullName} no tiene huella registrada.\n\n" +
                        "Intenta con otro método o contacta al administrador del sistema."
                    )
                    return@launch
                }

                // Verificar que tenga alias de KeyStore
                if (employee.fingerprintKeystoreAlias.isNullOrEmpty()) {
                    _uiState.value = BiometricAuthState.Error(
                        "${employee.fullName} no tiene huella vinculada.\n\n" +
                        "Contacta al administrador para registrar tu huella."
                    )
                    return@launch
                }

                currentEmployee = employee
                _uiState.value = BiometricAuthState.EmployeeFound(employee.fullName)
                _uiState.value = BiometricAuthState.Authenticating

                // Verificar huella usando la clave específica del empleado
                biometricKeyManager.verifyFingerprint(
                    keystoreAlias = employee.fingerprintKeystoreAlias!!,
                    employeeName = employee.fullName,
                    activity = activity,
                    onSuccess = {
                        // Autenticación exitosa, registrar asistencia
                        registerAttendance(employee, attendanceType)
                    },
                    onError = { errorMessage ->
                        _uiState.value = BiometricAuthState.Error(errorMessage)
                    }
                )

            } catch (e: Exception) {
                _uiState.value = BiometricAuthState.Error("Error: ${e.message}")
            }
        }
    }

    /**
     * Registra la asistencia del empleado
     */
    private fun registerAttendance(employee: Employee, attendanceType: AttendanceType) {
        viewModelScope.launch {
            try {
                // Obtener último registro del empleado
                val lastRecord = attendanceRepository.getLastRecordForEmployee(employee.id)

                // VALIDACIÓN: No permitir ENTRADA si ya hay entrada sin salida
                if (attendanceType == AttendanceType.ENTRY) {
                    if (lastRecord != null && lastRecord.type == AttendanceType.ENTRY) {
                        _uiState.value = BiometricAuthState.Error(
                            "Ya tienes ENTRADA sin SALIDA registrada.\nDebes registrar SALIDA primero."
                        )
                        return@launch
                    }
                }

                // VALIDACIÓN: No permitir SALIDA sin entrada previa
                if (attendanceType == AttendanceType.EXIT) {
                    if (lastRecord == null || lastRecord.type == AttendanceType.EXIT) {
                        _uiState.value = BiometricAuthState.Error(
                            "No puedes registrar SALIDA sin ENTRADA previa."
                        )
                        return@launch
                    }
                }

                // Crear registro
                val record = AttendanceRecord(
                    employeeId = employee.id,
                    employeeName = employee.fullName,
                    employeeIdNumber = employee.employeeId,
                    timestamp = System.currentTimeMillis(),
                    type = attendanceType,
                    confidence = 1.0f, // Huella siempre es 100% confianza
                    livenessScore = 1.0f,
                    livenessChallenge = "fingerprint",
                    isSynced = false
                )

                // Guardar en base de datos
                val recordId = attendanceRepository.insertRecord(record)

                // Registrar auditoría
                val metadata = mapOf(
                    "method" to "fingerprint",
                    "confidence" to 1.0f,
                    "type" to attendanceType.name
                )
                auditRepository.insertAudit(
                    AttendanceAudit(
                        attendanceId = recordId,
                        action = AuditAction.CREATED,
                        employeeIdDetected = employee.employeeId,
                        employeeIdActual = employee.employeeId,
                        metadata = gson.toJson(metadata)
                    )
                )

                _uiState.value = BiometricAuthState.Success(employee.fullName, attendanceType)

            } catch (e: Exception) {
                _uiState.value = BiometricAuthState.Error("Error al registrar: ${e.message}")
            }
        }
    }

    /**
     * Reinicia el estado
     */
    fun reset() {
        _enteredId.value = ""
        _uiState.value = BiometricAuthState.Idle
        currentEmployee = null
    }
}

/**
 * Estados de la UI de autenticación biométrica
 */
sealed class BiometricAuthState {
    object Idle : BiometricAuthState()
    data class EmployeeFound(val employeeName: String) : BiometricAuthState()
    object Authenticating : BiometricAuthState()
    data class Success(val employeeName: String, val attendanceType: AttendanceType) : BiometricAuthState()
    data class Error(val message: String) : BiometricAuthState()
}
