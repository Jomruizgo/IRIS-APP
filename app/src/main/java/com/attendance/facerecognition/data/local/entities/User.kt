package com.attendance.facerecognition.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val username: String,
    val fullName: String,
    val pinHash: String, // Hash SHA-256 del PIN
    val role: UserRole,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long? = null
)

enum class UserRole {
    ADMIN,      // Acceso completo: gestión de empleados, usuarios, reportes, configuración
    SUPERVISOR, // Solo lectura: ver empleados, ver reportes (no crear/editar)
    USER        // Mínimo: solo marcar asistencia (pantalla pública)
}
