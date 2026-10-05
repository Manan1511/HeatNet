package com.heatnet.measurement.probe

import com.heatnet.measurement.model.MeasurementConfig
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LatencyProbeTest {
    @Test
    fun sendsTenZeroByteGetsAndComputesMeanAndJitterFromSuccesses() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            repeat(10) { server.enqueue(MockResponse()) }
            val probe = LatencyProbe(clock = StepProbeClock())
            val config = MeasurementConfig(latencyUrl = server.url("/latency?keep=yes").toString())

            val result = probe.measureWithClient(OkHttpClient(), config, deadline())

            assertEquals(10, result.attemptedRequests)
            assertEquals(10, result.successfulRequests)
            assertEquals(1f, result.latencyMs!!, 0.001f)
            assertEquals(0f, result.jitterMs!!, 0.001f)
            repeat(10) {
                val request = server.takeRequest()
                assertEquals("GET", request.method)
                assertEquals("yes", request.url.queryParameter("keep"))
                assertEquals("0", request.url.queryParameter("bytes"))
            }
        } finally {
            server.close()
        }
    }

    @Test
    fun usesOnlySuccessfulLatencySamplesAndReportsHttpFailures() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse())
            server.enqueue(MockResponse.Builder().code(503).build())
            repeat(8) { server.enqueue(MockResponse()) }
            val result = LatencyProbe(clock = StepProbeClock()).measureWithClient(
                OkHttpClient(),
                MeasurementConfig(latencyUrl = server.url("/latency").toString()),
                deadline(),
            )

            assertEquals(10, result.attemptedRequests)
            assertEquals(9, result.successfulRequests)
            assertEquals(1f, result.latencyMs!!, 0.001f)
            assertTrue(result.issues.any { it.code.name == "ENDPOINT_FAILURE" })
        } finally {
            server.close()
        }
    }

    @Test
    fun recordsOneTimeoutAndStopsSendingWhenTheDeadlineHasExpired() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val result = LatencyProbe(clock = StepProbeClock()).measureWithClient(
                OkHttpClient(),
                MeasurementConfig(latencyUrl = server.url("/latency").toString()),
                deadlineNanos = 0L,
            )

            assertEquals(0, result.attemptedRequests)
            assertEquals(1, result.issues.size)
            assertEquals(0, server.requestCount)
        } finally {
            server.close()
        }
    }

    private fun deadline() = System.nanoTime() + 10_000_000_000L

    internal class StepProbeClock : ProbeClock {
        private var current = 0L
        override fun nowNanos(): Long = current.also { current += 1_000_000L }
    }
}
