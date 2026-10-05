package com.heatnet.measurement.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MetricCalculationsTest {
    @Test
    fun calculatesPayloadMbps() {
        val actual = calculatePayloadMbps(
            payloadBytes = 1_000_000L,
            elapsedNanos = 1_000_000_000L,
        )

        assertEquals(8f, actual!!, 0.0001f)
    }

    @Test
    fun returnsNullForNonPositiveDuration() {
        assertNull(calculatePayloadMbps(payloadBytes = 100L, elapsedNanos = 0L))
        assertNull(calculatePayloadMbps(payloadBytes = 100L, elapsedNanos = -1L))
    }

    @Test
    fun computesMeanAndSampleJitter() {
        val actual = calculateLatencySummary(listOf(10f, 12f, 14f))

        assertEquals(12f, actual!!.meanMs, 0.0001f)
        assertEquals(2f, actual.jitterMs!!, 0.0001f)
    }

    @Test
    fun returnsNullWithoutSuccessfulLatencySamples() {
        assertNull(calculateLatencySummary(emptyList()))

        val singleSample = calculateLatencySummary(listOf(10f))
        assertEquals(10f, singleSample!!.meanMs, 0.0001f)
        assertNull(singleSample.jitterMs)
    }

    @Test
    fun usesAttemptedProbeDenominator() {
        assertEquals(20f, calculateLossPercentage(failedProbes = 1, attemptedProbes = 5)!!, 0.0001f)
        assertNull(calculateLossPercentage(failedProbes = 0, attemptedProbes = 0))
    }

    @Test
    fun maps24FiveAndSixGhzChannels() {
        assertEquals(WifiFrequencyInfo(band = "2.4", channel = 1), mapWifiFrequency(2412))
        assertEquals(WifiFrequencyInfo(band = "5", channel = 36), mapWifiFrequency(5180))
        assertEquals(WifiFrequencyInfo(band = "6", channel = 1), mapWifiFrequency(5955))
        assertNull(mapWifiFrequency(0))
        assertNull(mapWifiFrequency(5000))
    }
}
