package com.attendance.facerecognition.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.attendance.facerecognition.data.local.database.AppDatabase
import com.attendance.facerecognition.data.repository.AttendanceAuditRepository
import com.attendance.facerecognition.data.repository.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.concurrent.TimeUnit

// Extension para DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Gestor de retención de datos
 * Controla cuánto tiempo se conservan los registros de asistencia y auditoría
 */
class DataRetentionManager(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val attendanceRepository = AttendanceRepository(database.attendanceDao())
    private val auditRepository = AttendanceAuditRepository(database.attendanceAuditDao())

    companion object {
        private val ATTENDANCE_RETENTION_DAYS = intPreferencesKey("attendance_retention_days")
        private val AUDIT_RETENTION_DAYS = intPreferencesKey("audit_retention_days")

        const val DEFAULT_ATTENDANCE_RETENTION = 90 // 90 días
        const val DEFAULT_AUDIT_RETENTION = 180 // 180 días
    }

    /**
     * Obtiene los días de retención para registros de asistencia
     */
    val attendanceRetentionDays: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[ATTENDANCE_RETENTION_DAYS] ?: DEFAULT_ATTENDANCE_RETENTION
        }

    /**
     * Obtiene los días de retención para registros de auditoría
     */
    val auditRetentionDays: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[AUDIT_RETENTION_DAYS] ?: DEFAULT_AUDIT_RETENTION
        }

    /**
     * Configura los días de retención para registros de asistencia
     */
    suspend fun setAttendanceRetentionDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[ATTENDANCE_RETENTION_DAYS] = days
        }
    }

    /**
     * Configura los días de retención para registros de auditoría
     */
    suspend fun setAuditRetentionDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[AUDIT_RETENTION_DAYS] = days
        }
    }

    /**
     * Elimina registros antiguos según la política de retención
     */
    suspend fun cleanOldRecords() {
        // Obtener configuración actual
        val attendanceDays = attendanceRetentionDays
        val auditDays = auditRetentionDays

        var attendanceRetention = DEFAULT_ATTENDANCE_RETENTION
        var auditRetention = DEFAULT_AUDIT_RETENTION

        attendanceDays.collect { attendanceRetention = it }
        auditDays.collect { auditRetention = it }

        // Calcular timestamp de corte
        val calendar = Calendar.getInstance()

        calendar.add(Calendar.DAY_OF_YEAR, -attendanceRetention)
        val attendanceCutoff = calendar.timeInMillis

        calendar.time = Calendar.getInstance().time
        calendar.add(Calendar.DAY_OF_YEAR, -auditRetention)
        val auditCutoff = calendar.timeInMillis

        // Eliminar registros antiguos
        attendanceRepository.deleteOldRecords(attendanceCutoff)
        auditRepository.deleteOldAudits(auditCutoff)
    }

    /**
     * Opciones de retención predefinidas
     */
    enum class RetentionOption(val days: Int, val label: String) {
        ONE_MONTH(30, "1 mes"),
        THREE_MONTHS(90, "3 meses"),
        SIX_MONTHS(180, "6 meses"),
        ONE_YEAR(365, "1 año"),
        TWO_YEARS(730, "2 años"),
        FOREVER(Int.MAX_VALUE, "Siempre")
    }
}
