package com.heatnet.ui.measure

import com.heatnet.measurement.engine.MeasurementEngine
import com.heatnet.measurement.model.ConnectionType
import com.heatnet.measurement.model.MeasurementProgress
import com.heatnet.measurement.model.MeasurementResult

/**
 * What the Measure button calls. The position is passed only so the fake source can make
 * believable values; Member 1's real engine ignores it (position is never measured, only tapped).
 */
interface MeasurementSource {
    val isFake: Boolean

    suspend fun measure(
        type: ConnectionType,
        xMetres: Float,
        yMetres: Float,
        onProgress: (MeasurementProgress) -> Unit,
    ): MeasurementResult
}

/** Member 1's real measurement engine. */
class EngineMeasurementSource(private val engine: MeasurementEngine) : MeasurementSource {
    override val isFake = false

    override suspend fun measure(
        type: ConnectionType,
        xMetres: Float,
        yMetres: Float,
        onProgress: (MeasurementProgress) -> Unit,
    ): MeasurementResult = engine.takeMeasurement(type, onProgress)
}
