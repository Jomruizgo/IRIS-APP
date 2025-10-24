package com.attendance.facerecognition.ui.viewmodels

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.facerecognition.data.local.database.AppDatabase
import com.attendance.facerecognition.data.local.entities.Employee
import com.attendance.facerecognition.data.repository.EmployeeRepository
import com.attendance.facerecognition.ml.FaceDetector
import com.attendance.facerecognition.ml.FaceRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EmployeeRegistrationViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = EmployeeRepository(database.employeeDao())
    private val faceDetector = FaceDetector()
    private val faceRecognizer = FaceRecognizer(application)

    private val _uiState = MutableStateFlow<RegistrationUiState>(RegistrationUiState.Idle)
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    private val _capturedPhotos = MutableStateFlow<List<Bitmap>>(emptyList())
    val capturedPhotos: StateFlow<List<Bitmap>> = _capturedPhotos.asStateFlow()

    private val _photoCount = MutableStateFlow(0)
    val photoCount: StateFlow<Int> = _photoCount.asStateFlow()

    private val targetPhotoCount = 10
    private val minPhotoCount = 7

    private var isProcessing = false
    private var lastCaptureTime = 0L

    /**
     * Procesa un frame de la cámara para detectar y capturar rostro
     */
    fun processFrame(frame: Bitmap) {
        // No procesar si ya tenemos suficientes fotos
        if (_photoCount.value >= targetPhotoCount) {
            if (_uiState.value !is RegistrationUiState.AllPhotosCaptured) {
                _uiState.value = RegistrationUiState.AllPhotosCaptured
            }
            return
        }

        // Evitar procesar múltiples frames simultáneamente
        if (isProcessing) {
            return
        }

        // Evitar capturar fotos muy rápido (mínimo 1 segundo entre capturas)
        val now = System.currentTimeMillis()
        if (now - lastCaptureTime < 1000) {
            return
        }

        isProcessing = true

        viewModelScope.launch {
            try {
                _uiState.value = RegistrationUiState.Processing
                android.util.Log.d("EmployeeRegistration", "Processing frame: ${frame.width}x${frame.height}")

                // Detectar rostros
                val faces = faceDetector.detectFaces(frame)
                android.util.Log.d("EmployeeRegistration", "Faces detected: ${faces.size}")

                if (faces.isEmpty()) {
                    _uiState.value = RegistrationUiState.NoFaceDetected
                    isProcessing = false
                    return@launch
                }

                if (faces.size > 1) {
                    _uiState.value = RegistrationUiState.MultipleFacesDetected
                    isProcessing = false
                    return@launch
                }

                val face = faces.first()
                android.util.Log.d("EmployeeRegistration", "Face angles - X: ${face.headEulerAngleX}, Y: ${face.headEulerAngleY}, Z: ${face.headEulerAngleZ}")

                // Validar que el rostro esté en el ángulo requerido según el número de fotos
                val currentCount = _photoCount.value
                val requiredAngle = getRequiredAngleForPhoto(currentCount)

                if (!isAngleCorrect(face.headEulerAngleY, requiredAngle)) {
                    android.util.Log.d("EmployeeRegistration", "Wrong angle. Current Y: ${face.headEulerAngleY}, Required: $requiredAngle")
                    _uiState.value = RegistrationUiState.FaceNotFacingForward
                    isProcessing = false
                    return@launch
                }

                // Recortar rostro
                val croppedFace = faceDetector.cropFace(frame, face.boundingBox, padding = 0.2f)
                android.util.Log.d("EmployeeRegistration", "Face cropped: ${croppedFace.width}x${croppedFace.height}")

                // Agregar foto a la lista
                val currentPhotos = _capturedPhotos.value.toMutableList()
                currentPhotos.add(croppedFace)
                _capturedPhotos.value = currentPhotos
                _photoCount.value = currentPhotos.size
                lastCaptureTime = now

                android.util.Log.d("EmployeeRegistration", "Photo captured! Total: ${currentPhotos.size}")
                _uiState.value = RegistrationUiState.PhotoCaptured(currentPhotos.size, targetPhotoCount)

                // Pequeña pausa antes de permitir capturar otra foto
                kotlinx.coroutines.delay(800)
                if (_photoCount.value < targetPhotoCount) {
                    _uiState.value = RegistrationUiState.ReadyToCapture
                } else {
                    _uiState.value = RegistrationUiState.AllPhotosCaptured
                }

                isProcessing = false

            } catch (e: Exception) {
                _uiState.value = RegistrationUiState.Error("Error al procesar frame: ${e.message}")
                isProcessing = false
            }
        }
    }

    /**
     * Registra el empleado con las fotos capturadas
     */
    fun registerEmployee(
        name: String,
        employeeId: String,
        department: String,
        position: String
    ) {
        if (_capturedPhotos.value.size < minPhotoCount) {
            _uiState.value = RegistrationUiState.Error("Necesitas al menos $minPhotoCount fotos")
            return
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _uiState.value = RegistrationUiState.RegisteringEmployee
                }

                android.util.Log.d("EmployeeRegistration", "Starting registration for: $name")

                // Verificar si el empleado ya existe
                android.util.Log.d("EmployeeRegistration", "Checking if employee $employeeId already exists...")
                if (repository.employeeExists(employeeId)) {
                    android.util.Log.e("EmployeeRegistration", "Employee already exists: $employeeId")
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        _uiState.value = RegistrationUiState.Error("Ya existe un empleado con ID: $employeeId")
                    }
                    return@launch
                }

                android.util.Log.d("EmployeeRegistration", "Employee does not exist, proceeding with registration")
                android.util.Log.d("EmployeeRegistration", "Generating embeddings for ${_capturedPhotos.value.size} photos")

                // Generar embeddings para todas las fotos
                val embeddings = _capturedPhotos.value.mapIndexed { index, photo ->
                    android.util.Log.d("EmployeeRegistration", "Processing photo $index: ${photo.width}x${photo.height}")
                    try {
                        val embedding = faceRecognizer.generateEmbedding(photo)
                        android.util.Log.d("EmployeeRegistration", "Embedding $index generated: ${embedding.size} dimensions")
                        embedding
                    } catch (e: Exception) {
                        android.util.Log.e("EmployeeRegistration", "Error generating embedding $index", e)
                        throw e
                    }
                }

                android.util.Log.d("EmployeeRegistration", "All embeddings generated successfully")

                // Crear empleado
                val employee = Employee(
                    employeeId = employeeId,
                    fullName = name,
                    department = department,
                    position = position,
                    faceEmbeddings = embeddings
                )

                android.util.Log.d("EmployeeRegistration", "Saving employee to database")

                // Guardar en base de datos
                val id = repository.insertEmployee(employee)

                android.util.Log.d("EmployeeRegistration", "Employee saved with ID: $id")

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _uiState.value = RegistrationUiState.RegistrationSuccess(employee)
                }

            } catch (e: Exception) {
                android.util.Log.e("EmployeeRegistration", "Error registering employee", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _uiState.value = RegistrationUiState.Error("Error al registrar empleado: ${e.message}\n${e.stackTraceToString().take(200)}")
                }
            }
        }
    }

    /**
     * Reinicia el proceso de captura
     */
    fun resetCapture() {
        _capturedPhotos.value = emptyList()
        _photoCount.value = 0
        _uiState.value = RegistrationUiState.Idle
        isProcessing = false
        lastCaptureTime = 0L
    }

    /**
     * Inicia el proceso de captura
     */
    fun startCapture() {
        _uiState.value = RegistrationUiState.ReadyToCapture
        isProcessing = false
        lastCaptureTime = 0L
    }

    /**
     * Determina el ángulo requerido según el número de foto
     * @return "front", "left", o "right"
     */
    private fun getRequiredAngleForPhoto(photoCount: Int): String {
        return when (photoCount) {
            in 0..2 -> "front"      // Fotos 1-3: De frente
            in 3..5 -> "left"       // Fotos 4-6: Girar a la izquierda
            in 6..8 -> "right"      // Fotos 7-9: Girar a la derecha
            9 -> "front"            // Foto 10: De frente
            else -> "front"
        }
    }

    /**
     * Verifica si el ángulo actual cumple con el requerido
     * @param currentAngleY Ángulo Y actual de la cabeza (yaw)
     * @param requiredAngle Ángulo requerido: "front", "left", o "right"
     */
    private fun isAngleCorrect(currentAngleY: Float, requiredAngle: String): Boolean {
        return when (requiredAngle) {
            "front" -> {
                // De frente: Y debe estar entre -15° y +15°
                currentAngleY >= -15f && currentAngleY <= 15f
            }
            "left" -> {
                // Girado a la izquierda: Y debe estar entre -45° y -20°
                currentAngleY >= -45f && currentAngleY <= -20f
            }
            "right" -> {
                // Girado a la derecha: Y debe estar entre +20° y +45°
                currentAngleY >= 20f && currentAngleY <= 45f
            }
            else -> false
        }
    }

    override fun onCleared() {
        super.onCleared()
        faceDetector.close()
        faceRecognizer.close()
    }
}

/**
 * Estados de la UI de registro
 */
sealed class RegistrationUiState {
    object Idle : RegistrationUiState()
    object ReadyToCapture : RegistrationUiState()
    object Processing : RegistrationUiState()
    object NoFaceDetected : RegistrationUiState()
    object MultipleFacesDetected : RegistrationUiState()
    object FaceNotFacingForward : RegistrationUiState()
    data class PhotoCaptured(val current: Int, val total: Int) : RegistrationUiState()
    object AllPhotosCaptured : RegistrationUiState()
    object RegisteringEmployee : RegistrationUiState()
    data class RegistrationSuccess(val employee: Employee) : RegistrationUiState()
    data class Error(val message: String) : RegistrationUiState()
}
