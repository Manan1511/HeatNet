package com.heatnet.measurement.probe

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Request
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class HttpCallAwaitTest {
    @Test
    fun callerCancellationCancelsCallAndClosesResponseStream() = runBlocking {
        val server = MockWebServer()
        val client = TestSslUtils.configureHttps(server)
        server.enqueue(MockResponse.Builder().body("x".repeat(20_000)).bodyDelay(5, TimeUnit.SECONDS).build())
        server.start()
        try {
            val call = client.newCall(Request.Builder().url(server.url("/slow")).build())
            val job = launch(Dispatchers.IO) {
                call.awaitResponse(deadline()) { response -> response.body.source().readByteArray() }
            }
            server.takeRequest()
            job.cancelAndJoin()

            assertTrue(call.isCanceled())
        } finally {
            server.close()
        }
    }

    private fun deadline() = System.nanoTime() + 10_000_000_000L
}
