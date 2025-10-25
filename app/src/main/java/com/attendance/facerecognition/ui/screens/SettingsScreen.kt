package com.attendance.facerecognition.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendance.facerecognition.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val attendanceRetentionDays by viewModel.attendanceRetentionDays.collectAsState(initial = 90)
    val auditRetentionDays by viewModel.auditRetentionDays.collectAsState(initial = 180)
    val serverUrl by viewModel.serverUrl.collectAsState(initial = "")
    val cleanupResult by viewModel.cleanupResult.collectAsState()

    var showCleanupDialog by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf(false) }
    var showServerUrlDialog by remember { mutableStateOf(false) }
    var editingServerUrl by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Sección de Retención de Datos
            Text(
                text = "Retención de Datos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Retención de Asistencia
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Registros de Asistencia",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Los registros se eliminarán automáticamente después de este período",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Retención actual: $attendanceRetentionDays días",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    // Botones de opciones rápidas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RetentionButton(
                            label = "30 días",
                            onClick = { viewModel.setAttendanceRetention(30) },
                            modifier = Modifier.weight(1f)
                        )
                        RetentionButton(
                            label = "90 días",
                            onClick = { viewModel.setAttendanceRetention(90) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RetentionButton(
                            label = "180 días",
                            onClick = { viewModel.setAttendanceRetention(180) },
                            modifier = Modifier.weight(1f)
                        )
                        RetentionButton(
                            label = "1 año",
                            onClick = { viewModel.setAttendanceRetention(365) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Retención de Auditoría
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Registros de Auditoría",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Los registros de auditoría se conservan más tiempo por seguridad",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Retención actual: $auditRetentionDays días",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    // Botones de opciones rápidas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RetentionButton(
                            label = "90 días",
                            onClick = { viewModel.setAuditRetention(90) },
                            modifier = Modifier.weight(1f)
                        )
                        RetentionButton(
                            label = "180 días",
                            onClick = { viewModel.setAuditRetention(180) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RetentionButton(
                            label = "1 año",
                            onClick = { viewModel.setAuditRetention(365) },
                            modifier = Modifier.weight(1f)
                        )
                        RetentionButton(
                            label = "2 años",
                            onClick = { viewModel.setAuditRetention(730) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Sección de Configuración de Sincronización
            Text(
                text = "Sincronización en la Nube",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "URL del Servidor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "Dirección del backend para sincronización de datos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    Text(
                        text = serverUrl.ifEmpty { "No configurada" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                editingServerUrl = serverUrl
                                showServerUrlDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Modificar")
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.resetServerUrl()
                                Toast.makeText(
                                    context,
                                    "URL restaurada al valor por defecto",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Restaurar")
                        }
                    }
                }
            }

            // Botón de limpieza manual
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Limpieza Manual",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Eliminar ahora todos los registros que excedan el período de retención configurado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Button(
                        onClick = { showCleanupDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Ejecutar Limpieza")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Diálogo de confirmación
    if (showCleanupDialog) {
        AlertDialog(
            onDismissRequest = { showCleanupDialog = false },
            title = { Text("Confirmar Limpieza") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("¿Estás seguro de eliminar todos los registros antiguos?")
                    Text(
                        text = "IMPORTANTE: Solo se eliminarán registros que ya fueron sincronizados con el servidor. Los registros pendientes de sincronización se conservarán.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Esta acción no se puede deshacer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cleanOldRecords()
                        showCleanupDialog = false
                        showResultDialog = true
                    }
                ) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanupDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de resultado de limpieza
    LaunchedEffect(cleanupResult) {
        if (cleanupResult != null) {
            showResultDialog = true
        }
    }

    if (showResultDialog && cleanupResult != null) {
        AlertDialog(
            onDismissRequest = {
                showResultDialog = false
                viewModel.clearCleanupResult()
            },
            title = { Text("Resultado de Limpieza") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Limpieza completada correctamente",
                        fontWeight = FontWeight.Bold
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text("Registros de asistencia eliminados: ${cleanupResult!!.attendanceDeleted}")
                    Text("Registros de auditoría eliminados: ${cleanupResult!!.auditDeleted}")

                    if (cleanupResult!!.unsyncedSkipped > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = "Registros pendientes de sincronización conservados: ${cleanupResult!!.unsyncedSkipped}",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Estos registros se eliminarán automáticamente después de ser sincronizados y cumplir el período de retención.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResultDialog = false
                        viewModel.clearCleanupResult()
                    }
                ) {
                    Text("Aceptar")
                }
            }
        )
    }

    // Diálogo para editar URL del servidor
    if (showServerUrlDialog) {
        AlertDialog(
            onDismissRequest = { showServerUrlDialog = false },
            title = { Text("Configurar URL del Servidor") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Ingresa la URL completa del backend (debe terminar con /)",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = editingServerUrl,
                        onValueChange = { editingServerUrl = it },
                        label = { Text("URL del servidor") },
                        placeholder = { Text("https://api.iris-attendance.com/") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text(
                        text = "Ejemplo: https://api.iris-attendance.com/",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "IMPORTANTE: La app se reconectará con el nuevo servidor. Asegúrate de que la URL sea correcta.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateServerUrl(editingServerUrl)
                        showServerUrlDialog = false
                        Toast.makeText(
                            context,
                            "URL actualizada correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    enabled = editingServerUrl.trim().isNotEmpty()
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerUrlDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun RetentionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(label)
    }
}
