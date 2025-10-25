package com.attendance.facerecognition.ui.viewmodels

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.facerecognition.data.local.database.AppDatabase
import com.attendance.facerecognition.data.local.entities.AttendanceAudit
import com.attendance.facerecognition.data.local.entities.AttendanceRecord
import com.attendance.facerecognition.data.local.entities.AttendanceType
import com.attendance.facerecognition.data.local.entities.AuditAction
import com.attendance.facerecognition.data.local.entities.Employee
import com.attendance.facerecognition.data.local.entities.PendingAttendanceRecord
import com.attendance.facerecognition.data.local.entities.PendingReason
import com.attendance.facerecognition.data.repository.AttendanceAuditRepository
import com.attendance.facerecognition.data.repository.AttendanceRepository
import com.attendance.facerecognition.data.repository.EmployeeRepository
import com.attendance.facerecognition.data.repository.PendingAttendanceRepository
import com.attendance.facerecognition.device.DeviceManager
import java.io.File
import java.io.FileOutputStream
import com.google.gson.Gson
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
    private val auditRepository = AttendanceAuditRepository(database.attendanceAuditDao())
    private val pendingRepository = PendingAttendanceRepository(database.pendingAttendanceDao())
    private val deviceManager = DeviceManager(application)
    private val faceDetector = FaceDetector()
    private val faceRecognizer = FaceRecognizer(application)
    private val livenessDetector = LivenessDetector()
    private val gson = Gson()

    private val _uiState = MutableStateFlow<RecognitionUiState>(RecognitionUiState.Idle)
    val uiState: StateFlow<RecognitionUiState> = _uiState.asStateFlow()

    private val _currentChallenge = MutableStateFlow<com.attendance.facerecognition.ml.LivenessChallenge?>(null)
    val currentChallenge: StateFlow<com.attendance.facerecognition.ml.LivenessChallenge?> = _currentChallenge.asStateFlow()

    private val _selectedType = MutableStateFlow<AttendanceType?>(null)
    val selectedType: StateFlow<AttendanceType?> = _selectedType.asStateFlow()

    private var isProcessingFrame = false
    private var livenessVerified = false
    private var allEmployees: List<Employee> = emptyList()
    private var lastInsertedRecordId: Long? = null
    private var lastCapturedFrame: Bitmap? = null  // Guardar último frame para registro manual

    // Umbral de confianza para reconocimiento
    private val confidenceThreshold = 0.85f

    /**
     * Selecciona el tipo de asistencia (ENTRADA o SALIDA)
     * Esta selección se valida después del reconocimiento
     */
    fun selectAttendanceType(type: AttendanceType) {
        _selectedType.value = type
        _uiState.value = RecognitionUiState.TypeSelected(type)
    }

    /**
     * Inicia el proceso de reconocimiento
     * Requiere que se haya seleccionado un tipo previamente
     */
    fun startRecognition() {
        viewModelScope.launch {
            try {
                // Verificar que se haya seleccionado tipo
                if (_selectedType.value == null) {
                    _uiState.value = RecognitionUiState.Error("Debes seleccionar ENTRADA o SALIDA primero")
                    return@launch
                }

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

        // No procesar frames si hay un error o validación pendiente
        val currentState = _uiState.value
        if (currentState is RecognitionUiState.Error ||
            currentState is RecognitionUiState.ValidationError ||
            currentState is RecognitionUiState.NotRecognized ||
            currentState is RecognitionUiState.RecognitionSuccess) {
            return
        }

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
                // Guardar frame para posible registro manual
                lastCapturedFrame = frame.copy(frame.config, true)
                _uiState.value = RecognitionUiState.NotRecognized(bestConfidence)
            }

        } catch (e: Exception) {
            _uiState.value = RecognitionUiState.Error("Error en reconocimiento: ${e.message}")
        }
    }

    /**
     * Registra la asistencia del empleado con validación del tipo seleccionado
     */
    private suspend fun registerAttendance(employee: Employee, confidence: Float) {
        try {
            val selectedType = _selectedType.value
            if (selectedType == null) {
                _uiState.value = RecognitionUiState.Error("No se ha seleccionado tipo de registro")
                return
            }

            // Obtener último registro del empleado
            val lastRecord = attendanceRepository.getLastRecordForEmployee(employee.id)

            // VALIDACIÓN: No permitir ENTRADA si ya hay entrada sin salida
            if (selectedType == AttendanceType.ENTRY) {
                if (lastRecord != null && lastRecord.type == AttendanceType.ENTRY) {
                    _uiState.value = RecognitionUiState.ValidationError(
                        employee = employee,
                        selectedType = selectedType,
                        lastRecord = lastRecord,
                        message = "Ya tienes ENTRADA sin SALIDA registrada el ${formatTimestamp(lastRecord.timestamp)}\n\nDebes registrar SALIDA primero."
                    )
                    return
                }
            }

            // VALIDACIÓN: No permitir SALIDA sin entrada previa
            if (selectedType == AttendanceType.EXIT) {
                if (lastRecord == null || lastRecord.type == AttendanceType.EXIT) {
                    _uiState.value = RecognitionUiState.ValidationError(
                        employee = employee,
                        selectedType = selectedType,
                        lastRecord = lastRecord,
                        message = "No puedes registrar SALIDA sin ENTRADA previa.\n\nPrimero debes registrar tu ENTRADA."
                    )
                    return
                }
            }

            // Crear registro
            val record = AttendanceRecord(
                employeeId = employee.id,
                employeeName = employee.fullName,
                employeeIdNumber = employee.employeeId,
                timestamp = System.currentTimeMillis(),
                type = selectedType,
                confidence = confidence,
                livenessScore = 0.9f, // Simplificado por ahora
                livenessChallenge = _currentChallenge.value?.instruction ?: "none",
                isSynced = false
            )

            // Guardar en base de datos
            lastInsertedRecordId = attendanceRepository.insertRecord(record)

            // Registrar auditoría de creación
            val metadata = mapOf(
                "confidence" to confidence,
                "livenessScore" to 0.9f,
                "challenge" to (_currentChallenge.value?.instruction ?: "none"),
                "type" to selectedType.name
            )
            auditRepository.insertAudit(
                AttendanceAudit(
                    attendanceId = lastInsertedRecordId,
                    action = AuditAction.CREATED,
                    employeeIdDetected = employee.employeeId,
                    employeeIdActual = employee.employeeId,
                    metadata = gson.toJson(metadata)
                )
            )

            _uiState.value = RecognitionUiState.RecognitionSuccess(employee, selectedType, confidence)

        } catch (e: Exception) {
            _uiState.value = RecognitionUiState.Error("Error al registrar asistencia: ${e.message}")
        }
    }

    /**
     * Formatea un timestamp a formato legible
     */
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    /**
     * Reinicia el proceso de reconocimiento
     */
    fun reset() {
        livenessVerified = false
        _currentChallenge.value = null
        _selectedType.value = null
        _uiState.value = RecognitionUiState.Idle
    }

    /**
     * Detiene el reconocimiento
     */
    fun stopRecognition() {
        reset()
    }

    /**
     * Cancela el último registro de asistencia (cuando el usuario dice "Este no soy yo")
     */
    fun cancelLastRegistration() {
        viewModelScope.launch {
            try {
                val recordId = lastInsertedRecordId
                if (recordId != null) {
                    // Obtener el registro antes de eliminarlo para auditoría
                    val record = attendanceRepository.getRecordById(recordId)

                    // Eliminar el registro
                    attendanceRepository.deleteRecord(recordId)

                    // Registrar auditoría de cancelación
                    if (record != null) {
                        val metadata = mapOf(
                            "original_timestamp" to record.timestamp,
                            "original_type" to record.type.name,
                            "confidence" to record.confidence
                        )
                        auditRepository.insertAudit(
                            AttendanceAudit(
                                attendanceId = null, // Ya fue eliminado
                                action = AuditAction.CANCELLED_BY_USER,
                                employeeIdDetected = record.employeeIdNumber,
                                reason = "Usuario rechazó identificación",
                                metadata = gson.toJson(metadata)
                            )
                        )
                    }

                    lastInsertedRecordId = null
                }
            } catch (e: Exception) {
                // Error silencioso - el usuario ya rechazó el registro
            }
        }
    }

    /**
     * Crea un registro manual pendiente de aprobación cuando falla el reconocimiento facial
     */
    fun createManualRegistration(employeeId: String, employeeName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val selectedType = _selectedType.value
                if (selectedType == null) {
                    onError("No se ha seleccionado tipo de registro")
                    return@launch
                }

                val frame = lastCapturedFrame
                if (frame == null) {
                    onError("No se ha capturado imagen")
                    return@launch
                }

                // Guardar foto en storage interno
                val photoPath = savePhotoToStorage(frame)

                // Obtener device ID
                val deviceId = deviceManager.getDeviceId() ?: "unknown"

                // Crear registro pendiente
                val pendingRecord = PendingAttendanceRecord(
                    employeeId = employeeId,
                    employeeName = employeeName,
                    timestamp = System.currentTimeMillis(),
                    type = selectedType,
                    photoPath = photoPath,
                    deviceId = deviceId,
                    reason = PendingReason.FACIAL_FAILED
                )

                // Guardar en base de datos
                pendingRepository.insertPending(pendingRecord)

                onSuccess()

            } catch (e: Exception) {
                onError("Error al crear registro manual: ${e.message}")
            }
        }
    }

    /**
     * Guarda el bitmap como archivo JPEG en storage interno
     * Retorna la ruta del archivo guardado
     */
    private fun savePhotoToStorage(bitmap: Bitmap): String {
        val context = getApplication<Application>()
        val photosDir = File(context.filesDir, "pending_photos")
        if (!photosDir.exists()) {
            photosDir.mkdirs()
        }

        val timestamp = System.currentTimeMillis()
        val photoFile = File(photosDir, "pending_$timestamp.jpg")

        FileOutputStream(photoFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        return photoFile.absolutePath
    }

    override fun onCleared() {
        super.onCleared()
        faceDetector.close()
        faceRecognizer.close()
        lastCapturedFrame?.recycle()
        lastCapturedFrame = null
    }
}

/**
 * Estados de la UI de reconocimiento
 */
sealed class RecognitionUiState {
    object Idle : RecognitionUiState()
    data class TypeSelected(val type: AttendanceType) : RecognitionUiState()
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
    data class ValidationError(
        val employee: Employee,
        val selectedType: AttendanceType,
        val lastRecord: AttendanceRecord?,
        val message: String
    ) : RecognitionUiState()
    data class Error(val message: String) : RecognitionUiState()
}
