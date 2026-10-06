package com.heatnet.data.analysis

import com.heatnet.data.model.Point
import com.heatnet.data.model.Reading
import com.heatnet.data.model.Session
import com.heatnet.measurement.model.ConnectionType

/** Starting thresholds from PRD section 10. Tune after real-room testing. */
data class WeakSpotThresholds(
    val wifiSignalDbm: Int = -75,
    /** PRD leaves this open until tested on the team's phones; -100 dBm is a common "poor" LTE level. */
    val mobileSignalDbm: Int = -100,
    val latencyMs: Float = 100f,
    val packetLossPct: Float = 2f,
    /** A download is weak if it is below this fraction of the best download in the session. */
    val downloadFractionOfBest: Float = 0.25f,
    /** A quadrant is flagged for a problem when at least this share of its readings show it. */
    val quadrantShare: Float = 0.5f,
    val minReadings: Int = 3,
)

enum class Quadrant(val label: String) {
    NORTH_WEST("north-west"),
    NORTH_EAST("north-east"),
    SOUTH_WEST("south-west"),
    SOUTH_EAST("south-east"),
}

enum class Problem(val phrase: String) {
    WEAK_SIGNAL("weak signal"),
    HIGH_LATENCY("high latency"),
    PACKET_LOSS("packet loss"),
    SLOW_DOWNLOAD("slow download"),
}

data class WeakSpot(
    val quadrant: Quadrant,
    val problems: List<Problem>,
    /** One plain sentence, for example "Weak signal in the north-east corner". */
    val sentence: String,
)

data class WeakSpotReport(
    val enoughReadings: Boolean,
    val spots: List<WeakSpot>,
    val summary: String,
)

object WeakSpotDetector {
    /**
     * Splits the room's bounding box into four quadrants (y = 0 is north) and flags each quadrant
     * for every problem shown by at least [WeakSpotThresholds.quadrantShare] of its readings.
     * Readings where a metric is null (failed test) are ignored for that metric only.
     */
    fun analyse(
        session: Session,
        readings: List<Reading>,
        thresholds: WeakSpotThresholds = WeakSpotThresholds(),
    ): WeakSpotReport {
        if (readings.size < thresholds.minReadings) {
            return WeakSpotReport(
                enoughReadings = false,
                spots = emptyList(),
                summary = "Take at least ${thresholds.minReadings} readings to check for weak spots.",
            )
        }

        val centre = centreOf(session, readings)
        val bestDownload = readings.mapNotNull { it.downloadMbps }.maxOrNull()
        val signalLimit = when (session.connectionType) {
            ConnectionType.WIFI -> thresholds.wifiSignalDbm
            ConnectionType.MOBILE -> thresholds.mobileSignalDbm
        }

        val spots = Quadrant.entries.mapNotNull { quadrant ->
            val inQuadrant = readings.filter { quadrantOf(it.x, it.y, centre) == quadrant }
            if (inQuadrant.isEmpty()) return@mapNotNull null

            val problems = buildList {
                if (flagged(inQuadrant, { it.signalDbm?.toFloat() }, thresholds.quadrantShare) { it < signalLimit }) {
                    add(Problem.WEAK_SIGNAL)
                }
                if (flagged(inQuadrant, { it.latencyMs }, thresholds.quadrantShare) { it > thresholds.latencyMs }) {
                    add(Problem.HIGH_LATENCY)
                }
                if (flagged(inQuadrant, { it.packetLossPct }, thresholds.quadrantShare) { it > thresholds.packetLossPct }) {
                    add(Problem.PACKET_LOSS)
                }
                if (bestDownload != null && bestDownload > 0f) {
                    val limit = bestDownload * thresholds.downloadFractionOfBest
                    if (flagged(inQuadrant, { it.downloadMbps }, thresholds.quadrantShare) { it < limit }) {
                        add(Problem.SLOW_DOWNLOAD)
                    }
                }
            }
            if (problems.isEmpty()) null else WeakSpot(quadrant, problems, sentenceFor(quadrant, problems))
        }

        val summary = if (spots.isEmpty()) {
            "No weak spots found."
        } else {
            spots.joinToString(". ", postfix = ".") { it.sentence }
        }
        return WeakSpotReport(enoughReadings = true, spots = spots, summary = summary)
    }

    internal fun quadrantOf(x: Float, y: Float, centre: Point): Quadrant {
        val west = x < centre.x
        val north = y < centre.y
        return when {
            north && west -> Quadrant.NORTH_WEST
            north -> Quadrant.NORTH_EAST
            west -> Quadrant.SOUTH_WEST
            else -> Quadrant.SOUTH_EAST
        }
    }

    internal fun sentenceFor(quadrant: Quadrant, problems: List<Problem>): String {
        val what = when (problems.size) {
            1 -> problems[0].phrase
            else -> problems.dropLast(1).joinToString(", ") { it.phrase } + " and " + problems.last().phrase
        }
        return "${what.replaceFirstChar { it.uppercase() }} in the ${quadrant.label} corner"
    }

    private fun centreOf(session: Session, readings: List<Reading>): Point {
        val xs = session.outline.map { it.x }.ifEmpty { readings.map { it.x } }
        val ys = session.outline.map { it.y }.ifEmpty { readings.map { it.y } }
        return Point((xs.min() + xs.max()) / 2f, (ys.min() + ys.max()) / 2f)
    }

    /** True when the share of readings (that have this metric) failing [isWeak] reaches [share]. */
    private fun flagged(
        readings: List<Reading>,
        value: (Reading) -> Float?,
        share: Float,
        isWeak: (Float) -> Boolean,
    ): Boolean {
        val values = readings.mapNotNull(value)
        if (values.isEmpty()) return false
        return values.count(isWeak).toFloat() / values.size >= share
    }
}
