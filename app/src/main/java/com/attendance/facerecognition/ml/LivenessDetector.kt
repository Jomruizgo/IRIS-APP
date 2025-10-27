package com.attendance.facerecognition.ml

import android.graphics.Bitmap
import kotlinx.coroutines.delay

/**
 * Detector de vida (liveness) usando challenge-response activo
 * Pide al usuario realizar acciones aleatorias para verificar que es una persona real
 */
class LivenessDetector {

    /**
     * Tipos de desafíos que se le pueden pedir al usuario
     * IMPORTANTE: Solo usar desafíos que tengan fotos correspondientes en el registro
     *
     * Registro captura:
     * - Fotos de FRENTE (3 fotos)
     * - Fotos GIRADAS A LA IZQUIERDA (3 fotos)
     * - Fotos GIRADAS A LA DERECHA (3 fotos)
     * - Foto de FRENTE final (1 foto)
     *
     * Por lo tanto, solo usamos desafíos congruentes con lo registrado.
     */
    enum class ChallengeType {
        BLINK,          // Parpadear - No afecta embedding, solo anti-spoofing
        TURN_LEFT,      // Girar cabeza a la izquierda - ✅ Tenemos fotos
        TURN_RIGHT      // Girar cabeza a la derecha - ✅ Tenemos fotos

        // DESHABILITADOS (no tenemos fotos de entrenamiento):
        // SMILE - No capturamos fotos sonriendo
        // LOOK_UP - No capturamos fotos mirando arriba
    }

    /**
     * Resultado del desafío de liveness
     */
    data class LivenessResult(
        val passed: Boolean,
        val challenge: ChallengeType,
        val score: Float, // 0.0 a 1.0
        val message: String,
        val frames: Int // Número de frames procesados
    )

    /**
     * Genera un desafío aleatorio
     */
    fun generateRandomChallenge(): ChallengeType {
        return ChallengeType.values().random()
    }

    /**
     * Genera un objeto de desafío completo
     */
    fun generateChallenge(): LivenessChallenge {
        val type = generateRandomChallenge()
        return LivenessChallenge(type, getChallengeText(type))
    }

    /**
     * Obtiene el texto para mostrar al usuario según el desafío
     */
    fun getChallengeText(challenge: ChallengeType): String {
        return when (challenge) {
            ChallengeType.BLINK -> "Parpadea 2 veces"
            ChallengeType.TURN_LEFT -> "Gira la cabeza a la DERECHA"
            ChallengeType.TURN_RIGHT -> "Gira la cabeza a la IZQUIERDA"
        }
    }

    /**
     * Verifica si un desafío fue completado exitosamente usando un DetectedFace
     */
    fun verifyChallenge(
        challenge: LivenessChallenge,
        face: DetectedFace
    ): Boolean {
        return when (challenge.type) {
            ChallengeType.BLINK -> face.areEyesClosed(threshold = 0.3f)
            ChallengeType.TURN_LEFT -> face.isHeadTurnedLeft(threshold = 25f)
            ChallengeType.TURN_RIGHT -> face.isHeadTurnedRight(threshold = 25f)
        }
    }

    /**
     * Verifica si un desafío fue completado exitosamente
     * Este método debe ser llamado múltiples veces con frames consecutivos
     */
    suspend fun verifyChallenge(
        challenge: ChallengeType,
        bitmap: Bitmap,
        faceDetector: FaceDetector
    ): ChallengeVerificationResult {
        val faces = try {
            faceDetector.detectFaces(bitmap)
        } catch (e: Exception) {
            return ChallengeVerificationResult(
                verified = false,
                confidence = 0f,
                message = "Error al detectar rostro: ${e.message}"
            )
        }

        // Debe haber exactamente un rostro
        if (faces.isEmpty()) {
            return ChallengeVerificationResult(
                verified = false,
                confidence = 0f,
                message = "No se detectó ningún rostro"
            )
        }

        if (faces.size > 1) {
            return ChallengeVerificationResult(
                verified = false,
                confidence = 0f,
                message = "Se detectaron múltiples rostros"
            )
        }

        val face = faces.first()

        return when (challenge) {
            ChallengeType.BLINK -> verifyBlink(face)
            ChallengeType.TURN_LEFT -> verifyTurnLeft(face)
            ChallengeType.TURN_RIGHT -> verifyTurnRight(face)
        }
    }

    private fun verifyBlink(face: DetectedFace): ChallengeVerificationResult {
        return if (face.areEyesClosed(threshold = 0.3f)) {
            ChallengeVerificationResult(
                verified = true,
                confidence = 1f - ((face.leftEyeOpenProbability ?: 0f) + (face.rightEyeOpenProbability ?: 0f)) / 2f,
                message = "Ojos cerrados detectados"
            )
        } else {
            ChallengeVerificationResult(
                verified = false,
                confidence = 0f,
                message = "Esperando parpadeo..."
            )
        }
    }

