package com.heatnet.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val connectionType: String,
    val outline: String,
    val widthMetres: Float,
    val heightMetres: Float,
)

/** Deleting a session deletes its readings (CASCADE). */
@Entity(
    tableName = "readings",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class ReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val x: Float,
    val y: Float,
    val timestamp: Long,
    val status: String,
    val downloadMbps: Float?,
    val uploadMbps: Float?,
    val latencyMs: Float?,
    val jitterMs: Float?,
    val packetLossPct: Float?,
    val packetLossMethod: String,
    val signalDbm: Int?,
    val linkSpeedMbps: Int?,
    val wifiBand: String?,
    val wifiChannel: Int?,
    val bssid: String?,
    val networkType: String?,
)

data class SessionSummaryRow(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val connectionType: String,
    val readingCount: Int,
)
