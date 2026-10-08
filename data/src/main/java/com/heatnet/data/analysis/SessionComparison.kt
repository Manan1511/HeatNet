package com.heatnet.data.analysis

import com.heatnet.data.model.Metric
import com.heatnet.data.model.SessionWithReadings
import com.heatnet.measurement.model.PacketLossMethod
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

data class MetricStats(val mean: Float, val min: Float, val max: Float, val count: Int)

enum class Verdict { IMPROVED, WORSE, SIMILAR, NOT_ENOUGH_DATA }

/**
 * Before/after numbers for one metric. [scaleMin] and [scaleMax] cover both sessions, so the two
 * side-by-side heatmaps must be coloured with this one range or the comparison is misleading.
 */
data class MetricComparison(
    val metric: Metric,
    val before: MetricStats?,
    val after: MetricStats?,
    /** after.mean - before.mean; null when either side has no data. */
    val delta: Float?,
    val verdict: Verdict,
    val scaleMin: Float?,
    val scaleMax: Float?,
)

data class Comparison(
    val before: SessionWithReadings,
    val after: SessionWithReadings,
    val metrics: List<MetricComparison>,
    val warnings: List<String>,
)

enum class CompareError(val message: String) {
    SAME_SESSION("Pick two different sessions."),
    DIFFERENT_CONNECTION_TYPE("Only sessions on the same connection type (Wi-Fi or mobile data) can be compared."),
    NO_READINGS("Both sessions need at least one reading."),
}

sealed interface ComparisonResult {
    data class Success(val comparison: Comparison) : ComparisonResult
    data class Failure(val error: CompareError) : ComparisonResult
}

object SessionComparison {
    /** Differences smaller than this fraction of the larger mean count as "similar" (measurement noise). */
    const val SIMILAR_FRACTION = 0.05f

    /** If the room sizes differ by more than this fraction, warn that the maps may not be the same room. */
    const val ROOM_SIZE_TOLERANCE = 0.2f
    const val SPATIAL_GRID_CELLS = 4

    fun compare(before: SessionWithReadings, after: SessionWithReadings): ComparisonResult {
        if (before.session.id == after.session.id) return ComparisonResult.Failure(CompareError.SAME_SESSION)
        if (before.session.connectionType != after.session.connectionType) {
            return ComparisonResult.Failure(CompareError.DIFFERENT_CONNECTION_TYPE)
        }
        if (before.readings.isEmpty() || after.readings.isEmpty()) {
            return ComparisonResult.Failure(CompareError.NO_READINGS)
        }

        val warnings = warningsFor(before, after)
        val metrics = Metric.availableFor(before.session.connectionType).map { metric ->
            compareMetric(metric, before, after)
        }
        return ComparisonResult.Success(Comparison(before, after, metrics, warnings))
    }

    internal fun statsOf(values: List<Float>): MetricStats? {
        if (values.isEmpty()) return null
        return MetricStats(values.average().toFloat(), values.min(), values.max(), values.size)
    }

    private fun compareMetric(metric: Metric, before: SessionWithReadings, after: SessionWithReadings): MetricComparison {
        val beforeByCell = cellMeans(before, metric)
        val afterByCell = cellMeans(after, metric)
        val sharedCells = beforeByCell.keys.intersect(afterByCell.keys)
        val beforeStats = statsOf(sharedCells.mapNotNull(beforeByCell::get))
        val afterStats = statsOf(sharedCells.mapNotNull(afterByCell::get))
        val beforeValues = before.readings.mapNotNull { analysisValue(metric, it) }
        val afterValues = after.readings.mapNotNull { analysisValue(metric, it) }
        val beforeRange = statsOf(beforeValues)
        val afterRange = statsOf(afterValues)
        val all = listOfNotNull(beforeRange, afterRange)

        val delta = if (beforeStats != null && afterStats != null) afterStats.mean - beforeStats.mean else null
        return MetricComparison(
            metric = metric,
            before = beforeStats,
            after = afterStats,
            delta = delta,
            verdict = verdictFor(metric, beforeStats, afterStats, delta),
            scaleMin = all.minOfOrNull { it.min },
            scaleMax = all.maxOfOrNull { it.max },
        )
    }

