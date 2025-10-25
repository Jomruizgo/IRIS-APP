package com.attendance.facerecognition.ui.screens

import androidx.camera.core.CameraSelector
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendance.facerecognition.ui.components.CameraPreview
import com.attendance.facerecognition.ui.viewmodels.EmployeeRegistrationViewModel
import com.attendance.facerecognition.ui.viewmodels.RegistrationUiState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun EmployeeRegistrationScreen(
    onNavigateBack: () -> Unit,
    viewModel: EmployeeRegistrationViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val photoCount by viewModel.photoCount.collectAsState()

    var employeeName by remember { mutableStateOf("") }
    var employeeId by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }
    var enableFingerprint by remember { mutableStateOf(false) }

    var showCamera by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showFingerprintEnrollment by remember { mutableStateOf(false) }
    var showFingerprintSuccessDialog by remember { mutableStateOf(false) }
    var fingerprintEnrollmentError by remember { mutableStateOf<String?>(null) }

    val registeredEmployeeData by viewModel.registeredEmployeeData.collectAsState()

    // Permiso de cámara
    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )

    // Manejar resultado exitoso
    LaunchedEffect(uiState, registeredEmployeeData) {
        when (val state = uiState) {
            is RegistrationUiState.RegistrationSuccess -> {
                val empData = registeredEmployeeData
                if (state.employee.hasFingerprintEnabled && empData != null) {
                    // Mostrar pantalla de registro de huella
                    showFingerprintEnrollment = true
                } else {
                    // Mostrar diálogo de éxito directo
                    showSuccessDialog = true
                }
            }
            else -> {}
        }
    }

    // Mostrar pantalla de registro de huella si corresponde
    if (showFingerprintEnrollment && registeredEmployeeData != null) {
        FingerprintEnrollmentScreen(
            employeeName = registeredEmployeeData!!.second,
            employeeId = registeredEmployeeData!!.first,
            onEnroll = { activity ->
                viewModel.enrollFingerprint(
                    activity = activity,
                    onSuccess = {
                        showFingerprintEnrollment = false
                        showFingerprintSuccessDialog = true
                    },
                    onError = { error ->
                        fingerprintEnrollmentError = error
                    }
                )
            },
            onSkip = {
                showFingerprintEnrollment = false
                showSuccessDialog = true
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Registrar Empleado") },
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (showCamera && cameraPermissionState.status.isGranted) {
                // Modo cámara
                CameraScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    photoCount = photoCount,
                    onStopCapture = {
                        showCamera = false
                        viewModel.resetCapture()
                    },
                    onRegister = {
                        viewModel.registerEmployee(
                            name = employeeName,
                            employeeId = employeeId,
                            department = department,
                            position = position,
                            hasFingerprintEnabled = enableFingerprint
                        )
                    }
                )
            } else {
                // Formulario de información
                FormScreen(
                    employeeName = employeeName,
                    onEmployeeNameChange = { employeeName = it },
                    employeeId = employeeId,
                    onEmployeeIdChange = { employeeId = it },
                    department = department,
                    onDepartmentChange = { department = it },
                    position = position,
                    onPositionChange = { position = it },
                    enableFingerprint = enableFingerprint,
                    onEnableFingerprintChange = { enableFingerprint = it },
                    photoCount = photoCount,
                    onStartCapture = {
                        if (cameraPermissionState.status.isGranted) {
                            showCamera = true
                            viewModel.startCapture()
                        } else {
                            cameraPermissionState.launchPermissionRequest()
                        }
                    },
                    onCancel = onNavigateBack,
                    canStartCapture = employeeName.isNotBlank() && employeeId.isNotBlank(),
                    onRegisterDirectly = {
                        // Registrar directamente sin fotos (solo con huella)
                        viewModel.registerEmployee(
                            name = employeeName,
                            employeeId = employeeId,
                            department = department,
                            position = position,
                            hasFingerprintEnabled = enableFingerprint
                        )
                    }
                )
            }

            // Loading overlay
            if (uiState is RegistrationUiState.RegisteringEmployee) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Registrando empleado...")
                        }
                    }
                }
            }
        }
    }

    // Diálogo de éxito
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            icon = { Icon(Icons.Filled.Check, contentDescription = null) },
            title = { Text("¡Registro Exitoso!") },
            text = { Text("El empleado ha sido registrado correctamente.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        viewModel.clearRegisteredEmployeeData()
                        onNavigateBack()
                    }
                ) {
                    Text("Aceptar")
                }
            }
        )
    }

            // Diálogo de error
            if (uiState is RegistrationUiState.Error) {
                val errorMessage = (uiState as RegistrationUiState.Error).message
                AlertDialog(
                    onDismissRequest = { viewModel.resetCapture() },
                    icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) },
                    title = { Text("Error") },
                    text = { Text(errorMessage) },
                    confirmButton = {
                        Button(onClick = { viewModel.resetCapture() }) {
                            Text("Aceptar")
                        }
                    }
                )
            }
        }

        // Diálogo de éxito de registro de huella
        if (showFingerprintSuccessDialog) {
            FingerprintEnrollmentSuccessDialog(
                onDismiss = {
                    showFingerprintSuccessDialog = false
                    viewModel.clearRegisteredEmployeeData()
                    onNavigateBack()
                }
            )
        }

        // Diálogo de error de registro de huella
        fingerprintEnrollmentError?.let { error ->
            FingerprintEnrollmentErrorDialog(
                errorMessage = error,
                onDismiss = {
                    fingerprintEnrollmentError = null
                    showFingerprintEnrollment = false
                    showSuccessDialog = true
                },
                onRetry = {
                    fingerprintEnrollmentError = null
                }
            )
        }
    }

