package com.attendance.facerecognition.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendance.facerecognition.data.local.entities.UserRole
import com.attendance.facerecognition.ui.viewmodels.UserRegistrationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserRegistrationScreen(
    onNavigateBack: () -> Unit,
    viewModel: UserRegistrationViewModel = viewModel()
) {
    val isRegistering by viewModel.isRegistering.collectAsState()

    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.USER) }
    var isConfirmingPin by remember { mutableStateOf(false) }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Usuario") },
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icono
            Icon(
                imageVector = Icons.Filled.PersonAdd,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Nuevo Usuario",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Formulario
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Nombre Completo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isRegistering
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it.lowercase() },
                label = { Text("Usuario") },
                placeholder = { Text("usuario123") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isRegistering,
                supportingText = { Text("Minúsculas, sin espacios") }
            )

            // Selector de rol
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Rol del Usuario",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    UserRole.values().forEach { role ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRole == role,
                                onClick = { selectedRole = role },
                                enabled = !isRegistering
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = role.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = when (role) {
                                        UserRole.ADMIN -> "Acceso completo: gestión de usuarios, empleados y configuración"
                                        UserRole.SUPERVISOR -> "Acceso a reportes y lista de empleados"
                                        UserRole.USER -> "Sin acceso administrativo"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isConfirmingPin) "Confirma el PIN" else "Crea un PIN",
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
                                fontSize = 40.sp,
                                letterSpacing = 12.sp
                            )
                        }
                    }
                }
            }

            // Mensaje de error
            errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            if (isRegistering) {
                CircularProgressIndicator()
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Teclado numérico
            RegistrationNumericKeypad(
                onNumberClick = { number ->
                    if (isConfirmingPin) {
                        if (confirmPin.length < 6) confirmPin += number.toString()
                    } else {
                        if (pin.length < 6) pin += number.toString()
                    }
                },
                onBackspace = {
                    if (isConfirmingPin) {
                        if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
                    } else {
                        if (pin.isNotEmpty()) pin = pin.dropLast(1)
                    }
                },
                onConfirm = {
                    if (!isConfirmingPin) {
                        // Primera vez: pasar a confirmar
                        if (pin.length >= 4) {
                            isConfirmingPin = true
                            errorMessage = null
                        } else {
                            errorMessage = "El PIN debe tener al menos 4 dígitos"
                        }
                    } else {
                        // Confirmar PIN y registrar
                        if (confirmPin != pin) {
                            errorMessage = "Los PINes no coinciden"
                            confirmPin = ""
                            isConfirmingPin = false
                        } else {
                            // Registrar usuario
                            viewModel.registerUser(
                                username = username,
                                fullName = fullName,
                                pin = pin,
                                role = selectedRole,
                                onSuccess = {
                                    showSuccessDialog = true
                                },
                                onError = { error ->
                                    errorMessage = error
                                    confirmPin = ""
                                    isConfirmingPin = false
                                }
                            )
                        }
                    }
                },
                enabled = !isRegistering,
                canConfirm = if (isConfirmingPin) {
                    confirmPin.length >= 4
                } else {
                    pin.length >= 4 && fullName.isNotBlank() && username.isNotBlank()
                }
            )
        }
    }

    // Diálogo de éxito
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("¡Usuario Registrado!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("El usuario $username ha sido creado exitosamente.")
                    Text(
                        text = "Rol asignado: ${selectedRole.name}",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Aceptar")
                }
            }
        )
    }
}

@Composable
private fun RegistrationNumericKeypad(
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
                    RegistrationKeypadButton(
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

            RegistrationKeypadButton(
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
private fun RegistrationKeypadButton(
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
