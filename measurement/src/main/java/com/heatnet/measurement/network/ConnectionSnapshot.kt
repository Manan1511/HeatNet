package com.heatnet.measurement.network

import android.net.Network
import com.heatnet.measurement.model.ConnectionType

data class ConnectionSnapshot(
    val network: Network?,
    val connectionType: ConnectionType?,
    val hasValidatedInternet: Boolean,
)
