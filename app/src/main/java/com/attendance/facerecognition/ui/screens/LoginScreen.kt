package com.attendance.facerecognition.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.ui.res.painterResource
import com.attendance.facerecognition.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendance.facerecognition.ui.viewmodels.LoginUiState
import com.attendance.facerecognition.ui.viewmodels.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onFirstTimeSetup: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pin by viewModel.pin.collectAsState()
    val username by viewModel.username.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkFirstTimeSetup(onFirstTimeSetup)
    }

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onLoginSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Iniciar Sesión") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            // Logo IRIS
            Image(
                painter = painterResource(id = R.drawable.logo_iris),
                contentDescription = "Logo IRIS",
                modifier = Modifier.size(80.dp)
            )

            Text(
                text = "IRIS",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Campo de usuario
            OutlinedTextField(
                value = username,
                onValueChange = { viewModel.updateUsername(it) },
                label = { Text("Usuario") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = uiState !is LoginUiState.Authenticating
            )

            // Display del PIN
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (pin.isEmpty()) {
                        Text(
                            text = "Ingresa tu PIN",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    } else {
                        Text(
                            text = "•".repeat(pin.length),
                            style = MaterialTheme.typography.displaySmall,
                            fontSize = 40.sp,
                            letterSpacing = 12.sp
                        )
                    }
                }
            }

            // Mensaje de error
            when (val state = uiState) {
                is LoginUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
                is LoginUiState.Authenticating -> {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
                else -> { }
            }

            // Teclado numérico
            NumericKeypad(
                onNumberClick = { number -> viewModel.addDigit(number) },
                onBackspace = { viewModel.removeLastDigit() },
                onConfirm = { viewModel.login() },
                enabled = uiState !is LoginUiState.Authenticating,
                canConfirm = pin.length >= 4 && username.isNotBlank()
            )
        }
    }
}

@Composable
private fun NumericKeypad(
    onNumberClick: (Int) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit,
    enabled: Boolean,
    canConfirm: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Filas 1-3
        for (row in 0..2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (col in 1..3) {
                    val number = row * 3 + col
                    KeypadButton(
                        text = number.toString(),
                        onClick = { onNumberClick(number) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Fila 4: Backspace, 0, Login
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = onBackspace,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                enabled = enabled,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Borrar",
                    modifier = Modifier.size(24.dp)
                )
            }

            KeypadButton(
                text = "0",
                onClick = { onNumberClick(0) },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                enabled = enabled && canConfirm,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "✓",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
