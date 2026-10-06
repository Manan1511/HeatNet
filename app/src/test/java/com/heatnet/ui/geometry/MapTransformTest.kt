package com.heatnet.ui.geometry

import com.heatnet.data.model.Point
import org.junit.Assert.assertEquals
import org.junit.Test

class MapTransformTest {
    @Test
    fun wideRoomFitsWidthAndIsCentredVertically() {
        // 10 m x 5 m room in a 1000 x 1000 px canvas with 0 padding: 100 px per metre.
        val t = MapTransform(10f, 5f, 1000f, 1000f, 0f)
        assertEquals(100f, t.scale, 1e-4f)
        assertEquals(Point(0f, 250f), t.toPx(Point(0f, 0f)))
        assertEquals(Point(1000f, 750f), t.toPx(Point(10f, 5f)))
    }

    @Test
    fun paddingIsRespected() {
        val t = MapTransform(4f, 4f, 500f, 900f, 50f)
        assertEquals(100f, t.scale, 1e-4f)
        assertEquals(Point(50f, 250f), t.toPx(Point(0f, 0f)))
    }

    @Test
    fun pixelsRoundTripToMetres() {
        val t = MapTransform(7.3f, 4.1f, 1080f, 1500f, 48f)
        val m = Point(3.2f, 1.7f)
        val back = t.toMetres(t.toPx(m))
        assertEquals(m.x, back.x, 1e-4f)
        assertEquals(m.y, back.y, 1e-4f)
    }
}
