package com.heatnet.ui.measure

import com.heatnet.measurement.model.ConnectionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FakeMeasurementSourceTest {
    @Test
    fun fakeMeasurementsCarryDemoProvenance() = runBlocking {
        val result = FakeMeasurementSource(stageDelayMillis = 0L, random = Random(4))
            .measure(ConnectionType.WIFI, xMetres = 1f, yMetres = 1f) {}

        assertTrue(result.isDemo)
    }
}
