package com.heatnet.measurement.probe

import com.heatnet.measurement.model.MeasurementConfig
import kotlinx.coroutines.runBlocking
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ThroughputProbeTest {
    @Test
    fun respectsSharedDirectionBudget() = runBlocking {
        val server = MockWebServer()
        val dispatcher = ParallelBodyDispatcher(expectedConcurrentRequests = 2, responseBytes = 100)
        server.dispatcher = dispatcher
        server.start()
        try {
            val config = MeasurementConfig(
                downloadUrl = server.url("/down").toString(),
                uploadUrl = server.url("/up").toString(),
                maxBytesPerDirection = 100,
                parallelStreams = 2,
            )
            val probe = ThroughputProbe(config, clock = LatencyProbeTest.StepProbeClock())

            val download = probe.measureWithClient(OkHttpClient(), TransferDirection.DOWNLOAD, 100, deadline())
            val upload = probe.measureWithClient(OkHttpClient(), TransferDirection.UPLOAD, 100, deadline())

            assertEquals(100L, download.payloadBytes)
            assertEquals(100L, upload.payloadBytes)
            assertEquals(2, download.successfulStreams)
            assertEquals(2, upload.successfulStreams)
            assertFalse(download.isPartial)
            assertFalse(upload.isPartial)
            assertTrue(download.payloadBytes <= config.maxBytesPerDirection)
            assertTrue(upload.payloadBytes <= config.maxBytesPerDirection)
            assertTrue(download.payloadBytes + upload.payloadBytes <= 8L * 1024L * 1024L)

            val downloadRequests = listOf(server.takeRequest(), server.takeRequest())
            assertTrue(downloadRequests.all { it.url.queryParameter("bytes") != null })
            val uploadRequests = listOf(server.takeRequest(), server.takeRequest())
            assertEquals(100L, uploadRequests.sumOf { it.body?.size?.toLong() ?: 0L })
            assertEquals(2, dispatcher.maximumConcurrentRequests("GET"))
            assertEquals(2, dispatcher.maximumConcurrentRequests("POST"))
        } finally {
            server.close()
        }
    }

    @Test
    fun budgetTruncationIsPartial() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse.Builder().body("short body one").build())
        server.enqueue(MockResponse.Builder().body("short body two").build())
        server.start()
        try {
            val config = MeasurementConfig(downloadUrl = server.url("/down").toString(), parallelStreams = 2)
            val result = ThroughputProbe(config, clock = LatencyProbeTest.StepProbeClock()).measureWithClient(
                OkHttpClient(),
                TransferDirection.DOWNLOAD,
                1_024,
                deadline(),
            )

            assertEquals(28L, result.payloadBytes)
            assertEquals(2, result.successfulStreams)
            assertTrue(result.isPartial)
            assertTrue(result.megabitsPerSecond != null)
        } finally {
            server.close()
        }
    }

    @Test
    fun capsCallerBudgetToConfiguredPerDirectionMaximum() = runBlocking {
        val server = MockWebServer()
        server.dispatcher = ParallelBodyDispatcher(expectedConcurrentRequests = 2, responseBytes = 4 * 1024 * 1024)
        server.start()
        try {
            val maximum = 64L
            val config = MeasurementConfig(downloadUrl = server.url("/down").toString(), maxBytesPerDirection = maximum)
            val result = ThroughputProbe(config, clock = LatencyProbeTest.StepProbeClock()).measureWithClient(
                OkHttpClient(),
                TransferDirection.DOWNLOAD,
                Long.MAX_VALUE,
                deadline(),
            )

            assertEquals(maximum, result.payloadBytes)
        } finally {
            server.close()
        }
    }

    @Test
    fun httpFailureReturnsAnIssueAndKeepsPayloadUnavailable() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse.Builder().code(503).build())
        server.enqueue(MockResponse.Builder().code(503).build())
        server.start()
        try {
            val config = MeasurementConfig(downloadUrl = server.url("/down").toString(), parallelStreams = 2)
            val result = ThroughputProbe(config, clock = LatencyProbeTest.StepProbeClock()).measureWithClient(
                OkHttpClient(),
                TransferDirection.DOWNLOAD,
                100,
                deadline(),
            )

            assertEquals(0L, result.payloadBytes)
            assertEquals(0, result.successfulStreams)
            assertNull("an HTTP failure is not a zero-speed measurement", result.megabitsPerSecond)
            assertTrue(result.isPartial)
            assertTrue(result.issues.any { it.code.name == "ENDPOINT_FAILURE" })
        } finally {
            server.close()
        }
    }

    @Test
    fun uploadRedirectDoesNotReplayPayloadOrExceedItsBudget() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse.Builder().code(307).addHeader("Location", "/replay").build())
        server.enqueue(MockResponse())
        server.start()
        try {
            val config = MeasurementConfig(
                uploadUrl = server.url("/upload").toString(),
                maxBytesPerDirection = 100,
                parallelStreams = 1,
            )
            val result = ThroughputProbe(config, clock = LatencyProbeTest.StepProbeClock()).measureWithClient(
                OkHttpClient(),
                TransferDirection.UPLOAD,
                100,
                deadline(),
            )

            val firstRequest = server.takeRequest()
            val replayedRequest = server.takeRequest(250, TimeUnit.MILLISECONDS)
            assertEquals(100L, firstRequest.body?.size?.toLong())
            assertNull("a redirect must not cause the upload body to be sent again", replayedRequest)
            assertTrue(result.payloadBytes <= 100L)
        } finally {
            server.close()
        }
    }

    @Test
    fun downloadFailureAfterReceivingBytesPreservesThePartialPayload() = runBlocking {
        val server = MockWebServer()
        server.enqueue(
            MockResponse.Builder()
                .body("x".repeat(20))
                .headers(okhttp3.Headers.Builder().add("Content-Length", "50").build())
                .build(),
        )
        server.enqueue(
            MockResponse.Builder()
                .body("y".repeat(20))
                .headers(okhttp3.Headers.Builder().add("Content-Length", "50").build())
                .build(),
        )
        server.start()
        try {
            val config = MeasurementConfig(downloadUrl = server.url("/down").toString(), parallelStreams = 2)
            val result = ThroughputProbe(config, clock = LatencyProbeTest.StepProbeClock()).measureWithClient(
                OkHttpClient(),
                TransferDirection.DOWNLOAD,
                100,
                System.nanoTime() + 500_000_000L,
            )

            assertEquals(40L, result.payloadBytes)
            assertEquals(2, result.successfulStreams)
            assertTrue(result.isPartial)
        } finally {
            server.close()
        }
    }

    private fun deadline() = System.nanoTime() + 10_000_000_000L

    private class ParallelBodyDispatcher(
        private val expectedConcurrentRequests: Int,
        private val responseBytes: Int,
    ) : Dispatcher() {
        private val activeRequests = ConcurrentHashMap<String, AtomicInteger>()
        private val maximumActive = ConcurrentHashMap<String, AtomicInteger>()
        private val requestsStarted = listOf("GET", "POST").associateWith { CountDownLatch(expectedConcurrentRequests) }
        fun maximumConcurrentRequests(method: String): Int = maximumActive[method]?.get() ?: 0

        override fun dispatch(request: RecordedRequest): MockResponse {
            val method = request.method
            val active = activeRequests.computeIfAbsent(method) { AtomicInteger() }.incrementAndGet()
            maximumActive.computeIfAbsent(method) { AtomicInteger() }.updateAndGet { current -> maxOf(current, active) }
            requestsStarted[method]?.countDown()
            requestsStarted[method]?.await(2, TimeUnit.SECONDS)
            try {
                return if (request.method == "GET") {
                    MockResponse.Builder().body("x".repeat(responseBytes)).build()
                } else {
                    MockResponse()
                }
            } finally {
                activeRequests.getValue(method).decrementAndGet()
            }
        }
    }
}
