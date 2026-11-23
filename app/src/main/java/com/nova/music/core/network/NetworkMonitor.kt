package com.nova.music.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NetworkState(val connected: Boolean = false, val metered: Boolean = true, val wifi: Boolean = false)

class NetworkMonitor(context: Context) {
    private val manager = requireNotNull(context.getSystemService(ConnectivityManager::class.java))
    private val mutable = MutableStateFlow(
        NetworkState(manager.activeNetwork != null, manager.isActiveNetworkMetered,
            manager.getNetworkCapabilities(manager.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true)
    )
    val state: StateFlow<NetworkState> = mutable.asStateFlow()

    // Application singleton. Android owns the callback for the lifetime of this process.
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            mutable.value = NetworkState(
                connected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
                metered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
                wifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            )
        }

        override fun onLost(network: Network) {
            mutable.value = mutable.value.copy(connected = false)
        }
    }

    init {
        manager.registerDefaultNetworkCallback(callback)
    }
}
