package com.attendance.facerecognition.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.facerecognition.device.DeviceManager
import com.attendance.facerecognition.device.DeviceRegistrationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeviceActivationViewModel(application: Application) : AndroidViewModel(application) {

    private val deviceManager = DeviceManager(application)

    private val _activationCode = MutableStateFlow("")
    val activationCode: StateFlow<String> = _activationCode.asStateFlow()

    private val _deviceName = MutableStateFlow("")
    val deviceName: StateFlow<String> = _deviceName.asStateFlow()

    private val _uiState = MutableStateFlow<ActivationUiState>(ActivationUiState.Idle)
    val uiState: StateFlow<ActivationUiState> = _uiState.asStateFlow()

    fun updateActivationCode(code: String) {
        // Permitir caracteres alfanuméricos y guión medio, convertir a mayúsculas
        val filtered = code.filter { it.isLetterOrDigit() || it == '-' }.uppercase().take(20)
        _activationCode.value = filtered
    }

    fun updateDeviceName(name: String) {
        _deviceName.value = name.take(50)
    }

    fun activateDevice() {
        if (!_activationCode.value.contains("-")) {
            _uiState.value = ActivationUiState.Error("El código debe tener el formato TENANT-CODIGO")
            return
        }

        if (_deviceName.value.isBlank()) {
            _uiState.value = ActivationUiState.Error("Debes asignar un nombre al dispositivo")
            return
        }

        viewModelScope.launch {
            _uiState.value = ActivationUiState.Activating

            when (val result = deviceManager.registerDevice(_activationCode.value, _deviceName.value)) {
                is DeviceRegistrationResult.Success -> {
                    _uiState.value = ActivationUiState.Success
                }
                is DeviceRegistrationResult.Error -> {
                    _uiState.value = ActivationUiState.Error(result.message)
                }
            }
        }
    }

    fun clearError() {
        if (_uiState.value is ActivationUiState.Error) {
            _uiState.value = ActivationUiState.Idle
        }
    }
}

sealed class ActivationUiState {
    object Idle : ActivationUiState()
    object Activating : ActivationUiState()
    object Success : ActivationUiState()
    data class Error(val message: String) : ActivationUiState()
}
