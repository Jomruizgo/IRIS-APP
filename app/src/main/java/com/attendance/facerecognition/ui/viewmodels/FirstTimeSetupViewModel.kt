package com.attendance.facerecognition.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.facerecognition.data.local.database.AppDatabase
import com.attendance.facerecognition.data.local.entities.User
import com.attendance.facerecognition.data.local.entities.UserRole
import com.attendance.facerecognition.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FirstTimeSetupViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val userRepository = UserRepository(database.userDao())

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _fullName = MutableStateFlow("")
    val fullName: StateFlow<String> = _fullName.asStateFlow()

    private val _pin = MutableStateFlow("")
    val pin: StateFlow<String> = _pin.asStateFlow()

    private val _confirmPin = MutableStateFlow("")
    val confirmPin: StateFlow<String> = _confirmPin.asStateFlow()

    private val _isConfirmingPin = MutableStateFlow(false)
    val isConfirmingPin: StateFlow<Boolean> = _isConfirmingPin.asStateFlow()

    private val _uiState = MutableStateFlow<FirstTimeSetupUiState>(FirstTimeSetupUiState.Idle)
    val uiState: StateFlow<FirstTimeSetupUiState> = _uiState.asStateFlow()

    /**
     * Actualiza el nombre de usuario
     */
    fun updateUsername(value: String) {
        _username.value = value
    }

    /**
     * Actualiza el nombre completo
     */
    fun updateFullName(value: String) {
        _fullName.value = value
    }

    /**
     * Agrega un dígito al PIN actual (PIN o confirmación)
     */
    fun addDigit(digit: Int) {
        if (_isConfirmingPin.value) {
            if (_confirmPin.value.length < 6) {
                _confirmPin.value += digit.toString()
            }
        } else {
            if (_pin.value.length < 6) {
                _pin.value += digit.toString()
            }
        }
    }

    /**
     * Elimina el último dígito del PIN actual
     */
    fun removeLastDigit() {
        if (_isConfirmingPin.value) {
            if (_confirmPin.value.isNotEmpty()) {
                _confirmPin.value = _confirmPin.value.dropLast(1)
            }
        } else {
            if (_pin.value.isNotEmpty()) {
                _pin.value = _pin.value.dropLast(1)
            }
        }
    }

    /**
     * Confirma el PIN o crea el usuario según el estado
     */
    fun confirmPinOrCreateUser() {
        if (_isConfirmingPin.value) {
            // Estamos confirmando el PIN
            if (_pin.value == _confirmPin.value) {
                createAdminUser()
            } else {
                _uiState.value = FirstTimeSetupUiState.Error("Los PINs no coinciden")
                _confirmPin.value = ""
            }
        } else {
            // Validar campos antes de pasar a confirmación
            val currentUsername = _username.value.trim()
            val currentFullName = _fullName.value.trim()
            val currentPin = _pin.value

            when {
                currentFullName.isBlank() -> {
                    _uiState.value = FirstTimeSetupUiState.Error("Ingresa tu nombre completo")
                }
                currentUsername.isBlank() -> {
                    _uiState.value = FirstTimeSetupUiState.Error("Ingresa un nombre de usuario")
                }
                currentUsername.length < 3 -> {
                    _uiState.value = FirstTimeSetupUiState.Error("El usuario debe tener al menos 3 caracteres")
                }
                currentPin.length < 4 -> {
                    _uiState.value = FirstTimeSetupUiState.Error("El PIN debe tener al menos 4 dígitos")
                }
                else -> {
                    // Pasar a confirmación de PIN
                    _isConfirmingPin.value = true
                    _uiState.value = FirstTimeSetupUiState.Idle
                }
            }
        }
    }

    /**
     * Crea el usuario administrador inicial
     */
    private fun createAdminUser() {
        viewModelScope.launch {
            try {
                _uiState.value = FirstTimeSetupUiState.Creating

                // Verificar que no exista el usuario
                val existingUser = userRepository.getUserByUsername(_username.value.trim())
                if (existingUser != null) {
                    _uiState.value = FirstTimeSetupUiState.Error("El usuario ya existe")
                    _isConfirmingPin.value = false
                    _confirmPin.value = ""
                    return@launch
                }

                // Crear el hash del PIN
                val pinHash = userRepository.hashPin(_pin.value)

                // Crear usuario ADMIN
                val adminUser = User(
                    username = _username.value.trim(),
                    fullName = _fullName.value.trim(),
                    pinHash = pinHash,
                    role = UserRole.ADMIN,
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                )

                userRepository.insertUser(adminUser)
                _uiState.value = FirstTimeSetupUiState.Success
            } catch (e: Exception) {
                _uiState.value = FirstTimeSetupUiState.Error("Error al crear usuario: ${e.message}")
                _isConfirmingPin.value = false
                _confirmPin.value = ""
            }
        }
    }

    /**
     * Resetea el estado a Idle
     */
    fun resetState() {
        _uiState.value = FirstTimeSetupUiState.Idle
    }
}

/**
 * Estados de la UI de configuración inicial
 */
sealed class FirstTimeSetupUiState {
    object Idle : FirstTimeSetupUiState()
    object Creating : FirstTimeSetupUiState()
    object Success : FirstTimeSetupUiState()
    data class Error(val message: String) : FirstTimeSetupUiState()
}
