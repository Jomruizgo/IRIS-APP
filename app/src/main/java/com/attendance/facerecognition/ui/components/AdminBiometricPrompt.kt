package com.attendance.facerecognition.ui.components

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendance.facerecognition.biometric.BiometricKeyManager
import com.attendance.facerecognition.data.local.database.AppDatabase
import com.attendance.facerecognition.data.repository.UserRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Componente reutilizable para solicitar autenticación biométrica de un administrador
 * antes de realizar operaciones críticas.
 *
 * USO:
 * ```kotlin
 * var showAdminAuth by remember { mutableStateOf(false) }
 *
 * if (showAdminAuth) {
 *     AdminBiometricPrompt(
 *         title = "Confirmar eliminación",
 *         message = "Esta acción no se puede deshacer",
 *         onSuccess = {
 *             // Realizar operación crítica
 *             deleteEmployee()
 *             showAdminAuth = false
 *         },
 *         onDismiss = { showAdminAuth = false }
 *     )
 * }
 * ```
 */
@Composable
fun AdminBiometricPrompt(
    title: String,
    message: String,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isAuthenticating by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    // Obtener la Activity correctamente desde el context
    val activity = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is FragmentActivity) {
                return@remember ctx
            }
            ctx = ctx.baseContext
        }
        null
    }

    // Iniciar autenticación automáticamente
    LaunchedEffect(Unit) {
        if (activity == null) {
            errorMessage = "Error: No se puede acceder a la autenticación biométrica"
            return@LaunchedEffect
        }

        isAuthenticating = true

        try {
            val database = AppDatabase.getDatabase(context)
            val userRepository = UserRepository(database.userDao())
            val biometricKeyManager = BiometricKeyManager(context.applicationContext as Application)

            // Buscar un admin con huella habilitada
            val adminWithFingerprint = userRepository.getAdminWithFingerprint()

            if (adminWithFingerprint == null) {
                // No hay admin con huella, mostrar diálogo de PIN
                isAuthenticating = false
                showPinDialog = true
                return@LaunchedEffect
            }

            if (adminWithFingerprint.fingerprintKeystoreAlias.isNullOrEmpty()) {
                errorMessage = "El administrador no tiene huella vinculada.\n\nContacta al administrador del sistema."
                isAuthenticating = false
                return@LaunchedEffect
            }

            // Mostrar prompt biométrico
            biometricKeyManager.verifyFingerprint(
                keystoreAlias = adminWithFingerprint.fingerprintKeystoreAlias!!,
                employeeName = "Administrador: ${adminWithFingerprint.fullName}",
                activity = activity,
                onSuccess = {
                    isAuthenticating = false
                    onSuccess()
                },
                onError = { error ->
                    isAuthenticating = false
                    errorMessage = error
                }
            )

        } catch (e: Exception) {
            isAuthenticating = false
            errorMessage = "Error: ${e.message}"
        }
    }

    // Mostrar diálogo de PIN si no hay huella
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(message)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            pin = it
                            pinError = null
                        },
                        label = { Text("PIN") },
                        placeholder = { Text("1234") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = pinError != null
                    )
                    if (pinError != null) {
                        Text(
                            text = pinError!!,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                android.util.Log.d("AdminBiometricPrompt", "Verificando PIN: ${pin.length} dígitos")
                                val database = AppDatabase.getDatabase(context)
                                val userRepository = UserRepository(database.userDao())

                                // Verificar PIN
                                val user = userRepository.verifyUserByPin(pin)
                                android.util.Log.d("AdminBiometricPrompt", "Usuario encontrado: ${user?.username}, Role: ${user?.role}")

                                if (user != null &&
                                    (user.role == com.attendance.facerecognition.data.local.entities.UserRole.ADMIN ||
                                     user.role == com.attendance.facerecognition.data.local.entities.UserRole.SUPERVISOR) &&
                                    user.isActive) {
                                    android.util.Log.d("AdminBiometricPrompt", "PIN correcto, ejecutando onSuccess()")
                                    showPinDialog = false
                                    onSuccess()
                                } else {
                                    android.util.Log.w("AdminBiometricPrompt", "PIN incorrecto o sin permisos")
                                    pinError = "PIN incorrecto o sin permisos"
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("AdminBiometricPrompt", "Error verificando PIN", e)
                                pinError = "Error: ${e.message}"
                            }
                        }
                    },
                    enabled = pin.length >= 4
                ) {
                    Text("Autenticar")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Mostrar diálogo de error si ocurre (solo cuando NO está mostrando PIN)
    if (errorMessage != null && !showPinDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Error de Autenticación") },
            text = { Text(errorMessage!!) },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
        )
    }
}

/**
 * Extensión para UserRepository para buscar admin con huella
 */
private suspend fun UserRepository.getAdminWithFingerprint(): com.attendance.facerecognition.data.local.entities.User? {
    // Usar first() en lugar de collect() para evitar bloqueo infinito
    val users = getAllUsers().first()

    return users.firstOrNull {
        it.role == com.attendance.facerecognition.data.local.entities.UserRole.ADMIN &&
        it.hasFingerprintEnabled &&
        it.isActive
    }
}
