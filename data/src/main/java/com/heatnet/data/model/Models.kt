package com.heatnet.data.model

import com.heatnet.measurement.model.ConnectionType
import com.heatnet.measurement.model.MeasurementStatus
import com.heatnet.measurement.model.PacketLossMethod

/** A point in metres inside the room. x grows east, y grows south (screen convention, y = 0 is north). */
data class Point(val x: Float, val y: Float)

data class Session(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val connectionType: ConnectionType,
    val outline: List<Point>,
    val widthMetres: Float,
    val heightMetres: Float,
)

/**
 * One saved measurement. Same fields as the PRD data model plus the two contract additions from the
 * Member 1 design: [packetLossMethod] and [status]; [wifiBand] may also be "6".
 * A null metric means that test failed or was not available.
 */
data class Reading(
    val id: Long,
    val sessionId: Long,
    val x: Float,
    val y: Float,
    val timestamp: Long,
    val status: MeasurementStatus,
    val downloadMbps: Float?,
    val uploadMbps: Float?,
    val latencyMs: Float?,
    val jitterMs: Float?,
    val packetLossPct: Float?,
    val packetLossMethod: PacketLossMethod,
    val signalDbm: Int?,
    val linkSpeedMbps: Int?,
    val wifiBand: String?,
    val wifiChannel: Int?,
    val bssid: String?,
    val networkType: String?,
    val isDemo: Boolean = false,
)

data class SessionWithReadings(
    val session: Session,
    val readings: List<Reading>,
)

/** Row shown on the session list screen. */
data class SessionSummary(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val connectionType: ConnectionType,
    val readingCount: Int,
)
