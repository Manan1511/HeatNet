package com.heatnet.measurement.radio

import com.heatnet.measurement.model.MeasurementIssue

data class RadioSnapshot(
    val signalDbm: Int? = null,
    val linkSpeedMbps: Int? = null,
    val wifiBand: String? = null,
    val wifiChannel: Int? = null,
    val bssid: String? = null,
    val networkType: String? = null,
    val issues: List<MeasurementIssue> = emptyList(),
)
