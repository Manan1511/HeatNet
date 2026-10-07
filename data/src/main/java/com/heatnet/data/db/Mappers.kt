package com.heatnet.data.db

import com.heatnet.data.model.OutlineCodec
import com.heatnet.data.model.Reading
import com.heatnet.data.model.Session
import com.heatnet.data.model.SessionSummary
import com.heatnet.measurement.model.ConnectionType
import com.heatnet.measurement.model.MeasurementResult
import com.heatnet.measurement.model.MeasurementStatus
import com.heatnet.measurement.model.PacketLossMethod

internal fun SessionEntity.toDomain() = Session(
    id = id,
    name = name,
    createdAt = createdAt,
    connectionType = ConnectionType.valueOf(connectionType),
    outline = OutlineCodec.decode(outline),
    widthMetres = widthMetres,
    heightMetres = heightMetres,
)

internal fun Session.toEntity() = SessionEntity(
    id = id,
    name = name,
    createdAt = createdAt,
    connectionType = connectionType.name,
    outline = OutlineCodec.encode(outline),
    widthMetres = widthMetres,
    heightMetres = heightMetres,
)

internal fun SessionSummaryRow.toDomain() = SessionSummary(
    id = id,
    name = name,
    createdAt = createdAt,
    connectionType = ConnectionType.valueOf(connectionType),
    readingCount = readingCount,
)

internal fun ReadingEntity.toDomain() = Reading(
    id = id,
    sessionId = sessionId,
    x = x,
    y = y,
    timestamp = timestamp,
    status = MeasurementStatus.valueOf(status),
    downloadMbps = downloadMbps,
    uploadMbps = uploadMbps,
    latencyMs = latencyMs,
    jitterMs = jitterMs,
    packetLossPct = packetLossPct,
    packetLossMethod = PacketLossMethod.valueOf(packetLossMethod),
    signalDbm = signalDbm,
    linkSpeedMbps = linkSpeedMbps,
    wifiBand = wifiBand,
    wifiChannel = wifiChannel,
    bssid = bssid,
    networkType = networkType,
    isDemo = isDemo,
)

internal fun Reading.toEntity() = ReadingEntity(
    id = id,
    sessionId = sessionId,
    x = x,
    y = y,
    timestamp = timestamp,
    status = status.name,
    downloadMbps = downloadMbps,
    uploadMbps = uploadMbps,
    latencyMs = latencyMs,
    jitterMs = jitterMs,
    packetLossPct = packetLossPct,
    packetLossMethod = packetLossMethod.name,
    signalDbm = signalDbm,
    linkSpeedMbps = linkSpeedMbps,
    wifiBand = wifiBand,
    wifiChannel = wifiChannel,
    bssid = bssid,
    networkType = networkType,
    isDemo = isDemo,
)

/** Combines Member 1's transient result with the session and tapped position (Member 2 supplies x, y). */
fun MeasurementResult.toReading(sessionId: Long, x: Float, y: Float) = Reading(
    id = 0,
    sessionId = sessionId,
    x = x,
    y = y,
    timestamp = measuredAtEpochMillis,
    status = status,
    downloadMbps = downloadMbps,
    uploadMbps = uploadMbps,
    latencyMs = latencyMs,
    jitterMs = jitterMs,
    packetLossPct = packetLossPct,
    packetLossMethod = packetLossMethod,
    signalDbm = signalDbm,
    linkSpeedMbps = linkSpeedMbps,
    wifiBand = wifiBand,
    wifiChannel = wifiChannel,
    bssid = bssid,
    networkType = networkType,
    isDemo = isDemo,
)
