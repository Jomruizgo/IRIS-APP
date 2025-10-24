package com.attendance.facerecognition.sync

import android.content.Context
import androidx.work.*
import com.attendance.facerecognition.data.local.database.AppDatabase
import com.attendance.facerecognition.data.repository.AttendanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Worker para sincronización automática de registros de asistencia con el backend
 * Se ejecuta periódicamente cuando hay conexión a internet
 */
class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val database = AppDatabase.getDatabase(context)
    private val attendanceRepository = AttendanceRepository(database.attendanceDao())

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Obtener registros no sincronizados
            val unsyncedRecords = attendanceRepository.getUnsyncedRecords()

            if (unsyncedRecords.isEmpty()) {
                return@withContext Result.success()
            }

            // TODO: Implementar llamada HTTP al backend
            // Por ahora, simulamos sincronización exitosa
            // En producción, aquí iría Retrofit o similar

            /*
            val api = RetrofitClient.getAttendanceApi()
            val response = api.syncAttendanceRecords(unsyncedRecords)

            if (response.isSuccessful) {
                val syncedIds = response.body()?.syncedIds ?: emptyList()
                attendanceRepository.markAsSynced(syncedIds)
                Result.success()
            } else {
                Result.retry()
            }
            */

            // Por ahora solo marcamos como pendiente
            Result.success(
                workDataOf(
                    "synced_count" to unsyncedRecords.size,
                    "timestamp" to System.currentTimeMillis()
                )
            )

        } catch (e: Exception) {
            // Reintentar en caso de error
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
