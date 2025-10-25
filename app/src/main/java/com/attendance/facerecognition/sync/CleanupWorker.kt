package com.attendance.facerecognition.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.attendance.facerecognition.data.local.database.AppDatabase
import com.attendance.facerecognition.data.repository.AttendanceRepository
import com.attendance.facerecognition.data.repository.AttendanceAuditRepository
import com.attendance.facerecognition.data.repository.PendingAttendanceRepository
import com.attendance.facerecognition.settings.DataRetentionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Worker para limpieza periódica de:
 * - Registros de asistencia antiguos
 * - Registros de auditoría antiguos
 * - Fotos de registros pendientes aprobados/rechazados
 * - Registros pendientes expirados
 */
class CleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "CleanupWorker"
        const val WORK_NAME = "cleanup_periodic"

        /**
         * Programa limpieza automática diaria
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val cleanupRequest = PeriodicWorkRequestBuilder<CleanupWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.MINUTES
                )
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                cleanupRequest
            )

            Log.i(TAG, "Scheduled periodic cleanup (every 24 hours)")
        }

        /**
         * Ejecuta limpieza manual inmediata
         */
        fun cleanupNow(context: Context) {
            val constraints = Constraints.Builder()
                .build()

            val cleanupRequest = OneTimeWorkRequestBuilder<CleanupWorker>()
                .setConstraints(constraints)
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueue(cleanupRequest)
            Log.i(TAG, "Manual cleanup requested")
        }

        /**
         * Cancela limpieza automática
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.i(TAG, "Canceled periodic cleanup")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting cleanup...")

            val database = AppDatabase.getDatabase(applicationContext)
            val attendanceRepository = AttendanceRepository(database.attendanceDao())
            val auditRepository = AttendanceAuditRepository(database.attendanceAuditDao())
            val pendingRepository = PendingAttendanceRepository(database.pendingAttendanceDao())
            val dataRetentionManager = DataRetentionManager(applicationContext)

            // Usar el método de limpieza del DataRetentionManager que ya maneja la configuración
            val cleanupResult = dataRetentionManager.cleanOldRecords()

            Log.i(TAG, "Deleted ${cleanupResult.attendanceDeleted} old attendance records")
            Log.i(TAG, "Deleted ${cleanupResult.auditDeleted} old audit records")
            Log.i(TAG, "Skipped ${cleanupResult.unsyncedSkipped} unsynced records")

            // 3. Marcar registros pendientes expirados (>7 días sin revisar)
            val markedExpired = pendingRepository.markExpiredRecords()
            Log.i(TAG, "Marked $markedExpired pending records as expired")

            // 4. Limpiar fotos de registros pendientes expirados
            val expiredPhotos = pendingRepository.cleanupExpiredPhotos()
            Log.i(TAG, "Deleted $expiredPhotos photos from expired pending records")

            // 5. Limpiar fotos y registros de pendientes aprobados/rechazados (>30 días)
            val cleanedPhotos = pendingRepository.cleanupPhotosAndRecords()
            Log.i(TAG, "Deleted $cleanedPhotos photos and records from old reviewed pending records")

            Log.i(TAG, "Cleanup completed successfully")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Cleanup failed", e)
            Result.retry()
        }
    }
}
