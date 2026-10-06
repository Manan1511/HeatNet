package com.heatnet.ui.screens

import com.heatnet.measurement.model.ConnectionType
import com.heatnet.measurement.model.MeasurementStage
import com.heatnet.measurement.model.PacketLossMethod

/** Plain words for the UI (PRD section 3: no technical knowledge assumed). */
val ConnectionType.label: String
    get() = when (this) {
        ConnectionType.WIFI -> "Wi-Fi"
        ConnectionType.MOBILE -> "Mobile data"
    }

val MeasurementStage.label: String
    get() = when (this) {
        MeasurementStage.VERIFYING_NETWORK -> "Checking the connection"
        MeasurementStage.CAPTURING_RADIO -> "Reading signal strength"
        MeasurementStage.DOWNLOAD -> "Testing download speed"
        MeasurementStage.UPLOAD -> "Testing upload speed"
        MeasurementStage.LATENCY -> "Measuring latency"
        MeasurementStage.PACKET_LOSS -> "Checking packet loss"
        MeasurementStage.COMPLETE -> "Done"
    }

val PacketLossMethod.label: String
    get() = when (this) {
        PacketLossMethod.ICMP -> "Ping (ICMP)"
        PacketLossMethod.HTTP_PROBE_FAILURES -> "HTTP request failures (ping was blocked)"
        PacketLossMethod.UNAVAILABLE -> "Not available"
    }
