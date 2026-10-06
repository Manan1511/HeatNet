package com.heatnet.ui.heatmap

import com.heatnet.data.model.Metric
import com.heatnet.data.model.Reading
import com.heatnet.measurement.model.ConnectionType
import com.heatnet.measurement.model.PacketLossMethod

/**
 * The colour range for one metric. Signal, latency and packet loss use fixed ranges so every map
 * (and both sides of a comparison) colours the same value the same way. Speeds depend on the
 * internet plan and phone, so they run from 0 to the best reading shown; for compare mode pass
 * the readings of both sessions so both maps share one range.
 */
data class MetricScale(val metric: Metric, val min: Float, val max: Float) {
    /** 0 = worst colour, 1 = best colour, whatever direction the metric runs in. */
    fun goodness(value: Float): Float {
        val t = ((value - min) / (max - min)).coerceIn(0f, 1f)
        return if (metric.higherIsBetter) t else 1f - t
    }

    fun colorOf(value: Float): Int = ColorRamp.colorAt(goodness(value))

    companion object {
        fun forReadings(metric: Metric, type: ConnectionType, readings: List<Reading>): MetricScale {
            fixedRange(metric, type)?.let { (lo, hi) -> return MetricScale(metric, lo, hi) }
            val best = readings.mapNotNull { metric.valueOf(it) }.maxOrNull() ?: 0f
            return MetricScale(metric, 0f, best.coerceAtLeast(1f))
        }

        fun fixedRange(metric: Metric, type: ConnectionType): Pair<Float, Float>? = when (metric) {
            Metric.SIGNAL -> if (type == ConnectionType.WIFI) -90f to -30f else -120f to -70f
            Metric.LATENCY -> 0f to 200f
            Metric.PACKET_LOSS -> 0f to 10f
            Metric.DOWNLOAD, Metric.UPLOAD, Metric.LINK_SPEED -> null
        }
    }
}

/** Plain-language name for a metric. Packet loss from the HTTP fallback is not real packet loss. */
fun Metric.displayLabel(readings: List<Reading>): String =
    if (this == Metric.PACKET_LOSS && readings.any { it.packetLossMethod == PacketLossMethod.HTTP_PROBE_FAILURES }) {
        "Packet loss / HTTP request failures"
    } else {
        label
    }

/** Red (bad) to yellow to green (good), as ARGB ints so it can be used outside Compose. */
object ColorRamp {
    private val stops = intArrayOf(
        0xFFD73027.toInt(),
        0xFFF46D43.toInt(),
        0xFFFEE08B.toInt(),
        0xFFA6D96A.toInt(),
        0xFF1A9850.toInt(),
    )

    fun colorAt(t: Float): Int {
        val clamped = t.coerceIn(0f, 1f) * (stops.size - 1)
        val i = clamped.toInt().coerceAtMost(stops.size - 2)
        val f = clamped - i
        return lerp(stops[i], stops[i + 1], f)
    }

    private fun lerp(a: Int, b: Int, f: Float): Int {
        fun ch(shift: Int) = (((a shr shift) and 0xFF) + (((b shr shift) and 0xFF) - ((a shr shift) and 0xFF)) * f).toInt()
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }
}
