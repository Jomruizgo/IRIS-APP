package com.attendance.facerecognition.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.attendance.facerecognition.data.local.entities.AttendanceType
import com.attendance.facerecognition.data.local.entities.PendingAttendanceRecord
import com.attendance.facerecognition.data.local.entities.PendingReason
import com.attendance.facerecognition.ui.components.AdminBiometricPrompt
import com.attendance.facerecognition.ui.viewmodels.PendingApprovalViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingApprovalScreen(
    onNavigateBack: () -> Unit,
    viewModel: PendingApprovalViewModel = viewModel()
) {
    val pendingRecords by viewModel.pendingRecords.collectAsState(initial = emptyList())
    val pendingCount by viewModel.pendingCount.collectAsState(initial = 0)

    var showApproveDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var showAdminAuth by remember { mutableStateOf(false) }
    var selectedRecord by remember { mutableStateOf<PendingAttendanceRecord?>(null) }
    var actionToPerform by remember { mutableStateOf<(() -> Unit)?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registros Pendientes ($pendingCount)") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
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
        if (pendingRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "No hay registros pendientes",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "Todos los registros han sido revisados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pendingRecords) { record ->
                    PendingRecordCard(
                        record = record,
                        onApprove = {
                            selectedRecord = record
                            showApproveDialog = true
                        },
                        onReject = {
                            selectedRecord = record
                            showRejectDialog = true
                        }
                    )
                }
            }
        }
    }

    // Diálogo de confirmar aprobación
    if (showApproveDialog && selectedRecord != null) {
        AlertDialog(
            onDismissRequest = { showApproveDialog = false },
            icon = { Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Aprobar Registro") },
            text = {
                Column {
                    Text("¿Confirmas la aprobación de este registro?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Empleado: ${selectedRecord!!.employeeName ?: selectedRecord!!.employeeId}",
                        fontWeight = FontWeight.Bold
                    )
                    Text("Tipo: ${if (selectedRecord!!.type == AttendanceType.ENTRY) "ENTRADA" else "SALIDA"}")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showApproveDialog = false
                        actionToPerform = {
                            viewModel.approveRecord(
                                record = selectedRecord!!,
                                supervisorId = 1L, // TODO: Obtener del SessionManager
                                onSuccess = { selectedRecord = null },
                                onError = { /* Mostrar error */ }
                            )
                        }
                        showAdminAuth = true
                    }
                ) {
                    Text("Aprobar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApproveDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de rechazar
    if (showRejectDialog && selectedRecord != null) {
        var rejectNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            icon = { Icon(Icons.Filled.Cancel, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Rechazar Registro") },
            text = {
                Column {
                    Text("¿Por qué rechazas este registro?")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = rejectNotes,
                        onValueChange = { rejectNotes = it },
                        label = { Text("Razón del rechazo") },
                        placeholder = { Text("Ej: Foto no corresponde al empleado") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rejectNotes.isNotBlank()) {
                            showRejectDialog = false
                            actionToPerform = {
                                viewModel.rejectRecord(
                                    record = selectedRecord!!,
                                    supervisorId = 1L,
                                    notes = rejectNotes,
                                    onSuccess = { selectedRecord = null },
                                    onError = { /* Mostrar error */ }
                                )
                            }
                            showAdminAuth = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Rechazar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Autenticación administrativa
    if (showAdminAuth && actionToPerform != null) {
        AdminBiometricPrompt(
            title = "Autorizar Acción",
            message = "Confirma tu identidad para procesar este registro",
            onSuccess = {
                actionToPerform?.invoke()
                showAdminAuth = false
                actionToPerform = null
            },
            onDismiss = {
                showAdminAuth = false
                actionToPerform = null
            }
        )
    }
}

@Composable
private fun PendingRecordCard(
    record: PendingAttendanceRecord,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Foto capturada
            AsyncImage(
                model = record.photoPath,
                contentDescription = "Foto del empleado",
                modifier = Modifier.size(80.dp)
            )

            // Información
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = record.employeeName ?: "ID: ${record.employeeId}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        if (record.type == AttendanceType.ENTRY) Icons.AutoMirrored.Filled.Login else Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        if (record.type == AttendanceType.ENTRY) "ENTRADA" else "SALIDA",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    dateFormat.format(Date(record.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    "Razón: ${getReasonText(record.reason)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Botones de acción
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onApprove,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(Icons.Filled.Check, "Aprobar", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    onClick = onReject,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Icon(Icons.Filled.Close, "Rechazar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun getReasonText(reason: PendingReason): String {
    return when (reason) {
        PendingReason.FACIAL_FAILED -> "Reconocimiento facial falló"
        PendingReason.NOT_ENROLLED -> "Empleado no registrado"
        PendingReason.MANUAL_REQUEST -> "Solicitud manual"
        PendingReason.TECHNICAL_ISSUE -> "Problema técnico"
    }
}
