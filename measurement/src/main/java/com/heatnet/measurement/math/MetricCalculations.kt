package com.heatnet.measurement.math

import kotlin.math.sqrt

data class LatencySummary(
    val meanMs: Float,
    val jitterMs: Float?,
)

fun calculatePayloadMbps(payloadBytes: Long, elapsedNanos: Long): Float? {
    if (payloadBytes < 0L || elapsedNanos <= 0L) return null

    val elapsedSeconds = elapsedNanos.toDouble() / NANOS_PER_SECOND
    val mbps = payloadBytes.toDouble() * BITS_PER_BYTE / elapsedSeconds / BITS_PER_MEGABIT
    return mbps.takeIf(Double::isFinite)?.toFloat()
}

fun calculateLatencySummary(successfulSamplesMs: List<Float>): LatencySummary? {
    val samples = successfulSamplesMs.filter { it.isFinite() && it >= 0f }
    if (samples.isEmpty()) return null

    val mean = samples.average()
    val jitter = if (samples.size < 2) {
        null
    } else {
        val squaredDeviationSum = samples.sumOf { sample ->
            val delta = sample.toDouble() - mean
            delta * delta
        }
        sqrt(squaredDeviationSum / (samples.size - 1)).toFloat()
    }

    return LatencySummary(meanMs = mean.toFloat(), jitterMs = jitter)
}

fun calculateLossPercentage(failedProbes: Int, attemptedProbes: Int): Float? {
    if (attemptedProbes <= 0 || failedProbes !in 0..attemptedProbes) return null
    return failedProbes.toFloat() * 100f / attemptedProbes.toFloat()
}

private const val NANOS_PER_SECOND = 1_000_000_000.0
private const val BITS_PER_BYTE = 8.0
private const val BITS_PER_MEGABIT = 1_000_000.0