    private fun verifyTurnLeft(face: DetectedFace): ChallengeVerificationResult {
        val isTurnedLeft = face.isHeadTurnedLeft(threshold = 25f)
        val angle = kotlin.math.abs(face.headEulerAngleY)
        return ChallengeVerificationResult(
            verified = isTurnedLeft,
            confidence = if (isTurnedLeft) (angle / 90f).coerceAtMost(1f) else 0f,
            message = if (isTurnedLeft) "Giro izquierda detectado" else "Gira más a la izquierda..."
        )
    }

    private fun verifyTurnRight(face: DetectedFace): ChallengeVerificationResult {
        val isTurnedRight = face.isHeadTurnedRight(threshold = 25f)
        val angle = kotlin.math.abs(face.headEulerAngleY)
        return ChallengeVerificationResult(
            verified = isTurnedRight,
            confidence = if (isTurnedRight) (angle / 90f).coerceAtMost(1f) else 0f,
            message = if (isTurnedRight) "Giro derecha detectado" else "Gira más a la derecha..."
        )
    }

    /**
     * Ejecuta un test de liveness completo con estado
     * Esta es la función principal que debe usar la UI
     */
    suspend fun runLivenessTest(
        challenge: ChallengeType,
        onFrameProcessed: suspend (Bitmap) -> Unit,
        maxDuration: Long = 10000L // 10 segundos máximo
    ): LivenessResult {
        val startTime = System.currentTimeMillis()
        var framesProcessed = 0
        var consecutiveSuccesses = 0
        val requiredSuccesses = if (challenge == ChallengeType.BLINK) 2 else 3 // Para parpadeo necesitamos 2 detecciones

        var totalConfidence = 0f
        var eyesWereOpen = false // Para detectar parpadeos

        // La UI debe llamar a processFrame() con cada bitmap capturado
        // Este es un ejemplo de cómo se usaría
        return LivenessResult(
            passed = false,
            challenge = challenge,
            score = 0f,
            message = "Iniciar captura de frames desde la UI",
            frames = 0
        )
    }

    /**
     * Procesa un frame individual durante el test de liveness
     */
    suspend fun processFrame(
        challenge: ChallengeType,
        bitmap: Bitmap,
        state: LivenessState,
        faceDetector: FaceDetector
    ): LivenessState {
        val result = verifyChallenge(challenge, bitmap, faceDetector)
        state.framesProcessed++

        when (challenge) {
            ChallengeType.BLINK -> {
                // Lógica especial para parpadeo: detectar transición abierto -> cerrado -> abierto
                val faces = faceDetector.detectFaces(bitmap)
                if (faces.isNotEmpty()) {
                    val face = faces.first()
                    val eyesCurrentlyOpen = face.areEyesOpen(threshold = 0.7f)
                    val eyesCurrentlyClosed = face.areEyesClosed(threshold = 0.3f)

                    when (state.blinkPhase) {
                        BlinkPhase.WAITING_EYES_OPEN -> {
                            if (eyesCurrentlyOpen) {
                                state.blinkPhase = BlinkPhase.WAITING_BLINK
                            }
                        }
                        BlinkPhase.WAITING_BLINK -> {
                            if (eyesCurrentlyClosed) {
                                state.blinkPhase = BlinkPhase.WAITING_EYES_REOPEN
                            }
                        }
                        BlinkPhase.WAITING_EYES_REOPEN -> {
                            if (eyesCurrentlyOpen) {
                                state.blinksDetected++
                                state.blinkPhase = if (state.blinksDetected < 2) {
                                    BlinkPhase.WAITING_BLINK
                                } else {
                                    BlinkPhase.COMPLETED
                                }
                            }
                        }
                        BlinkPhase.COMPLETED -> {
                            // Ya completó los 2 parpadeos
                        }
                    }
                }

                if (state.blinkPhase == BlinkPhase.COMPLETED) {
                    state.passed = true
                    state.totalConfidence = 0.95f
                }
            }
            else -> {
                // Para otros desafíos, necesitamos N frames consecutivos verificados
                if (result.verified) {
                    state.consecutiveSuccesses++
                    state.totalConfidence += result.confidence
                } else {
                    state.consecutiveSuccesses = 0
                }

                if (state.consecutiveSuccesses >= 3) {
                    state.passed = true
                    state.totalConfidence /= state.consecutiveSuccesses.toFloat()
                }
            }
        }

        state.lastMessage = result.message
        return state
    }
}

/**
 * Clase que representa un desafío de liveness
 */
data class LivenessChallenge(
    val type: LivenessDetector.ChallengeType,
    val instruction: String
)

/**
 * Estado del proceso de liveness detection
 */
data class LivenessState(
    var framesProcessed: Int = 0,
    var consecutiveSuccesses: Int = 0,
    var totalConfidence: Float = 0f,
    var passed: Boolean = false,
    var lastMessage: String = "",

    // Para detección de parpadeo
    var blinkPhase: BlinkPhase = BlinkPhase.WAITING_EYES_OPEN,
    var blinksDetected: Int = 0
)

enum class BlinkPhase {
    WAITING_EYES_OPEN,
    WAITING_BLINK,
    WAITING_EYES_REOPEN,
    COMPLETED
}

/**
 * Resultado de verificar un frame individual
 */
data class ChallengeVerificationResult(
    val verified: Boolean,
    val confidence: Float,
    val message: String
)
