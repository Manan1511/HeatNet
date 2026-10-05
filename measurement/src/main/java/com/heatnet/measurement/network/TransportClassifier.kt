package com.heatnet.measurement.network

import com.heatnet.measurement.model.ConnectionType

enum class NetworkTransport {
    WIFI,
    CELLULAR,
    VPN,
    ETHERNET,
    OTHER,
}

object TransportClassifier {
    fun classify(transports: Set<NetworkTransport>): ConnectionType? = when (transports) {
        setOf(NetworkTransport.WIFI) -> ConnectionType.WIFI
        setOf(NetworkTransport.CELLULAR) -> ConnectionType.MOBILE
        else -> null
    }
}
