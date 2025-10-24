package com.attendance.facerecognition.data.local.dao

import androidx.room.*
import com.attendance.facerecognition.data.local.entities.AttendanceAudit
import com.attendance.facerecognition.data.local.entities.AuditAction
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceAuditDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(audit: AttendanceAudit): Long

    @Query("SELECT * FROM attendance_audit ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAudits(limit: Int = 100): Flow<List<AttendanceAudit>>

    @Query("SELECT * FROM attendance_audit WHERE attendanceId = :attendanceId ORDER BY timestamp DESC")
    fun getAuditsByAttendanceId(attendanceId: Long): Flow<List<AttendanceAudit>>

    @Query("SELECT * FROM attendance_audit WHERE employeeIdDetected = :employeeId OR employeeIdActual = :employeeId ORDER BY timestamp DESC")
    fun getAuditsByEmployeeId(employeeId: String): Flow<List<AttendanceAudit>>

    @Query("SELECT * FROM attendance_audit WHERE action = :action ORDER BY timestamp DESC LIMIT :limit")
    fun getAuditsByAction(action: AuditAction, limit: Int = 50): Flow<List<AttendanceAudit>>

    @Query("SELECT * FROM attendance_audit WHERE performedByUserId = :userId ORDER BY timestamp DESC")
    fun getAuditsByUser(userId: Long): Flow<List<AttendanceAudit>>

    @Query("SELECT * FROM attendance_audit WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp ORDER BY timestamp DESC")
    fun getAuditsByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<AttendanceAudit>>

    @Query("DELETE FROM attendance_audit WHERE timestamp < :olderThan")
    suspend fun deleteOldAudits(olderThan: Long)

    @Query("SELECT COUNT(*) FROM attendance_audit WHERE action = :action")
    suspend fun getAuditCountByAction(action: AuditAction): Int

    @Query("DELETE FROM attendance_audit")
    suspend fun deleteAllAudits()
}
