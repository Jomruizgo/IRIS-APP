package com.attendance.facerecognition.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendance.facerecognition.R
import com.attendance.facerecognition.ui.viewmodels.ActivationUiState
import com.attendance.facerecognition.ui.viewmodels.DeviceActivationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceActivationScreen(
    onActivationSuccess: () -> Unit,
    viewModel: DeviceActivationViewModel = viewModel()
) {
    val activationCode by viewModel.activationCode.collectAsState()
    val deviceName by viewModel.deviceName.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // Navegar cuando se activa exitosamente
    LaunchedEffect(uiState) {
        if (uiState is ActivationUiState.Success) {
            onActivationSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activación de Dispositivo") },
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Logo IRIS
            Image(
                painter = painterResource(id = R.drawable.logo_iris),
                contentDescription = "Logo IRIS",
                modifier = Modifier.size(80.dp)
            )

            // Título
            Text(
                text = "Bienvenido a IRIS",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            // Descripción
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Para usar este dispositivo necesitas:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "1. Código de activación con el formato TENANT-CODIGO",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "   Ejemplo: ACME-XYZ789",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "2. Asignar un nombre a este dispositivo",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Campo: Código de activación
            OutlinedTextField(
                value = activationCode,
                onValueChange = { viewModel.updateActivationCode(it) },
                label = { Text("Código de Activación") },
                placeholder = { Text("ACME-ABC123") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = uiState !is ActivationUiState.Activating,
                supportingText = {
                    Text("Formato: TENANT-CODIGO (ej: ACME-XYZ789)")
                }
            )

            // Campo: Nombre del dispositivo
            OutlinedTextField(
                value = deviceName,
                onValueChange = { viewModel.updateDeviceName(it) },
                label = { Text("Nombre del Dispositivo") },
                placeholder = { Text("Tablet Entrada Principal") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = uiState !is ActivationUiState.Activating,
                supportingText = {
                    Text("Ejemplo: Tablet RH, Terminal Recepción")
                }
            )

            // Mensaje de error
            if (uiState is ActivationUiState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = (uiState as ActivationUiState.Error).message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Estado de activación en progreso
            if (uiState is ActivationUiState.Activating) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Activando dispositivo...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botón de activación
            Button(
                onClick = { viewModel.activateDevice() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = uiState !is ActivationUiState.Activating &&
                        activationCode.length >= 6 &&
                        deviceName.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Activar Dispositivo",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nota informativa
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
                        text = "Nota",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "El código de activación es único y solo puede usarse una vez. Si no tienes un código, contacta al administrador del sistema.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
