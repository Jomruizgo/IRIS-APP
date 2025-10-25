package com.attendance.facerecognition.data.local.dao

import androidx.room.*
import com.attendance.facerecognition.data.local.entities.DeviceRegistration
import com.attendance.facerecognition.data.local.entities.SyncStatusType
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceRegistrationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: DeviceRegistration): Long

    @Update
    suspend fun update(device: DeviceRegistration)

    @Query("SELECT * FROM device_registration LIMIT 1")
    suspend fun getDeviceRegistration(): DeviceRegistration?

    @Query("SELECT * FROM device_registration LIMIT 1")
    fun getDeviceRegistrationFlow(): Flow<DeviceRegistration?>

    @Query("UPDATE device_registration SET lastSyncAt = :timestamp, lastSyncAttemptAt = :timestamp, lastSyncStatus = :status, lastSyncError = NULL")
    suspend fun updateSyncSuccess(timestamp: Long, status: SyncStatusType = SyncStatusType.SUCCESS)

    @Query("UPDATE device_registration SET lastSyncAttemptAt = :timestamp, lastSyncStatus = :status, lastSyncError = :error")
    suspend fun updateSyncError(timestamp: Long, status: SyncStatusType, error: String?)

    @Query("UPDATE device_registration SET lastSyncStatus = :status")
    suspend fun updateSyncStatus(status: SyncStatusType)

    @Query("UPDATE device_registration SET isActive = :isActive")
    suspend fun updateActiveStatus(isActive: Boolean)

    @Query("UPDATE device_registration SET deviceToken = :token, tokenExpiresAt = :expiresAt")
    suspend fun updateToken(token: String, expiresAt: Long?)

    @Query("SELECT COUNT(*) FROM device_registration")
    suspend fun hasRegistration(): Int

    @Query("DELETE FROM device_registration")
    suspend fun deleteAll()
}
