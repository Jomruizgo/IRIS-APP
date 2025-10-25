package com.attendance.facerecognition.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendance.facerecognition.ui.viewmodels.EmployeeDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDetailScreen(
    employeeId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit = {},
    viewModel: EmployeeDetailViewModel = viewModel()
) {
    val employee by viewModel.employee.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showToggleStatusDialog by remember { mutableStateOf(false) }

    LaunchedEffect(employeeId) {
        viewModel.loadEmployee(employeeId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalles del Empleado") },
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
        if (employee == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val emp = employee!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card de información principal
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = emp.fullName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "ID: ${emp.employeeId}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        // Badge de estado
                        Surface(
                            color = if (emp.isActive)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.error,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = if (emp.isActive) "ACTIVO" else "INACTIVO",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (emp.isActive)
                                    MaterialTheme.colorScheme.onTertiary
                                else
                                    MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }

                // Card de información laboral
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Información Laboral",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        DetailRow(
                            icon = Icons.Filled.Business,
                            label = "Departamento",
                            value = emp.department
                        )

                        DetailRow(
                            icon = Icons.Filled.Work,
                            label = "Cargo",
                            value = emp.position
                        )

                        DetailRow(
                            icon = Icons.Filled.CalendarToday,
                            label = "Fecha de Registro",
                            value = formatDate(emp.createdAt)
                        )
                    }
                }

                // Card de métodos biométricos
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Métodos Biométricos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        BiometricMethodRow(
                            icon = Icons.Filled.Face,
                            label = "Reconocimiento Facial",
                            enabled = emp.faceEmbeddings.isNotEmpty(),
                            detail = if (emp.faceEmbeddings.isNotEmpty())
                                "${emp.faceEmbeddings.size} fotos registradas"
                            else
                                "No configurado"
                        )

                        BiometricMethodRow(
                            icon = Icons.Filled.Fingerprint,
                            label = "Huella Digital",
                            enabled = emp.hasFingerprintEnabled,
                            detail = if (emp.hasFingerprintEnabled)
                                "Configurada"
                            else
                                "No configurada"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón editar
                    OutlinedButton(
                        onClick = { onNavigateToEdit(employeeId) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar")
                    }

                    // Botón activar/desactivar
                    OutlinedButton(
                        onClick = { showToggleStatusDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (emp.isActive)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Icon(
                            if (emp.isActive) Icons.Filled.Block else Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (emp.isActive) "Desactivar" else "Activar")
                    }
                }

                // Botón eliminar
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Eliminar Empleado")
                }
            }
        }
    }

    // Diálogo de confirmación de eliminación
    if (showDeleteDialog && employee != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
            title = { Text("Confirmar Eliminación") },
            text = {
                Text("¿Estás seguro de eliminar a ${employee!!.fullName}?\n\nEsta acción no se puede deshacer y se eliminarán todos los registros de asistencia asociados.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEmployee()
                        showDeleteDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de confirmación de cambio de estado
    if (showToggleStatusDialog && employee != null) {
        val emp = employee!!
        AlertDialog(
            onDismissRequest = { showToggleStatusDialog = false },
            icon = { Icon(
                if (emp.isActive) Icons.Filled.Block else Icons.Filled.CheckCircle,
                contentDescription = null
            ) },
            title = { Text(if (emp.isActive) "Desactivar Empleado" else "Activar Empleado") },
            text = {
                Text(
                    if (emp.isActive)
                        "¿Desactivar a ${emp.fullName}?\n\nNo podrá registrar asistencia mientras esté inactivo."
                    else
                        "¿Activar a ${emp.fullName}?\n\nPodrá volver a registrar asistencia."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleEmployeeStatus()
                        showToggleStatusDialog = false
                    }
                ) {
                    Text(if (emp.isActive) "Desactivar" else "Activar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showToggleStatusDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun BiometricMethodRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    detail: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = if (enabled) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
