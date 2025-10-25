package com.attendance.facerecognition.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/**
 * Registros de asistencia pendientes de aprobación por supervisor
 *
 * Se crean cuando:
 * - El reconocimiento facial falla (confianza < 70%)
 * - El empleado solicita registro manual
 * - El empleado no tiene embeddings registrados
 */
@Entity(tableName = "pending_attendance_records")
data class PendingAttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Datos del empleado
    val employeeId: String,        // ID ingresado manualmente
    val employeeName: String?,      // Nombre si se encontró en BD, null si no existe

    // Registro
    val timestamp: Long = System.currentTimeMillis(),
    val type: AttendanceType,       // ENTRY o EXIT

    // Evidencia
    val photoPath: String,          // Ruta a foto capturada: /storage/.../pending/{timestamp}_{employeeId}.jpg
    val deviceId: String,           // ID del dispositivo que registró
    val reason: PendingReason,      // Razón por la que es pendiente

    // Estado de revisión
    val status: PendingStatus = PendingStatus.PENDING,
    val reviewedBy: Long? = null,   // ID del supervisor que revisó
    val reviewedAt: Long? = null,
    val reviewNotes: String? = null, // Notas del supervisor (opcional, ej: por qué se rechazó)

    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Razón por la que el registro está pendiente
 */
enum class PendingReason {
    FACIAL_FAILED,      // Reconocimiento facial falló (confianza < 70%)
    NOT_ENROLLED,       // Empleado no tiene embeddings faciales
    MANUAL_REQUEST,     // Empleado solicitó registro manual explícitamente
    TECHNICAL_ISSUE     // Problema técnico (cámara no disponible, etc.)
}

/**
 * Estado del registro pendiente
 */
enum class PendingStatus {
    PENDING,    // Esperando revisión
    APPROVED,   // Aprobado por supervisor (se crea AttendanceRecord)
    REJECTED,   // Rechazado por supervisor
    EXPIRED     // Expirado (> 7 días sin revisión)
}

/**
 * TypeConverter para PendingReason
 */
class PendingReasonConverter {
    @TypeConverter
    fun fromPendingReason(value: PendingReason): String {
        return value.name
    }

    @TypeConverter
    fun toPendingReason(value: String): PendingReason {
        return PendingReason.valueOf(value)
    }
}

/**
 * TypeConverter para PendingStatus
 */
class PendingStatusConverter {
    @TypeConverter
    fun fromPendingStatus(value: PendingStatus): String {
        return value.name
    }

    @TypeConverter
    fun toPendingStatus(value: String): PendingStatus {
        return PendingStatus.valueOf(value)
    }
}
