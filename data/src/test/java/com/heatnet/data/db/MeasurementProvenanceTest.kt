package com.heatnet.data.db

import com.heatnet.data.reading
import com.heatnet.measurement.model.ConnectionType
import com.heatnet.measurement.model.MeasurementResult
import com.heatnet.measurement.model.MeasurementStatus
import com.heatnet.measurement.model.PacketLossMethod
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementProvenanceTest {
    @Test
    fun measurementResultSourceIsPreservedInSavedReading() {
        val result = MeasurementResult(
            expectedConnectionType = ConnectionType.WIFI,
            observedConnectionType = ConnectionType.WIFI,
            measuredAtEpochMillis = 1L,
            status = MeasurementStatus.COMPLETE,
            packetLossMethod = PacketLossMethod.ICMP,
            isDemo = true,
        )

        assertTrue(result.toReading(sessionId = 7L, x = 1f, y = 2f).isDemo)
    }

    @Test
    fun entityMappersPreserveDemoAndLegacyRealSources() {
        val demo = reading(1f, 2f).copy(isDemo = true).toEntity()
        val real = reading(2f, 3f).copy(isDemo = false).toEntity()

        assertTrue(demo.isDemo)
        assertTrue(demo.toDomain().isDemo)
        assertFalse(real.isDemo)
        assertFalse(real.toDomain().isDemo)
    }
}
