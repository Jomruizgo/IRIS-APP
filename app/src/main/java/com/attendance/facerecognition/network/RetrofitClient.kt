package com.attendance.facerecognition.network

import android.content.Context
import com.attendance.facerecognition.device.DeviceManager
import com.attendance.facerecognition.network.api.AttendanceApiService
import com.attendance.facerecognition.settings.SettingsManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente Retrofit singleton para comunicación con el backend
 * Incluye interceptores para autenticación y logging
 */
object RetrofitClient {

    @Volatile
    private var apiService: AttendanceApiService? = null

    fun getApiService(context: Context): AttendanceApiService {
        return apiService ?: synchronized(this) {
            apiService ?: buildApiService(context).also { apiService = it }
        }
    }

    private fun buildApiService(context: Context): AttendanceApiService {
        val deviceManager = DeviceManager(context)
        val settingsManager = SettingsManager(context)

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(deviceManager))
            .addInterceptor(createLoggingInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        // Obtener base URL de configuración
        val baseUrl = runBlocking {
            settingsManager.getServerUrl()
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(AttendanceApiService::class.java)
    }

    /**
     * Invalida la instancia del API service
     * Útil cuando cambia la URL del servidor en configuración
     */
    fun invalidate() {
        synchronized(this) {
            apiService = null
        }
    }
}

/**
 * Interceptor para agregar token de autenticación
 */
class AuthInterceptor(
    private val deviceManager: DeviceManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()

        // Obtener token del dispositivo
        val token = runBlocking {
            deviceManager.getDeviceToken()
        }

        // Si no hay token (dispositivo no registrado), continuar sin Authorization header
        val request = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(request)
    }
}

/**
 * Crea un interceptor de logging para desarrollo
 * En producción, cambiar a Level.NONE
 */
fun createLoggingInterceptor(): HttpLoggingInterceptor {
    return HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
}
