package com.attendance.facerecognition.network.api

import com.attendance.facerecognition.network.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * API Service para sincronización con backend
 * Implementa todos los endpoints según API Requirements v1.0
 */
interface AttendanceApiService {

    /**
     * 1.1. Registro de Dispositivo
     * POST /api/devices/register
     */
    @POST("api/devices/register")
    suspend fun registerDevice(
        @Body request: DeviceRegistrationRequest
    ): Response<DeviceRegistrationResponse>

    /**
     * 1.2. Renovación de Token
     * POST /api/devices/refresh-token
     */
    @POST("api/devices/refresh-token")
    suspend fun refreshToken(): Response<TokenRefreshResponse>

    /**
     * 1.3. Verificación de Estado del Dispositivo
     * GET /api/devices/status
     */
    @GET("api/devices/status")
    suspend fun getDeviceStatus(): Response<DeviceStatusResponse>

    /**
     * 2.1. Subir Registros de Asistencia
     * POST /api/attendance/sync
     */
    @POST("api/attendance/sync")
    suspend fun syncAttendanceRecords(
        @Header("X-Tenant-ID") tenantId: String,
        @Body request: AttendanceSyncRequest
    ): Response<AttendanceSyncResponse>

    /**
     * 2.2. Descargar Actualizaciones
     * GET /api/attendance/updates?since={timestamp}
     */
    @GET("api/attendance/updates")
    suspend fun getAttendanceUpdates(
        @Query("since") sinceTimestamp: Long
    ): Response<AttendanceUpdatesResponse>

    /**
     * 3.1. Subir Registros de Auditoría
     * POST /api/audit/sync
     */
    @POST("api/audit/sync")
    suspend fun syncAuditRecords(
        @Body request: AuditSyncRequest
    ): Response<AuditSyncResponse>
}

// DTOs adicionales para endpoints faltantes

data class TokenRefreshResponse(
    val device_token: String,
    val token_expires_at: Long?
)

data class DeviceStatusResponse(
    val device_id: String,
    val device_name: String,
    val is_active: Boolean,
    val last_sync_at: Long?,
    val pending_records: Int
)

data class AttendanceUpdatesResponse(
    val updates: List<AttendanceUpdate>,
    val last_sync_timestamp: Long
)

data class AttendanceUpdate(
    val server_id: Long,
    val employee_id: String,
    val type: String,
    val timestamp: Long,
    val device_id: String,
    val action: String, // "CREATED", "DELETED"
    val deleted_by_admin_id: Long?,
    val deletion_reason: String?
)

data class AuditSyncRequest(
    val audits: List<AuditRecordDto>
)

data class AuditRecordDto(
    val local_id: Long,
    val attendance_id: Long,
    val action: String,
    val employee_id_detected: String?,
    val employee_id_actual: String?,
    val performed_by_user_id: Long?,
    val reason: String?,
    val metadata: String?,
    val timestamp: Long
)

data class AuditSyncResponse(
    val success: Boolean,
    val synced_count: Int,
    val synced_audits: List<SyncedAudit>
)

data class SyncedAudit(
    val local_id: Long,
    val server_id: Long,
    val synced_at: Long
)
