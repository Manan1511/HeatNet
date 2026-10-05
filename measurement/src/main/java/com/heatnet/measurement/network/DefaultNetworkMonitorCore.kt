package com.heatnet.measurement.network

internal data class NetworkObservation<N>(
    val network: N?,
    val transports: Set<NetworkTransport>,
    val hasValidatedInternet: Boolean,
)

internal interface NetworkEventsCallback<N> {
    fun onAvailable(network: N)
    fun onCapabilitiesChanged(
        network: N,
        transports: Set<NetworkTransport>,
        hasValidatedInternet: Boolean,
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

    fun start() {
        synchronized(lock) {
            if (activeCallback != null) return

            val callback = object : NetworkEventsCallback<N> {
                override fun onAvailable(network: N) = updateForCurrentCallback(this) {
                    activeNetwork = network
                    transports = emptySet()
                    hasValidatedInternet = false
                    publish()
                }

                override fun onCapabilitiesChanged(
                    network: N,
                    transports: Set<NetworkTransport>,
                    hasValidatedInternet: Boolean,
                ) = updateForCurrentCallback(this) {
                    if (network == activeNetwork) {
                        this@DefaultNetworkMonitorCore.transports = transports.toSet()
                        this@DefaultNetworkMonitorCore.hasValidatedInternet = hasValidatedInternet
                        publish()
                    }
                }

                override fun onLost(network: N) = updateForCurrentCallback(this) {
                    if (network == activeNetwork) {
                        activeNetwork = null
                        transports = emptySet()
                        hasValidatedInternet = false
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
        onObservation(NetworkObservation(activeNetwork, transports, hasValidatedInternet))
    }
}
