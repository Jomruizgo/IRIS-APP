package com.attendance.facerecognition.data.local.dao

import androidx.room.*
import com.attendance.facerecognition.data.local.entities.AttendanceRecord
import com.attendance.facerecognition.data.local.entities.AttendanceType
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: AttendanceRecord): Long

    @Update
    suspend fun updateAttendance(record: AttendanceRecord)

    @Delete
    suspend fun deleteAttendance(record: AttendanceRecord)

    @Query("SELECT * FROM attendance_records WHERE id = :recordId")
    suspend fun getAttendanceById(recordId: Long): AttendanceRecord?

    @Query("SELECT * FROM attendance_records ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAttendance(limit: Int = 50): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE employeeId = :employeeId ORDER BY timestamp DESC")
    fun getAttendanceByEmployee(employeeId: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp ORDER BY timestamp DESC")
    fun getAttendanceByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedRecords(): List<AttendanceRecord>

    @Query("UPDATE attendance_records SET isSynced = 1, syncedAt = :syncedAt WHERE id IN (:recordIds)")
    suspend fun markAsSynced(recordIds: List<Long>, syncedAt: Long = System.currentTimeMillis())

    @Query("UPDATE attendance_records SET syncRetries = syncRetries + 1 WHERE id = :recordId")
    suspend fun incrementSyncRetries(recordId: Int)

    @Query("SELECT * FROM attendance_records WHERE employeeId = :employeeId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastAttendanceForEmployee(employeeId: Long): AttendanceRecord?

    @Query("""
        SELECT * FROM attendance_records
        WHERE employeeId = :employeeId
        AND DATE(timestamp / 1000, 'unixepoch', 'localtime') = DATE('now', 'localtime')
        ORDER BY timestamp DESC
    """)
    suspend fun getTodayAttendanceForEmployee(employeeId: Long): List<AttendanceRecord>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE isSynced = 0")
    suspend fun getUnsyncedRecordCount(): Int

    @Query("""
        SELECT COUNT(DISTINCT employeeId) FROM attendance_records
        WHERE DATE(timestamp / 1000, 'unixepoch', 'localtime') = DATE('now', 'localtime')
        AND type = :type
    """)
    suspend fun getTodayUniqueEmployeeCount(type: AttendanceType): Int

    @Query("DELETE FROM attendance_records WHERE timestamp < :olderThan")
    suspend fun deleteOldRecords(olderThan: Long)

    @Query("DELETE FROM attendance_records")
    suspend fun deleteAllRecords()
}