@Composable
private fun FormScreen(
    employeeName: String,
    onEmployeeNameChange: (String) -> Unit,
    employeeId: String,
    onEmployeeIdChange: (String) -> Unit,
    department: String,
    onDepartmentChange: (String) -> Unit,
    position: String,
    onPositionChange: (String) -> Unit,
    enableFingerprint: Boolean,
    onEnableFingerprintChange: (Boolean) -> Unit,
    photoCount: Int,
    onStartCapture: () -> Unit,
    onCancel: () -> Unit,
    canStartCapture: Boolean,
    onRegisterDirectly: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Información del Empleado",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = employeeName,
            onValueChange = onEmployeeNameChange,
            label = { Text("Nombre Completo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = employeeId,
            onValueChange = onEmployeeIdChange,
            label = { Text("ID del Empleado") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = department,
            onValueChange = onDepartmentChange,
            label = { Text("Departamento") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = position,
            onValueChange = onPositionChange,
            label = { Text("Cargo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Opción de habilitar huella
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
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
                        text = "Habilitar Huella Digital",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                    Text(
                        text = "Permitir registrar asistencia con ID + Huella del sistema",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
                Switch(
                    checked = enableFingerprint,
                    onCheckedChange = onEnableFingerprintChange
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Métodos de Autenticación",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Debe configurar al menos uno de los siguientes métodos:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "1. Captura Facial (Opcional)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (photoCount >= 7)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (photoCount >= 7) Icons.Filled.Check else Icons.Filled.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = if (photoCount >= 7)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (photoCount >= 7)
                        "¡Fotos capturadas con éxito!"
                    else if (photoCount > 0)
                        "Continúa capturando (${photoCount}/10 fotos)"
                    else
                        "Captura 7-10 fotos del rostro desde diferentes ángulos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (photoCount >= 7)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                if (photoCount == 0) {
                    Text(
                        text = "(Opcional si habilitas Huella Digital)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Fotos capturadas: $photoCount/10",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (photoCount >= 7)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Botón de captura facial (opcional)
        Button(
            onClick = onStartCapture,
            modifier = Modifier.fillMaxWidth(),
            enabled = canStartCapture
        ) {
            Icon(Icons.Filled.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (photoCount > 0) "Continuar Capturando" else "Capturar Fotos del Rostro")
        }

        // Si solo quiere huella (sin fotos), permitir registrar directamente
        if (enableFingerprint && photoCount == 0) {
            FilledTonalButton(
                onClick = onRegisterDirectly,
                modifier = Modifier.fillMaxWidth(),
                enabled = canStartCapture
            ) {
                Icon(Icons.Filled.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Registrar Solo con Huella")
            }
        }

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancelar")
        }
    }
}

@Composable
private fun CameraScreen(
    viewModel: EmployeeRegistrationViewModel,
    uiState: RegistrationUiState,
    photoCount: Int,
    onStopCapture: () -> Unit,
    onRegister: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Vista previa de cámara
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
            onFrameAnalyzed = { bitmap ->
                viewModel.processFrame(bitmap)
            }
        )

        // Overlay con información
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Información superior
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Fotos: $photoCount/10",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { photoCount / 10f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (uiState) {
                            is RegistrationUiState.NoFaceDetected -> "❌ No se detecta rostro"
                            is RegistrationUiState.MultipleFacesDetected -> "❌ Múltiples rostros detectados"
                            is RegistrationUiState.FaceNotFacingForward -> "⚠️ Ajusta tu posición"
                            is RegistrationUiState.Processing -> "📸 Procesando..."
                            is RegistrationUiState.PhotoCaptured -> "✅ ¡FOTO CAPTURADA!"
                            is RegistrationUiState.AllPhotosCaptured -> "✅ ¡Todas las fotos capturadas!"
                            is RegistrationUiState.ReadyToCapture -> "✓ Perfecto - Mantén quieto"
                            else -> "👤 Posiciona tu rostro de frente"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = when (uiState) {
                            is RegistrationUiState.PhotoCaptured -> MaterialTheme.colorScheme.primary
                            is RegistrationUiState.AllPhotosCaptured -> MaterialTheme.colorScheme.primary
                            is RegistrationUiState.ReadyToCapture -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )

                    // Instrucciones adicionales con guía de ángulos
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when (photoCount) {
                                in 0..2 -> MaterialTheme.colorScheme.primaryContainer
                                in 3..6 -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.tertiaryContainer
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = when {
                                    photoCount == 0 -> "📷 Foto 1-3: Mira de FRENTE a la cámara"
                                    photoCount in 1..2 -> "📷 Capturando vista frontal..."
                                    photoCount in 3..5 -> "📷 Foto 4-6: Gira LIGERAMENTE a la IZQUIERDA"
                                    photoCount in 6..8 -> "📷 Foto 7-9: Gira LIGERAMENTE a la DERECHA"
                                    photoCount == 9 -> "📷 Última foto: Vuelve al FRENTE"
                                    else -> "✓ Captura completa"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = when (photoCount) {
                                    in 0..2 -> MaterialTheme.colorScheme.onPrimaryContainer
                                    in 3..6 -> MaterialTheme.colorScheme.onSecondaryContainer
                                    else -> MaterialTheme.colorScheme.onTertiaryContainer
                                }
                            )
                            if (uiState is RegistrationUiState.ReadyToCapture) {
                                Text(
                                    text = "⏱️ Mantén la posición por 1 segundo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when (photoCount) {
                                        in 0..2 -> MaterialTheme.colorScheme.onPrimaryContainer
                                        in 3..6 -> MaterialTheme.colorScheme.onSecondaryContainer
                                        else -> MaterialTheme.colorScheme.onTertiaryContainer
                                    }.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // Controles inferiores
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (photoCount >= 7) {
                        Button(
                            onClick = onRegister,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Registrar Empleado")
                        }
                    }

                    OutlinedButton(
                        onClick = onStopCapture,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}
