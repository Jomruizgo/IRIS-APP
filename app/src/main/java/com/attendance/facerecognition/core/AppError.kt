package com.attendance.facerecognition.core

/**
 * Representa los diferentes tipos de errores que pueden ocurrir en la aplicación
 */
sealed class AppError(
    open val message: String,
    open val cause: Throwable? = null
) {
    /**
     * Errores de base de datos
     */
    sealed class DatabaseError(message: String, cause: Throwable? = null) : AppError(message, cause) {
        class InsertFailed(cause: Throwable) : DatabaseError("Error al insertar datos", cause)
        class UpdateFailed(cause: Throwable) : DatabaseError("Error al actualizar datos", cause)
        class DeleteFailed(cause: Throwable) : DatabaseError("Error al eliminar datos", cause)
        class QueryFailed(cause: Throwable) : DatabaseError("Error al consultar datos", cause)
        class EncryptionFailed(cause: Throwable) : DatabaseError("Error de encriptación", cause)
    }

    /**
     * Errores de red
     */
    sealed class NetworkError(message: String, cause: Throwable? = null) : AppError(message, cause) {
        object NoConnection : NetworkError("Sin conexión a Internet")
        class Timeout : NetworkError("Tiempo de espera agotado")
        class ServerError(val code: Int) : NetworkError("Error del servidor: $code")
        class UnknownError(cause: Throwable) : NetworkError("Error de red desconocido", cause)
    }

    /**
     * Errores de reconocimiento facial
     */
    sealed class FaceRecognitionError(message: String, cause: Throwable? = null) : AppError(message, cause) {
        object NoFaceDetected : FaceRecognitionError("No se detectó ningún rostro")
        object MultipleFacesDetected : FaceRecognitionError("Se detectaron múltiples rostros")
        object FaceNotRecognized : FaceRecognitionError("Rostro no reconocido")
        object LivenessCheckFailed : FaceRecognitionError("Verificación de vida fallida")
        class ModelLoadFailed(cause: Throwable) : FaceRecognitionError("Error al cargar el modelo", cause)
        class ProcessingFailed(cause: Throwable) : FaceRecognitionError("Error al procesar imagen", cause)
    }

    /**
     * Errores de cámara
     */
    sealed class CameraError(message: String, cause: Throwable? = null) : AppError(message, cause) {
        object PermissionDenied : CameraError("Permiso de cámara denegado")
        class InitializationFailed(cause: Throwable) : CameraError("Error al inicializar cámara", cause)
        class CaptureFailed(cause: Throwable) : CameraError("Error al capturar imagen", cause)
    }

    /**
     * Errores de autenticación
     */
    sealed class AuthenticationError(message: String, cause: Throwable? = null) : AppError(message, cause) {
        object InvalidCredentials : AuthenticationError("Credenciales inválidas")
        object SessionExpired : AuthenticationError("La sesión ha expirado")
        object Unauthorized : AuthenticationError("No autorizado")
        class BiometricFailed(message: String) : AuthenticationError(message)
    }

    /**
     * Errores de validación
     */
    sealed class ValidationError(message: String) : AppError(message) {
        object EmptyField : ValidationError("Campo vacío")
        object InvalidFormat : ValidationError("Formato inválido")
        object DuplicateEntry : ValidationError("Ya existe un registro con estos datos")
        class Custom(customMessage: String) : ValidationError(customMessage)
    }

    /**
     * Errores de sincronización
     */
    sealed class SyncError(message: String, cause: Throwable? = null) : AppError(message, cause) {
        object NoDataToSync : SyncError("No hay datos para sincronizar")
        class UploadFailed(cause: Throwable) : SyncError("Error al subir datos", cause)
        class DownloadFailed(cause: Throwable) : SyncError("Error al descargar datos", cause)
        class ConflictResolutionFailed : SyncError("Error al resolver conflictos")
    }

    /**
     * Error desconocido
     */
    class UnknownError(cause: Throwable) : AppError("Error desconocido: ${cause.message}", cause)
}

/**
 * Resultado que puede contener un valor exitoso o un error
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val error: AppError) : Result<Nothing>()

    val isSuccess: Boolean
        get() = this is Success

    val isError: Boolean
        get() = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun getErrorOrNull(): AppError? = when (this) {
        is Success -> null
        is Error -> error
    }

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (AppError) -> Unit): Result<T> {
        if (this is Error) action(error)
        return this
    }
}

/**
 * Extensión para convertir excepciones en AppError
 */
fun Throwable.toAppError(): AppError {
    return when (this) {
        is java.net.UnknownHostException -> AppError.NetworkError.NoConnection
        is java.net.SocketTimeoutException -> AppError.NetworkError.Timeout()
        is java.io.IOException -> AppError.NetworkError.UnknownError(this)
        is android.database.sqlite.SQLiteException -> AppError.DatabaseError.QueryFailed(this)
        else -> AppError.UnknownError(this)
    }
}
