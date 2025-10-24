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

class EmployeeListViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val employeeRepository = EmployeeRepository(database.employeeDao())

    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()

    private val _uiState = MutableStateFlow<EmployeeListUiState>(EmployeeListUiState.Loading)
    val uiState: StateFlow<EmployeeListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadEmployees()
    }

    /**
     * Carga todos los empleados
     */
    fun loadEmployees() {
        viewModelScope.launch {
            try {
                _uiState.value = EmployeeListUiState.Loading
                employeeRepository.getAllEmployees().collect { employeesList ->
                    _employees.value = employeesList
                    _uiState.value = if (employeesList.isEmpty()) {
                        EmployeeListUiState.Empty
                    } else {
                        EmployeeListUiState.Success
                    }
                }
            } catch (e: Exception) {
                _uiState.value = EmployeeListUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Busca empleados por nombre o ID
     */
    fun searchEmployees(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            try {
                if (query.isBlank()) {
                    loadEmployees()
                } else {
                    employeeRepository.searchEmployees(query).collect { employeesList ->
                        _employees.value = employeesList
                        _uiState.value = if (employeesList.isEmpty()) {
                            EmployeeListUiState.Empty
                        } else {
                            EmployeeListUiState.Success
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = EmployeeListUiState.Error(e.message ?: "Error en búsqueda")
            }
        }
    }

    /**
     * Elimina un empleado
     */
    fun deleteEmployee(employee: Employee) {
        viewModelScope.launch {
            try {
                employeeRepository.deleteEmployee(employee.id)
                loadEmployees()
            } catch (e: Exception) {
                _uiState.value = EmployeeListUiState.Error("Error al eliminar: ${e.message}")
            }
        }
    }
}

/**
 * Estados de la UI de lista de empleados
 */
sealed class EmployeeListUiState {
    object Loading : EmployeeListUiState()
    object Success : EmployeeListUiState()
    object Empty : EmployeeListUiState()
    data class Error(val message: String) : EmployeeListUiState()
}
