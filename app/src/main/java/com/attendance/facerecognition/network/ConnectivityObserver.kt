package com.attendance.facerecognition.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Observa el estado de conectividad de red
 */
class ConnectivityObserver(private val context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * Estado de conectividad
     */
    enum class Status {
        AVAILABLE,      // Red disponible
        UNAVAILABLE,    // Sin red
        LOSING,         // Perdiendo conexión
        LOST            // Conexión perdida
    }

    /**
     * Flow que emite el estado de conectividad
     */
    val networkStatus: Flow<Status> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                trySend(Status.AVAILABLE)
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                super.onLosing(network, maxMsToLive)
                trySend(Status.LOSING)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                trySend(Status.LOST)
            }

            override fun onUnavailable() {
                super.onUnavailable()
                trySend(Status.UNAVAILABLE)
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, callback)

        // Emitir estado inicial
        trySend(getCurrentStatus())

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    /**
     * Obtiene el estado actual de conectividad
     */
    fun getCurrentStatus(): Status {
        val network = connectivityManager.activeNetwork ?: return Status.UNAVAILABLE
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return Status.UNAVAILABLE

        return if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            Status.AVAILABLE
        } else {
            Status.UNAVAILABLE
        }
    }

    /**
     * Verifica si hay conexión WiFi activa
     */
    fun isWifiAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Verifica si hay conexión a Internet (WiFi o datos móviles)
     */
    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    }
}
