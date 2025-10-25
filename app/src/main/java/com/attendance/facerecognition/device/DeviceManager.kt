package com.attendance.facerecognition.device

import android.content.Context
import android.os.Build
import com.attendance.facerecognition.data.local.database.AppDatabase
import com.attendance.facerecognition.data.local.entities.DeviceRegistration
import com.attendance.facerecognition.data.local.entities.SyncStatusType
import com.attendance.facerecognition.data.repository.DeviceRegistrationRepository
import com.attendance.facerecognition.network.RetrofitClient
import com.attendance.facerecognition.network.dto.DeviceRegistrationRequest
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Gestor de registro y autenticación del dispositivo
 * Maneja el ciclo de vida del dispositivo en el sistema
 */
class DeviceManager(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val repository = DeviceRegistrationRepository(database.deviceRegistrationDao())

    /**
     * Verifica si el dispositivo está registrado
     */
    suspend fun isDeviceRegistered(): Boolean {
        return repository.isDeviceRegistered()
    }

    /**
     * Obtiene el registro del dispositivo actual
     */
    suspend fun getDeviceRegistration(): DeviceRegistration? {
        return repository.getDeviceRegistration()
    }

    /**
     * Observa el registro del dispositivo
     */
    fun observeDeviceRegistration(): Flow<DeviceRegistration?> {
        return repository.getDeviceRegistrationFlow()
    }

    /**
     * Obtiene el token del dispositivo para autenticación
     */
    suspend fun getDeviceToken(): String? {
        return repository.getDeviceRegistration()?.deviceToken
    }

    /**
     * Obtiene el ID único del dispositivo
     */
    suspend fun getDeviceId(): String? {
        return repository.getDeviceRegistration()?.deviceId
    }

    /**
     * Parsea el código de activación y extrae el tenant ID
     * Formato esperado: TENANT-CODIGO (ej: "ACME-ABC123")
     *
     * @return Pair(tenantId, code) o null si el formato es inválido
     */
    private fun parseActivationCode(activationCode: String): Pair<String, String>? {
        val parts = activationCode.split("-", limit = 2)
        if (parts.size != 2) {
            return null
        }

        val tenantId = parts[0].trim().uppercase()
        val code = parts[1].trim().uppercase()

        // Validar que ambas partes no estén vacías
        if (tenantId.isEmpty() || code.isEmpty()) {
            return null
        }

        // Validar que tenant tenga formato válido (solo letras y números)
        if (!tenantId.matches(Regex("^[A-Z0-9]+$"))) {
            return null
        }

        // Validar que el código tenga al menos 3 caracteres
        if (code.length < 3) {
            return null
        }

        return Pair(tenantId, code)
    }

    /**
     * Registra un nuevo dispositivo con el código de activación
     *
     * NOTA: En producción, esto debe hacer una llamada al servidor para validar
     * el código y obtener el token. Por ahora, genera un token mock.
     *
     * @param activationCode Código en formato TENANT-CODIGO (ej: "ACME-ABC123")
     * @param deviceName Nombre amigable para el dispositivo
     * @return DeviceRegistrationResult con el resultado del registro
     */
    suspend fun registerDevice(
        activationCode: String,
        deviceName: String
    ): DeviceRegistrationResult {
        // Parsear código para extraer tenant
        val parsed = parseActivationCode(activationCode)
        if (parsed == null) {
            return DeviceRegistrationResult.Error(
                "Formato de código inválido. Use: TENANT-CODIGO (ej: ACME-ABC123)"
            )
        }

        val (tenantId, code) = parsed

        // Verificar si ya está registrado
        if (isDeviceRegistered()) {
            return DeviceRegistrationResult.Error("Este dispositivo ya está registrado")
        }

        try {
            // Generar UUID para este dispositivo
            val deviceId = UUID.randomUUID().toString()

            // Crear request para el API
            val request = DeviceRegistrationRequest(
                activationCode = activationCode,
                deviceId = deviceId,
                deviceName = deviceName,
                deviceModel = Build.MODEL,
                deviceManufacturer = Build.MANUFACTURER,
                androidVersion = Build.VERSION.RELEASE
            )

            // Llamar al endpoint POST /api/devices/register
            val api = RetrofitClient.getApiService(context)
            val response = api.registerDevice(request)

            when {
                response.isSuccessful -> {
                    val responseData = response.body()?.data
                    if (responseData != null) {
                        // Crear entidad con datos del servidor
                        val device = DeviceRegistration(
                            deviceId = responseData.deviceId,
                            tenantId = responseData.tenantId,
                            deviceName = deviceName,
                            activationCode = activationCode,
                            deviceToken = responseData.deviceToken,
                            deviceModel = Build.MODEL,
                            deviceManufacturer = Build.MANUFACTURER,
                            androidVersion = Build.VERSION.RELEASE,
                            registeredAt = responseData.registeredAt,
                            lastSyncAt = null,
                            lastSyncStatus = SyncStatusType.NEVER_SYNCED,
                            isActive = responseData.isActive,
                            tokenExpiresAt = responseData.tokenExpiresAt
                        )

                        repository.registerDevice(device)
                        return DeviceRegistrationResult.Success(device)
                    } else {
                        return DeviceRegistrationResult.Error("Respuesta inválida del servidor")
                    }
                }
                response.code() == 400 -> {
                    return DeviceRegistrationResult.Error("Código de activación inválido o ya usado")
                }
                response.code() == 409 -> {
                    return DeviceRegistrationResult.Error("Este dispositivo ya está registrado")
                }
                response.code() == 422 -> {
                    return DeviceRegistrationResult.Error("Datos de registro inválidos")
                }
                else -> {
                    val errorMsg = try {
                        response.errorBody()?.string() ?: "Error al conectar con el servidor"
                    } catch (e: Exception) {
                        "Error al conectar con el servidor"
                    }
                    return DeviceRegistrationResult.Error(errorMsg)
                }
            }

        } catch (e: Exception) {
            return DeviceRegistrationResult.Error("Error de red: ${e.message}")
        }
    }

    /**
     * Actualiza el estado de sincronización después de un intento exitoso
     */
    suspend fun updateSyncSuccess() {
        repository.updateSyncSuccess()
    }

    /**
     * Actualiza el estado de sincronización después de un error
     */
    suspend fun updateSyncError(status: SyncStatusType, error: String?) {
        repository.updateSyncError(status, error)
    }

    /**
     * Marca la sincronización como en progreso
     */
    suspend fun setSyncInProgress() {
        repository.updateSyncStatus(SyncStatusType.IN_PROGRESS)
    }

    /**
     * Desregistra el dispositivo (elimina toda la información local)
     * Útil para reiniciar la app o cambiar de servidor
     */
    suspend fun unregisterDevice() {
        repository.unregisterDevice()
    }

    /**
     * Genera un token mock para desarrollo
     * En producción, esto viene del servidor
     */
    private fun generateMockToken(tenantId: String): String {
        // JWT mock simplificado (solo para desarrollo)
        val header = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        val payload = android.util.Base64.encodeToString(
            """{"tenant_id":"$tenantId","device_id":"${UUID.randomUUID()}","iat":${System.currentTimeMillis()/1000}}""".toByteArray(),
            android.util.Base64.NO_WRAP
        )
        val signature = "mock_signature_for_development"
        return "$header.$payload.$signature"
    }

    /**
     * Obtiene el tenant ID del dispositivo
     */
    suspend fun getTenantId(): String? {
        return repository.getDeviceRegistration()?.tenantId
    }

    /**
     * Verifica si el token ha expirado
     */
    suspend fun isTokenExpired(): Boolean {
        val device = repository.getDeviceRegistration() ?: return true
        val expiresAt = device.tokenExpiresAt ?: return false // Token sin expiración
        return System.currentTimeMillis() > expiresAt
    }

    /**
     * Verifica si el dispositivo está activo
     */
    suspend fun isDeviceActive(): Boolean {
        return repository.getDeviceRegistration()?.isActive ?: false
    }
}

/**
 * Resultado del proceso de registro
 */
sealed class DeviceRegistrationResult {
    data class Success(val device: DeviceRegistration) : DeviceRegistrationResult()
    data class Error(val message: String) : DeviceRegistrationResult()
}
