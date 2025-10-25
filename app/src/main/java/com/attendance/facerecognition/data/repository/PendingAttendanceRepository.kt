package com.attendance.facerecognition.data.repository

import com.attendance.facerecognition.data.local.dao.PendingAttendanceDao
import com.attendance.facerecognition.data.local.entities.PendingAttendanceRecord
import com.attendance.facerecognition.data.local.entities.PendingStatus
import kotlinx.coroutines.flow.Flow
import java.io.File

class PendingAttendanceRepository(private val dao: PendingAttendanceDao) {

    fun getPendingRecords(): Flow<List<PendingAttendanceRecord>> = dao.getPendingRecords()

    fun getPendingCount(): Flow<Int> = dao.getPendingCount()

    fun getAllRecords(): Flow<List<PendingAttendanceRecord>> = dao.getAllRecords()

    fun getRecordsByStatus(status: PendingStatus): Flow<List<PendingAttendanceRecord>> =
        dao.getRecordsByStatus(status)

    suspend fun getPendingById(id: Long): PendingAttendanceRecord? = dao.getPendingById(id)

    fun getTodayPendingRecords(): Flow<List<PendingAttendanceRecord>> = dao.getTodayPendingRecords()

    fun getRecordsByEmployee(employeeId: String): Flow<List<PendingAttendanceRecord>> =
        dao.getRecordsByEmployee(employeeId)

    suspend fun insertPending(record: PendingAttendanceRecord): Long = dao.insertPending(record)

    suspend fun updatePending(record: PendingAttendanceRecord) = dao.updatePending(record)

    suspend fun deletePending(record: PendingAttendanceRecord) = dao.deletePending(record)

    /**
     * Marca registros pendientes como expirados si tienen más de 7 días
     */
    suspend fun markExpiredRecords(): Int {
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        return dao.markExpiredRecords(sevenDaysAgo)
    }

    /**
     * Limpia registros aprobados/rechazados de más de 30 días
     */
    suspend fun cleanupOldReviewedRecords(): Int {
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        return dao.cleanupOldReviewedRecords(thirtyDaysAgo)
    }

    /**
     * Aprueba un registro pendiente
     */
    suspend fun approveRecord(recordId: Long, supervisorId: Long, notes: String? = null) {
        val record = getPendingById(recordId) ?: return
        val approved = record.copy(
            status = PendingStatus.APPROVED,
            reviewedBy = supervisorId,
            reviewedAt = System.currentTimeMillis(),
            reviewNotes = notes
        )
        updatePending(approved)
    }

    /**
     * Rechaza un registro pendiente
     */
    suspend fun rejectRecord(recordId: Long, supervisorId: Long, notes: String) {
        val record = getPendingById(recordId) ?: return
        val rejected = record.copy(
            status = PendingStatus.REJECTED,
            reviewedBy = supervisorId,
            reviewedAt = System.currentTimeMillis(),
            reviewNotes = notes
        )
        updatePending(rejected)
    }

    /**
     * Limpia fotos de registros aprobados/rechazados y elimina los registros
     * Retorna número de fotos eliminadas
     */
    suspend fun cleanupPhotosAndRecords(): Int {
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)

        // Obtener registros antiguos aprobados o rechazados
        val oldRecords = dao.getOldReviewedRecords(thirtyDaysAgo)

        var photosDeleted = 0

        oldRecords.forEach { record ->
            // Eliminar foto del storage
            try {
                val photoFile = File(record.photoPath)
                if (photoFile.exists() && photoFile.delete()) {
                    photosDeleted++
                }
            } catch (e: Exception) {
                // Ignorar errores al eliminar fotos
            }

            // Eliminar registro de la DB
            dao.deletePending(record)
        }

        return photosDeleted
    }

    /**
     * Limpia solo las fotos de registros expirados (sin revisar por 7+ días)
     * pero mantiene los registros en DB para auditoría
     */
    suspend fun cleanupExpiredPhotos(): Int {
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)

        // Obtener registros expirados
        val expiredRecords = dao.getExpiredRecords(sevenDaysAgo)

        var photosDeleted = 0

        expiredRecords.forEach { record ->
            // Eliminar foto del storage
            try {
                val photoFile = File(record.photoPath)
                if (photoFile.exists() && photoFile.delete()) {
                    photosDeleted++
                }
            } catch (e: Exception) {
                // Ignorar errores
            }
        }

        return photosDeleted
    }
}
