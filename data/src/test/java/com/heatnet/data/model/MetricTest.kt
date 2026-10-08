package com.heatnet.data.model

import com.heatnet.data.reading
import com.heatnet.measurement.model.ConnectionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class MetricTest {

    @Test
    fun `valueOf returns correct values for a complete reading`() {
        val r = reading(
            x = 1f,
            y = 1f,
            download = 120.5f,
            upload = 45.2f,
            latency = 15.0f,
            loss = 0.5f,
            signal = -65,
            linkSpeed = 866,
        )

        assertEquals(120.5f, Metric.DOWNLOAD.valueOf(r))
        assertEquals(45.2f, Metric.UPLOAD.valueOf(r))
        assertEquals(15.0f, Metric.LATENCY.valueOf(r))
        assertEquals(0.5f, Metric.PACKET_LOSS.valueOf(r))
        assertEquals(-65.0f, Metric.SIGNAL.valueOf(r))
        assertEquals(866.0f, Metric.LINK_SPEED.valueOf(r))
    }

    @Test
    fun `valueOf returns null when reading metric fields are null`() {
        val r = reading(
            x = 1f,
            y = 1f,
            download = null,
            upload = null,
            latency = null,
            loss = null,
            signal = null,
            linkSpeed = null,
        )

        assertNull(Metric.DOWNLOAD.valueOf(r))
        assertNull(Metric.UPLOAD.valueOf(r))
        assertNull(Metric.LATENCY.valueOf(r))
        assertNull(Metric.PACKET_LOSS.valueOf(r))
        assertNull(Metric.SIGNAL.valueOf(r))
        assertNull(Metric.LINK_SPEED.valueOf(r))
    }

    @Test
    fun `availableFor returns all metrics for WIFI`() {
        val wifiMetrics = Metric.availableFor(ConnectionType.WIFI)

        assertEquals(Metric.entries, wifiMetrics)
        assertTrue(wifiMetrics.contains(Metric.LINK_SPEED))
    }

    @Test
    fun `availableFor excludes wifiOnly metrics for MOBILE`() {
        val mobileMetrics = Metric.availableFor(ConnectionType.MOBILE)

        assertFalse(mobileMetrics.contains(Metric.LINK_SPEED))
        assertEquals(Metric.entries.filter { !it.wifiOnly }, mobileMetrics)
    }
}
