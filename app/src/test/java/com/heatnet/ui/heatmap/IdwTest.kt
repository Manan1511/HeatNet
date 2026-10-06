package com.heatnet.ui.heatmap

import com.heatnet.data.model.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IdwTest {
    private val room = listOf(Point(0f, 0f), Point(10f, 0f), Point(10f, 10f), Point(0f, 10f))

    @Test
    fun onTopOfAReadingReturnsThatReading() {
        val samples = listOf(Sample(1f, 1f, 50f), Sample(9f, 9f, 10f))
        assertEquals(50f, Idw.interpolate(samples, 1f, 1f, 2f), 1e-4f)
    }

    @Test
    fun midpointOfTwoReadingsIsTheirAverage() {
        val samples = listOf(Sample(0f, 0f, 100f), Sample(10f, 0f, 0f))
        assertEquals(50f, Idw.interpolate(samples, 5f, 0f, 2f), 1e-3f)
    }

    @Test
    fun closerReadingWeighsMore() {
        val samples = listOf(Sample(0f, 0f, 100f), Sample(10f, 0f, 0f))
        // Distances 2 and 8, power 2: weights 1/4 and 1/64, so 100 * 16/17.
        assertEquals(100f * 16f / 17f, Idw.interpolate(samples, 2f, 0f, 2f), 1e-3f)
    }

    @Test
    fun valuesStayWithinReadingRange() {
        val samples = listOf(Sample(1f, 1f, 20f), Sample(9f, 2f, 80f), Sample(5f, 9f, 50f))
        val grid = Idw.computeGrid(room, 10f, 10f, samples)!!
        val inside = grid.values.filter { !it.isNaN() }
        assertTrue(inside.isNotEmpty())
        assertTrue(inside.all { it in 20f..80f })
    }

    @Test
    fun noHeatmapWithFewerThanThreeReadings() {
        assertNull(Idw.computeGrid(room, 10f, 10f, listOf(Sample(1f, 1f, 1f), Sample(2f, 2f, 2f))))
        assertNotNull(Idw.computeGrid(room, 10f, 10f, listOf(Sample(1f, 1f, 1f), Sample(2f, 2f, 2f), Sample(3f, 3f, 3f))))
    }

    @Test
    fun cellsOutsideTheOutlineAreEmpty() {
        val triangle = listOf(Point(0f, 0f), Point(10f, 0f), Point(0f, 10f))
        val samples = listOf(Sample(1f, 1f, 1f), Sample(5f, 1f, 2f), Sample(1f, 5f, 3f))
        val grid = Idw.computeGrid(triangle, 10f, 10f, samples)!!
        val bottomRight = (grid.rows - 1) * grid.cols + (grid.cols - 1)
        assertTrue(grid.values[bottomRight].isNaN())
        assertEquals(0f, grid.alphas[bottomRight], 0f)
        assertTrue(!grid.values[0].isNaN())
    }

    @Test
    fun fadeIsBrightNearReadingsAndWashedOutFarAway() {
        val config = HeatmapConfig()
        val diagonal = 10f
        assertEquals(config.maxAlpha, Idw.fadeAlpha(0f, diagonal, config), 1e-4f)
        assertEquals(config.minAlpha, Idw.fadeAlpha(diagonal, diagonal, config), 1e-4f)
        val middle = Idw.fadeAlpha(2f, diagonal, config)
        assertTrue(middle < config.maxAlpha && middle > config.minAlpha)
    }
}
