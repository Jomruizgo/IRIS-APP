package com.attendance.facerecognition.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors

/**
 * Componente de vista previa de cámara usando CameraX
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
    onFrameAnalyzed: ((Bitmap) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val previewView = remember { PreviewView(context) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier.fillMaxSize(),
        update = { view ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                // Preview
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(view.surfaceProvider)
                    }

                // Image Analysis
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            try {
                                onFrameAnalyzed?.let { callback ->
                                    imageProxy.toBitmap()?.let { bitmap ->
                                        callback(bitmap)
                                    } ?: android.util.Log.e("CameraPreview", "Bitmap is null")
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("CameraPreview", "Error converting frame", e)
                            } finally {
                                imageProxy.close()
                            }
                        }
                    }

                try {
                    // Unbind all use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                } catch (exc: Exception) {
                    android.util.Log.e("CameraPreview", "Error al iniciar cámara", exc)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

/**
 * Convierte ImageProxy (formato YUV) a Bitmap
 */
private fun ImageProxy.toBitmap(): Bitmap? {
    return try {
        // Verificar formato
        if (format != ImageFormat.YUV_420_888) {
            android.util.Log.e("CameraPreview", "Formato no soportado: $format")
            return null
        }

        val yBuffer = planes[0].buffer // Y
        val uBuffer = planes[1].buffer // U
        val vBuffer = planes[2].buffer // V

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // Copiar plano Y
        yBuffer.get(nv21, 0, ySize)

        // Convertir planos U y V a formato NV21 (entrelazado VU)
        val uvBuffer = ByteBuffer.allocate(uSize + vSize)
        vBuffer.get(uvBuffer.array(), 0, vSize)
        uBuffer.get(uvBuffer.array(), vSize, uSize)

        // Copiar UV entrelazado
        System.arraycopy(uvBuffer.array(), 0, nv21, ySize, uSize + vSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 90, out)
        val imageBytes = out.toByteArray()

        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        if (bitmap == null) {
            android.util.Log.e("CameraPreview", "Failed to decode bitmap")
            return null
        }

        // Rotar la imagen según la orientación
        val rotationDegrees = imageInfo.rotationDegrees
        val rotatedBitmap = if (rotationDegrees != 0) {
            rotateBitmap(bitmap, rotationDegrees.toFloat())
        } else {
            bitmap
        }

        android.util.Log.d("CameraPreview", "Bitmap created: ${rotatedBitmap.width}x${rotatedBitmap.height}")
        rotatedBitmap

    } catch (e: Exception) {
        android.util.Log.e("CameraPreview", "Error converting to bitmap", e)
        null
    }
}

/**
 * Rota un bitmap
 */
private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(degrees)
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}