    internal fun verdictFor(metric: Metric, before: MetricStats?, after: MetricStats?, delta: Float?): Verdict {
        if (before == null || after == null || delta == null) return Verdict.NOT_ENOUGH_DATA
        val reference = max(abs(before.mean), abs(after.mean))
        if (abs(delta) <= reference * SIMILAR_FRACTION) return Verdict.SIMILAR
        val better = if (metric.higherIsBetter) delta > 0 else delta < 0
        return if (better) Verdict.IMPROVED else Verdict.WORSE
    }

    private fun warningsFor(before: SessionWithReadings, after: SessionWithReadings): List<String> {
        val warnings = mutableListOf<String>()
        val a = before.session
        val b = after.session
        if (differsByMoreThan(a.widthMetres, b.widthMetres) || differsByMoreThan(a.heightMetres, b.heightMetres)) {
            warnings += "The room outlines differ in size, so these may not be the same room."
        }
        val beforeBssids = before.readings.mapNotNull { it.bssid }.toSet()
        val afterBssids = after.readings.mapNotNull { it.bssid }.toSet()
        if (beforeBssids.isNotEmpty() && afterBssids.isNotEmpty() && beforeBssids.intersect(afterBssids).isEmpty()) {
            warnings += "The sessions were measured on different access points."
        }
        val beforeCells = before.readings.mapNotNull { cellFor(before, it) }.toSet()
        val afterCells = after.readings.mapNotNull { cellFor(after, it) }.toSet()
        if (beforeCells.intersect(afterCells).isEmpty()) {
            warnings += "The sessions have no shared sampled room areas; add readings in matching areas to compare them."
        }
        if ((before.readings + after.readings).any { it.isDemo }) {
            warnings += "At least one session includes simulated demo readings; results are not field measurements."
        }
        if ((before.readings + after.readings).any {
                it.packetLossPct != null && it.packetLossMethod == PacketLossMethod.HTTP_PROBE_FAILURES
            }
        ) {
            warnings += "HTTP request-failure rates are application-level probes and are not compared as ICMP packet loss."
        }
        return warnings
    }

    private fun cellMeans(data: SessionWithReadings, metric: Metric): Map<RoomCell, Float> =
        data.readings.mapNotNull { reading ->
            val cell = cellFor(data, reading) ?: return@mapNotNull null
            val value = analysisValue(metric, reading) ?: return@mapNotNull null
            cell to value
        }.groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> values.average().toFloat() }

    private fun analysisValue(metric: Metric, reading: com.heatnet.data.model.Reading): Float? {
        if (metric == Metric.PACKET_LOSS && reading.packetLossMethod != PacketLossMethod.ICMP) return null
        return metric.valueOf(reading)?.takeIf { it.isFinite() }
    }

    private fun cellFor(data: SessionWithReadings, reading: com.heatnet.data.model.Reading): RoomCell? {
        val width = data.session.widthMetres
        val height = data.session.heightMetres
        if (!width.isFinite() || !height.isFinite() || width <= 0f || height <= 0f ||
            !reading.x.isFinite() || !reading.y.isFinite()
        ) return null
        val x = floor((reading.x / width).coerceIn(0f, 1f) * SPATIAL_GRID_CELLS)
            .toInt().coerceIn(0, SPATIAL_GRID_CELLS - 1)
        val y = floor((reading.y / height).coerceIn(0f, 1f) * SPATIAL_GRID_CELLS)
            .toInt().coerceIn(0, SPATIAL_GRID_CELLS - 1)
        return RoomCell(x, y)
    }

    private data class RoomCell(val x: Int, val y: Int)

    private fun differsByMoreThan(a: Float, b: Float): Boolean {
        val larger = max(a, b)
        return larger > 0f && abs(a - b) / larger > ROOM_SIZE_TOLERANCE
    }
}
