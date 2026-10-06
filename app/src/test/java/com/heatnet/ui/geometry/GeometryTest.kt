package com.heatnet.ui.geometry

import com.heatnet.data.model.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometryTest {
    private val square = listOf(Point(0f, 0f), Point(10f, 0f), Point(10f, 10f), Point(0f, 10f))
    private val lShape = listOf(
        Point(0f, 0f), Point(10f, 0f), Point(10f, 4f), Point(4f, 4f), Point(4f, 10f), Point(0f, 10f),
    )

    @Test
    fun containsInsideAndOutside() {
        assertTrue(Geometry.contains(square, Point(5f, 5f)))
        assertFalse(Geometry.contains(square, Point(11f, 5f)))
        assertTrue(Geometry.contains(lShape, Point(2f, 8f)))
        assertFalse(Geometry.contains(lShape, Point(8f, 8f)))
    }

    @Test
    fun areaOfSquareAndLShape() {
        assertEquals(100f, Geometry.area(square), 1e-4f)
        assertEquals(64f, Geometry.area(lShape), 1e-4f)
    }

    @Test
    fun bowTieIsNotSimple() {
        val bowTie = listOf(Point(0f, 0f), Point(10f, 10f), Point(10f, 0f), Point(0f, 10f))
        assertFalse(Geometry.isSimple(bowTie))
        assertTrue(Geometry.isSimple(square))
        assertTrue(Geometry.isSimple(lShape))
    }

    @Test
    fun newEdgeThatCrossesEarlierWallIsDetected() {
        val chain = listOf(Point(0f, 0f), Point(10f, 0f), Point(10f, 10f))
        assertTrue(Geometry.newEdgeCrosses(chain, Point(5f, -5f)))
        assertFalse(Geometry.newEdgeCrosses(chain, Point(0f, 10f)))
    }

    @Test
    fun closingEdgeCrossingIsDetected() {
        // Closing (10,0) back to (0,0) runs along y = 0 through the wall x = 5.
        val chain = listOf(Point(0f, 0f), Point(5f, -5f), Point(5f, 5f), Point(10f, 0f))
        assertTrue(Geometry.closingEdgeCrosses(chain))
        assertFalse(Geometry.closingEdgeCrosses(square))
    }

    @Test
    fun snapMakesNearlyStraightWallsStraight() {
        val from = Point(0f, 0f)
        assertEquals(Point(100f, 0f), Geometry.snapToRightAngle(from, Point(100f, 8f)))
        assertEquals(Point(0f, 100f), Geometry.snapToRightAngle(from, Point(-8f, 100f)))
        assertEquals(Point(100f, 100f), Geometry.snapToRightAngle(from, Point(100f, 100f)))
    }

    @Test
    fun nearestEdgeFindsWallUnderFinger() {
        assertEquals(1, Geometry.nearestEdge(square, Point(10.5f, 5f), 2f))
        assertNull(Geometry.nearestEdge(square, Point(5f, 5f), 2f))
    }

    @Test
    fun outlineToMetresScalesByChosenWallAndStartsAtZero() {
        // Drawn in pixels: 200 x 100 px rectangle, offset on the canvas.
        val px = listOf(Point(50f, 80f), Point(250f, 80f), Point(250f, 180f), Point(50f, 180f))
        // Wall 1 (right side, 100 px) is 4 m, so 25 px per metre.
        val m = Geometry.outlineToMetres(px, wallIndex = 1, wallLengthMetres = 4f)
        assertEquals(listOf(Point(0f, 0f), Point(8f, 0f), Point(8f, 4f), Point(0f, 4f)), m)
    }
}
