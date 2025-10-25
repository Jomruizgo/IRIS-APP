package com.attendance.facerecognition.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.facerecognition.data.local.database.AppDatabase
import com.attendance.facerecognition.data.local.entities.AttendanceAudit
import com.attendance.facerecognition.data.local.entities.AttendanceRecord
import com.attendance.facerecognition.data.local.entities.AuditAction
import com.attendance.facerecognition.data.repository.AttendanceAuditRepository
import com.attendance.facerecognition.data.repository.AttendanceRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AttendanceHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val attendanceRepository = AttendanceRepository(database.attendanceDao())
    private val auditRepository = AttendanceAuditRepository(database.attendanceAuditDao())
    private val gson = Gson()

    private val _records = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val records: StateFlow<List<AttendanceRecord>> = _records.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _filter = MutableStateFlow("")
    val filter: StateFlow<String> = _filter.asStateFlow()

    init {
        loadRecords()
    }

    /**
     * Carga los últimos 50 registros
     */
    private fun loadRecords() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                when (_filter.value) {
                    "last_hour" -> {
                        val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
                        // Obtener todos los registros y filtrar
                        attendanceRepository.getRecentRecords(50).collect { allRecords ->
                            _records.value = allRecords.filter { it.timestamp >= oneHourAgo }
                            _isLoading.value = false
                        }
                    }
                    else -> {
                        attendanceRepository.getRecentRecords(50).collect { allRecords ->
                            _records.value = allRecords
                            _isLoading.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    /**
     * Alterna filtro de última hora
     */
    fun toggleLastHourFilter() {
        _filter.value = if (_filter.value == "last_hour") "" else "last_hour"
        loadRecords()
    }

    /**
     * Elimina un registro con auditoría
     */
    fun deleteRecord(
        record: AttendanceRecord,
        reason: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (reason.trim().length < 10) {
            onError("La razón debe tener al menos 10 caracteres")
            return
        }

        viewModelScope.launch {
            try {
                // Registrar auditoría ANTES de eliminar
                val metadata = mapOf(
                    "deleted_at" to System.currentTimeMillis(),
                    "original_timestamp" to record.timestamp,
                    "original_type" to record.type.name,
                    "confidence" to record.confidence,
                    "reason" to reason.trim()
                )
                auditRepository.insertAudit(
                    AttendanceAudit(
                        attendanceId = null, // Ya fue eliminado
                        action = AuditAction.DELETED_BY_ADMIN,
                        employeeIdDetected = record.employeeIdNumber,
                        employeeIdActual = record.employeeIdNumber,
                        reason = reason.trim(),
                        metadata = gson.toJson(metadata)
                    )
                )

                // Eliminar registro
                attendanceRepository.deleteRecord(record.id)

                // Recargar lista
                loadRecords()

                onSuccess()

            } catch (e: Exception) {
                onError(e.message ?: "Error desconocido")
            }
        }
    }
}
