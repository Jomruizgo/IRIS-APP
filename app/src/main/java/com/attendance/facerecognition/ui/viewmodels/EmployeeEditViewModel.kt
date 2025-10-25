package com.attendance.facerecognition.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.facerecognition.data.local.database.AppDatabase
import com.attendance.facerecognition.data.local.entities.Employee
import com.attendance.facerecognition.data.repository.EmployeeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EmployeeEditViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val employeeRepository = EmployeeRepository(database.employeeDao())

    private val _employee = MutableStateFlow<Employee?>(null)
    val employee: StateFlow<Employee?> = _employee.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /**
     * Carga los detalles de un empleado
     */
    fun loadEmployee(employeeId: Long) {
        viewModelScope.launch {
            try {
                val emp = employeeRepository.getEmployeeById(employeeId)
                _employee.value = emp
            } catch (e: Exception) {
                _employee.value = null
            }
        }
    }

    /**
     * Actualiza la información del empleado
     */
    fun updateEmployee(
        fullName: String,
        department: String,
        position: String,
        isActive: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (fullName.isBlank() || department.isBlank() || position.isBlank()) {
            onError("Todos los campos son obligatorios")
            return
        }

        viewModelScope.launch {
            try {
                _isSaving.value = true

                _employee.value?.let { emp ->
                    val updatedEmployee = emp.copy(
                        fullName = fullName.trim(),
                        department = department.trim(),
                        position = position.trim(),
                        isActive = isActive
                    )

                    employeeRepository.updateEmployee(updatedEmployee)
                    _employee.value = updatedEmployee

                    onSuccess()
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error desconocido")
            } finally {
                _isSaving.value = false
            }
        }
    }
}
