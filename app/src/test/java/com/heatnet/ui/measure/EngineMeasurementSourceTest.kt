package com.heatnet.ui.measure

import com.heatnet.measurement.engine.MeasurementDependencies
import com.heatnet.measurement.engine.MeasurementEngine
import com.heatnet.measurement.model.ConnectionType
import com.heatnet.measurement.model.MeasurementProgress
import com.heatnet.measurement.model.MeasurementStage
import com.heatnet.measurement.model.MeasurementStatus
import com.heatnet.measurement.network.ConnectionSnapshot
import com.heatnet.measurement.permissions.PermissionOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EngineMeasurementSourceTest {

    @Test
    fun isFakeIsFalse() {
        val dependencies = createFakeDependencies()
        val engine = MeasurementEngine(dependencies)
        val source = EngineMeasurementSource(engine)

        assertFalse(source.isFake)
    }

    @Test
    fun measureDelegatesToEngineAndReturnsResult() = runBlocking {
        val dependencies = createFakeDependencies()
        val engine = MeasurementEngine(dependencies)
        val source = EngineMeasurementSource(engine)

        val progressReports = mutableListOf<MeasurementProgress>()
        val result = source.measure(
            type = ConnectionType.WIFI,
            xMetres = 12.3f,
            yMetres = 45.6f,
            onProgress = progressReports::add,
        )

        assertEquals(ConnectionType.WIFI, result.expectedConnectionType)
        assertEquals(MeasurementStatus.BLOCKED, result.status)
        assertEquals(1, progressReports.size)
        assertEquals(MeasurementStage.VERIFYING_NETWORK, progressReports[0].stage)
    }

    @Test
    fun measureIgnoresCoordinatesAndPassesConnectionTypeAndProgressCallback() = runBlocking {
        val dependencies = createFakeDependencies()
        val engine = MeasurementEngine(dependencies)
        val source = EngineMeasurementSource(engine)

        val progress1 = mutableListOf<MeasurementProgress>()
        val result1 = source.measure(ConnectionType.MOBILE, xMetres = 0f, yMetres = 0f, onProgress = progress1::add)

        val progress2 = mutableListOf<MeasurementProgress>()
        val result2 = source.measure(ConnectionType.MOBILE, xMetres = 100f, yMetres = 200f, onProgress = progress2::add)

        assertEquals(result1.expectedConnectionType, result2.expectedConnectionType)
        assertEquals(result1.status, result2.status)
        assertEquals(progress1, progress2)
    }

    private fun createFakeDependencies(): MeasurementDependencies {
        val emptySnapshot = ConnectionSnapshot(
            network = null,
            connectionType = null,
            hasValidatedInternet = false,
            hasTransportObservation = true,
        )
        return MeasurementDependencies(
            connectionState = MutableStateFlow(emptySnapshot),
            permissionCheck = { PermissionOutcome.GRANTED },
            readCellularRadio = { error("Not called") },
            measureThroughput = { _, _, _, _ -> error("Not called") },
            measureLatency = { _, _, _ -> error("Not called") },
            measurePacketLoss = { _, _, _, _ -> error("Not called") },
        )
    }
}
