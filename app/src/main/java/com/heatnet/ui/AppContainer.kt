package com.heatnet.ui

import android.content.Context
import com.heatnet.data.db.HeatNetDatabase
import com.heatnet.data.db.SessionRepository
import com.heatnet.measurement.engine.MeasurementDependencies
import com.heatnet.measurement.engine.MeasurementEngine
import com.heatnet.measurement.network.ConnectionMonitor
import com.heatnet.ui.measure.EngineMeasurementSource
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

    val measurementSource: MeasurementSource by lazy {
        EngineMeasurementSource(MeasurementEngine(MeasurementDependencies.forAndroid(appContext, connectionMonitor)))
    }
}
