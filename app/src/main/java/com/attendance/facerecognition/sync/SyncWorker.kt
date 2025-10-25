package com.attendance.facerecognition.sync

import android.content.Context
import androidx.work.*
import com.attendance.facerecognition.data.local.database.AppDatabase
import com.attendance.facerecognition.data.local.entities.AttendanceType
import com.attendance.facerecognition.data.local.entities.SyncStatusType
import com.attendance.facerecognition.data.repository.AttendanceRepository
import com.attendance.facerecognition.device.DeviceManager
import com.attendance.facerecognition.network.RetrofitClient
import com.attendance.facerecognition.network.dto.AttendanceRecordDto
import com.attendance.facerecognition.network.dto.AttendanceSyncRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Worker para sincronización automática de registros de asistencia con el backend
 * Se ejecuta periódicamente cuando hay conexión a internet
 *
 * IMPORTANTE: Solo funciona si el dispositivo está registrado y tiene un token válido
 */
class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val database = AppDatabase.getDatabase(context)
    private val attendanceRepository = AttendanceRepository(database.attendanceDao())
    private val deviceManager = DeviceManager(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Verificar que el dispositivo esté registrado
            if (!deviceManager.isDeviceRegistered()) {
                deviceManager.updateSyncError(
                    SyncStatusType.ERROR_UNAUTHORIZED,
                    "Dispositivo no registrado"
                )
                return@withContext Result.failure()
            }

            // Verificar que el dispositivo esté activo
            if (!deviceManager.isDeviceActive()) {
                deviceManager.updateSyncError(
                    SyncStatusType.ERROR_DEVICE_DISABLED,
                    "Dispositivo desactivado por el administrador"
                )
                return@withContext Result.failure()
            }

            // Obtener el token de autenticación
            val deviceToken = deviceManager.getDeviceToken()
            if (deviceToken == null) {
                deviceManager.updateSyncError(
                    SyncStatusType.ERROR_UNAUTHORIZED,
                    "Token de autenticación no disponible"
                )
                return@withContext Result.failure()
            }

            // Verificar si el token ha expirado
            if (deviceManager.isTokenExpired()) {
                deviceManager.updateSyncError(
                    SyncStatusType.ERROR_UNAUTHORIZED,
                    "Token de autenticación expirado"
                )
                return@withContext Result.failure()
            }

            // Marcar sincronización en progreso
            deviceManager.setSyncInProgress()

            // Obtener registros no sincronizados
            val unsyncedRecords = attendanceRepository.getUnsyncedRecords()

            if (unsyncedRecords.isEmpty()) {
                // No hay nada que sincronizar, pero actualizamos el timestamp
                deviceManager.updateSyncSuccess()
                return@withContext Result.success()
            }

            // Obtener tenant ID para las requests
            val tenantId = deviceManager.getTenantId()
            if (tenantId == null) {
                deviceManager.updateSyncError(
                    SyncStatusType.ERROR_UNAUTHORIZED,
                    "Tenant ID no disponible"
                )
                return@withContext Result.failure()
            }

            // Convertir registros a DTOs
            val recordDtos = unsyncedRecords.map { record ->
                AttendanceRecordDto(
                    localId = record.id,
                    employeeId = record.employeeIdNumber,
                    type = record.type.name,
                    timestamp = record.timestamp,
                    confidence = record.confidence,
                    livenessPassed = record.livenessScore >= 0.5f,
                    deviceId = deviceManager.getDeviceId() ?: "",
                    createdAt = record.timestamp
                )
            }

            // Llamada HTTP al backend
            val api = RetrofitClient.getApiService(applicationContext)
            val request = AttendanceSyncRequest(records = recordDtos)
            val response = api.syncAttendanceRecords(
                tenantId = tenantId,
                request = request
            )

            when {
                response.isSuccessful -> {
                    val body = response.body()
                    if (body != null && body.success) {
                        // Marcar registros sincronizados
                        val syncedIds = body.syncedRecords.map { it.localId }
                        attendanceRepository.markAsSynced(syncedIds)

                        // Log conflictos si los hay
                        if (body.conflicts.isNotEmpty()) {
                            body.conflicts.forEach { conflict ->
                                android.util.Log.w("SyncWorker", "Conflicto en registro ${conflict.localId}: ${conflict.message}")
                            }
                        }

                        deviceManager.updateSyncSuccess()
                        Result.success(
                            workDataOf(
                                "synced_count" to body.syncedCount,
                                "conflicts" to body.conflicts.size,
                                "timestamp" to System.currentTimeMillis()
                            )
                        )
                    } else {
                        deviceManager.updateSyncError(
                            SyncStatusType.ERROR_NETWORK,
                            "Respuesta inválida del servidor"
                        )
                        Result.failure()
                    }
                }
                response.code() == 401 -> {
                    // Token inválido o expirado
                    deviceManager.updateSyncError(
                        SyncStatusType.ERROR_UNAUTHORIZED,
                        "Token de autenticación inválido"
                    )
                    Result.failure()
                }
                response.code() == 403 -> {
                    // Dispositivo desactivado
                    deviceManager.updateSyncError(
                        SyncStatusType.ERROR_DEVICE_DISABLED,
                        "Dispositivo desactivado por el administrador"
                    )
                    Result.failure()
                }
                response.code() >= 500 -> {
                    // Error del servidor
                    deviceManager.updateSyncError(
                        SyncStatusType.ERROR_SERVER,
                        "Error del servidor: ${response.code()}"
                    )
                    Result.retry()
                }
                else -> {
                    val errorMsg = try {
                        response.errorBody()?.string() ?: "Error desconocido"
                    } catch (e: Exception) {
                        "Error ${response.code()}"
                    }
                    deviceManager.updateSyncError(
                        SyncStatusType.ERROR_NETWORK,
                        errorMsg
                    )
                    Result.retry()
                }
            }

        } catch (e: Exception) {
            // Error de red u otro error inesperado
            deviceManager.updateSyncError(
                SyncStatusType.ERROR_NETWORK,
                "Error de red: ${e.message}"
            )
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "attendance_sync_work"

        /**
         * Programa la sincronización periódica
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                repeatInterval = 15, // Cada 15 minutos
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }

        /**
         * Fuerza una sincronización inmediata
         */
        fun syncNow(context: Context) {
            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueue(syncRequest)
        }

        /**
         * Cancela la sincronización periódica
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
