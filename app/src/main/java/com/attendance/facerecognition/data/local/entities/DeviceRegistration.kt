package com.attendance.facerecognition.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Registro del dispositivo en el sistema
 * Un dispositivo debe estar registrado y autenticado para poder sincronizar datos con el servidor
 */
@Entity(tableName = "device_registration")
data class DeviceRegistration(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * UUID único del dispositivo (generado localmente)
     */
    val deviceId: String,

    /**
     * ID de la organización a la que pertenece este dispositivo
     * Ejemplo: "ACME", "TECH", "CORP01"
     * Se extrae automáticamente del código de activación
     */
    val tenantId: String,

    /**
     * Nombre amigable asignado al dispositivo
     * Ejemplos: "Tablet Entrada Principal", "Celular RH", "Terminal Recepción"
     */
    val deviceName: String,

    /**
     * Código de activación usado para registrar
     * Formato: TENANT-CODIGO (ej: "ACME-ABC123")
     * Se almacena para auditoría
     */
    val activationCode: String,

    /**
     * Token JWT recibido del servidor tras la activación
     * Se envía en el header Authorization de cada request
     */
    val deviceToken: String,

    /**
     * Modelo del dispositivo (Build.MODEL)
     */
    val deviceModel: String,

    /**
     * Marca del dispositivo (Build.MANUFACTURER)
     */
    val deviceManufacturer: String,

    /**
     * Versión de Android (Build.VERSION.RELEASE)
     */
    val androidVersion: String,

    /**
     * Timestamp de cuando se registró el dispositivo
     */
    val registeredAt: Long,

    /**
     * Timestamp de la última sincronización EXITOSA con el servidor
     * NULL si nunca se ha sincronizado
     */
    val lastSyncAt: Long? = null,

    /**
     * Timestamp del último intento de sincronización (exitoso o no)
     */
    val lastSyncAttemptAt: Long? = null,

    /**
     * Estado de la última sincronización
     */
    val lastSyncStatus: SyncStatusType = SyncStatusType.NEVER_SYNCED,

    /**
     * Mensaje de error de la última sincronización fallida
     */
    val lastSyncError: String? = null,

    /**
     * Indica si el dispositivo está activo (el admin puede desactivarlo desde el servidor)
     */
    val isActive: Boolean = true,

    /**
     * Timestamp de cuando el token expira
     * NULL si el token no expira
     */
    val tokenExpiresAt: Long? = null
)

/**
 * Estados posibles de sincronización
 */
enum class SyncStatusType {
    /**
     * Nunca se ha sincronizado desde el registro
     */
    NEVER_SYNCED,

    /**
     * Última sincronización fue exitosa
     */
    SUCCESS,

    /**
     * Sincronización en progreso
     */
    IN_PROGRESS,

    /**
     * Falló por error de red
     */
    ERROR_NETWORK,

    /**
     * Falló por token inválido o expirado
     */
    ERROR_UNAUTHORIZED,

    /**
     * Falló por error del servidor (5xx)
     */
    ERROR_SERVER,

    /**
     * Dispositivo desactivado por el administrador
     */
    ERROR_DEVICE_DISABLED
}
