package com.attendance.facerecognition.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.facerecognition.data.local.database.AppDatabase
import com.attendance.facerecognition.data.local.entities.AttendanceRecord
import com.attendance.facerecognition.data.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class DailyReportViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val attendanceRepository = AttendanceRepository(database.attendanceDao())

    private val _records = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val records: StateFlow<List<AttendanceRecord>> = _records.asStateFlow()

    private val _uiState = MutableStateFlow<DailyReportUiState>(DailyReportUiState.Loading)
    val uiState: StateFlow<DailyReportUiState> = _uiState.asStateFlow()

    private val _selectedDate = MutableStateFlow(Date())
    val selectedDate: StateFlow<Date> = _selectedDate.asStateFlow()

    init {
        loadTodayRecords()
    }

    /**
     * Carga los registros del día actual
     */
    fun loadTodayRecords() {
        _selectedDate.value = Date()
        loadRecordsForDate(_selectedDate.value)
    }

    /**
     * Carga los registros para una fecha específica
     */
    fun loadRecordsForDate(date: Date) {
        viewModelScope.launch {
            try {
                _uiState.value = DailyReportUiState.Loading
                _selectedDate.value = date

                // Obtener inicio y fin del día seleccionado
                val calendar = Calendar.getInstance()
                calendar.time = date
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.time

                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endOfDay = calendar.time

                attendanceRepository.getRecordsByDateRange(startOfDay, endOfDay).collect { recordsList ->
                    _records.value = recordsList
                    _uiState.value = if (recordsList.isEmpty()) {
                        DailyReportUiState.Empty
                    } else {
                        DailyReportUiState.Success
                    }
                }
            } catch (e: Exception) {
                _uiState.value = DailyReportUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Estadísticas del día
     */
    fun getDailyStats(): DailyStats {
        val entriesCount = _records.value.count { it.type == com.attendance.facerecognition.data.local.entities.AttendanceType.ENTRY }
        val exitsCount = _records.value.count { it.type == com.attendance.facerecognition.data.local.entities.AttendanceType.EXIT }
        val uniqueEmployees = _records.value.map { it.employeeId }.distinct().size

        return DailyStats(
            totalRecords = _records.value.size,
            entries = entriesCount,
            exits = exitsCount,
            uniqueEmployees = uniqueEmployees
        )
    }
}

/**
 * Estados de la UI de reporte diario
 */
sealed class DailyReportUiState {
    object Loading : DailyReportUiState()
    object Success : DailyReportUiState()
    object Empty : DailyReportUiState()
    data class Error(val message: String) : DailyReportUiState()
}

/**
 * Estadísticas diarias
 */
data class DailyStats(
    val totalRecords: Int,
    val entries: Int,
    val exits: Int,
    val uniqueEmployees: Int
)
