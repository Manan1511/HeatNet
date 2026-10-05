package com.heatnet.measurement.engine

import android.content.Context
import android.net.Network
import com.heatnet.measurement.model.ConnectionType
import com.heatnet.measurement.model.MeasurementConfig
import com.heatnet.measurement.network.ConnectionMonitor
import com.heatnet.measurement.network.ConnectionSnapshot
import com.heatnet.measurement.permissions.PermissionGate
import com.heatnet.measurement.permissions.PermissionOutcome
import com.heatnet.measurement.probe.LatencyMeasurement
import com.heatnet.measurement.probe.LatencyProbe
import com.heatnet.measurement.probe.PacketLossMeasurement
import com.heatnet.measurement.probe.PacketLossProbe
import com.heatnet.measurement.probe.ThroughputMeasurement
import com.heatnet.measurement.probe.ThroughputProbe
import com.heatnet.measurement.probe.TransferDirection
import com.heatnet.measurement.radio.CellularRadioReader
import com.heatnet.measurement.radio.RadioSnapshot
import com.heatnet.measurement.radio.WifiRadioReader
import kotlinx.coroutines.flow.StateFlow

/** Injectable platform and measurement boundaries used by one [MeasurementEngine]. */
data class MeasurementDependencies(
    val connectionState: StateFlow<ConnectionSnapshot>,
    val ensureMonitoringStarted: () -> Unit = {},
    val permissionCheck: (ConnectionType) -> PermissionOutcome,
    val readWifiRadio: (Network) -> RadioSnapshot,
    val readCellularRadio: () -> RadioSnapshot,
    val measureThroughput: suspend (Network, TransferDirection, Long, Long) -> ThroughputMeasurement,
    val measureLatency: suspend (Network, MeasurementConfig, Long) -> LatencyMeasurement,
    val measurePacketLoss: suspend (Network, MeasurementConfig, Boolean, Long) -> PacketLossMeasurement,
    val config: MeasurementConfig = MeasurementConfig(),
    val wallClockMillis: () -> Long = System::currentTimeMillis,
    val monotonicNanos: () -> Long = System::nanoTime,
) {
    companion object {
        /**
         * Connects the measurement engine to the Android readers and probes. The shared monitor
         * is started when a measurement begins and remains owned by the application lifecycle.
         */
        fun forAndroid(
            context: Context,
            connectionMonitor: ConnectionMonitor,
            config: MeasurementConfig = MeasurementConfig(),
        ): MeasurementDependencies {
            val appContext = context.applicationContext
            val permissionGate = PermissionGate(appContext)
            val wifiReader = WifiRadioReader(appContext)
            val cellularReader = CellularRadioReader(appContext)
            val throughputProbe = ThroughputProbe(config)
            val latencyProbe = LatencyProbe()
            val packetLossProbe = PacketLossProbe()

            return MeasurementDependencies(
                connectionState = connectionMonitor.state,
                ensureMonitoringStarted = connectionMonitor::start,
                permissionCheck = permissionGate::check,
                readWifiRadio = wifiReader::read,
                readCellularRadio = cellularReader::read,
                measureThroughput = { network, direction, budget, deadline ->
                    throughputProbe.measure(network, direction, budget, deadline)
                },
                measureLatency = latencyProbe::measure,
                measurePacketLoss = packetLossProbe::measure,
                config = config,
            )
        }
    }
}
