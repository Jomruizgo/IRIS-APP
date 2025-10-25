package com.attendance.facerecognition

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendance.facerecognition.auth.SessionManager
import com.attendance.facerecognition.data.local.entities.AttendanceType
import com.attendance.facerecognition.data.repository.UserRepository
import com.attendance.facerecognition.device.DeviceManager
import kotlinx.coroutines.flow.first
import com.attendance.facerecognition.ui.screens.BiometricAuthScreen
import com.attendance.facerecognition.ui.screens.DailyReportScreen
import com.attendance.facerecognition.ui.screens.DeviceActivationScreen
import com.attendance.facerecognition.ui.screens.EmployeeDetailScreen
import com.attendance.facerecognition.ui.screens.EmployeeEditScreen
import com.attendance.facerecognition.ui.screens.EmployeeListScreen
import com.attendance.facerecognition.ui.screens.EmployeeRegistrationScreen
import com.attendance.facerecognition.ui.screens.FaceRecognitionScreen
import com.attendance.facerecognition.ui.screens.FirstTimeSetupScreen
import com.attendance.facerecognition.ui.screens.HomeScreen
import com.attendance.facerecognition.ui.screens.LoginScreen
import com.attendance.facerecognition.ui.screens.SettingsScreen
import com.attendance.facerecognition.ui.screens.UserListScreen
import com.attendance.facerecognition.ui.screens.UserRegistrationScreen
import com.attendance.facerecognition.ui.theme.FaceRecognitionTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FaceRecognitionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FaceRecognitionApp()
                }
            }
        }
    }
}

@Composable
fun FaceRecognitionApp() {
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Verificar estado inicial
    LaunchedEffect(Unit) {
        try {
            val context = navController.context
            val database = com.attendance.facerecognition.data.local.database.AppDatabase.getDatabase(context)
            val userRepository = UserRepository(database.userDao())
            val sessionManager = SessionManager(context)

            // 1. Verificar si hay usuarios en el sistema
            val hasAdmin = userRepository.hasAdmin()

            if (!hasAdmin) {
                // No hay usuarios → Primera vez
                startDestination = "first_time_setup"
            } else {
                // Hay usuarios → Verificar sesión
                val isLoggedIn = sessionManager.isLoggedIn.first()
                val sessionExpired = sessionManager.isSessionExpired()

                if (isLoggedIn && !sessionExpired) {
                    // Sesión activa y válida → Home
                    startDestination = "home"
                } else {
                    // No hay sesión o expiró → Login
                    startDestination = "login"
                }
            }
        } catch (e: Exception) {
            // En caso de error, ir a login por seguridad
            startDestination = "login"
        } finally {
            isLoading = false
        }
    }

    if (isLoading || startDestination == null) {
        // Mostrar splash mientras se verifica
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = startDestination!!,
                modifier = Modifier.padding(paddingValues)
            ) {
            // Pantalla de activación de dispositivo (opcional, desde Configuración)
            composable("device_activation") {
                DeviceActivationScreen(
                    onActivationSuccess = {
                        navController.navigate("home") {
                            popUpTo("device_activation") { inclusive = true }
                        }
                    }
                )
            }
            composable("home") {
                HomeScreen(
                    onNavigateToRegistration = {
                        navController.navigate("registration")
                    },
                    onNavigateToRecognition = {
                        navController.navigate("recognition")
                    },
                    onNavigateToEmployeeList = {
                        navController.navigate("employee_list")
                    },
                    onNavigateToReports = {
                        navController.navigate("daily_report")
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    },
                    onNavigateToUserManagement = {
                        navController.navigate("user_list")
                    },
                    onNavigateToLogin = {
                        navController.navigate("login")
                    },
                    onLogout = {
                        // Solo hacer popBackStack, no navegar a login
                    }
                )
            }

            composable("registration") {
                EmployeeRegistrationScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("recognition") {
                FaceRecognitionScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToBiometric = { attendanceType ->
                        val typeParam = if (attendanceType == AttendanceType.ENTRY) "ENTRY" else "EXIT"
                        navController.navigate("biometric_auth/$typeParam")
                    }
                )
            }

            composable("employee_list") {
                EmployeeListScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onEmployeeClick = { employee ->
                        navController.navigate("employee_detail/${employee.id}")
                    }
                )
            }

            composable("daily_report") {
                DailyReportScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "biometric_auth/{attendanceType}",
                arguments = listOf(navArgument("attendanceType") { type = NavType.StringType })
            ) { backStackEntry ->
                val typeParam = backStackEntry.arguments?.getString("attendanceType") ?: "ENTRY"
                val attendanceType = if (typeParam == "ENTRY") AttendanceType.ENTRY else AttendanceType.EXIT

                BiometricAuthScreen(
                    attendanceType = attendanceType,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onSuccess = {
                        // Volver a home después de éxito
                        navController.popBackStack("home", inclusive = false)
                    }
                )
            }

            composable(
                route = "employee_detail/{employeeId}",
                arguments = listOf(navArgument("employeeId") { type = NavType.LongType })
            ) { backStackEntry ->
                val employeeId = backStackEntry.arguments?.getLong("employeeId") ?: 0L

                EmployeeDetailScreen(
                    employeeId = employeeId,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToEdit = { empId ->
                        navController.navigate("employee_edit/$empId")
                    }
                )
            }

            composable(
                route = "employee_edit/{employeeId}",
                arguments = listOf(navArgument("employeeId") { type = NavType.LongType })
            ) { backStackEntry ->
                val employeeId = backStackEntry.arguments?.getLong("employeeId") ?: 0L

                EmployeeEditScreen(
                    employeeId = employeeId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Pantalla de login (para funciones administrativas)
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate("home") {
                            // Limpiar todo el back stack para evitar volver a login
                            popUpTo(0) { inclusive = false }
                        }
                    },
                    onFirstTimeSetup = {
                        navController.navigate("first_time_setup")
                    }
                )
            }

            // Pantalla de configuración inicial
            composable("first_time_setup") {
                FirstTimeSetupScreen(
                    onSetupComplete = {
                        navController.navigate("login") {
                            popUpTo("first_time_setup") { inclusive = true }
                        }
                    }
                )
            }

            // Gestión de usuarios (solo ADMIN)
            composable("user_list") {
                UserListScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToRegister = {
                        navController.navigate("user_registration")
                    }
                )
            }

            composable("user_registration") {
                UserRegistrationScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
      }
    }
}
