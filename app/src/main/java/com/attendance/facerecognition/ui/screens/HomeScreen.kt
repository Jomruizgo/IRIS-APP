package com.attendance.facerecognition.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendance.facerecognition.network.ConnectivityObserver
import com.attendance.facerecognition.sync.SyncWorker
import com.attendance.facerecognition.ui.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToRegistration: () -> Unit,
    onNavigateToRecognition: () -> Unit,
    onNavigateToEmployeeList: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToUserManagement: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentUsername by viewModel.currentUsername.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val canManageEmployees by viewModel.canManageEmployees.collectAsState()
    val canViewReports by viewModel.canViewReports.collectAsState()
    val employeeCount by viewModel.employeeCount.collectAsState()
    val pendingRecordsCount by viewModel.pendingRecordsCount.collectAsState()
    val networkStatus by viewModel.networkStatus.collectAsState()
    val deviceRegistration by viewModel.deviceRegistration.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }

    // Función helper para navegar con verificación de login
    fun navigateWithAuth(action: () -> Unit) {
        if (isLoggedIn) {
            action()
        } else {
            onNavigateToLogin()
        }
    }

    // Determinar estado de sincronización basado en deviceRegistration
    val device = deviceRegistration // Capturar en variable local para smart cast
    val (syncIcon, syncText, syncColor) = when {
        // Dispositivo no registrado: solo modo local
        device == null -> Triple(
            Icons.Filled.CloudOff,
            "Solo local",
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        // Hay registros pendientes
        pendingRecordsCount > 0 -> Triple(
            Icons.Filled.SyncProblem,
            "$pendingRecordsCount pendiente${if (pendingRecordsCount != 1) "s" else ""}",
            MaterialTheme.colorScheme.tertiary
        )
        // No hay registros pendientes, mostrar última sincronización
        else -> {
            val lastSync = device.lastSyncAt
            val syncText = if (lastSync != null) {
                val elapsed = System.currentTimeMillis() - lastSync
                when {
                    elapsed < 60_000 -> "Hace menos de 1 min"
                    elapsed < 3600_000 -> "Hace ${elapsed / 60_000} min"
                    elapsed < 86400_000 -> "Hace ${elapsed / 3600_000}h"
                    else -> "Hace ${elapsed / 86400_000}d"
                }
            } else {
                "Nunca sincronizado"
            }
            Triple(
                Icons.Filled.CheckCircle,
                syncText,
                MaterialTheme.colorScheme.primary
            )
        }
    }

    // Actualizar actividad para timeout
    LaunchedEffect(Unit) {
        viewModel.updateActivity()
    }

    // Diálogo de confirmación de logout
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar Sesión") },
            text = { Text("¿Estás seguro de que quieres cerrar sesión?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.logout()
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Cerrar Sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("IRIS")
                        currentUsername?.let { name ->
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                actions = {
                    // Botón de sincronización manual
                    IconButton(
                        onClick = {
                            SyncWorker.syncNow(context)
                            Toast.makeText(
                                context,
                                "Sincronización iniciada",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Icon(
                            Icons.Filled.Sync,
                            contentDescription = "Sincronizar ahora"
                        )
                    }

                    // Botón de logout (solo si está logueado)
                    if (isLoggedIn) {
                        IconButton(
                            onClick = { showLogoutDialog = true }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Cerrar sesión"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título
            Text(
                text = "IRIS",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )

            Text(
                text = "Identificación Rápida e Inteligente Sin conexión",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Botón Registrar Asistencia
            ElevatedCard(
                onClick = onNavigateToRecognition,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
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
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Registrar Asistencia",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Botones de administración (solo para usuarios con permisos)
            if (canManageEmployees) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón Registrar Empleado
                    OutlinedCard(
                        onClick = { navigateWithAuth(onNavigateToRegistration) },
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Registrar",
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Botón Ver Empleados
                    OutlinedCard(
                        onClick = { navigateWithAuth(onNavigateToEmployeeList) },
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.People,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Ver Lista",
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Botones de reportes y configuración (según permisos)
            if (canViewReports || canManageEmployees) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón Reportes (ADMIN y SUPERVISOR)
                    if (canViewReports) {
                        OutlinedCard(
                            onClick = { navigateWithAuth(onNavigateToReports) },
                            modifier = Modifier
                                .weight(1f)
                                .height(100.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Assessment,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Reportes",
                                    style = MaterialTheme.typography.titleSmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Botón Configuración (solo ADMIN)
                    if (canManageEmployees) {
                        OutlinedCard(
                            onClick = { navigateWithAuth(onNavigateToSettings) },
                            modifier = Modifier
                                .weight(1f)
                                .height(100.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Ajustes",
                                    style = MaterialTheme.typography.titleSmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Botón Gestionar Usuarios (solo ADMIN)
            if (canManageEmployees) {
                OutlinedCard(
                    onClick = { navigateWithAuth(onNavigateToUserManagement) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ManageAccounts,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Gestionar Usuarios",
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Información del sistema
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Estado de sincronización con ícono
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estado",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = syncIcon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = syncColor
                            )
                            Text(
                                text = syncText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = syncColor
                            )
                        }
                    }

                    InfoRow(label = "Empleados registrados", value = employeeCount.toString())

                    // Solo mostrar registros pendientes si hay alguno
                    if (pendingRecordsCount > 0) {
                        InfoRow(
                            label = "Por sincronizar",
                            value = pendingRecordsCount.toString()
                        )
                    }
                }
            }

            // Botón de login para administradores (solo si NO está logueado)
            if (!isLoggedIn) {
                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onNavigateToLogin,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Acceso Administrativo",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
