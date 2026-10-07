package com.heatnet.ui.screens

import com.heatnet.data.model.Point
import org.junit.Assert.assertEquals
import org.junit.Test

class OutlineCanvasTransformTest {
    @Test
    fun shrinkingCanvasScalesDrawingDownAndMapsTapsBackToStoredCoordinates() {
        val scale = OutlineCanvasTransform.scale(300f, 200f, 150f, 100f)

        assertEquals(0.5f, scale, 0.001f)
        assertEquals(Point(50f, 25f), OutlineCanvasTransform.toCanvas(Point(100f, 50f), scale))
        assertEquals(Point(100f, 50f), OutlineCanvasTransform.toStored(Point(50f, 25f), scale))
    }

    @Test
    fun largerCanvasDoesNotScaleExistingCoordinatesUp() {
        assertEquals(1f, OutlineCanvasTransform.scale(150f, 100f, 300f, 200f), 0.001f)
    }
}
