package com.heatnet.ui.measure

import com.heatnet.measurement.engine.MeasurementEngine
import com.heatnet.measurement.model.ConnectionType
import com.heatnet.measurement.model.MeasurementProgress
import com.heatnet.measurement.model.MeasurementResult

/** What the Measure button calls. Position is stored by the app; the network engine never infers it. */
interface MeasurementSource {
    suspend fun measure(
        type: ConnectionType,
        xMetres: Float,
        yMetres: Float,
        onProgress: (MeasurementProgress) -> Unit,
    ): MeasurementResult
}

/** Member 1's real measurement engine. */
class EngineMeasurementSource(private val engine: MeasurementEngine) : MeasurementSource {
    override suspend fun measure(
        type: ConnectionType,
        xMetres: Float,
        yMetres: Float,
        onProgress: (MeasurementProgress) -> Unit,
    ): MeasurementResult = engine.takeMeasurement(type, onProgress)
}
