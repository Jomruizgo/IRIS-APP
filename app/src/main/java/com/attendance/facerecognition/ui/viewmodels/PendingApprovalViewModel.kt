package com.attendance.facerecognition.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.facerecognition.data.local.database.AppDatabase
import com.attendance.facerecognition.data.local.entities.AttendanceRecord
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
        viewModelScope.launch {
            try {
                _uiState.value = ApprovalUiState.Processing

                // 1. Marcar como aprobado
                pendingRepository.approveRecord(record.id, supervisorId, notes)

                // 2. Crear registro real de asistencia
                val attendanceRecord = AttendanceRecord(
                    employeeId = 0L, // Se llenará si existe el empleado
                    employeeName = record.employeeName ?: "Desconocido",
                    employeeIdNumber = record.employeeId,
                    timestamp = record.timestamp,
                    type = record.type,
                    confidence = 1.0f, // Aprobado manualmente = 100%
                    livenessScore = 1.0f,
                    livenessChallenge = "manual_approval",
                    isSynced = false
                )

                attendanceRepository.insertRecord(attendanceRecord)

                _uiState.value = ApprovalUiState.Success("Registro aprobado exitosamente")
                onSuccess()

            } catch (e: Exception) {
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
