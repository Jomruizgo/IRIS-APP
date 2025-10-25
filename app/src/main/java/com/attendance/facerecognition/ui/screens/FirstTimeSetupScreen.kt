package com.attendance.facerecognition.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendance.facerecognition.R
import com.attendance.facerecognition.ui.viewmodels.FirstTimeSetupUiState
import com.attendance.facerecognition.ui.viewmodels.FirstTimeSetupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstTimeSetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: FirstTimeSetupViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val username by viewModel.username.collectAsState()
    val fullName by viewModel.fullName.collectAsState()
    val pin by viewModel.pin.collectAsState()
    val confirmPin by viewModel.confirmPin.collectAsState()
    val isConfirmingPin by viewModel.isConfirmingPin.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is FirstTimeSetupUiState.Success) {
            onSetupComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración Inicial") },
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Logo IRIS
            Image(
                painter = painterResource(id = R.drawable.logo_iris),
                contentDescription = "Logo IRIS",
                modifier = Modifier.size(80.dp)
            )

            Text(
                text = "Crear Administrador",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "No existe ningún usuario. Crea el primer administrador para comenzar.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Campo de nombre completo
            OutlinedTextField(
                value = fullName,
                onValueChange = { viewModel.updateFullName(it) },
                label = { Text("Nombre Completo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = uiState !is FirstTimeSetupUiState.Creating
            )

            // Campo de usuario
            OutlinedTextField(
                value = username,
                onValueChange = { viewModel.updateUsername(it) },
                label = { Text("Usuario") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = uiState !is FirstTimeSetupUiState.Creating
            )

            // Display del PIN
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isConfirmingPin) "Confirma tu PIN" else "Crea tu PIN",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val currentPin = if (isConfirmingPin) confirmPin else pin
                        if (currentPin.isEmpty()) {
                            Text(
                                text = "Mínimo 4 dígitos",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        } else {
                            Text(
                                text = "•".repeat(currentPin.length),
                                style = MaterialTheme.typography.displayMedium,
                                fontSize = 48.sp,
                                letterSpacing = 16.sp
                            )
                        }
                    }
                }
            }

            // Mensaje de error
            when (val state = uiState) {
                is FirstTimeSetupUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
                is FirstTimeSetupUiState.Creating -> {
                    CircularProgressIndicator()
                }
                else -> Spacer(modifier = Modifier.height(24.dp))
            }

            // Teclado numérico
            SetupNumericKeypad(
                onNumberClick = { number -> viewModel.addDigit(number) },
                onBackspace = { viewModel.removeLastDigit() },
                onConfirm = { viewModel.confirmPinOrCreateUser() },
                enabled = uiState !is FirstTimeSetupUiState.Creating,
                canConfirm = if (isConfirmingPin) {
                    confirmPin.length >= 4
                } else {
                    pin.length >= 4 && username.isNotBlank() && fullName.isNotBlank()
                }
            )
        }
    }
}

@Composable
private fun SetupNumericKeypad(
    onNumberClick: (Int) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit,
    enabled: Boolean,
    canConfirm: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Filas 1-3
        for (row in 0..2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (col in 1..3) {
                    val number = row * 3 + col
                    SetupKeypadButton(
                        text = number.toString(),
                        onClick = { onNumberClick(number) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Fila 4: Backspace, 0, Confirmar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(
                onClick = onBackspace,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                enabled = enabled,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Borrar",
                    modifier = Modifier.size(28.dp)
                )
            }

            SetupKeypadButton(
                text = "0",
                onClick = { onNumberClick(0) },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                enabled = enabled && canConfirm,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "✓",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SetupKeypadButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
