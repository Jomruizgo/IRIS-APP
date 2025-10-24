package com.attendance.facerecognition.data.repository

import com.attendance.facerecognition.data.local.dao.AttendanceAuditDao
import com.attendance.facerecognition.data.local.entities.AttendanceAudit
import com.attendance.facerecognition.data.local.entities.AuditAction
import kotlinx.coroutines.flow.Flow

class AttendanceAuditRepository(private val auditDao: AttendanceAuditDao) {

    /**
     * Registra una acción de auditoría
     */
    suspend fun insertAudit(audit: AttendanceAudit): Long =
        auditDao.insertAudit(audit)

    /**
     * Obtiene los registros de auditoría más recientes
     */
    fun getRecentAudits(limit: Int = 100): Flow<List<AttendanceAudit>> =
        auditDao.getRecentAudits(limit)

    /**
     * Obtiene auditorías para un registro de asistencia específico
     */
    fun getAuditsByAttendanceId(attendanceId: Long): Flow<List<AttendanceAudit>> =
        auditDao.getAuditsByAttendanceId(attendanceId)

    /**
     * Obtiene auditorías para un empleado
     */
    fun getAuditsByEmployeeId(employeeId: String): Flow<List<AttendanceAudit>> =
        auditDao.getAuditsByEmployeeId(employeeId)

    /**
     * Obtiene auditorías por tipo de acción
     */
    fun getAuditsByAction(action: AuditAction, limit: Int = 50): Flow<List<AttendanceAudit>> =
        auditDao.getAuditsByAction(action, limit)

    /**
     * Obtiene auditorías realizadas por un usuario admin
     */
    fun getAuditsByUser(userId: Long): Flow<List<AttendanceAudit>> =
        auditDao.getAuditsByUser(userId)

    /**
     * Obtiene auditorías en un rango de fechas
     */
    fun getAuditsByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<AttendanceAudit>> =
        auditDao.getAuditsByDateRange(startTimestamp, endTimestamp)

    /**
     * Elimina auditorías antiguas
     */
    suspend fun deleteOldAudits(olderThan: Long) =
        auditDao.deleteOldAudits(olderThan)

    /**
     * Obtiene el conteo de auditorías por acción
     */
    suspend fun getAuditCountByAction(action: AuditAction): Int =
        auditDao.getAuditCountByAction(action)
}
