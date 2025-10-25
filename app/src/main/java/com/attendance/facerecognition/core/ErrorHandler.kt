package com.attendance.facerecognition.core

import android.util.Log

/**
 * Manejador centralizado de errores
 */
object ErrorHandler {

    @PublishedApi
    internal const val TAG = "ErrorHandler"

    /**
     * Convierte un AppError en un mensaje amigable para el usuario
     */
    fun getUserMessage(error: AppError): String {
        return when (error) {
            // Errores de base de datos
            is AppError.DatabaseError.InsertFailed -> "No se pudo guardar la información. Intenta nuevamente."
            is AppError.DatabaseError.UpdateFailed -> "No se pudo actualizar la información. Intenta nuevamente."
            is AppError.DatabaseError.DeleteFailed -> "No se pudo eliminar la información. Intenta nuevamente."
            is AppError.DatabaseError.QueryFailed -> "No se pudo consultar la información. Intenta nuevamente."
            is AppError.DatabaseError.EncryptionFailed -> "Error de seguridad. Contacta al administrador."

            // Errores de red
            is AppError.NetworkError.NoConnection -> "Sin conexión a Internet. Los datos se sincronizarán cuando haya WiFi."
            is AppError.NetworkError.Timeout -> "El servidor tardó demasiado en responder. Intenta nuevamente."
            is AppError.NetworkError.ServerError -> "Error del servidor (${error.code}). Intenta más tarde."
            is AppError.NetworkError.UnknownError -> "Error de conexión. Verifica tu red e intenta nuevamente."

            // Errores de reconocimiento facial
            is AppError.FaceRecognitionError.NoFaceDetected -> "No se detectó ningún rostro. Asegúrate de estar frente a la cámara."
            is AppError.FaceRecognitionError.MultipleFacesDetected -> "Se detectaron múltiples rostros. Asegúrate de estar solo frente a la cámara."
            is AppError.FaceRecognitionError.FaceNotRecognized -> "Rostro no reconocido. Verifica tu identidad o regístrate."
            is AppError.FaceRecognitionError.LivenessCheckFailed -> "Verificación de vida fallida. Completa los desafíos solicitados."
            is AppError.FaceRecognitionError.ModelLoadFailed -> "Error al cargar el modelo de reconocimiento. Reinicia la aplicación."
            is AppError.FaceRecognitionError.ProcessingFailed -> "Error al procesar la imagen. Intenta nuevamente."

            // Errores de cámara
            is AppError.CameraError.PermissionDenied -> "Permiso de cámara denegado. Ve a Ajustes para habilitarlo."
            is AppError.CameraError.InitializationFailed -> "No se pudo inicializar la cámara. Verifica los permisos."
            is AppError.CameraError.CaptureFailed -> "Error al capturar imagen. Intenta nuevamente."

            // Errores de autenticación
            is AppError.AuthenticationError.InvalidCredentials -> "Usuario o PIN incorrecto."
            is AppError.AuthenticationError.SessionExpired -> "Tu sesión ha expirado. Inicia sesión nuevamente."
            is AppError.AuthenticationError.Unauthorized -> "No tienes permisos para realizar esta acción."
            is AppError.AuthenticationError.BiometricFailed -> error.message

            // Errores de validación
            is AppError.ValidationError.EmptyField -> "Por favor completa todos los campos."
            is AppError.ValidationError.InvalidFormat -> "Formato inválido. Verifica los datos ingresados."
            is AppError.ValidationError.DuplicateEntry -> "Ya existe un registro con estos datos."
            is AppError.ValidationError.Custom -> error.message

            // Errores de sincronización
            is AppError.SyncError.NoDataToSync -> "No hay datos pendientes para sincronizar."
            is AppError.SyncError.UploadFailed -> "Error al subir datos. Se reintentará automáticamente."
            is AppError.SyncError.DownloadFailed -> "Error al descargar actualizaciones. Intenta más tarde."
            is AppError.SyncError.ConflictResolutionFailed -> "Error al sincronizar. Contacta al administrador."

            // Error desconocido
            is AppError.UnknownError -> "Ocurrió un error inesperado. Intenta nuevamente."
        }
    }

    /**
     * Registra el error en los logs (internal para poder ser usado por inline functions)
     */
    @PublishedApi
    internal fun logError(error: AppError, tag: String = TAG) {
        val logMessage = buildString {
            append("Error: ${error.message}")
            error.cause?.let {
                append("\nCause: ${it.message}")
                append("\nStackTrace: ${it.stackTraceToString()}")
            }
        }

        when (error) {
            is AppError.ValidationError -> Log.w(tag, logMessage)
            is AppError.NetworkError.NoConnection -> Log.i(tag, logMessage)
            else -> Log.e(tag, logMessage, error.cause)
        }
    }

    /**
     * Maneja el error: lo registra y devuelve el mensaje para el usuario
     */
    fun handleError(error: AppError, tag: String = TAG): String {
        logError(error, tag)
        return getUserMessage(error)
    }

    /**
     * Ejecuta un bloque de código y maneja cualquier excepción
     */
    inline fun <T> runCatching(
        tag: String = TAG,
        block: () -> T
    ): Result<T> {
        return try {
            Result.Success(block())
        } catch (e: Exception) {
            val error = e.toAppError()
            logError(error, tag)
            Result.Error(error)
        }
    }

    /**
     * Ejecuta un bloque de código suspendido y maneja cualquier excepción
     */
    suspend inline fun <T> runCatchingSuspend(
        tag: String = TAG,
        crossinline block: suspend () -> T
    ): Result<T> {
        return try {
            Result.Success(block())
        } catch (e: Exception) {
            val error = e.toAppError()
            logError(error, tag)
            Result.Error(error)
        }
    }
}
