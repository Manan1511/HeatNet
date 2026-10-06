package com.heatnet.data

import com.heatnet.data.model.Point
import com.heatnet.data.model.Reading
import com.heatnet.data.model.Session
import com.heatnet.data.model.SessionWithReadings
import com.heatnet.measurement.model.ConnectionType
import com.heatnet.measurement.model.MeasurementStatus
import com.heatnet.measurement.model.PacketLossMethod

/** A 10 m by 8 m rectangular room. */
fun room(id: Long = 1, type: ConnectionType = ConnectionType.WIFI, width: Float = 10f, height: Float = 8f) = Session(
    id = id,
    name = "Room $id",
    createdAt = 0,
    connectionType = type,
    outline = listOf(Point(0f, 0f), Point(width, 0f), Point(width, height), Point(0f, height)),
    widthMetres = width,
    heightMetres = height,
)

fun reading(
    x: Float,
    y: Float,
    sessionId: Long = 1,
    download: Float? = 100f,
    upload: Float? = 50f,
    latency: Float? = 20f,
    loss: Float? = 0f,
    signal: Int? = -55,
    linkSpeed: Int? = 400,
    bssid: String? = null,
) = Reading(
    id = 0,
    sessionId = sessionId,
    x = x,
    y = y,
    timestamp = 0,
    status = MeasurementStatus.COMPLETE,
    downloadMbps = download,
    uploadMbps = upload,
    latencyMs = latency,
    jitterMs = 2f,
    packetLossPct = loss,
    packetLossMethod = PacketLossMethod.ICMP,
    signalDbm = signal,
    linkSpeedMbps = linkSpeed,
    wifiBand = "5",
    wifiChannel = 36,
    bssid = bssid,
    networkType = null,
)

fun sessionOf(session: Session, vararg readings: Reading) = SessionWithReadings(session, readings.toList())
