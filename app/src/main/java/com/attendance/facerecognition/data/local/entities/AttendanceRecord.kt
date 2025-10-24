package com.attendance.facerecognition.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = Employee::class,
            parentColumns = ["id"],
            childColumns = ["employeeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["employeeId"]), Index(value = ["timestamp"])]
)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val employeeId: Long, // FK a Employee
    val employeeName: String, // Desnormalizado para queries rápidas
    val employeeIdNumber: String, // ID del empleado en el sistema

    val timestamp: Long = System.currentTimeMillis(),
    val type: AttendanceType, // ENTRY o EXIT

    // Metadatos del reconocimiento
    val confidence: Float, // Nivel de confianza del reconocimiento (0-100)
    val livenessScore: Float, // Score de la verificación de vida
    val livenessChallenge: String, // Qué challenge se usó (ej: "blink_twice")

    // Estado de sincronización
    val isSynced: Boolean = false,
    val syncedAt: Long? = null,
    val syncRetries: Int = 0,

    // Ubicación (opcional, si agregas GPS en el futuro)
    val latitude: Double? = null,
    val longitude: Double? = null
)

enum class AttendanceType {
    ENTRY,  // Entrada
    EXIT    // Salida
}

/**
 * Data class para vistas más complejas
 */
data class AttendanceWithEmployee(
    val record: AttendanceRecord,
    val employee: Employee
)

/**
 * Estadísticas de asistencia por día
 */
data class DailyAttendanceStats(
    val date: String, // formato YYYY-MM-DD
    val totalEntries: Int,
    val totalExits: Int,
    val uniqueEmployees: Int
)
