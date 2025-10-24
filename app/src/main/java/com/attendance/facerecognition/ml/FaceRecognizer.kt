package com.attendance.facerecognition.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * Reconocedor facial usando TensorFlow Lite con MobileFaceNet
 * Genera embeddings de 192 dimensiones para cada rostro
 */
class FaceRecognizer(context: Context) {

    private var interpreter: Interpreter? = null
    private val inputSize = 112 // MobileFaceNet usa imágenes de 112x112
    private val embeddingSize = 192 // Vector de salida de 192 dimensiones (depende del modelo)

    private val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
        .add(NormalizeOp(127.5f, 127.5f)) // Normalizar a rango [-1, 1]
        .build()

    init {
        try {
            // Cargar el modelo MobileFaceNet desde assets
            val model = FileUtil.loadMappedFile(context, "mobilefacenet.tflite")

            val options = Interpreter.Options().apply {
                setNumThreads(4) // Usar 4 threads para mejor rendimiento
                // Descomentar si quieres usar GPU (requiere más configuración)
                // addDelegate(GpuDelegate())
            }

            interpreter = Interpreter(model, options)
        } catch (e: Exception) {
            throw RuntimeException("Error al cargar el modelo MobileFaceNet: ${e.message}", e)
        }
    }

    /**
     * Genera un embedding (vector de 192 dimensiones) para un rostro
     * @param faceBitmap Imagen del rostro recortado
     * @return FloatArray de 192 dimensiones representando el rostro
     */
    fun generateEmbedding(faceBitmap: Bitmap): FloatArray {
        if (interpreter == null) {
            throw IllegalStateException("El intérprete no está inicializado")
        }

        // Procesar imagen
        var tensorImage = TensorImage.fromBitmap(faceBitmap)
        tensorImage = TensorImage.fromBitmap(
            Bitmap.createScaledBitmap(faceBitmap, inputSize, inputSize, true)
        )

        // Preparar buffer de entrada
        val inputBuffer = convertBitmapToByteBuffer(faceBitmap)

        // Preparar buffer de salida
        val outputBuffer = Array(1) { FloatArray(embeddingSize) }

        // Ejecutar inferencia
        interpreter?.run(inputBuffer, outputBuffer)

        // Normalizar el embedding (L2 normalization)
        return normalizeEmbedding(outputBuffer[0])
    }

    /**
     * Convierte un bitmap a ByteBuffer para el modelo
     */
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3) // 4 bytes por float, 3 canales RGB
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputSize * inputSize)
        resizedBitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

        var pixel = 0
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val value = intValues[pixel++]

                // Extraer canales RGB y normalizar a [-1, 1]
                val r = ((value shr 16 and 0xFF) - 127.5f) / 127.5f
                val g = ((value shr 8 and 0xFF) - 127.5f) / 127.5f
                val b = ((value and 0xFF) - 127.5f) / 127.5f

                byteBuffer.putFloat(r)
                byteBuffer.putFloat(g)
                byteBuffer.putFloat(b)
            }
        }

        return byteBuffer
    }

    /**
     * Normaliza el embedding usando L2 normalization
     * Esto mejora la precisión de la comparación
     */
    private fun normalizeEmbedding(embedding: FloatArray): FloatArray {
        var sum = 0f
        for (value in embedding) {
            sum += value * value
        }
        val norm = sqrt(sum)

        return if (norm > 0) {
            FloatArray(embedding.size) { i -> embedding[i] / norm }
        } else {
            embedding
        }
    }

    /**
     * Calcula la similitud coseno entre dos embeddings
     * @return Valor entre 0 (totalmente diferentes) y 1 (idénticos)
     */
    fun calculateCosineSimilarity(embedding1: FloatArray, embedding2: FloatArray): Float {
        if (embedding1.size != embedding2.size) {
            throw IllegalArgumentException("Los embeddings deben tener el mismo tamaño")
        }

        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f

        for (i in embedding1.indices) {
            dotProduct += embedding1[i] * embedding2[i]
            norm1 += embedding1[i] * embedding1[i]
            norm2 += embedding2[i] * embedding2[i]
        }

        val magnitude = sqrt(norm1) * sqrt(norm2)

        return if (magnitude > 0) {
            dotProduct / magnitude
        } else {
            0f
        }
    }

    /**
     * Calcula la distancia euclidiana entre dos embeddings
     * Menor distancia = mayor similitud
     */
    fun calculateEuclideanDistance(embedding1: FloatArray, embedding2: FloatArray): Float {
        if (embedding1.size != embedding2.size) {
            throw IllegalArgumentException("Los embeddings deben tener el mismo tamaño")
        }

        var sum = 0f
        for (i in embedding1.indices) {
            val diff = embedding1[i] - embedding2[i]
            sum += diff * diff
        }

        return sqrt(sum)
    }

    /**
     * Compara un embedding con múltiples embeddings de referencia
     * Retorna la mejor coincidencia
     * @param targetEmbedding El embedding a comparar
     * @param referenceEmbeddings Lista de embeddings de referencia
     * @param threshold Umbral mínimo de similitud (0.0 a 1.0)
     * @return Tupla de (índice del mejor match, similitud) o null si no hay match
     */
    fun findBestMatch(
        targetEmbedding: FloatArray,
        referenceEmbeddings: List<FloatArray>,
        threshold: Float = 0.7f
    ): Pair<Int, Float>? {
        if (referenceEmbeddings.isEmpty()) {
            return null
        }

        var bestMatchIndex = -1
        var bestSimilarity = 0f

        referenceEmbeddings.forEachIndexed { index, referenceEmbedding ->
            val similarity = calculateCosineSimilarity(targetEmbedding, referenceEmbedding)
            if (similarity > bestSimilarity && similarity >= threshold) {
                bestSimilarity = similarity
                bestMatchIndex = index
            }
        }

        return if (bestMatchIndex >= 0) {
            Pair(bestMatchIndex, bestSimilarity)
        } else {
            null
        }
    }

    /**
     * Genera múltiples embeddings para un empleado (mejora la precisión)
     * Útil durante el registro
     */
    fun generateMultipleEmbeddings(faceBitmaps: List<Bitmap>): List<FloatArray> {
        return faceBitmaps.map { generateEmbedding(it) }
    }

    /**
     * Libera recursos
     */
    fun close() {
        interpreter?.close()
        interpreter = null
    }
}

/**
 * Resultado del reconocimiento facial
 */
data class RecognitionResult(
    val matched: Boolean,
    val employeeId: Long?,
    val confidence: Float, // 0.0 a 1.0
    val embedding: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RecognitionResult

        if (matched != other.matched) return false
        if (employeeId != other.employeeId) return false
        if (confidence != other.confidence) return false
        if (!embedding.contentEquals(other.embedding)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = matched.hashCode()
        result = 31 * result + (employeeId?.hashCode() ?: 0)
        result = 31 * result + confidence.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}
