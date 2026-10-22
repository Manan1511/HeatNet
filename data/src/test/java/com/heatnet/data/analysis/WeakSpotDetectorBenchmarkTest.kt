package com.heatnet.data.analysis

import com.heatnet.data.model.Point
import com.heatnet.data.reading
import com.heatnet.data.room
import org.junit.Test
import kotlin.system.measureNanoTime

class WeakSpotDetectorBenchmarkTest {

    @Test
    fun benchmarkCentreOfWithOutline() {
        val sessionWithOutline = room().copy(
            outline = (0..19).map { i ->
                Point(i % 5 * 2f, i / 5 * 2f)
            }
        )
        val readings = (0..99).map { i ->
            reading((i % 10) * 1f, (i / 10) * 0.8f)
        }

        // Warmup
        repeat(10_000) {
            WeakSpotDetector.centreOf(sessionWithOutline, readings)
        }

        // Benchmark
        val iterations = 1_000_000
        val elapsedNanos = measureNanoTime {
            repeat(iterations) {
                WeakSpotDetector.centreOf(sessionWithOutline, readings)
            }
        }

        val avgNs = elapsedNanos / iterations.toDouble()
        println("BENCHMARK_CENTRE_OF_WITH_OUTLINE: $avgNs ns per centreOf call ($elapsedNanos ns total for $iterations iterations)")
    }

    @Test
    fun benchmarkCentreOfWithoutOutline() {
        val sessionWithoutOutline = room().copy(outline = emptyList())
        val readings = (0..99).map { i ->
            reading((i % 10) * 1f, (i / 10) * 0.8f)
        }

        // Warmup
        repeat(10_000) {
            WeakSpotDetector.centreOf(sessionWithoutOutline, readings)
        }

        // Benchmark
        val iterations = 1_000_000
        val elapsedNanos = measureNanoTime {
            repeat(iterations) {
                WeakSpotDetector.centreOf(sessionWithoutOutline, readings)
            }
        }

        val avgNs = elapsedNanos / iterations.toDouble()
        println("BENCHMARK_CENTRE_OF_WITHOUT_OUTLINE: $avgNs ns per centreOf call ($elapsedNanos ns total for $iterations iterations)")
    }

    @Test
    fun benchmarkWeakSpotDetectorAnalyseWithOutline() {
        val sessionWithOutline = room().copy(
            outline = (0..19).map { i ->
                Point(i % 5 * 2f, i / 5 * 2f)
            }
        )
        val readings = (0..99).map { i ->
            reading((i % 10) * 1f, (i / 10) * 0.8f)
        }

        // Warmup
        repeat(1_000) {
            WeakSpotDetector.analyse(sessionWithOutline, readings)
        }

        // Benchmark
        val iterations = 100_000
        val elapsedNanos = measureNanoTime {
            repeat(iterations) {
                WeakSpotDetector.analyse(sessionWithOutline, readings)
            }
        }

        val avgMs = (elapsedNanos / iterations.toDouble()) / 1_000_000.0
        println("BENCHMARK_RESULT_WITH_OUTLINE: $avgMs ms per analyse call ($elapsedNanos ns total for $iterations iterations)")
    }

    @Test
    fun benchmarkWeakSpotDetectorAnalyseWithoutOutline() {
        val sessionWithoutOutline = room().copy(outline = emptyList())
        val readings = (0..99).map { i ->
            reading((i % 10) * 1f, (i / 10) * 0.8f)
        }

        // Warmup
        repeat(1_000) {
            WeakSpotDetector.analyse(sessionWithoutOutline, readings)
        }

        // Benchmark
        val iterations = 100_000
        val elapsedNanos = measureNanoTime {
            repeat(iterations) {
                WeakSpotDetector.analyse(sessionWithoutOutline, readings)
            }
        }

        val avgMs = (elapsedNanos / iterations.toDouble()) / 1_000_000.0
        println("BENCHMARK_RESULT_WITHOUT_OUTLINE: $avgMs ms per analyse call ($elapsedNanos ns total for $iterations iterations)")
    }
}
