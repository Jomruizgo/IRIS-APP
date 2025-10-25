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

class UserRegistrationViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val userRepository = UserRepository(database.userDao())

    private val _isRegistering = MutableStateFlow(false)
    val isRegistering: StateFlow<Boolean> = _isRegistering.asStateFlow()

    /**
     * Registra un nuevo usuario
     */
    fun registerUser(
        username: String,
        fullName: String,
        pin: String,
        role: UserRole,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Validaciones
        if (username.isBlank() || fullName.isBlank() || pin.isBlank()) {
            onError("Todos los campos son obligatorios")
            return
        }

        if (username.length < 3) {
            onError("El usuario debe tener al menos 3 caracteres")
            return
        }

        if (pin.length < 4) {
            onError("El PIN debe tener al menos 4 dígitos")
            return
        }

        if (!pin.all { it.isDigit() }) {
            onError("El PIN solo puede contener números")
            return
        }

        viewModelScope.launch {
            try {
                _isRegistering.value = true

                // Verificar si el usuario ya existe
                val existingUser = userRepository.getUserByUsername(username)
                if (existingUser != null) {
                    onError("Ya existe un usuario con ese nombre")
                    return@launch
                }

                // Hashear el PIN con SHA-256
                val hashedPin = userRepository.hashPin(pin)

                // Crear usuario
                val user = User(
                    username = username.trim(),
                    fullName = fullName.trim(),
                    pinHash = hashedPin,
                    role = role,
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                )

                userRepository.insertUser(user)
                onSuccess()

            } catch (e: Exception) {
                onError(e.message ?: "Error al registrar usuario")
            } finally {
                _isRegistering.value = false
            }
        }
    }
}
