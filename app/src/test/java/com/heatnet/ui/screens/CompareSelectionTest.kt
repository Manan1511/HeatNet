package com.heatnet.ui.screens

import com.heatnet.data.analysis.MetricComparison
import com.heatnet.data.analysis.MetricStats
import com.heatnet.data.analysis.Verdict
import com.heatnet.data.model.Metric
import com.heatnet.data.model.SessionSummary
import com.heatnet.measurement.model.ConnectionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompareSelectionTest {
    private fun session(id: Long, type: ConnectionType, createdAt: Long = id * 100) =
        SessionSummary(id, "Room $id", createdAt, type, readingCount = 5)

    private val wifiA = session(1, ConnectionType.WIFI)
    private val wifiB = session(2, ConnectionType.WIFI)
    private val wifiC = session(3, ConnectionType.WIFI)
    private val mobile = session(4, ConnectionType.MOBILE)

    @Test
    fun firstPickFixesTheConnectionType() {
        val selected = CompareSelection.toggle(emptyList(), wifiA)
        assertTrue(CompareSelection.canSelect(selected, wifiB))
        assertFalse(CompareSelection.canSelect(selected, mobile))
    }

    @Test
    fun differentTypeIsIgnored() {
        val selected = CompareSelection.toggle(listOf(wifiA), mobile)
        assertEquals(listOf(wifiA), selected)
    }

    @Test
    fun cannotPickMoreThanTwo() {
        val selected = CompareSelection.toggle(listOf(wifiA, wifiB), wifiC)
        assertEquals(listOf(wifiA, wifiB), selected)
        assertFalse(CompareSelection.canSelect(selected, wifiC))
    }

    @Test
    fun tappingASelectedSessionDeselectsIt() {
        val selected = CompareSelection.toggle(listOf(wifiA, wifiB), wifiA)
        assertEquals(listOf(wifiB), selected)
    }

    @Test
    fun deselectingTheOnlyPickFreesTheType() {
        val selected = CompareSelection.toggle(listOf(wifiA), wifiA)
        assertTrue(selected.isEmpty())
        assertTrue(CompareSelection.canSelect(selected, mobile))
    }

    @Test
    fun orderedIdsNeedsExactlyTwoAndPutsOlderFirst() {
        assertNull(CompareSelection.orderedIds(listOf(wifiA)))
        assertEquals(1L to 2L, CompareSelection.orderedIds(listOf(wifiB, wifiA)))
    }

    @Test
    fun describeUsesPlainWords() {
        val comparison = MetricComparison(
            metric = Metric.LATENCY,
            before = MetricStats(mean = 80f, min = 60f, max = 100f, count = 5),
            after = MetricStats(mean = 40f, min = 30f, max = 50f, count = 5),
            delta = -40f,
            verdict = Verdict.IMPROVED,
            scaleMin = 30f,
            scaleMax = 100f,
        )
        assertEquals("Latency: better. Average across 5 shared room areas went from 80 ms to 40 ms.", describe(comparison))
    }

    @Test
    fun describeExplainsMissingData() {
        val comparison = MetricComparison(
            metric = Metric.DOWNLOAD,
            before = null,
            after = MetricStats(10f, 5f, 15f, 3),
            delta = null,
            verdict = Verdict.NOT_ENOUGH_DATA,
            scaleMin = 5f,
            scaleMax = 15f,
        )
        assertEquals("Download speed: not enough shared room areas with usable data to compare.", describe(comparison))
    }
}
