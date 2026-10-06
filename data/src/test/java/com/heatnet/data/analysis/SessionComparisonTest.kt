package com.heatnet.data.analysis

import com.heatnet.data.model.Metric
import com.heatnet.data.reading
import com.heatnet.data.room
import com.heatnet.data.sessionOf
import com.heatnet.measurement.model.ConnectionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionComparisonTest {
    private fun success(result: ComparisonResult) = (result as ComparisonResult.Success).comparison

    private fun metric(c: Comparison, m: Metric) = c.metrics.single { it.metric == m }

    @Test
    fun `different connection types are refused`() {
        val wifi = sessionOf(room(id = 1), reading(1f, 1f))
        val mobile = sessionOf(room(id = 2, type = ConnectionType.MOBILE), reading(1f, 1f, sessionId = 2))
        val result = SessionComparison.compare(wifi, mobile)
        assertEquals(CompareError.DIFFERENT_CONNECTION_TYPE, (result as ComparisonResult.Failure).error)
    }

    @Test
    fun `same session is refused`() {
        val s = sessionOf(room(id = 1), reading(1f, 1f))
        assertEquals(CompareError.SAME_SESSION, (SessionComparison.compare(s, s) as ComparisonResult.Failure).error)
    }

    @Test
    fun `empty session is refused`() {
        val full = sessionOf(room(id = 1), reading(1f, 1f))
        val empty = sessionOf(room(id = 2))
        assertEquals(CompareError.NO_READINGS, (SessionComparison.compare(full, empty) as ComparisonResult.Failure).error)
    }

    @Test
    fun `higher download after is improved and lower latency after is improved`() {
        val before = sessionOf(room(id = 1), reading(1f, 1f, download = 50f, latency = 80f))
        val after = sessionOf(room(id = 2), reading(1f, 1f, sessionId = 2, download = 100f, latency = 30f))
        val c = success(SessionComparison.compare(before, after))
        assertEquals(Verdict.IMPROVED, metric(c, Metric.DOWNLOAD).verdict)
        assertEquals(50f, metric(c, Metric.DOWNLOAD).delta!!, 0.001f)
        assertEquals(Verdict.IMPROVED, metric(c, Metric.LATENCY).verdict)
    }

    @Test
    fun `worse signal after is worse`() {
        val before = sessionOf(room(id = 1), reading(1f, 1f, signal = -55))
        val after = sessionOf(room(id = 2), reading(1f, 1f, sessionId = 2, signal = -80))
        assertEquals(Verdict.WORSE, metric(success(SessionComparison.compare(before, after)), Metric.SIGNAL).verdict)
    }

    @Test
    fun `tiny differences count as similar`() {
        val before = sessionOf(room(id = 1), reading(1f, 1f, download = 100f))
        val after = sessionOf(room(id = 2), reading(1f, 1f, sessionId = 2, download = 102f))
        assertEquals(Verdict.SIMILAR, metric(success(SessionComparison.compare(before, after)), Metric.DOWNLOAD).verdict)
    }

    @Test
    fun `shared colour scale spans both sessions`() {
        val before = sessionOf(room(id = 1), reading(1f, 1f, download = 20f), reading(2f, 2f, download = 60f))
        val after = sessionOf(room(id = 2), reading(1f, 1f, sessionId = 2, download = 90f))
        val m = metric(success(SessionComparison.compare(before, after)), Metric.DOWNLOAD)
        assertEquals(20f, m.scaleMin!!, 0.001f)
        assertEquals(90f, m.scaleMax!!, 0.001f)
    }

    @Test
    fun `metric with no data on one side is not enough data`() {
        val before = sessionOf(room(id = 1), reading(1f, 1f, upload = null))
        val after = sessionOf(room(id = 2), reading(1f, 1f, sessionId = 2, upload = 40f))
        val m = metric(success(SessionComparison.compare(before, after)), Metric.UPLOAD)
        assertEquals(Verdict.NOT_ENOUGH_DATA, m.verdict)
        assertEquals(null, m.delta)
    }

    @Test
    fun `mobile comparison omits link speed`() {
        val a = sessionOf(room(id = 1, type = ConnectionType.MOBILE), reading(1f, 1f))
        val b = sessionOf(room(id = 2, type = ConnectionType.MOBILE), reading(1f, 1f, sessionId = 2))
        val c = success(SessionComparison.compare(a, b))
        assertFalse(c.metrics.any { it.metric == Metric.LINK_SPEED })
    }

    @Test
    fun `different room sizes and access points raise warnings`() {
        val before = sessionOf(room(id = 1), reading(1f, 1f, bssid = "aa:aa"))
        val after = sessionOf(room(id = 2, width = 20f), reading(1f, 1f, sessionId = 2, bssid = "bb:bb"))
        val c = success(SessionComparison.compare(before, after))
        assertEquals(2, c.warnings.size)
    }

    @Test
    fun `matching rooms and access point raise no warnings`() {
        val before = sessionOf(room(id = 1), reading(1f, 1f, bssid = "aa:aa"))
        val after = sessionOf(room(id = 2), reading(1f, 1f, sessionId = 2, bssid = "aa:aa"))
        val c = success(SessionComparison.compare(before, after))
        assertTrue(c.warnings.isEmpty())
        assertNotNull(c.metrics)
    }
}
