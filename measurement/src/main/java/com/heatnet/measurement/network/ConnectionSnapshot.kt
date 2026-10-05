package com.heatnet.measurement.network

import android.net.Network
import com.heatnet.measurement.model.ConnectionType

data class ConnectionSnapshot(
    val network: Network?,
    val connectionType: ConnectionType?,
    val hasValidatedInternet: Boolean,
    val hasTransportObservation: Boolean = true,
) {
    // Network's Android stub throws from equals/hashCode in local JVM tests. Keep production
    // handle equality while making snapshots safe for the module's mockable Android unit tests.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConnectionSnapshot) return false
        return sameNetworkHandle(network, other.network) &&
            connectionType == other.connectionType &&
            hasValidatedInternet == other.hasValidatedInternet &&
            hasTransportObservation == other.hasTransportObservation
    }

    override fun hashCode(): Int {
        var result = safeNetworkHash(network)
        result = 31 * result + (connectionType?.hashCode() ?: 0)
        result = 31 * result + hasValidatedInternet.hashCode()
        result = 31 * result + hasTransportObservation.hashCode()
        return result
    }
}

internal fun sameNetworkHandle(first: Network?, second: Network?): Boolean {
    if (first === second) return true
    if (first == null || second == null) return false
    return try {
        first == second
    } catch (_: RuntimeException) {
        false
    }
}

private fun safeNetworkHash(network: Network?): Int {
    if (network == null) return 0
    return try {
        network.hashCode()
    } catch (_: RuntimeException) {
        System.identityHashCode(network)
    }
}
