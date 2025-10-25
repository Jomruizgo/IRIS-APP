package com.attendance.facerecognition.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendance.facerecognition.biometric.BiometricKeyManager
import kotlinx.coroutines.launch

/**
 * Diálogo para registrar huella digital de un usuario administrador
 *
 * Uso:
 * ```kotlin
 * var showFingerprintEnrollment by remember { mutableStateOf(false) }
 *
 * if (showFingerprintEnrollment) {
 *     UserFingerprintEnrollmentDialog(
 *         userId = user.id,
 *         username = user.username,
 *         onSuccess = { keystoreAlias ->
 *             // Actualizar usuario con el alias
 *             viewModel.updateUserFingerprint(userId, keystoreAlias)
 *             showFingerprintEnrollment = false
 *         },
 *         onDismiss = { showFingerprintEnrollment = false }
 *     )
 * }
 * ```
 */
@Composable
fun UserFingerprintEnrollmentDialog(
    userId: Long,
    username: String,
    onSuccess: (keystoreAlias: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isEnrolling by remember { mutableStateOf(false) }

    // Obtener la Activity
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

    AlertDialog(
        onDismissRequest = { if (!isEnrolling) onDismiss() },
        icon = {
            Icon(
                Icons.Filled.Fingerprint,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                "Vincular Huella Digital",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Usuario: $username",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (isEnrolling) {
                    CircularProgressIndicator(modifier = Modifier.size(40.dp))
                    Text(
                        "Coloca tu huella en el sensor...",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                } else if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            errorMessage!!,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        "Esta huella será requerida para autorizar operaciones críticas como:\n\n" +
                        "• Eliminar empleados\n" +
                        "• Eliminar registros de asistencia\n" +
                        "• Exportar reportes\n" +
                        "• Cambiar configuración del sistema",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start
                    )

                    HorizontalDivider()

                    Text(
                        "⚠️ IMPORTANTE: Debes tener al menos una huella registrada en la configuración de Android.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        },
        confirmButton = {
            if (!isEnrolling && errorMessage == null) {
                Button(
                    onClick = {
                        if (activity == null) {
                            errorMessage = "Error: No se puede acceder a la autenticación biométrica"
                            return@Button
                        }

                        isEnrolling = true
                        scope.launch {
                            try {
                                val biometricKeyManager = BiometricKeyManager(context.applicationContext as Application)

                                biometricKeyManager.enrollFingerprint(
                                    employeeId = "user_$userId",
                                    activity = activity,
                                    onSuccess = { keystoreAlias ->
                                        isEnrolling = false
                                        onSuccess(keystoreAlias)
                                    },
                                    onError = { error ->
                                        isEnrolling = false
                                        errorMessage = error
                                    }
                                )
                            } catch (e: Exception) {
                                isEnrolling = false
                                errorMessage = "Error: ${e.message}"
                            }
                        }
                    }
                ) {
                    Text("Registrar Huella")
                }
            }
        },
        dismissButton = {
            if (!isEnrolling) {
                TextButton(onClick = onDismiss) {
                    Text(if (errorMessage != null) "Cerrar" else "Cancelar")
                }
            }
        }
    )
}
