package com.smartexambox.security.monitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NetworkState(
    val isConnected: Boolean = true,
    val isWifi: Boolean = true,
    val isCellular: Boolean = false,
    val networkName: String = "VELS-Secure-WLAN",
    val hasInternet: Boolean = true,
    val isSimulatedOffline: Boolean = false
)

class NetworkConnectivityMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private val _networkState = MutableStateFlow(getCurrentNetworkState())
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    private var simulatedOffline: Boolean = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (!simulatedOffline) {
                _networkState.value = getCurrentNetworkState()
            }
        }

        override fun onLost(network: Network) {
            if (!simulatedOffline) {
                _networkState.value = getCurrentNetworkState()
            }
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            if (!simulatedOffline) {
                _networkState.value = getCurrentNetworkState()
            }
        }
    }

    init {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            // Fallback gracefully if network callback cannot register
        }
    }

    fun getCurrentNetworkState(): NetworkState {
        if (simulatedOffline) {
            return NetworkState(
                isConnected = false,
                isWifi = false,
                isCellular = false,
                networkName = "Disconnected (Simulated Demo)",
                hasInternet = false,
                isSimulatedOffline = true
            )
        }

        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        if (activeNetwork == null || capabilities == null) {
            return NetworkState(
                isConnected = false,
                isWifi = false,
                isCellular = false,
                networkName = "Disconnected",
                hasInternet = false,
                isSimulatedOffline = false
            )
        }

        val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        var ssid = if (isWifi) "Wi-Fi (Active)" else if (isCellular) "Cellular LTE/5G" else "Connected Network"
        if (isWifi) {
            try {
                val info = wifiManager?.connectionInfo
                val currentSsid = info?.ssid?.replace("\"", "")
                if (!currentSsid.isNullOrBlank() && currentSsid != "<unknown ssid>") {
                    ssid = currentSsid
                }
            } catch (e: Exception) {
                // Ignore permission restrictions for SSID
            }
        }

        return NetworkState(
            isConnected = true,
            isWifi = isWifi,
            isCellular = isCellular,
            networkName = ssid,
            hasInternet = hasInternet,
            isSimulatedOffline = false
        )
    }

    fun simulateOfflineMode(offline: Boolean) {
        simulatedOffline = offline
        _networkState.value = getCurrentNetworkState()
    }

    companion object {
        @Volatile
        private var instance: NetworkConnectivityMonitor? = null

        fun getInstance(context: Context): NetworkConnectivityMonitor {
            return instance ?: synchronized(this) {
                instance ?: NetworkConnectivityMonitor(context.applicationContext).also { instance = it }
            }
        }
    }
}
