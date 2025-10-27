package com.attendance.facerecognition.ui.screens

import androidx.camera.core.CameraSelector
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendance.facerecognition.ui.components.CameraPreview
import com.attendance.facerecognition.ui.viewmodels.EmployeeRegistrationViewModel
import com.attendance.facerecognition.ui.viewmodels.RegistrationUiState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun EmployeeRegistrationScreen(
    onNavigateBack: () -> Unit,
    viewModel: EmployeeRegistrationViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val photoCount by viewModel.photoCount.collectAsState()

    var employeeName by remember { mutableStateOf("") }
    var employeeId by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }

    var showCamera by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val registeredEmployeeData by viewModel.registeredEmployeeData.collectAsState()

    // Permiso de cámara
    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )

    // Manejar resultado exitoso y errores
    LaunchedEffect(uiState) {
        when (uiState) {
            is RegistrationUiState.RegistrationSuccess -> {
                showSuccessDialog = true
            }
            // NO detener showCamera aquí - se detendrá cuando el usuario cierre el diálogo
            else -> {}
        }
    }

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
                    onPhotosComplete = {
                        // Solo cerrar cámara SIN borrar fotos
                        showCamera = false
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
                    photoCount = photoCount,
                    onStartCapture = {
                        // Validar ID antes de comenzar captura
                        scope.launch {
                            val isValid = viewModel.validateEmployeeId(employeeId)
                            if (!isValid) {
                                // Mostrar error sin activar cámara
                                viewModel.showError("Ya existe un empleado con ID: $employeeId")
                                return@launch
                            }

                            // ID válido, continuar con captura
                            if (cameraPermissionState.status.isGranted) {
                                showCamera = true
                                viewModel.startCapture()
                            } else {
                                cameraPermissionState.launchPermissionRequest()
                            }
                        }
                    },
                    onCancel = onNavigateBack,
                    canStartCapture = employeeName.isNotBlank() && employeeId.isNotBlank(),
                    onRegisterDirectly = {
                        // Registrar directamente sin fotos
                        viewModel.registerEmployee(
                            name = employeeName,
                            employeeId = employeeId,
                            department = department,
                            position = position
                        )
                    },
                    capturedPhotos = viewModel.capturedPhotos.collectAsState().value,
                    onRegisterWithPhotos = {
                        viewModel.registerEmployee(
                            name = employeeName,
                            employeeId = employeeId,
                            department = department,
                            position = position
                        )
                    },
                    onRetakePhotos = {
                        viewModel.resetCapture()
                        if (cameraPermissionState.status.isGranted) {
                            showCamera = true
                            viewModel.startCapture()
                        }
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
            onDismissRequest = {
                showCamera = false
                viewModel.resetCapture()
            },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                Button(onClick = {
                    showCamera = false
                    viewModel.resetCapture()
                }) {
                    Text("Aceptar")
                }
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
    photoCount: Int,
    onStartCapture: () -> Unit,
    onCancel: () -> Unit,
    canStartCapture: Boolean,
    onRegisterDirectly: () -> Unit = {},
    capturedPhotos: List<android.graphics.Bitmap> = emptyList(),
    onRegisterWithPhotos: () -> Unit = {},
    onRetakePhotos: () -> Unit = {}
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

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Reconocimiento Facial",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
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

        // Preview de fotos capturadas
        if (capturedPhotos.isNotEmpty()) {
            Text(
                text = "Fotos Capturadas",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(capturedPhotos.size) { index ->
                    Card(
                        modifier = Modifier.size(80.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = capturedPhotos[index].asImageBitmap(),
                            contentDescription = "Foto ${index + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botones para fotos capturadas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRetakePhotos,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📷 Volver a Capturar")
                }

                Button(
                    onClick = onRegisterWithPhotos,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Registrar")
                }
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
    onPhotosComplete: () -> Unit
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

        // Óvalo guía para el rostro
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val ovalWidth = size.width * 0.75f
            val ovalHeight = size.height * 0.55f  // Más alto (más ovalado verticalmente)
            val ovalLeft = (size.width - ovalWidth) / 2
            val ovalTop = (size.height - ovalHeight) / 2 - 80.dp.toPx()  // Un poco más arriba

            // Fondo semi-transparente oscuro
            drawRect(
                color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f),
                size = size
            )

            // Recortar óvalo (área transparente)
            drawOval(
                color = androidx.compose.ui.graphics.Color.Transparent,
                topLeft = androidx.compose.ui.geometry.Offset(ovalLeft, ovalTop),
                size = androidx.compose.ui.geometry.Size(ovalWidth, ovalHeight),
                blendMode = androidx.compose.ui.graphics.BlendMode.Clear
            )

            // Borde del óvalo
            val borderColor = when (uiState) {
                is RegistrationUiState.ReadyToCapture -> androidx.compose.ui.graphics.Color.Green
                is RegistrationUiState.PhotoCaptured -> androidx.compose.ui.graphics.Color.Green
                is RegistrationUiState.NoFaceDetected -> androidx.compose.ui.graphics.Color.Red
                is RegistrationUiState.MultipleFacesDetected -> androidx.compose.ui.graphics.Color.Red
                is RegistrationUiState.FaceNotFacingForward -> androidx.compose.ui.graphics.Color.Yellow
                else -> androidx.compose.ui.graphics.Color.White
            }

            drawOval(
                color = borderColor,
                topLeft = androidx.compose.ui.geometry.Offset(ovalLeft, ovalTop),
                size = androidx.compose.ui.geometry.Size(ovalWidth, ovalHeight),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
            )
        }

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
                                    photoCount in 3..5 -> "📷 Foto 4-6: Gira tu rostro a tu DERECHA →"
                                    photoCount in 6..8 -> "📷 Foto 7-9: Gira tu rostro a tu IZQUIERDA ←"
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
                            onClick = onPhotosComplete,  // Solo cerrar cámara, mantener fotos
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (uiState is RegistrationUiState.AllPhotosCaptured)
                                "Ver Fotos Capturadas" else "Continuar con $photoCount fotos")
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
