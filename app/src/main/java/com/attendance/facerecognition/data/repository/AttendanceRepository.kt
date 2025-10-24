package com.attendance.facerecognition.data.repository

import com.attendance.facerecognition.data.local.dao.AttendanceDao
import com.attendance.facerecognition.data.local.entities.AttendanceRecord
import com.attendance.facerecognition.data.local.entities.AttendanceType
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.Date

class AttendanceRepository(private val attendanceDao: AttendanceDao) {

    /**
     * Obtener registros recientes
     */
    fun getRecentRecords(limit: Int = 50): Flow<List<AttendanceRecord>> =
        attendanceDao.getRecentAttendance(limit)

    /**
     * Obtener registros de un empleado específico
     */
    fun getRecordsByEmployee(employeeId: Long): Flow<List<AttendanceRecord>> =
        attendanceDao.getAttendanceByEmployee(employeeId)

    /**
     * Obtener registros en un rango de fechas
     */
    fun getRecordsByDateRange(startDate: Date, endDate: Date): Flow<List<AttendanceRecord>> =
        attendanceDao.getAttendanceByDateRange(startDate.time, endDate.time)

    /**
     * Insertar nuevo registro de asistencia
     */
    suspend fun insertRecord(record: AttendanceRecord): Long =
        attendanceDao.insertAttendance(record)

    /**
     * Obtener el último registro de un empleado
     */
    suspend fun getLastRecordForEmployee(employeeId: Long): AttendanceRecord? =
        attendanceDao.getLastAttendanceForEmployee(employeeId)

    /**
     * Verificar si el empleado ya registró entrada hoy
     */
    suspend fun hasCheckedInToday(employeeId: Long): Boolean {
        val todayRecords = attendanceDao.getTodayAttendanceForEmployee(employeeId)
        return todayRecords.isNotEmpty()
    }

    /**
     * Determinar si el siguiente registro debe ser entrada o salida
     */
    suspend fun getNextRecordType(employeeId: Long): AttendanceType {
        val lastRecord = getLastRecordForEmployee(employeeId)

        // Si no hay registro previo o el último fue salida, el siguiente es entrada
        if (lastRecord == null || lastRecord.type == AttendanceType.EXIT) {
            return AttendanceType.ENTRY
        }

        // Si el último fue entrada, el siguiente es salida
        return AttendanceType.EXIT
    }

    /**
     * Obtener registros no sincronizados
     */
    suspend fun getUnsyncedRecords(): List<AttendanceRecord> =
        attendanceDao.getUnsyncedRecords()

    /**
     * Marcar registros como sincronizados
     */
    suspend fun markAsSynced(recordIds: List<Long>) =
        attendanceDao.markAsSynced(recordIds)
}
