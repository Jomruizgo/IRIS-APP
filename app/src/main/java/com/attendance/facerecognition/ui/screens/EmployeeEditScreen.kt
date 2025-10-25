package com.attendance.facerecognition.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendance.facerecognition.biometric.BiometricKeyManager
import com.attendance.facerecognition.ui.viewmodels.EmployeeEditViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeEditScreen(
    employeeId: Long,
    onNavigateBack: () -> Unit,
    viewModel: EmployeeEditViewModel = viewModel()
) {
    val context = LocalContext.current
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
    val employee by viewModel.employee.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    var fullName by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(true) }
    var hasFingerprintEnabled by remember { mutableStateOf(false) }
    var hasRegisteredFingerprint by remember { mutableStateOf(false) }

    var showFingerprintEnrollment by remember { mutableStateOf(false) }
    var fingerprintEnrollmentError by remember { mutableStateOf<String?>(null) }

    val biometricKeyManager = remember { BiometricKeyManager(context) }

    // Cargar datos del empleado
    LaunchedEffect(employeeId) {
        viewModel.loadEmployee(employeeId)
    }

    // Actualizar campos cuando se cargue el empleado
    LaunchedEffect(employee) {
        employee?.let { emp ->
            fullName = emp.fullName
            department = emp.department
            position = emp.position
            isActive = emp.isActive
            hasFingerprintEnabled = emp.hasFingerprintEnabled
            hasRegisteredFingerprint = !emp.fingerprintKeystoreAlias.isNullOrEmpty()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Empleado") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.updateEmployee(
                                fullName = fullName,
                                department = department,
                                position = position,
                                isActive = isActive,
                                hasFingerprintEnabled = hasFingerprintEnabled,
                                onSuccess = {
                                    Toast.makeText(
                                        context,
                                        "Empleado actualizado correctamente",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onNavigateBack()
                                },
                                onError = { error ->
                                    Toast.makeText(
                                        context,
                                        "Error: $error",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        },
                        enabled = !isSaving && fullName.isNotBlank() && department.isNotBlank() && position.isNotBlank()
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Save, contentDescription = "Guardar")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Información no editable
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Información Fija",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "ID: ${employee!!.employeeId}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Este ID no se puede modificar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // Campos editables
                Text(
                    text = "Información Editable",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Nombre Completo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSaving
                )

                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Departamento") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSaving
                )

                OutlinedTextField(
                    value = position,
                    onValueChange = { position = it },
                    label = { Text("Cargo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSaving
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Switch de estado activo
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Estado del Empleado",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isActive) "Activo - Puede registrar asistencia" else "Inactivo - No puede registrar asistencia",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it },
                            enabled = !isSaving
                        )
                    }
                }

                // Switch de huella digital
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Habilitar Huella Digital",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (hasRegisteredFingerprint) {
                                        "Huella registrada ✓"
                                    } else {
                                        "Permitir autenticación con huella"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (hasRegisteredFingerprint) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    },
                                    fontWeight = if (hasRegisteredFingerprint) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                            Switch(
                                checked = hasFingerprintEnabled,
                                onCheckedChange = { hasFingerprintEnabled = it },
                                enabled = !isSaving
                            )
                        }

                        // Botón para registrar huella
                        if (hasFingerprintEnabled && !hasRegisteredFingerprint) {
                            Button(
                                onClick = {
                                    when {
                                        activity == null -> {
                                            Toast.makeText(
                                                context,
                                                "Error: No se puede acceder a la actividad",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        employee == null -> {
                                            Toast.makeText(
                                                context,
                                                "Error: Datos del empleado no cargados",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        else -> {
                                            biometricKeyManager.enrollFingerprint(
                                                employeeId = employee!!.employeeId,
                                                activity = activity,
                                                onSuccess = { keystoreAlias ->
                                                    viewModel.updateFingerprintAlias(
                                                        keystoreAlias = keystoreAlias,
                                                        onSuccess = {
                                                            hasRegisteredFingerprint = true
                                                            Toast.makeText(context, "Huella registrada exitosamente", Toast.LENGTH_SHORT).show()
                                                        },
                                                        onError = { error ->
                                                            fingerprintEnrollmentError = error
                                                        }
                                                    )
                                                },
                                                onError = { error ->
                                                    fingerprintEnrollmentError = error
                                                }
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                Icon(Icons.Filled.Fingerprint, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Registrar Huella Ahora")
                            }

                            Text(
                                text = "Debes registrar la huella del empleado para que pueda autenticarse",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botón guardar (también está en TopBar)
                Button(
                    onClick = {
                        viewModel.updateEmployee(
                            fullName = fullName,
                            department = department,
                            position = position,
                            isActive = isActive,
                            hasFingerprintEnabled = hasFingerprintEnabled,
                            onSuccess = {
                                Toast.makeText(
                                    context,
                                    "Empleado actualizado correctamente",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onNavigateBack()
                            },
                            onError = { error ->
                                Toast.makeText(
                                    context,
                                    "Error: $error",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving && fullName.isNotBlank() && department.isNotBlank() && position.isNotBlank()
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardando...")
                    } else {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar Cambios")
                    }
                }

                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                ) {
                    Text("Cancelar")
                }
            }
        }
    }

    // Diálogo de error de huella
    fingerprintEnrollmentError?.let { error ->
        AlertDialog(
            onDismissRequest = { fingerprintEnrollmentError = null },
            icon = { Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Error al Registrar Huella") },
            text = { Text(error) },
            confirmButton = {
                Button(onClick = { fingerprintEnrollmentError = null }) {
                    Text("Entendido")
                }
            }
        )
    }
}
