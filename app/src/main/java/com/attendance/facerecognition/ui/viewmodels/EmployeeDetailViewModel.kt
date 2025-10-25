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

class EmployeeDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val employeeRepository = EmployeeRepository(database.employeeDao())

    private val _employee = MutableStateFlow<Employee?>(null)
    val employee: StateFlow<Employee?> = _employee.asStateFlow()

    /**
     * Carga los detalles de un empleado
     */
    fun loadEmployee(employeeId: Long) {
        viewModelScope.launch {
            try {
                val emp = employeeRepository.getEmployeeById(employeeId)
                _employee.value = emp
            } catch (e: Exception) {
                // Handle error
                _employee.value = null
            }
        }
    }

    /**
     * Alterna el estado activo/inactivo del empleado
     */
    fun toggleEmployeeStatus() {
        viewModelScope.launch {
            try {
                _employee.value?.let { emp ->
                    val updatedEmployee = emp.copy(isActive = !emp.isActive)
                    employeeRepository.updateEmployee(updatedEmployee)
                    _employee.value = updatedEmployee
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    /**
     * Elimina el empleado
     */
    fun deleteEmployee() {
        viewModelScope.launch {
            try {
                _employee.value?.let { emp ->
                    employeeRepository.deleteEmployee(emp)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
