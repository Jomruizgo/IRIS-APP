package com.attendance.facerecognition.ml

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wrapper para ML Kit Face Detection
 * Detecta rostros y extrae características necesarias para liveness detection
 */
class FaceDetector {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.15f) // Tamaño mínimo del rostro (15% de la imagen)
        .enableTracking() // Para tracking entre frames
        .build()

    private val detector = FaceDetection.getClient(options)

    /**
     * Detecta rostros en un bitmap
     * @param bitmap Imagen a procesar
     * @return Lista de rostros detectados con sus características
     */
    suspend fun detectFaces(bitmap: Bitmap): List<DetectedFace> =
        suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)

            detector.process(image)
                .addOnSuccessListener { faces ->
                    val detectedFaces = faces.map { face ->
                        DetectedFace(
                            boundingBox = face.boundingBox,
                            trackingId = face.trackingId,
                            headEulerAngleX = face.headEulerAngleX, // Pitch (arriba/abajo)
                            headEulerAngleY = face.headEulerAngleY, // Yaw (izquierda/derecha)
                            headEulerAngleZ = face.headEulerAngleZ, // Roll (inclinación)
                            leftEyeOpenProbability = face.leftEyeOpenProbability,
                            rightEyeOpenProbability = face.rightEyeOpenProbability,
                            smilingProbability = face.smilingProbability
                        )
                    }
                    continuation.resume(detectedFaces)
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }

            continuation.invokeOnCancellation {
                // Cleanup si se cancela
            }
        }

    /**
     * Recorta el rostro del bitmap basado en el bounding box
     * Añade padding para incluir más contexto
     */
    fun cropFace(bitmap: Bitmap, boundingBox: Rect, padding: Float = 0.2f): Bitmap {
        val paddingX = (boundingBox.width() * padding).toInt()
        val paddingY = (boundingBox.height() * padding).toInt()

        val left = (boundingBox.left - paddingX).coerceAtLeast(0)
        val top = (boundingBox.top - paddingY).coerceAtLeast(0)
        val right = (boundingBox.right + paddingX).coerceAtMost(bitmap.width)
        val bottom = (boundingBox.bottom + paddingY).coerceAtMost(bitmap.height)

        val width = right - left
        val height = bottom - top

        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }

    /**
     * Cierra el detector para liberar recursos
     */
    fun close() {
        detector.close()
    }
}

/**
 * Data class que encapsula toda la información del rostro detectado
 */
data class DetectedFace(
    val boundingBox: Rect,
    val trackingId: Int?,

    // Rotación de la cabeza
    val headEulerAngleX: Float, // -90 a 90 (arriba/abajo)
    val headEulerAngleY: Float, // -90 a 90 (izquierda/derecha)
    val headEulerAngleZ: Float, // -90 a 90 (inclinación)

    // Características para liveness detection
    val leftEyeOpenProbability: Float?, // 0.0 = cerrado, 1.0 = abierto
    val rightEyeOpenProbability: Float?,
    val smilingProbability: Float?
) {
    /**
     * Verifica si ambos ojos están cerrados
     */
    fun areEyesClosed(threshold: Float = 0.4f): Boolean {
        return (leftEyeOpenProbability ?: 1f) < threshold &&
               (rightEyeOpenProbability ?: 1f) < threshold
    }

    /**
     * Verifica si ambos ojos están abiertos
     */
    fun areEyesOpen(threshold: Float = 0.6f): Boolean {
        return (leftEyeOpenProbability ?: 0f) > threshold &&
               (rightEyeOpenProbability ?: 0f) > threshold
    }

    /**
     * Verifica si está sonriendo
     */
    fun isSmiling(threshold: Float = 0.7f): Boolean {
        return (smilingProbability ?: 0f) > threshold
    }

    /**
     * Verifica si la cabeza está girada a la izquierda
     */
    fun isHeadTurnedLeft(threshold: Float = 20f): Boolean {
        return headEulerAngleY < -threshold
    }

    /**
     * Verifica si la cabeza está girada a la derecha
     */
    fun isHeadTurnedRight(threshold: Float = 20f): Boolean {
        return headEulerAngleY > threshold
    }

    /**
     * Verifica si la cabeza está mirando al frente
     * Solo verifica pitch (X) y yaw (Y), ignora roll (Z) porque puede ser rotación de imagen
     */
    fun isHeadFacingForward(threshold: Float = 15f): Boolean {
        return kotlin.math.abs(headEulerAngleX) < threshold &&
               kotlin.math.abs(headEulerAngleY) < threshold
        // Ignoramos headEulerAngleZ (roll) porque puede ser rotación de la cámara/imagen
    }
}
