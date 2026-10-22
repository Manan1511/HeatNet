package com.heatnet.data.analysis

import com.heatnet.data.reading
import com.heatnet.data.room
import org.junit.Test
import kotlin.system.measureNanoTime

class WeakSpotDetectorBenchmarkTest {
    @Test
    fun benchmarkAnalyse() {
        val room = room()
        val readings = (0 until 1000).map { i ->
            reading(
                x = (i % 10).toFloat(),
                y = (i / 10 % 8).toFloat(),
                download = if (i % 7 == 0) 10f else 100f,
                latency = if (i % 5 == 0) 150f else 30f,
                loss = if (i % 11 == 0) 5f else 0f,
                signal = if (i % 3 == 0) -85 else -50,
            )
        }

        // Warmup
        repeat(50) {
            WeakSpotDetector.analyse(room, readings)
        }

        // Benchmark
        val iterations = 500
        val elapsedNanos = measureNanoTime {
            repeat(iterations) {
                WeakSpotDetector.analyse(room, readings)
            }
        }

        val avgMs = (elapsedNanos / iterations) / 1_000_000.0
        println("BENCHMARK_RESULT: $avgMs ms per WeakSpotDetector.analyse call")
    }
}
