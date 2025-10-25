package com.attendance.facerecognition.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendance.facerecognition.data.local.entities.AttendanceType
import com.attendance.facerecognition.ui.viewmodels.BiometricAuthState
import com.attendance.facerecognition.ui.viewmodels.BiometricAuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiometricAuthScreen(
    attendanceType: AttendanceType,
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: BiometricAuthViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val enteredId by viewModel.enteredId.collectAsState()

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

    val typeText = when (attendanceType) {
        AttendanceType.ENTRY -> "ENTRADA"
        AttendanceType.EXIT -> "SALIDA"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Autenticación por Huella") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Indicador de tipo de registro
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "Registro de $typeText",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Ingresa tu ID de empleado",
                    style = MaterialTheme.typography.titleLarge
                )

                // Display del ID ingresado
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
                        Text(
                            text = if (enteredId.isEmpty()) "---" else enteredId,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 48.sp
                        )
                    }
                }

                // Mensaje de estado
                when (val state = uiState) {
                    is BiometricAuthState.Error -> {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                    is BiometricAuthState.EmployeeFound -> {
                        Text(
                            text = "Empleado: ${state.employeeName}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    is BiometricAuthState.Success -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "¡Asistencia registrada!",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(1500)
                            onSuccess()
                        }
                    }
                    else -> Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Teclado numérico
            NumericKeypad(
                onNumberClick = { number -> viewModel.addDigit(number) },
                onBackspace = { viewModel.removeLastDigit() },
                onConfirm = {
                    if (activity == null) {
                        android.widget.Toast.makeText(
                            context,
                            "Error: No se puede acceder a la autenticación biométrica",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    } else {
                        viewModel.authenticateWithFingerprint(activity, attendanceType)
                    }
                },
                enabled = uiState !is BiometricAuthState.Authenticating && uiState !is BiometricAuthState.Success,
                canConfirm = enteredId.isNotEmpty()
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
                    KeypadButton(
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
            // Backspace
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

            // 0
            KeypadButton(
                text = "0",
                onClick = { onNumberClick(0) },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )

            // Confirmar
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                enabled = enabled && canConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
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
private fun KeypadButton(
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
