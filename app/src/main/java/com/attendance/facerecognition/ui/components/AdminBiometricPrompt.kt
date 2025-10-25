package com.attendance.facerecognition.ui.components

import android.app.Application
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendance.facerecognition.biometric.BiometricKeyManager
import com.attendance.facerecognition.data.local.database.AppDatabase
import com.attendance.facerecognition.data.repository.UserRepository
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
                errorMessage = "No hay administradores con huella registrada.\n\nUsa el PIN para autenticarte."
                isAuthenticating = false
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

    // Mostrar diálogo de error si ocurre
    if (errorMessage != null) {
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
    val allUsers = getAllUsers()
    var adminUser: com.attendance.facerecognition.data.local.entities.User? = null

    allUsers.collect { users ->
        adminUser = users.firstOrNull {
            it.role == com.attendance.facerecognition.data.local.entities.UserRole.ADMIN &&
            it.hasFingerprintEnabled &&
            it.isActive
        }
    }

    return adminUser
}
