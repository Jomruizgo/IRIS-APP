package com.attendance.facerecognition.data.repository

import com.attendance.facerecognition.data.local.dao.DeviceRegistrationDao
import com.attendance.facerecognition.data.local.entities.DeviceRegistration
import com.attendance.facerecognition.data.local.entities.SyncStatusType
import kotlinx.coroutines.flow.Flow

class DeviceRegistrationRepository(private val dao: DeviceRegistrationDao) {

    /**
     * Obtiene el registro del dispositivo
     */
    suspend fun getDeviceRegistration(): DeviceRegistration? =
        dao.getDeviceRegistration()

    /**
     * Observa el registro del dispositivo
     */
    fun getDeviceRegistrationFlow(): Flow<DeviceRegistration?> =
        dao.getDeviceRegistrationFlow()

    /**
     * Registra un nuevo dispositivo
     */
    suspend fun registerDevice(device: DeviceRegistration): Long =
        dao.insert(device)

    /**
     * Actualiza el registro del dispositivo
     */
    suspend fun updateDevice(device: DeviceRegistration) =
        dao.update(device)

    /**
     * Actualiza después de una sincronización exitosa
     */
    suspend fun updateSyncSuccess(timestamp: Long = System.currentTimeMillis()) =
        dao.updateSyncSuccess(timestamp)

    /**
     * Actualiza después de un error de sincronización
     */
    suspend fun updateSyncError(
        status: SyncStatusType,
        error: String?,
        timestamp: Long = System.currentTimeMillis()
    ) = dao.updateSyncError(timestamp, status, error)

    /**
     * Actualiza el estado de sincronización
     */
    suspend fun updateSyncStatus(status: SyncStatusType) =
        dao.updateSyncStatus(status)

    /**
     * Verifica si el dispositivo está registrado
     */
    suspend fun isDeviceRegistered(): Boolean =
        dao.hasRegistration() > 0

    /**
     * Actualiza el token del dispositivo
     */
    suspend fun updateToken(token: String, expiresAt: Long? = null) =
        dao.updateToken(token, expiresAt)

    /**
     * Actualiza el estado activo del dispositivo
     */
    suspend fun updateActiveStatus(isActive: Boolean) =
        dao.updateActiveStatus(isActive)

    /**
     * Elimina el registro del dispositivo (para desactivación completa)
     */
    suspend fun unregisterDevice() =
        dao.deleteAll()
}
