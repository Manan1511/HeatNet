package com.heatnet.ui.screens

import com.heatnet.data.analysis.MetricComparison
import com.heatnet.data.analysis.MetricStats
import com.heatnet.data.analysis.Verdict
import com.heatnet.data.model.Metric
import org.junit.Assert.assertTrue
import org.junit.Test

class CompareTextTest {
    @Test
    fun descriptionSaysAveragesUseSharedRoomAreas() {
        val comparison = MetricComparison(
            metric = Metric.DOWNLOAD,
            before = MetricStats(mean = 40f, min = 30f, max = 50f, count = 2),
            after = MetricStats(mean = 60f, min = 50f, max = 70f, count = 2),
            delta = 20f,
            verdict = Verdict.IMPROVED,
            scaleMin = 30f,
            scaleMax = 70f,
        )

        assertTrue(describe(comparison).contains("2 shared room areas"))
    }
}
