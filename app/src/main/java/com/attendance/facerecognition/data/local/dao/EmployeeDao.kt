package com.attendance.facerecognition.data.local.dao

import androidx.room.*
import com.attendance.facerecognition.data.local.entities.Employee
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: Employee): Long

    @Update
    suspend fun updateEmployee(employee: Employee)

    @Delete
    suspend fun deleteEmployee(employee: Employee)

    @Query("SELECT * FROM employees WHERE id = :employeeId")
    suspend fun getEmployeeById(employeeId: Long): Employee?

    @Query("SELECT * FROM employees WHERE employeeId = :employeeIdNumber")
    suspend fun getEmployeeByIdNumber(employeeIdNumber: String): Employee?

    @Query("SELECT * FROM employees WHERE isActive = 1 ORDER BY fullName ASC")
    fun getAllActiveEmployees(): Flow<List<Employee>>

    @Query("SELECT * FROM employees ORDER BY fullName ASC")
    fun getAllEmployees(): Flow<List<Employee>>

    @Query("SELECT * FROM employees WHERE isActive = 1")
    suspend fun getAllActiveEmployeesOnce(): List<Employee>

    @Query("UPDATE employees SET isActive = :isActive WHERE id = :employeeId")
    suspend fun setEmployeeActive(employeeId: Long, isActive: Boolean)

    @Query("SELECT COUNT(*) FROM employees WHERE isActive = 1")
    suspend fun getActiveEmployeeCount(): Int

    @Query("SELECT * FROM employees WHERE fullName LIKE '%' || :searchQuery || '%' OR employeeId LIKE '%' || :searchQuery || '%'")
    fun searchEmployees(searchQuery: String): Flow<List<Employee>>

    @Query("DELETE FROM employees")
    suspend fun deleteAllEmployees()
}
