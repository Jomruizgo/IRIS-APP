package com.attendance.facerecognition.ui.screens

import androidx.camera.core.CameraSelector
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
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
            if (isScanning && cameraPermissionState.status.isGranted) {
                // Modo cámara con reconocimiento
                ScanningScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    currentChallenge = currentChallenge,
                    onStopScan = {
                        isScanning = false
                        viewModel.stopRecognition()
                    }
                )
            } else {
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
                    onNavigateBack = onNavigateBack
                )
            }
        }
    }

    // Diálogo de éxito
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
            title = { Text("¡Asistencia Registrada!") },
            text = {
                Text(
                    text = successMessage,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        viewModel.reset()
                    }
                ) {
                    Text("Aceptar")
                }
            }
        )
    }

    // Diálogo de error
    if (uiState is RecognitionUiState.Error) {
        val errorMessage = (uiState as RecognitionUiState.Error).message
        AlertDialog(
            onDismissRequest = { viewModel.reset() },
            icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                Button(onClick = { viewModel.reset() }) {
                    Text("Aceptar")
                }
            }
        )
    }

    // Diálogo de no reconocido
    if (uiState is RecognitionUiState.NotRecognized) {
        val confidence = (uiState as RecognitionUiState.NotRecognized).confidence
        AlertDialog(
            onDismissRequest = { viewModel.reset() },
            icon = { Icon(Icons.Filled.Face, contentDescription = null) },
            title = { Text("No Reconocido") },
            text = {
                Text(
                    text = "No se pudo reconocer el rostro.\nConfianza: ${(confidence * 100).toInt()}%\n\nAsegúrate de estar registrado en el sistema.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.reset()
                    isScanning = false
                }) {
                    Text("Aceptar")
                }
            }
        )
    }
}

@Composable
private fun IdleScreen(
    onStartScan: () -> Unit,
    onNavigateBack: () -> Unit
) {
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
                    text = "Sistema de Reconocimiento Facial",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Presiona 'Iniciar' para registrar tu asistencia",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Información
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Instrucciones",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "• Posiciónate frente a la cámara",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• Asegúrate de tener buena iluminación",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• Sigue las instrucciones en pantalla",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• Completa el desafío de verificación",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Botones
        Button(
            onClick = onStartScan,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Face, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Iniciar Reconocimiento")
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
    onStopScan: () -> Unit
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
