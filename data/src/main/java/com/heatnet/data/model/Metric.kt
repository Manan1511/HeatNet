package com.heatnet.data.model

import com.heatnet.measurement.model.ConnectionType

/** The metrics a heatmap can show (PRD 6.4). Shared by the metric switcher and compare mode. */
enum class Metric(val label: String, val unit: String, val higherIsBetter: Boolean, val wifiOnly: Boolean = false) {
    DOWNLOAD("Download speed", "Mbps", true),
    UPLOAD("Upload speed", "Mbps", true),
    LATENCY("Latency", "ms", false),
    PACKET_LOSS("Packet loss", "%", false),
    SIGNAL("Signal strength", "dBm", true),
    LINK_SPEED("Link speed", "Mbps", true, wifiOnly = true),
    ;

    fun valueOf(reading: Reading): Float? = when (this) {
        DOWNLOAD -> reading.downloadMbps
        UPLOAD -> reading.uploadMbps
        LATENCY -> reading.latencyMs
        PACKET_LOSS -> reading.packetLossPct
        SIGNAL -> reading.signalDbm?.toFloat()
        LINK_SPEED -> reading.linkSpeedMbps?.toFloat()
    }

    companion object {
        fun availableFor(type: ConnectionType): List<Metric> =
            entries.filter { type == ConnectionType.WIFI || !it.wifiOnly }
    }
}
