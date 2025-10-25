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
    val lastLogin: Long? = null,

    // Autenticación biométrica (solo para ADMIN)
    // Permite usar huella digital para autorizar operaciones críticas
    val hasFingerprintEnabled: Boolean = false,

    // Alias de la clave en Android KeyStore asociada a la huella del usuario
    // Solo tiene valor si hasFingerprintEnabled = true
    // Esta clave solo puede desbloquearse con una huella válida del dispositivo
    // NOTA: Debido a limitaciones de Android, cualquier huella registrada en el dispositivo
    // puede desbloquear esta clave. Por eso solo se usa para ADMIN (2-3 personas máximo).
    val fingerprintKeystoreAlias: String? = null
)

enum class UserRole {
    ADMIN,      // Acceso completo: gestión de empleados, usuarios, reportes, configuración
    SUPERVISOR, // Solo lectura: ver empleados, ver reportes (no crear/editar)
    USER        // Mínimo: solo marcar asistencia (pantalla pública)
}
