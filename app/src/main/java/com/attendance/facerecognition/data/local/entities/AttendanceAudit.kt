package com.attendance.facerecognition.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Registro de auditoría para todas las acciones sobre attendance_records
 * Permite rastrear:
 * - Creaciones
 * - Cancelaciones por usuario ("Este no soy yo")
 * - Eliminaciones por admin
 * - Registros forzados
 */
@Entity(
    tableName = "attendance_audit",
    foreignKeys = [
        ForeignKey(
            entity = AttendanceRecord::class,
            parentColumns = ["id"],
            childColumns = ["attendanceId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("attendanceId"),
        Index("employeeIdDetected"),
        Index("performedByUserId"),
        Index("timestamp")
    ]
)
data class AttendanceAudit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * ID del registro de asistencia afectado (puede ser NULL si fue cancelado antes de guardar)
     */
    val attendanceId: Long? = null,

    /**
     * Tipo de acción realizada
     */
    val action: AuditAction,

    /**
     * ID del empleado que fue detectado por el sistema (puede ser incorrecto en caso de falso positivo)
     */
    val employeeIdDetected: String? = null,

    /**
     * ID del empleado real (si se corrigió después)
     */
    val employeeIdActual: String? = null,

    /**
     * ID del usuario administrador que realizó la acción (NULL para acciones del usuario normal)
     */
    val performedByUserId: Long? = null,

    /**
     * Razón de la acción (especialmente importante para DELETED_BY_ADMIN y FORCED_BY_ADMIN)
     */
    val reason: String? = null,

    /**
     * Metadatos adicionales en formato JSON
     * Ejemplos:
     * - confidence score
     * - timestamp original
     * - tipo de registro (ENTRY/EXIT)
     */
    val metadata: String? = null,

    /**
     * Timestamp de cuándo se realizó la acción
     */
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Tipos de acciones de auditoría
 */
enum class AuditAction {
    /**
     * Registro creado exitosamente
     */
    CREATED,

    /**
     * Usuario rechazó la identificación presionando "Este no soy yo"
     */
    CANCELLED_BY_USER,

    /**
     * Administrador eliminó un registro incorrecto
     */
    DELETED_BY_ADMIN,

    /**
     * Administrador forzó un registro que violaba reglas de validación
     */
    FORCED_BY_ADMIN,

    /**
     * Administrador modificó un registro existente
     */
    MODIFIED_BY_ADMIN
}
