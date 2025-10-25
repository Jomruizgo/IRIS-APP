package com.attendance.facerecognition.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.facerecognition.auth.SessionManager
import com.attendance.facerecognition.data.local.database.AppDatabase
import com.attendance.facerecognition.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val userRepository = UserRepository(database.userDao())
    private val sessionManager = SessionManager(application)

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _pin = MutableStateFlow("")
    val pin: StateFlow<String> = _pin.asStateFlow()

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Actualiza el nombre de usuario
     */
    fun updateUsername(value: String) {
        _username.value = value
    }

    /**
     * Agrega un dígito al PIN
     */
    fun addDigit(digit: Int) {
        if (_pin.value.length < 6) {
            _pin.value += digit.toString()
        }
    }

    /**
     * Elimina el último dígito del PIN
     */
    fun removeLastDigit() {
        if (_pin.value.isNotEmpty()) {
            _pin.value = _pin.value.dropLast(1)
        }
    }

    /**
     * Limpia el formulario
     */
    private fun clearForm() {
        _username.value = ""
        _pin.value = ""
    }

    /**
     * Verifica si es la primera vez que se usa la app (no hay admins)
     */
    fun checkFirstTimeSetup(onFirstTime: () -> Unit) {
        viewModelScope.launch {
            if (!userRepository.hasAdmin()) {
                onFirstTime()
            }
        }
    }

    /**
     * Intenta iniciar sesión
     */
    fun login() {
        val currentUsername = _username.value.trim()
        val currentPin = _pin.value

        // Validaciones básicas
        if (currentUsername.isBlank()) {
            _uiState.value = LoginUiState.Error("Ingresa tu nombre de usuario")
            return
        }

        if (currentPin.length < 4) {
            _uiState.value = LoginUiState.Error("El PIN debe tener al menos 4 dígitos")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = LoginUiState.Authenticating

                val user = userRepository.validateCredentials(currentUsername, currentPin)

                if (user != null) {
                    // Login exitoso
                    sessionManager.login(
                        userId = user.id,
                        username = user.username,
                        fullName = user.fullName,
                        role = user.role
                    )
                    clearForm()
                    _uiState.value = LoginUiState.Success
                } else {
                    // Credenciales incorrectas
                    _pin.value = "" // Limpia solo el PIN
                    _uiState.value = LoginUiState.Error("Usuario o PIN incorrecto")
                }
            } catch (e: Exception) {
                _pin.value = ""
                _uiState.value = LoginUiState.Error("Error al iniciar sesión: ${e.message}")
            }
        }
    }

    /**
     * Resetea el estado a Idle (para limpiar errores)
     */
    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}

/**
 * Estados de la UI de login
 */
sealed class LoginUiState {
    object Idle : LoginUiState()
    object Authenticating : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
