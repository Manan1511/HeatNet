package com.heatnet.ui.heatmap

import com.heatnet.data.model.Metric
import com.heatnet.data.model.Reading
import com.heatnet.measurement.model.ConnectionType
import com.heatnet.measurement.model.MeasurementStatus
import com.heatnet.measurement.model.PacketLossMethod
import org.junit.Assert.assertEquals
import org.junit.Test

class MetricScaleTest {
    @Test
    fun higherIsBetterForSpeedLowerIsBetterForLatency() {
        val speed = MetricScale(Metric.DOWNLOAD, 0f, 100f)
        assertEquals(1f, speed.goodness(100f), 1e-4f)
        assertEquals(0f, speed.goodness(0f), 1e-4f)
        val latency = MetricScale(Metric.LATENCY, 0f, 200f)
        assertEquals(1f, latency.goodness(0f), 1e-4f)
        assertEquals(0f, latency.goodness(500f), 1e-4f)
    }

    @Test
    fun fixedRangesIgnoreTheReadings() {
        val scale = MetricScale.forReadings(Metric.SIGNAL, ConnectionType.WIFI, listOf(reading(signal = -50)))
        assertEquals(-90f, scale.min, 0f)
        assertEquals(-30f, scale.max, 0f)
    }

    @Test
    fun speedRangeRunsToTheBestReading() {
        val scale = MetricScale.forReadings(
            Metric.DOWNLOAD,
            ConnectionType.WIFI,
            listOf(reading(download = 40f), reading(download = 120f), reading(download = null)),
        )
        assertEquals(0f, scale.min, 0f)
        assertEquals(120f, scale.max, 0f)
    }

    @Test
    fun rampEndsAreRedAndGreen() {
        assertEquals(0xFFD73027.toInt(), ColorRamp.colorAt(0f))
        assertEquals(0xFF1A9850.toInt(), ColorRamp.colorAt(1f))
    }

    @Test
    fun httpFallbackIsLabelledHonestly() {
        val r = reading(lossMethod = PacketLossMethod.HTTP_PROBE_FAILURES)
        assertEquals("Packet loss / HTTP request failures", Metric.PACKET_LOSS.displayLabel(listOf(r)))
        assertEquals(Metric.PACKET_LOSS.label, Metric.PACKET_LOSS.displayLabel(listOf(reading())))
    }

    private fun reading(
        download: Float? = 100f,
        signal: Int? = -55,
        lossMethod: PacketLossMethod = PacketLossMethod.ICMP,
    ) = Reading(
        id = 0, sessionId = 1, x = 0f, y = 0f, timestamp = 0, status = MeasurementStatus.COMPLETE,
        downloadMbps = download, uploadMbps = 10f, latencyMs = 20f, jitterMs = 2f, packetLossPct = 0f,
        packetLossMethod = lossMethod, signalDbm = signal, linkSpeedMbps = 400, wifiBand = "5",
        wifiChannel = 36, bssid = null, networkType = null,
    )
}
