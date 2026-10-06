package com.heatnet.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.heatnet.data.db.HeatNetDatabase
import com.heatnet.data.db.SessionRepository
import com.heatnet.measurement.engine.MeasurementDependencies
import com.heatnet.measurement.engine.MeasurementEngine
import com.heatnet.measurement.network.ConnectionMonitor
import com.heatnet.ui.measure.EngineMeasurementSource
import com.heatnet.ui.measure.FakeMeasurementSource
import com.heatnet.ui.measure.MeasurementSource

/**
 * Manual dependency wiring for the app. Member 1's engine and Member 3's repository are created
 * here once; screens only see [MeasurementSource] and [SessionRepository].
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val repository: SessionRepository by lazy { SessionRepository(HeatNetDatabase.get(appContext)) }

    /** Shared for the whole app lifetime, as Member 1's design requires. */
    val connectionMonitor: ConnectionMonitor by lazy { ConnectionMonitor(appContext).also { it.start() } }

    private val realSource: MeasurementSource by lazy {
        EngineMeasurementSource(MeasurementEngine(MeasurementDependencies.forAndroid(appContext, connectionMonitor)))
    }
    private val fakeSource: MeasurementSource by lazy { FakeMeasurementSource() }

    /** Demo mode: fake readings, so the map can be shown without a network or on an emulator. */
    var useFakeMeasurements by mutableStateOf(false)

    val measurementSource: MeasurementSource
        get() = if (useFakeMeasurements) fakeSource else realSource
}
