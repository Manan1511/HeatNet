package com.heatnet.measurement.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.heatnet.measurement.model.ConnectionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConnectionMonitor(context: Context) {
    private val mutableState = MutableStateFlow(ConnectionSnapshot(null, null, false))
    val state: StateFlow<ConnectionSnapshot> = mutableState.asStateFlow()

    private val core = DefaultNetworkMonitorCore(
        registrar = AndroidNetworkCallbackRegistrar(
            context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager,
        ),
        onObservation = { observation ->
            mutableState.value = ConnectionSnapshot(
                network = observation.network,
                connectionType = TransportClassifier.classify(observation.transports),
                hasValidatedInternet = observation.hasValidatedInternet,
            )
        },
    )

    fun start() = core.start()

    fun stop() = core.stop()
}

private class AndroidNetworkCallbackRegistrar(
    private val connectivityManager: ConnectivityManager,
) : NetworkCallbackRegistrar<Network> {
    private val lock = Any()
    private val callbacks = java.util.IdentityHashMap<NetworkEventsCallback<Network>, ConnectivityManager.NetworkCallback>()

    override fun register(callback: NetworkEventsCallback<Network>) {
        synchronized(lock) {
            if (callbacks.containsKey(callback)) return

            val platformCallback = object : ConnectivityManager.NetworkCallback(
                ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO,
            ) {
                override fun onAvailable(network: Network) {
                    callback.onAvailable(network)
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    callback.onCapabilitiesChanged(
                        network = network,
                        transports = networkCapabilities.toHeatNetTransports(),
                        hasValidatedInternet = networkCapabilities.hasCapability(
                            NetworkCapabilities.NET_CAPABILITY_VALIDATED,
                        ),
                    )
                }

                override fun onLost(network: Network) {
                    callback.onLost(network)
                }
            }

            callbacks[callback] = platformCallback
            try {
                connectivityManager.registerDefaultNetworkCallback(platformCallback)
            } catch (failure: Throwable) {
                callbacks.remove(callback)
                throw failure
            }
        }
    }

    override fun unregister(callback: NetworkEventsCallback<Network>) {
        val platformCallback = synchronized(lock) { callbacks.remove(callback) } ?: return
        connectivityManager.unregisterNetworkCallback(platformCallback)
    }
}

private fun NetworkCapabilities.toHeatNetTransports(): Set<NetworkTransport> = buildSet {
    if (hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add(NetworkTransport.WIFI)
    if (hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add(NetworkTransport.CELLULAR)
    if (hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add(NetworkTransport.VPN)
    if (hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) add(NetworkTransport.ETHERNET)

    val hasUnsupportedTransport = listOf(
        NetworkCapabilities.TRANSPORT_BLUETOOTH,
        NetworkCapabilities.TRANSPORT_LOWPAN,
        NetworkCapabilities.TRANSPORT_SATELLITE,
        NetworkCapabilities.TRANSPORT_THREAD,
        NetworkCapabilities.TRANSPORT_USB,
        NetworkCapabilities.TRANSPORT_WIFI_AWARE,
    ).any(::hasTransport)
    if (hasUnsupportedTransport) add(NetworkTransport.OTHER)
}
