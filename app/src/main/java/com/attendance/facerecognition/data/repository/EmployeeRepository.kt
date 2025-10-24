package com.attendance.facerecognition.data.repository

import com.attendance.facerecognition.data.local.dao.EmployeeDao
import com.attendance.facerecognition.data.local.entities.Employee
import kotlinx.coroutines.flow.Flow

class EmployeeRepository(private val employeeDao: EmployeeDao) {

    /**
     * Obtener todos los empleados
     */
    fun getAllEmployees(): Flow<List<Employee>> = employeeDao.getAllEmployees()

    /**
     * Obtener todos los empleados activos
     */
    fun getAllActiveEmployees(): Flow<List<Employee>> = employeeDao.getAllActiveEmployees()

    /**
     * Obtener empleado por ID
     */
    suspend fun getEmployeeById(id: Long): Employee? = employeeDao.getEmployeeById(id)

    /**
     * Buscar empleado por employeeId (ID de negocio)
     */
    suspend fun getEmployeeByEmployeeId(employeeId: String): Employee? =
        employeeDao.getEmployeeByIdNumber(employeeId)

    /**
     * Insertar nuevo empleado
     */
    suspend fun insertEmployee(employee: Employee): Long = employeeDao.insertEmployee(employee)

    /**
     * Actualizar empleado existente
     */
    suspend fun updateEmployee(employee: Employee) = employeeDao.updateEmployee(employee)

    /**
     * Eliminar empleado
     */
    suspend fun deleteEmployee(employee: Employee) = employeeDao.deleteEmployee(employee)

    /**
     * Verificar si existe un empleado con el employeeId
     */
    suspend fun employeeExists(employeeId: String): Boolean =
        getEmployeeByEmployeeId(employeeId) != null

    /**
     * Obtener todos los embeddings de todos los empleados para comparación
     */
    suspend fun getAllEmployeesWithEmbeddings(): List<Employee> =
        employeeDao.getAllActiveEmployeesOnce()
}
