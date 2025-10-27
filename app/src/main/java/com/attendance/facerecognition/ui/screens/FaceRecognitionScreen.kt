package com.attendance.facerecognition.ui.screens

import androidx.camera.core.CameraSelector
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendance.facerecognition.data.local.entities.AttendanceType
import com.attendance.facerecognition.ui.components.CameraPreview
import com.attendance.facerecognition.ui.viewmodels.FaceRecognitionViewModel
import com.attendance.facerecognition.ui.viewmodels.RecognitionUiState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun FaceRecognitionScreen(
    onNavigateBack: () -> Unit,
    viewModel: FaceRecognitionViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val currentChallenge by viewModel.currentChallenge.collectAsState()

    var isScanning by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var isCapturingManualPhoto by remember { mutableStateOf(false) }
    var manualEmployeeId by remember { mutableStateOf<String?>(null) }
    var showManualDialog by remember { mutableStateOf(false) }

    // Permiso de cámara
    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )

    // Manejar resultado exitoso
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is RecognitionUiState.RecognitionSuccess -> {
                isScanning = false
                showSuccessDialog = true
                val recordTypeText = when (state.recordType) {
                    AttendanceType.ENTRY -> "ENTRADA"
                    AttendanceType.EXIT -> "SALIDA"
                }
                successMessage = "${state.employee.fullName}\n$recordTypeText registrada\nConfianza: ${(state.confidence * 100).toInt()}%"
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Asistencia") },
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
            // NO detener isScanning aquí - se detendrá cuando el usuario cierre el diálogo

            when {
                isCapturingManualPhoto && cameraPermissionState.status.isGranted -> {
                    // Modo captura de foto para registro manual
                    ManualPhotoCaptureScreen(
                        employeeId = manualEmployeeId ?: "",
                        onPhotoCaptured = { bitmap ->
                            // Guardar el frame y crear el registro
                            viewModel.setManualCapturedFrame(bitmap)
                            manualEmployeeId?.let { id ->
                                viewModel.createManualRegistrationById(
                                    employeeId = id,
                                    onSuccess = {
                                        isCapturingManualPhoto = false
                                        manualEmployeeId = null
                                        android.widget.Toast.makeText(
                                            context,
                                            "Registro manual creado. Pendiente de aprobación.",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                        onNavigateBack()
                                    },
                                    onError = { error ->
                                        isCapturingManualPhoto = false
                                        manualEmployeeId = null
                                        android.widget.Toast.makeText(
                                            context,
                                            "Error: $error",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            }
                        },
                        onCancel = {
                            isCapturingManualPhoto = false
                            manualEmployeeId = null
                        }
                    )
                }
                isScanning && cameraPermissionState.status.isGranted -> {
                    // Modo cámara con reconocimiento
                    ScanningScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        currentChallenge = currentChallenge,
                        isScanning = isScanning,
                        onStopScan = {
                            isScanning = false
                            viewModel.stopRecognition()
                        }
                    )
                }
                else -> {
                    // Pantalla de inicio
                    IdleScreen(
                        onStartScan = {
                            if (cameraPermissionState.status.isGranted) {
                                isScanning = true
                                viewModel.startRecognition()
                            } else {
                                cameraPermissionState.launchPermissionRequest()
                            }
                        },
                        onStartManualRegistration = {
                            showManualDialog = true
                        },
                        onNavigateBack = onNavigateBack,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // Diálogo de registro manual (movido fuera de IdleScreen)
    if (showManualDialog) {
        val selectedType by viewModel.selectedType.collectAsState()
        if (selectedType != null) {
            ManualRegistrationDialog(
                attendanceType = selectedType!!,
                onDismiss = { showManualDialog = false },
                onConfirm = { employeeId ->
                    // Guardar ID y activar captura de foto
                    manualEmployeeId = employeeId
                    showManualDialog = false

                    // Activar cámara para capturar foto
                    if (cameraPermissionState.status.isGranted) {
                        isCapturingManualPhoto = true
                    } else {
                        cameraPermissionState.launchPermissionRequest()
                        // Después de obtener permiso, activar captura
                        isCapturingManualPhoto = true
                    }
                }
            )
        }
    }

    // Diálogo de confirmación del reconocimiento
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            icon = { Icon(Icons.Filled.Face, contentDescription = null) },
            title = { Text("¿Eres tú?") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = successMessage,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Confirma si la identificación es correcta",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        viewModel.reset()
                    }
                ) {
                    Text("✓ Sí, soy yo")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        viewModel.cancelLastRegistration()
                        viewModel.reset()
                    }
                ) {
                    Text("✗ No soy yo")
                }
            }
        )
    }

    // Diálogo de error
    if (uiState is RecognitionUiState.Error) {
        val errorMessage = (uiState as RecognitionUiState.Error).message
        AlertDialog(
            onDismissRequest = {
                isScanning = false
                viewModel.reset()
            },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                Button(onClick = {
                    isScanning = false
                    viewModel.reset()
                }) {
                    Text("Aceptar")
                }
            }
        )
    }

    // Diálogo de no reconocido
    if (uiState is RecognitionUiState.NotRecognized) {
        val confidence = (uiState as RecognitionUiState.NotRecognized).confidence
        AlertDialog(
            onDismissRequest = {
                viewModel.reset()
                isScanning = false
            },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
            title = { Text("No Reconocido") },
            text = {
                Text(
                    text = "No se pudo reconocer tu rostro.\nConfianza: ${(confidence * 100).toInt()}%\n\nIntenta nuevamente o usa el botón de Registro Manual desde la pantalla inicial.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.reset()
                    isScanning = false
                }) {
                    Text("Intentar de Nuevo")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.reset()
                    isScanning = false
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de error de validación
    if (uiState is RecognitionUiState.ValidationError) {
        val validationState = uiState as RecognitionUiState.ValidationError
        AlertDialog(
            onDismissRequest = {
                viewModel.reset()
                isScanning = false
            },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
            title = { Text("Validación de Registro") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Empleado: ${validationState.employee.fullName}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = validationState.message,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.reset()
                    isScanning = false
                }) {
                    Text("Entendido")
                }
            }
        )
    }

    // (Diálogo de registro manual movido a IdleScreen)
}

@Composable
private fun IdleScreen(
    onStartScan: () -> Unit,
    onStartManualRegistration: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: FaceRecognitionViewModel
) {
    val selectedType by viewModel.selectedType.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Card de vista previa
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Face,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Registrar Asistencia",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Selecciona el tipo de registro",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Selección de tipo
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "¿Qué deseas registrar?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Botón ENTRADA
                    OutlinedButton(
                        onClick = { viewModel.selectAttendanceType(AttendanceType.ENTRY) },
                        modifier = Modifier.weight(1f).height(100.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedType == AttendanceType.ENTRY)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 2.dp,
                            color = if (selectedType == AttendanceType.ENTRY)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "🏢",
                                style = MaterialTheme.typography.headlineLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ENTRADA",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    // Botón SALIDA
                    OutlinedButton(
                        onClick = { viewModel.selectAttendanceType(AttendanceType.EXIT) },
                        modifier = Modifier.weight(1f).height(100.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedType == AttendanceType.EXIT)
                                MaterialTheme.colorScheme.secondaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 2.dp,
                            color = if (selectedType == AttendanceType.EXIT)
                                MaterialTheme.colorScheme.secondary
                            else
                                MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "🏠",
                                style = MaterialTheme.typography.headlineLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "SALIDA",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                if (selectedType != null) {
                    Text(
                        text = "✓ Seleccionado: ${if (selectedType == AttendanceType.ENTRY) "ENTRADA" else "SALIDA"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Botones de acción
        Button(
            onClick = onStartScan,
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedType != null
        ) {
            Icon(Icons.Filled.Face, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reconocimiento Facial")
        }

        OutlinedButton(
            onClick = onStartManualRegistration,
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedType != null
        ) {
            Icon(Icons.Filled.PersonAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Registro Manual")
        }

        OutlinedButton(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }
    }
}

@Composable
private fun ScanningScreen(
    viewModel: FaceRecognitionViewModel,
    uiState: RecognitionUiState,
    currentChallenge: com.attendance.facerecognition.ml.LivenessChallenge?,
    isScanning: Boolean,
    onStopScan: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Vista previa de cámara
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
            onFrameAnalyzed = { bitmap ->
                // Solo procesar frames cuando está escaneando activamente
                if (isScanning) {
                    viewModel.processFrame(bitmap)
                }
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
                    containerColor = when (uiState) {
                        is RecognitionUiState.LivenessChallenge -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                        is RecognitionUiState.LivenessVerified -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
                        is RecognitionUiState.Recognizing -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)
                        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (uiState) {
                        is RecognitionUiState.LivenessChallenge -> {
                            Text(
                                text = currentChallenge?.instruction ?: "Preparando desafío...",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                        is RecognitionUiState.LivenessVerified -> {
                            Text(
                                text = "✅ Verificación completada",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                        is RecognitionUiState.Recognizing -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = "Reconociendo...",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                        is RecognitionUiState.NoFaceDetected -> {
                            Text(
                                text = "❌ No se detecta rostro",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                        is RecognitionUiState.MultipleFacesDetected -> {
                            Text(
                                text = "❌ Múltiples rostros detectados",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                        else -> {
                            Text(
                                text = "👤 Posiciona tu rostro",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
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
                    Text(
                        text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = onStopScan,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualPhotoCaptureScreen(
    employeeId: String,
    onPhotoCaptured: (android.graphics.Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    var capturedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Vista previa de cámara
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
            onFrameAnalyzed = { bitmap ->
                // Capturar frame continuamente
                capturedBitmap = bitmap
            }
        )

        // Overlay con instrucciones
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Instrucciones arriba
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Registro Manual",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ID Empleado: $employeeId",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Toma una foto de tu rostro",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Botones abajo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Botón de captura
                Button(
                    onClick = {
                        capturedBitmap?.let { bitmap ->
                            onPhotoCaptured(bitmap)
                        }
                    },
                    modifier = Modifier
                        .size(80.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = capturedBitmap != null
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Capturar",
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = "Capturar Foto",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar")
                }
            }
        }
    }
}

@Composable
private fun ManualRegistrationDialog(
    attendanceType: AttendanceType,
    onDismiss: () -> Unit,
    onConfirm: (employeeId: String) -> Unit
) {
    var employeeId by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.PersonAdd, contentDescription = null) },
        title = { Text("Registro Manual") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Crear registro ${if (attendanceType == AttendanceType.ENTRY) "de ENTRADA" else "de SALIDA"} pendiente de aprobación:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "El sistema buscará tus datos usando tu ID de empleado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = employeeId,
                    onValueChange = {
                        employeeId = it
                        errorMessage = null
                    },
                    label = { Text("ID Empleado") },
                    placeholder = { Text("12345") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        employeeId.isBlank() -> errorMessage = "Ingresa tu ID de empleado"
                        else -> onConfirm(employeeId.trim())
                    }
                },
                enabled = employeeId.isNotBlank()
            ) {
                Text("Continuar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
