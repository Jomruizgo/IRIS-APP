package com.attendance.facerecognition.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.biometric.BiometricManager
import androidx.fragment.app.FragmentActivity

/**
 * Pantalla para registrar/vincular huella dactilar de un empleado
 * Se muestra después de capturar las fotos faciales si el usuario
 * activó la opción de huella digital
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FingerprintEnrollmentScreen(
    employeeName: String,
    employeeId: String,
    onEnroll: (FragmentActivity) -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current

    // Obtener la Activity correctamente desde el context
    val activity = remember(context) {
        // Método 1: Intentar cast directo
        (context as? FragmentActivity) ?:
        // Método 2: Buscar en la cadena de ContextWrapper
        generateSequence(context as? android.content.ContextWrapper) {
            it.baseContext as? android.content.ContextWrapper
        }.firstOrNull { it is FragmentActivity } as? FragmentActivity
    }

    // Mostrar error si no se puede obtener la activity
    var showActivityError by remember { mutableStateOf(false) }

    // Verificar si hay huellas registradas en el sistema
    val biometricManager = remember { BiometricManager.from(context) }
    val canAuthenticate = remember {
        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    }

    val hasNoFingerprints = canAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED

    LaunchedEffect(activity) {
        if (activity == null) {
            showActivityError = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Huella Digital") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Icono de huella grande
            Icon(
                imageVector = Icons.Filled.Fingerprint,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            // Información del empleado
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = employeeName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "ID: $employeeId",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Instrucciones
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "¿Qué necesitas saber?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    InfoItem(
                        number = "1",
                        text = "Debes tener al menos una huella registrada en Configuración de Android"
                    )
                    InfoItem(
                        number = "2",
                        text = "Se te pedirá colocar una de tus huellas para vincularla con tu cuenta"
                    )
                    InfoItem(
                        number = "3",
                        text = "Recuerda cuál huella usaste, la necesitarás para registrar asistencia"
                    )
                    InfoItem(
                        number = "4",
                        text = "Si omites este paso, solo podrás usar reconocimiento facial"
                    )
                }
            }

            // Advertencia si no hay huellas registradas
            if (hasNoFingerprints) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⚠️ No hay huellas registradas",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Ve a Configuración → Seguridad → Huella Digital y registra al menos una huella en tu dispositivo Android antes de continuar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Botones
            Button(
                onClick = {
                    if (activity != null) {
                        onEnroll(activity)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = activity != null && !hasNoFingerprints
            ) {
                Icon(
                    Icons.Filled.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Registrar Huella Ahora",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Omitir (Solo Reconocimiento Facial)")
            }
        }

        // Diálogo de error si no se puede acceder a la Activity
        if (showActivityError) {
            AlertDialog(
                onDismissRequest = onSkip,
                icon = {
                    Icon(
                        Icons.Filled.Fingerprint,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("Error de Configuración") },
                text = {
                    Text(
                        text = "No se puede acceder al sistema de autenticación biométrica en este momento.\n\n" +
                               "Puedes omitir este paso y usar solo reconocimiento facial, o reiniciar la aplicación e intentar nuevamente.",
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(onClick = onSkip) {
                        Text("Omitir y Continuar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onSkip) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
private fun InfoItem(number: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Diálogo de éxito después de registrar huella
 */
@Composable
fun FingerprintEnrollmentSuccessDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
        },
        title = {
            Text(
                text = "¡Huella Registrada!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Tu huella dactilar se ha vinculado exitosamente con tu cuenta.",
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Ahora podrás registrar tu asistencia usando:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "• Reconocimiento Facial\n• ID + Huella Digital",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Entendido")
            }
        }
    )
}

/**
 * Diálogo de error durante registro
 */
@Composable
fun FingerprintEnrollmentErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.Fingerprint,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Error al Registrar Huella") },
        text = {
            Text(
                text = errorMessage,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(onClick = onRetry) {
                Text("Reintentar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
