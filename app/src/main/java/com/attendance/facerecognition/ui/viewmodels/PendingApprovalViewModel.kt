package com.attendance.facerecognition.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.facerecognition.data.local.database.AppDatabase
import com.attendance.facerecognition.data.local.entities.AttendanceRecord
import com.attendance.facerecognition.data.local.entities.AttendanceType
import com.attendance.facerecognition.data.local.entities.PendingAttendanceRecord
import com.attendance.facerecognition.data.local.entities.PendingStatus
import com.attendance.facerecognition.data.repository.AttendanceRepository
import com.attendance.facerecognition.data.repository.PendingAttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PendingApprovalViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val pendingRepository = PendingAttendanceRepository(database.pendingAttendanceDao())
    private val attendanceRepository = AttendanceRepository(database.attendanceDao())

    val pendingRecords = pendingRepository.getPendingRecords()
    val pendingCount = pendingRepository.getPendingCount()

    private val _uiState = MutableStateFlow<ApprovalUiState>(ApprovalUiState.Idle)
    val uiState: StateFlow<ApprovalUiState> = _uiState.asStateFlow()

    /**
     * Aprueba un registro pendiente y crea el registro real de asistencia
     */
    fun approveRecord(
        record: PendingAttendanceRecord,
        supervisorId: Long,
        notes: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        android.util.Log.d("PendingApprovalVM", "approveRecord() llamado para registro ID: ${record.id}")
        viewModelScope.launch {
            try {
                _uiState.value = ApprovalUiState.Processing
                android.util.Log.d("PendingApprovalVM", "Estado: Processing")

                // 1. Buscar el empleado por su ID
                android.util.Log.d("PendingApprovalVM", "Buscando empleado con ID: ${record.employeeId}")
                val employeeRepository = com.attendance.facerecognition.data.repository.EmployeeRepository(
                    database.employeeDao()
                )
                val employee = employeeRepository.getEmployeeByEmployeeId(record.employeeId)

                if (employee == null) {
                    android.util.Log.e("PendingApprovalVM", "Empleado no encontrado: ${record.employeeId}")
                    throw Exception("No se puede aprobar: El empleado con ID ${record.employeeId} no está registrado en el sistema. Debe registrarlo primero.")
                }

                android.util.Log.d("PendingApprovalVM", "Empleado encontrado: ${employee.fullName} (DB ID: ${employee.id})")

                // 2. Validar que no sea entrada/salida consecutiva
                android.util.Log.d("PendingApprovalVM", "Validando entrada/salida consecutivas...")
                val lastRecord = attendanceRepository.getLastRecordForEmployee(employee.id)

                if (lastRecord != null && lastRecord.type == record.type) {
                    val typeText = when (record.type) {
                        AttendanceType.ENTRY -> "entrada"
                        AttendanceType.EXIT -> "salida"
                    }
                    android.util.Log.w("PendingApprovalVM", "Intento de $typeText consecutiva detectado")
                    throw Exception("No se puede registrar $typeText consecutiva. El último registro del empleado ya fue una $typeText.")
                }

                // 3. Marcar como aprobado
                android.util.Log.d("PendingApprovalVM", "Marcando registro como aprobado...")
                pendingRepository.approveRecord(record.id, supervisorId, notes)

                // 4. Crear registro real de asistencia
                android.util.Log.d("PendingApprovalVM", "Creando registro de asistencia...")
                val attendanceRecord = AttendanceRecord(
                    employeeId = employee.id, // ID real del empleado en la BD
                    employeeName = employee.fullName,
                    employeeIdNumber = record.employeeId,
                    timestamp = record.timestamp,
                    type = record.type,
                    confidence = 1.0f, // Aprobado manualmente = 100%
                    livenessScore = 1.0f,
                    livenessChallenge = "manual_approval",
                    isSynced = false
                )

                attendanceRepository.insertRecord(attendanceRecord)
                android.util.Log.d("PendingApprovalVM", "Registro insertado exitosamente")

                _uiState.value = ApprovalUiState.Success("Registro aprobado exitosamente")
                android.util.Log.d("PendingApprovalVM", "Ejecutando onSuccess()")
                onSuccess()

            } catch (e: Exception) {
                android.util.Log.e("PendingApprovalVM", "Error al aprobar registro", e)
                _uiState.value = ApprovalUiState.Error(e.message ?: "Error al aprobar")
                onError(e.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Rechaza un registro pendiente
     */
    fun rejectRecord(
        record: PendingAttendanceRecord,
        supervisorId: Long,
        notes: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = ApprovalUiState.Processing

                if (notes.isBlank()) {
                    onError("Debes proporcionar una razón para rechazar")
                    return@launch
                }

                pendingRepository.rejectRecord(record.id, supervisorId, notes)

                _uiState.value = ApprovalUiState.Success("Registro rechazado")
                onSuccess()

            } catch (e: Exception) {
                _uiState.value = ApprovalUiState.Error(e.message ?: "Error al rechazar")
                onError(e.message ?: "Error desconocido")
            }
        }
    }

    fun resetState() {
        _uiState.value = ApprovalUiState.Idle
    }
}

sealed class ApprovalUiState {
    object Idle : ApprovalUiState()
    object Processing : ApprovalUiState()
    data class Success(val message: String) : ApprovalUiState()
    data class Error(val message: String) : ApprovalUiState()
}
