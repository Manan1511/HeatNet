package com.heatnet.measurement.model

import android.net.Network
import com.heatnet.measurement.probe.NetworkBoundHttpClientFactory
import com.heatnet.measurement.probe.withByteCount
import okhttp3.ConnectionSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import javax.net.SocketFactory

class MeasurementConfigTest {

    @Test
    fun defaultMeasurementConfigIsValidHttps() {
        val config = MeasurementConfig()
        assertTrue(config.latencyUrl.startsWith("https://"))
        assertTrue(config.downloadUrl.startsWith("https://"))
        assertTrue(config.uploadUrl.startsWith("https://"))
    }

    @Test
    fun customHttpsUrlsSucceed() {
        val config = MeasurementConfig(
            latencyUrl = "https://example.com/latency",
            downloadUrl = "https://example.com/download",
            uploadUrl = "https://example.com/upload",
        )
        assertEquals("https://example.com/latency", config.latencyUrl)
        assertEquals("https://example.com/download", config.downloadUrl)
        assertEquals("https://example.com/upload", config.uploadUrl)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectingHttpLatencyUrl() {
        MeasurementConfig(latencyUrl = "http://example.com/latency")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectingHttpDownloadUrl() {
        MeasurementConfig(downloadUrl = "http://example.com/download")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectingHttpUploadUrl() {
        MeasurementConfig(uploadUrl = "http://example.com/upload")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectingInvalidLatencyUrl() {
        MeasurementConfig(latencyUrl = "not-a-valid-url")
    }

    @Test(expected = IllegalArgumentException::class)
    fun withByteCountRejectsCleartextHttpUrl() {
        withByteCount("http://example.com/latency", 0)
    }

    @Test
    fun withByteCountAppendsQueryToHttpsUrl() {
        val result = withByteCount("https://example.com/latency", 100)
        assertEquals("https://example.com/latency?bytes=100", result)
    }

    @Test
    fun httpClientFactoryDisallowsCleartextConnectionSpecs() {
        val network = mock(Network::class.java)
        `when`(network.socketFactory).thenReturn(SocketFactory.getDefault())

        val factory = NetworkBoundHttpClientFactory()
        val client = factory.create(network)

        val specs = client.connectionSpecs
        assertFalse("OkHttpClient must not permit cleartext traffic", specs.contains(ConnectionSpec.CLEARTEXT))
        assertTrue("OkHttpClient must require TLS", specs.all { it.isTls })
    }
}
