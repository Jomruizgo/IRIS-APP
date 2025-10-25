package com.attendance.facerecognition.data.local.dao

import androidx.room.*
import com.attendance.facerecognition.data.local.entities.PendingAttendanceRecord
import com.attendance.facerecognition.data.local.entities.PendingStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingAttendanceDao {

    /**
     * Obtiene todos los registros pendientes ordenados por fecha (más recientes primero)
     */
    @Query("SELECT * FROM pending_attendance_records WHERE status = 'PENDING' ORDER BY timestamp DESC")
    fun getPendingRecords(): Flow<List<PendingAttendanceRecord>>

    /**
     * Obtiene contador de registros pendientes (para badge de notificaciones)
     */
    @Query("SELECT COUNT(*) FROM pending_attendance_records WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    /**
     * Obtiene todos los registros (incluidos aprobados, rechazados, expirados)
     */
    @Query("SELECT * FROM pending_attendance_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<PendingAttendanceRecord>>

    /**
     * Obtiene registros por estado
     */
    @Query("SELECT * FROM pending_attendance_records WHERE status = :status ORDER BY timestamp DESC")
    fun getRecordsByStatus(status: PendingStatus): Flow<List<PendingAttendanceRecord>>

    /**
     * Obtiene un registro por ID
     */
    @Query("SELECT * FROM pending_attendance_records WHERE id = :id")
    suspend fun getPendingById(id: Long): PendingAttendanceRecord?

    /**
     * Obtiene registros de hoy (pendientes)
     */
    @Query("""
        SELECT * FROM pending_attendance_records
        WHERE status = 'PENDING'
        AND date(timestamp / 1000, 'unixepoch') = date('now')
        ORDER BY timestamp DESC
    """)
    fun getTodayPendingRecords(): Flow<List<PendingAttendanceRecord>>

    /**
     * Obtiene registros de un empleado específico
     */
    @Query("SELECT * FROM pending_attendance_records WHERE employeeId = :employeeId ORDER BY timestamp DESC")
    fun getRecordsByEmployee(employeeId: String): Flow<List<PendingAttendanceRecord>>

    /**
     * Inserta un nuevo registro pendiente
     */
    @Insert
    suspend fun insertPending(record: PendingAttendanceRecord): Long

    /**
     * Actualiza un registro pendiente (usado para aprobar/rechazar)
     */
    @Update
    suspend fun updatePending(record: PendingAttendanceRecord)

    /**
     * Elimina un registro pendiente
     */
    @Delete
    suspend fun deletePending(record: PendingAttendanceRecord)

    /**
     * Marca registros viejos como expirados (> 7 días sin revisión)
     */
    @Query("""
        UPDATE pending_attendance_records
        SET status = 'EXPIRED'
        WHERE status = 'PENDING'
        AND createdAt < :expirationTimestamp
    """)
    suspend fun markExpiredRecords(expirationTimestamp: Long): Int

    /**
     * Elimina registros aprobados/rechazados antiguos (> 30 días) para limpieza
     */
    @Query("""
        DELETE FROM pending_attendance_records
        WHERE status IN ('APPROVED', 'REJECTED')
        AND reviewedAt < :cleanupTimestamp
    """)
    suspend fun cleanupOldReviewedRecords(cleanupTimestamp: Long): Int

    /**
     * Obtiene registros aprobados/rechazados antiguos para limpieza de fotos
     */
    @Query("""
        SELECT * FROM pending_attendance_records
        WHERE status IN ('APPROVED', 'REJECTED')
        AND reviewedAt < :cleanupTimestamp
    """)
    suspend fun getOldReviewedRecords(cleanupTimestamp: Long): List<PendingAttendanceRecord>

    /**
     * Obtiene registros expirados (sin revisar por 7+ días)
     */
    @Query("""
        SELECT * FROM pending_attendance_records
        WHERE status = 'PENDING'
        AND createdAt < :expirationTimestamp
    """)
    suspend fun getExpiredRecords(expirationTimestamp: Long): List<PendingAttendanceRecord>
}
