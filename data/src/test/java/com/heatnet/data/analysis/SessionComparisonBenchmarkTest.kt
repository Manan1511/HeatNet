package com.heatnet.data.analysis

import com.heatnet.data.reading
import com.heatnet.data.room
import com.heatnet.data.sessionOf
import com.heatnet.measurement.model.PacketLossMethod
import org.junit.Test
import kotlin.system.measureNanoTime

class SessionComparisonBenchmarkTest {
    @Test
    fun benchmarkSessionComparison() {
        val count = 5000
        val beforeReadings = Array(count) { i ->
            reading(
                x = (i % 10).toFloat(),
                y = (i / 10 % 10).toFloat(),
                download = 50f + i % 10,
                bssid = "ap_${i % 50}"
            )
        }
        val afterReadings = Array(count) { i ->
            reading(
                x = (i % 10).toFloat(),
                y = (i / 10 % 10).toFloat(),
                sessionId = 2,
                download = 60f + i % 10,
                bssid = "ap_${i % 50 + 25}"
            )
        }

        val before = sessionOf(room(id = 1), *beforeReadings)
        val after = sessionOf(room(id = 2), *afterReadings)

        // Warmup
        repeat(20) {
            SessionComparison.compare(before, after)
        }

        val iterations = 100
        val elapsedNanos = measureNanoTime {
            repeat(iterations) {
                SessionComparison.compare(before, after)
            }
        }

        val avgMs = (elapsedNanos / iterations) / 1_000_000.0
        println("BENCHMARK_RESULT: $avgMs ms per SessionComparison.compare call")
    }
}
