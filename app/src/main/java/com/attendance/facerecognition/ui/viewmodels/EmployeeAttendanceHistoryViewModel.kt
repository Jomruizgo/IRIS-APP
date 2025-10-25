package com.attendance.facerecognition.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.facerecognition.data.local.database.AppDatabase
import com.attendance.facerecognition.data.local.entities.AttendanceRecord
import com.attendance.facerecognition.data.local.entities.AttendanceType
import com.attendance.facerecognition.data.local.entities.Employee
import com.attendance.facerecognition.data.repository.AttendanceRepository
import com.attendance.facerecognition.data.repository.EmployeeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EmployeeAttendanceHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val employeeRepository = EmployeeRepository(database.employeeDao())
    private val attendanceRepository = AttendanceRepository(database.attendanceDao())

    private val _employee = MutableStateFlow<Employee?>(null)
    val employee: StateFlow<Employee?> = _employee.asStateFlow()

    private val _records = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val records: StateFlow<List<AttendanceRecord>> = _records.asStateFlow()

    private val _stats = MutableStateFlow(EmployeeStats())
    val stats: StateFlow<EmployeeStats> = _stats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Carga la información del empleado y sus registros
     */
    fun loadEmployee(employeeId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Cargar empleado
                val emp = employeeRepository.getEmployeeById(employeeId)
                _employee.value = emp

                // Cargar registros
                if (emp != null) {
                    attendanceRepository.getRecordsByEmployee(employeeId).collect { recordsList ->
                        _records.value = recordsList

                        // Calcular estadísticas
                        val entriesCount = recordsList.count { it.type == AttendanceType.ENTRY }
                        val exitsCount = recordsList.count { it.type == AttendanceType.EXIT }
                        val avgConfidence = if (recordsList.isNotEmpty()) {
                            recordsList.map { it.confidence }.average().toFloat()
                        } else {
                            0f
                        }

                        _stats.value = EmployeeStats(
                            totalRecords = recordsList.size,
                            entriesCount = entriesCount,
                            exitsCount = exitsCount,
                            averageConfidence = avgConfidence
                        )

                        _isLoading.value = false
                    }
                } else {
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }
}

data class EmployeeStats(
    val totalRecords: Int = 0,
    val entriesCount: Int = 0,
    val exitsCount: Int = 0,
    val averageConfidence: Float = 0f
)
