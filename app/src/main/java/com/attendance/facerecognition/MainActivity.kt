package com.attendance.facerecognition

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.attendance.facerecognition.ui.screens.HomeScreen
import com.attendance.facerecognition.ui.screens.EmployeeRegistrationScreen
import com.attendance.facerecognition.ui.screens.FaceRecognitionScreen
import com.attendance.facerecognition.ui.theme.FaceRecognitionTheme

class MainActivity : ComponentActivity() {
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

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") {
                HomeScreen(
                    onNavigateToRegistration = {
                        navController.navigate("registration")
                    },
                    onNavigateToRecognition = {
                        navController.navigate("recognition")
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
                    }
                )
            }
        }
    }
}
