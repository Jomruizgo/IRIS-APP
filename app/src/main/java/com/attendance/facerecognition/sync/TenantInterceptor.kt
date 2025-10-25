package com.attendance.facerecognition.sync

import com.attendance.facerecognition.tenant.TenantManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor que agrega el código de tenant a todas las requests HTTP
 * Necesario para que el backend pueda segmentar datos por empresa
 */
class TenantInterceptor(private val tenantManager: TenantManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Obtener el tenant code (bloqueante porque interceptor no es suspending)
        val tenantCode = runBlocking {
            tenantManager.getTenantCode()
        }

        // Si no hay tenant configurado, continuar sin el header
        if (tenantCode == null) {
            return chain.proceed(originalRequest)
        }

        // Agregar header X-Tenant-Code
        val requestWithTenant = originalRequest.newBuilder()
            .addHeader("X-Tenant-Code", tenantCode)
            .build()

        return chain.proceed(requestWithTenant)
    }
}
