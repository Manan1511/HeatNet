package com.heatnet.measurement.network

import com.heatnet.measurement.radio.RadioSnapshot

internal data class NetworkObservation<N>(
    val network: N?,
    val transports: Set<NetworkTransport>,
    val hasValidatedInternet: Boolean,
    val hasTransportObservation: Boolean = false,
    val wifiRadioSnapshot: RadioSnapshot? = null,
)

internal interface NetworkEventsCallback<N> {
    fun onAvailable(network: N)
    fun onCapabilitiesChanged(
        network: N,
        transports: Set<NetworkTransport>,
        hasValidatedInternet: Boolean,
        wifiRadioSnapshot: RadioSnapshot? = null,
    )
    fun onLost(network: N)
}

internal interface NetworkCallbackRegistrar<N> {
    fun register(callback: NetworkEventsCallback<N>)
    fun unregister(callback: NetworkEventsCallback<N>)
}

/** Lifecycle and stale-event handling kept independent of Android for deterministic unit tests. */
internal class DefaultNetworkMonitorCore<N>(
    private val registrar: NetworkCallbackRegistrar<N>,
    private val onObservation: (NetworkObservation<N>) -> Unit,
) {
    private val lock = Any()
    private var activeCallback: NetworkEventsCallback<N>? = null
    private var activeNetwork: N? = null
    private var transports: Set<NetworkTransport> = emptySet()
    private var hasValidatedInternet = false
    private var hasTransportObservation = false
    private var wifiRadioSnapshot: RadioSnapshot? = null

    fun start() {
        synchronized(lock) {
            if (activeCallback != null) return

            val callback = object : NetworkEventsCallback<N> {
                override fun onAvailable(network: N) = updateForCurrentCallback(this) {
                    if (activeNetwork != network || !hasTransportObservation) {
                        activeNetwork = network
                        transports = emptySet()
                        hasValidatedInternet = false
                        hasTransportObservation = false
                        wifiRadioSnapshot = null
                        publish()
                    }
                }

                override fun onCapabilitiesChanged(
                    network: N,
                    transports: Set<NetworkTransport>,
                    hasValidatedInternet: Boolean,
                    wifiRadioSnapshot: RadioSnapshot?,
                ) = updateForCurrentCallback(this) {
                    if (network == activeNetwork) {
                        this@DefaultNetworkMonitorCore.transports = transports.toSet()
                        this@DefaultNetworkMonitorCore.hasValidatedInternet = hasValidatedInternet
                        this@DefaultNetworkMonitorCore.hasTransportObservation = true
                        this@DefaultNetworkMonitorCore.wifiRadioSnapshot =
                            wifiRadioSnapshot.takeIf { NetworkTransport.WIFI in transports }
                        publish()
                    }
                }

                override fun onLost(network: N) = updateForCurrentCallback(this) {
                    if (network == activeNetwork) {
                        activeNetwork = null
                        transports = emptySet()
                        hasValidatedInternet = false
                        hasTransportObservation = false
                        wifiRadioSnapshot = null
                        publish()
                    }
                }
            }

            activeCallback = callback
            try {
                registrar.register(callback)
            } catch (failure: Throwable) {
                activeCallback = null
                throw failure
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            val callback = activeCallback ?: return
            activeCallback = null
            activeNetwork = null
            transports = emptySet()
            hasValidatedInternet = false
            hasTransportObservation = false
            wifiRadioSnapshot = null
            try {
                registrar.unregister(callback)
            } finally {
                publish()
            }
        }
    }

    private inline fun updateForCurrentCallback(
        callback: NetworkEventsCallback<N>,
        update: () -> Unit,
    ) {
        synchronized(lock) {
            if (activeCallback === callback) update()
        }
    }

    private fun publish() {
        onObservation(
            NetworkObservation(
                activeNetwork,
                transports,
                hasValidatedInternet,
                hasTransportObservation,
                wifiRadioSnapshot,
            ),
        )
    }
}
