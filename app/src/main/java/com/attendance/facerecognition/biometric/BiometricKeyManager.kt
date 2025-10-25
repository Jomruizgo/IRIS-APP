package com.attendance.facerecognition.biometric

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Gestor de claves criptográficas en Android KeyStore
 * para vincular huellas dactilares específicas por empleado
 *
 * Cada empleado tiene su propia clave SecretKey en KeyStore.
 * La clave está protegida por autenticación biométrica y solo
 * puede desbloquearse con la huella que se registró originalmente.
 */
class BiometricKeyManager(private val context: Context) {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_PREFIX = "fingerprint_key_"
        private const val CIPHER_TRANSFORMATION =
            "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}"
    }

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }

    /**
     * Genera una clave única para un empleado y pide autenticación biométrica
     * para vincularla a su huella
     *
     * @param employeeId ID del empleado
     * @param activity Actividad para mostrar el prompt biométrico
     * @param onSuccess Callback cuando la huella se vincula exitosamente
     * @param onError Callback en caso de error
     */
    fun enrollFingerprint(
        employeeId: String,
        activity: FragmentActivity,
        onSuccess: (keystoreAlias: String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val keyAlias = generateKeyAlias(employeeId)

            // Si ya existe una clave, eliminarla primero
            if (keyStore.containsAlias(keyAlias)) {
                keyStore.deleteEntry(keyAlias)
            }

            // Generar nueva clave con autenticación biométrica requerida
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )

            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(true)
                // No usar setUserAuthenticationValidityDurationSeconds
                // Para que SIEMPRE requiera biometría por operación
                .setInvalidatedByBiometricEnrollment(true) // Se invalida si se agregan nuevas huellas
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()

            // Crear cipher para forzar autenticación inmediata
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            val secretKey = getSecretKey(keyAlias)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            // Mostrar prompt biométrico para vincular la huella
            val biometricPrompt = createBiometricPrompt(
                activity,
                onSuccess = {
                    // La huella se vinculó exitosamente con esta clave
                    onSuccess(keyAlias)
                },
                onError = { error ->
                    // Si falla, eliminar la clave generada
                    keyStore.deleteEntry(keyAlias)
                    onError(error)
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Vincular Huella Digital")
                .setSubtitle("Coloca una de tus huellas registradas en Android")
                .setDescription("Recuerda cuál huella usas, la necesitarás para registrar asistencia")
                .setNegativeButtonText("Cancelar")
                .build()

            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))

        } catch (e: Exception) {
            onError("Error al generar clave: ${e.message}")
        }
    }

    /**
     * Verifica la huella del empleado usando su clave almacenada
     *
     * @param keystoreAlias Alias de la clave del empleado
     * @param activity Actividad para mostrar el prompt biométrico
     * @param onSuccess Callback cuando la huella se verifica exitosamente
     * @param onError Callback en caso de error
     */
    fun verifyFingerprint(
        keystoreAlias: String,
        employeeName: String,
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // Verificar que la clave existe
            if (!keyStore.containsAlias(keystoreAlias)) {
                onError("No hay huella registrada para este empleado.\nContacta al administrador.")
                return
            }

            // Obtener la clave y crear cipher
            val secretKey = getSecretKey(keystoreAlias)
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            // Mostrar prompt biométrico
            val biometricPrompt = createBiometricPrompt(
                activity,
                onSuccess = onSuccess,
                onError = onError
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Verificar Identidad")
                .setSubtitle(employeeName)
                .setDescription("Coloca tu huella digital")
                .setNegativeButtonText("Cancelar")
                .build()

            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))

        } catch (e: Exception) {
            onError("Error al verificar huella: ${e.message}")
        }
    }

    /**
     * Elimina la clave asociada a un empleado
     */
    fun deleteFingerprintKey(keystoreAlias: String): Boolean {
        return try {
            if (keyStore.containsAlias(keystoreAlias)) {
                keyStore.deleteEntry(keystoreAlias)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Verifica si un empleado tiene huella registrada
     */
    fun hasFingerprintKey(keystoreAlias: String): Boolean {
        return try {
            keyStore.containsAlias(keystoreAlias)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Genera el alias de la clave para un empleado
     */
    private fun generateKeyAlias(employeeId: String): String {
        return "$KEY_PREFIX$employeeId"
    }

    /**
     * Obtiene la SecretKey del KeyStore
     */
    private fun getSecretKey(keyAlias: String): SecretKey {
        return keyStore.getKey(keyAlias, null) as SecretKey
    }

    /**
     * Crea un BiometricPrompt con los callbacks
     */
    private fun createBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ): BiometricPrompt {
        val executor = androidx.core.content.ContextCompat.getMainExecutor(context)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)

                // Mensajes más amigables para errores comunes
                val friendlyMessage = when (errorCode) {
                    BiometricPrompt.ERROR_CANCELED -> "Autenticación cancelada"
                    BiometricPrompt.ERROR_USER_CANCELED -> "Cancelado por el usuario"
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> "Cancelado"
                    BiometricPrompt.ERROR_LOCKOUT -> "Demasiados intentos. Intenta más tarde."
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> "Sensor bloqueado. Reinicia el dispositivo."
                    BiometricPrompt.ERROR_NO_BIOMETRICS -> "No hay huellas registradas en el dispositivo"
                    BiometricPrompt.ERROR_HW_NOT_PRESENT -> "Este dispositivo no tiene sensor de huella"
                    BiometricPrompt.ERROR_HW_UNAVAILABLE -> "Sensor de huella no disponible"
                    else -> errString.toString()
                }

                onError(friendlyMessage)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // No llamamos onError aquí porque el usuario puede reintentar
                // El diálogo permanece abierto
            }
        }

        return BiometricPrompt(activity, executor, callback)
    }
}

/**
 * Resultado del registro de huella
 */
sealed class FingerprintEnrollmentResult {
    data class Success(val keystoreAlias: String) : FingerprintEnrollmentResult()
    data class Error(val message: String) : FingerprintEnrollmentResult()
}

/**
 * Resultado de la verificación de huella
 */
sealed class FingerprintVerificationResult {
    object Success : FingerprintVerificationResult()
    data class Error(val message: String) : FingerprintVerificationResult()
}
