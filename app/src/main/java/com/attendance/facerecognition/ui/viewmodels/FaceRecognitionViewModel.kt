package com.attendance.facerecognition.ui.viewmodels

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.facerecognition.data.local.database.AppDatabase
import com.attendance.facerecognition.data.local.entities.AttendanceRecord
import com.attendance.facerecognition.data.local.entities.AttendanceType
import com.attendance.facerecognition.data.local.entities.Employee
import com.attendance.facerecognition.data.repository.AttendanceRepository
import com.attendance.facerecognition.data.repository.EmployeeRepository
import com.attendance.facerecognition.ml.DetectedFace
import com.attendance.facerecognition.ml.FaceDetector
import com.attendance.facerecognition.ml.FaceRecognizer
import com.attendance.facerecognition.ml.LivenessChallenge
import com.attendance.facerecognition.ml.LivenessDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class FaceRecognitionViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val employeeRepository = EmployeeRepository(database.employeeDao())
    private val attendanceRepository = AttendanceRepository(database.attendanceDao())
    private val faceDetector = FaceDetector()
    private val faceRecognizer = FaceRecognizer(application)
    private val livenessDetector = LivenessDetector()

    private val _uiState = MutableStateFlow<RecognitionUiState>(RecognitionUiState.Idle)
    val uiState: StateFlow<RecognitionUiState> = _uiState.asStateFlow()

    private val _currentChallenge = MutableStateFlow<com.attendance.facerecognition.ml.LivenessChallenge?>(null)
    val currentChallenge: StateFlow<com.attendance.facerecognition.ml.LivenessChallenge?> = _currentChallenge.asStateFlow()

    private var isProcessingFrame = false
    private var livenessVerified = false
    private var allEmployees: List<Employee> = emptyList()

    // Umbral de confianza para reconocimiento
    private val confidenceThreshold = 0.85f

    /**
     * Inicia el proceso de reconocimiento
     */
    fun startRecognition() {
        viewModelScope.launch {
            try {
                // Cargar todos los empleados
                allEmployees = employeeRepository.getAllEmployeesWithEmbeddings()

                if (allEmployees.isEmpty()) {
                    _uiState.value = RecognitionUiState.Error("No hay empleados registrados en el sistema")
                    return@launch
                }

                // Generar desafío aleatorio
                val challenge = livenessDetector.generateChallenge()
                _currentChallenge.value = challenge
                _uiState.value = RecognitionUiState.LivenessChallenge

            } catch (e: Exception) {
                _uiState.value = RecognitionUiState.Error("Error al iniciar reconocimiento: ${e.message}")
            }
        }
    }

    /**
     * Procesa un frame de la cámara
     */
    fun processFrame(frame: Bitmap) {
        if (isProcessingFrame) return

        viewModelScope.launch {
            try {
                isProcessingFrame = true

                // Detectar rostros
                val faces = faceDetector.detectFaces(frame)

                if (faces.isEmpty()) {
                    _uiState.value = RecognitionUiState.NoFaceDetected
                    return@launch
                }

                if (faces.size > 1) {
                    _uiState.value = RecognitionUiState.MultipleFacesDetected
                    return@launch
                }

                val face = faces.first()

                // Si aún no se verifica liveness, procesar desafío
                if (!livenessVerified) {
                    processLivenessChallenge(face)
                } else {
                    // Ya pasó liveness, proceder con reconocimiento
                    recognizeFace(frame, face)
                }

            } catch (e: Exception) {
                _uiState.value = RecognitionUiState.Error("Error al procesar frame: ${e.message}")
            } finally {
                isProcessingFrame = false
            }
        }
    }

    /**
     * Procesa el desafío de liveness
     */
    private suspend fun processLivenessChallenge(face: DetectedFace) {
        val challenge = _currentChallenge.value ?: return

        val challengePassed = livenessDetector.verifyChallenge(challenge, face)

        if (challengePassed) {
            livenessVerified = true
            _uiState.value = RecognitionUiState.LivenessVerified
            kotlinx.coroutines.delay(500) // Pequeña pausa
            _uiState.value = RecognitionUiState.Recognizing
        } else {
            // Actualizar UI con el desafío actual
            _uiState.value = RecognitionUiState.LivenessChallenge
        }
    }

    /**
     * Reconoce el rostro comparando con empleados registrados
     */
    private suspend fun recognizeFace(frame: Bitmap, face: DetectedFace) {
        try {
            // Recortar rostro
            val croppedFace = faceDetector.cropFace(frame, face.boundingBox, padding = 0.2f)

            // Generar embedding
            val targetEmbedding = faceRecognizer.generateEmbedding(croppedFace)

            // Buscar mejor coincidencia entre todos los empleados
            var bestMatch: Employee? = null
            var bestConfidence = 0f

            for (employee in allEmployees) {
                // Comparar con todos los embeddings del empleado
                for (embedding in employee.faceEmbeddings) {
                    val similarity = faceRecognizer.calculateCosineSimilarity(targetEmbedding, embedding)
                    if (similarity > bestConfidence) {
                        bestConfidence = similarity
                        bestMatch = employee
                    }
                }
            }

            // Verificar si la confianza supera el umbral
            if (bestMatch != null && bestConfidence >= confidenceThreshold) {
                // Registrar asistencia
                registerAttendance(bestMatch, bestConfidence)
            } else {
                _uiState.value = RecognitionUiState.NotRecognized(bestConfidence)
            }

        } catch (e: Exception) {
            _uiState.value = RecognitionUiState.Error("Error en reconocimiento: ${e.message}")
        }
    }

    /**
     * Registra la asistencia del empleado
     */
    private suspend fun registerAttendance(employee: Employee, confidence: Float) {
        try {
            // Determinar tipo de registro (entrada o salida)
            val recordType = attendanceRepository.getNextRecordType(employee.id)

            // Crear registro
            val record = AttendanceRecord(
                employeeId = employee.id,
                employeeName = employee.fullName,
                employeeIdNumber = employee.employeeId,
                timestamp = System.currentTimeMillis(),
                type = recordType,
                confidence = confidence,
                livenessScore = 0.9f, // Simplificado por ahora
                livenessChallenge = _currentChallenge.value?.instruction ?: "none",
                isSynced = false
            )

            // Guardar en base de datos
            attendanceRepository.insertRecord(record)

            _uiState.value = RecognitionUiState.RecognitionSuccess(employee, recordType, confidence)

        } catch (e: Exception) {
            _uiState.value = RecognitionUiState.Error("Error al registrar asistencia: ${e.message}")
        }
    }

    /**
     * Reinicia el proceso de reconocimiento
     */
    fun reset() {
        livenessVerified = false
        _currentChallenge.value = null
        _uiState.value = RecognitionUiState.Idle
    }

    /**
     * Detiene el reconocimiento
     */
    fun stopRecognition() {
        reset()
    }

    override fun onCleared() {
        super.onCleared()
        faceDetector.close()
        faceRecognizer.close()
    }
}

/**
 * Estados de la UI de reconocimiento
 */
sealed class RecognitionUiState {
    object Idle : RecognitionUiState()
    object LivenessChallenge : RecognitionUiState()
    object LivenessVerified : RecognitionUiState()
    object Recognizing : RecognitionUiState()
    object NoFaceDetected : RecognitionUiState()
    object MultipleFacesDetected : RecognitionUiState()
    data class NotRecognized(val confidence: Float) : RecognitionUiState()
    data class RecognitionSuccess(
        val employee: Employee,
        val recordType: AttendanceType,
        val confidence: Float
    ) : RecognitionUiState()
    data class Error(val message: String) : RecognitionUiState()
}
