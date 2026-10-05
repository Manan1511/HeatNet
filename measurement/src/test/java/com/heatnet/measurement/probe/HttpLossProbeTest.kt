package com.heatnet.measurement.probe

import com.heatnet.measurement.model.MeasurementConfig
import com.heatnet.measurement.model.IssueCode
import com.heatnet.measurement.model.PacketLossMethod
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpLossProbeTest {
    @Test
    fun countsTwentyStartedHttpProbesAndUsesTheirFailurePercentage() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            repeat(18) { server.enqueue(MockResponse()) }
            repeat(2) { server.enqueue(MockResponse.Builder().code(503).build()) }
            val config = MeasurementConfig(
                latencyUrl = server.url("/probe").toString(),
                httpFallbackProbeCount = 20,
            )

            val result = HttpLossProbe().measureWithClient(OkHttpClient(), config, deadline())

            assertEquals(PacketLossMethod.HTTP_PROBE_FAILURES, result.method)
            assertEquals(20, result.attemptedProbes)
            assertEquals(18, result.successfulProbes)
            assertEquals(10f, result.lossPct!!, 0.001f)
            assertTrue(result.issues.any { it.code == IssueCode.ENDPOINT_FAILURE })
            repeat(20) {
                val request = server.takeRequest()
                assertEquals("GET", request.method)
                assertEquals("0", request.url.queryParameter("bytes"))
            }
        } finally {
            server.close()
        }
    }

    @Test
    fun excludesUnstartedFallbackAttemptsFromTheDenominator() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse())
        server.enqueue(MockResponse.Builder().code(503).build())
        server.start()
        try {
            val base = System.nanoTime()
            val clock = AdvancingClock(base, stepNanos = 2_000_000_000L)
            val result = HttpLossProbe(clock = clock).measureWithClient(
                OkHttpClient(),
                MeasurementConfig(
                    latencyUrl = server.url("/probe").toString(),
                    httpFallbackProbeCount = 20,
                ),
                deadlineNanos = base + 3_000_000_000L,
            )

            assertEquals(PacketLossMethod.HTTP_PROBE_FAILURES, result.method)
            assertEquals(2, result.attemptedProbes)
            assertEquals(1, result.successfulProbes)
            assertEquals(50f, result.lossPct!!, 0.001f)
            assertEquals(2, server.requestCount)
        } finally {
            server.close()
        }
    }

    @Test
    fun expiredDeadlineWithNoStartedRequestIsUnavailable() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val result = HttpLossProbe().measureWithClient(
                OkHttpClient(),
                MeasurementConfig(latencyUrl = server.url("/probe").toString()),
                deadlineNanos = 0L,
            )

            assertEquals(PacketLossMethod.UNAVAILABLE, result.method)
            assertEquals(0, result.attemptedProbes)
            assertEquals(null, result.lossPct)
            assertTrue(result.issues.isNotEmpty())
            assertEquals(0, server.requestCount)
        } finally {
            server.close()
        }
    }

    private fun deadline() = System.nanoTime() + 10_000_000_000L

    private class AdvancingClock(baseNanos: Long, private val stepNanos: Long) : ProbeClock {
        private var current = baseNanos
        override fun nowNanos(): Long = current.also { current += stepNanos }
    }
}
