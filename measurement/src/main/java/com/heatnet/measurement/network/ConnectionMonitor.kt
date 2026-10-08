package com.heatnet.measurement.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.heatnet.measurement.model.ConnectionType
import com.heatnet.measurement.radio.WifiRadioReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConnectionMonitor(context: Context) {
    private val mutableState = MutableStateFlow(ConnectionSnapshot(null, null, false, hasTransportObservation = false))
    val state: StateFlow<ConnectionSnapshot> = mutableState.asStateFlow()
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiRadioReader = WifiRadioReader()

    private val core = DefaultNetworkMonitorCore(
        registrar = AndroidNetworkCallbackRegistrar(connectivityManager),
        onObservation = { observation ->
            mutableState.value = ConnectionSnapshot(
                network = observation.network,
                connectionType = TransportClassifier.classify(observation.transports),
                hasValidatedInternet = observation.hasValidatedInternet,
                hasTransportObservation = observation.hasTransportObservation,
            )
        },
    )

    fun start() {
        core.start()
        refreshActiveDefaultNetwork()
    }

    fun stop() {
        core.stop()
        mutableState.value = ConnectionSnapshot(null, null, false, hasTransportObservation = true)
    }

    /** Reads location-aware Wi-Fi details from a fresh callback scoped to the captured network. */
    suspend fun readWifiRadio(network: Network) = wifiRadioReader.readForNetwork(connectivityManager, network)

    private fun refreshActiveDefaultNetwork() {
        val network = runCatching { connectivityManager.activeNetwork }.getOrNull()
        if (network == null) {
            mutableState.value = ConnectionSnapshot(null, null, false, hasTransportObservation = true)
            return
        }
        val capabilities = runCatching { connectivityManager.getNetworkCapabilities(network) }.getOrNull()
        if (capabilities == null) {
            mutableState.value = ConnectionSnapshot(network, null, false, hasTransportObservation = false)
            return
        }
        mutableState.value = ConnectionSnapshot(
            network = network,
            connectionType = TransportClassifier.classify(capabilities.toHeatNetTransports()),
            hasValidatedInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            hasTransportObservation = true,
        )
    }
}

private class AndroidNetworkCallbackRegistrar(
    private val connectivityManager: ConnectivityManager,
) : NetworkCallbackRegistrar<Network> {
    private val lock = Any()
    private val callbacks = java.util.IdentityHashMap<NetworkEventsCallback<Network>, ConnectivityManager.NetworkCallback>()

    override fun register(callback: NetworkEventsCallback<Network>) {
        synchronized(lock) {
            if (callbacks.containsKey(callback)) return

            val platformCallback = object : ConnectivityManager.NetworkCallback() {
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
