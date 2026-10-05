package com.heatnet.measurement.probe

import com.heatnet.measurement.model.MeasurementIssue

data class LatencyMeasurement(
    val latencyMs: Float?,
    val jitterMs: Float?,
    val attemptedRequests: Int,
    val successfulRequests: Int,
    val issues: List<MeasurementIssue> = emptyList(),
)

data class ThroughputMeasurement(
    val megabitsPerSecond: Float?,
    val payloadBytes: Long,
    val budgetBytes: Long,
    val isPartial: Boolean,
    val issues: List<MeasurementIssue> = emptyList(),
)

enum class TransferDirection {
    DOWNLOAD,
    UPLOAD,
}

internal fun interface ProbeClock {
    fun nowNanos(): Long
}

internal object SystemProbeClock : ProbeClock {
    override fun nowNanos(): Long = System.nanoTime()
}
