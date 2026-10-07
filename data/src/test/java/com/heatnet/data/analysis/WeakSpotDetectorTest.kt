package com.heatnet.data.analysis

import com.heatnet.data.reading
import com.heatnet.data.room
import com.heatnet.measurement.model.ConnectionType
import com.heatnet.measurement.model.PacketLossMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeakSpotDetectorTest {
    private val defaultRoom = room()

    @Test
    fun `fewer than three readings gives no verdict`() {
        val report = WeakSpotDetector.analyse(defaultRoom, listOf(reading(1f, 1f), reading(9f, 7f)))
        assertFalse(report.enoughReadings)
        assertTrue(report.spots.isEmpty())
    }

    @Test
    fun `healthy room has no weak spots`() {
        val readings = listOf(reading(2f, 2f), reading(8f, 2f), reading(2f, 6f), reading(8f, 6f))
        val report = WeakSpotDetector.analyse(defaultRoom, readings)
        assertTrue(report.enoughReadings)
        assertTrue(report.spots.isEmpty())
        assertEquals("No weak spots found.", report.summary)
    }

    @Test
    fun `weak wifi signal in the north-east is named`() {
        val readings = listOf(
            reading(2f, 2f),
            reading(8f, 2f, signal = -82),
            reading(2f, 6f),
            reading(8f, 6f),
        )
        val report = WeakSpotDetector.analyse(defaultRoom, readings)
        assertEquals(1, report.spots.size)
        assertEquals(Quadrant.NORTH_EAST, report.spots[0].quadrant)
        assertEquals("Weak signal in the north-east corner", report.spots[0].sentence)
    }

    @Test
    fun `several problems in one quadrant are combined into one sentence`() {
        val readings = listOf(
            reading(2f, 2f),
            reading(2f, 6f, signal = -85, latency = 180f),
            reading(8f, 2f),
            reading(8f, 6f),
        )
        val spot = WeakSpotDetector.analyse(defaultRoom, readings).spots.single()
        assertEquals(Quadrant.SOUTH_WEST, spot.quadrant)
        assertEquals("Weak signal and high latency in the south-west corner", spot.sentence)
    }

    @Test
    fun `download below a quarter of the best is flagged`() {
        val readings = listOf(
            reading(2f, 2f, download = 200f),
            reading(8f, 2f, download = 40f),
            reading(2f, 6f, download = 180f),
            reading(8f, 6f, download = 150f),
        )
        val spot = WeakSpotDetector.analyse(defaultRoom, readings).spots.single()
        assertEquals(listOf(Problem.SLOW_DOWNLOAD), spot.problems)
    }

    @Test
    fun `packet loss above two percent is flagged`() {
        val readings = listOf(
            reading(2f, 2f),
            reading(8f, 2f),
            reading(2f, 6f),
            reading(8f, 6f, loss = 5f),
        )
        val spot = WeakSpotDetector.analyse(defaultRoom, readings).spots.single()
        assertEquals(Quadrant.SOUTH_EAST, spot.quadrant)
        assertEquals("Packet loss in the south-east corner", spot.sentence)
    }

    @Test
    fun `one bad reading among many good ones in a quadrant is not flagged`() {
        val readings = listOf(
            reading(1f, 1f, signal = -90),
            reading(2f, 2f),
            reading(3f, 1f),
            reading(8f, 6f),
        )
        assertTrue(WeakSpotDetector.analyse(defaultRoom, readings).spots.isEmpty())
    }

    @Test
    fun `failed tests with null metrics are ignored`() {
        val readings = listOf(
            reading(2f, 2f, download = null, latency = null, loss = null, signal = null),
            reading(8f, 2f),
            reading(2f, 6f),
            reading(8f, 6f),
        )
        assertTrue(WeakSpotDetector.analyse(defaultRoom, readings).spots.isEmpty())
    }

    @Test
    fun `mobile sessions use the mobile signal threshold`() {
        val mobile = room(type = ConnectionType.MOBILE)
        val readings = listOf(
            reading(2f, 2f, signal = -90),
            reading(8f, 2f, signal = -90),
            reading(2f, 6f, signal = -90),
            reading(8f, 6f, signal = -110),
        )
        val spot = WeakSpotDetector.analyse(mobile, readings).spots.single()
        assertEquals(Quadrant.SOUTH_EAST, spot.quadrant)
    }

    @Test
    fun `HTTP probe failures are not described as packet loss`() {
        val readings = listOf(
            reading(1f, 1f, loss = 5f).copy(packetLossMethod = PacketLossMethod.HTTP_PROBE_FAILURES),
            reading(1.2f, 1f, loss = 5f).copy(packetLossMethod = PacketLossMethod.HTTP_PROBE_FAILURES),
            reading(1.4f, 1f, loss = 0f),
            reading(1.6f, 1f, loss = 0f),
        )

        assertTrue(WeakSpotDetector.analyse(defaultRoom, readings).spots.isEmpty())
    }

    @Test
    fun `enough readings with no usable metrics says measurements are unavailable`() {
        val readings = listOf(
            reading(1f, 1f, download = null, upload = null, latency = null, loss = null, signal = null),
            reading(8f, 2f, download = null, upload = null, latency = null, loss = null, signal = null),
            reading(2f, 6f, download = null, upload = null, latency = null, loss = null, signal = null),
        )

        val report = WeakSpotDetector.analyse(defaultRoom, readings)

        assertTrue(report.enoughReadings)
        assertTrue(report.spots.isEmpty())
        assertEquals("No usable measurements are available to identify weak spots.", report.summary)
    }
}
