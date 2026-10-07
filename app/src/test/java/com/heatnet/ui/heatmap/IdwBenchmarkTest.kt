package com.heatnet.ui.heatmap

import com.heatnet.data.model.Point
import org.junit.Test
import kotlin.system.measureNanoTime

class IdwBenchmarkTest {
    @Test
    fun benchmarkComputeGrid() {
        val room = listOf(Point(0f, 0f), Point(20f, 0f), Point(20f, 20f), Point(0f, 20f))
        val samples = (0..30).map { i ->
            Sample(
                x = (i * 0.6f) % 20f,
                y = (i * 0.7f) % 20f,
                value = 20f + i
            )
        }
        val config = HeatmapConfig(gridResolution = 96)

        // Warmup
        repeat(5) {
            Idw.computeGrid(room, 20f, 20f, samples, config)
        }

        // Benchmark
        val iterations = 20
        val elapsedNanos = measureNanoTime {
            repeat(iterations) {
                Idw.computeGrid(room, 20f, 20f, samples, config)
            }
        }

        val avgMs = (elapsedNanos / iterations) / 1_000_000.0
        println("BENCHMARK_RESULT: $avgMs ms per computeGrid call")
    }
}
