package com.heatnet.measurement.engine

import android.net.Network
import com.heatnet.measurement.model.ConnectionType
import com.heatnet.measurement.model.IssueCode
import com.heatnet.measurement.model.MeasurementIssue
import com.heatnet.measurement.model.MeasurementProgress
import com.heatnet.measurement.model.MeasurementResult
import com.heatnet.measurement.model.MeasurementStage
import com.heatnet.measurement.model.MeasurementStatus
import com.heatnet.measurement.network.ConnectionSnapshot
import com.heatnet.measurement.network.sameNetworkHandle
import com.heatnet.measurement.permissions.PermissionOutcome
import com.heatnet.measurement.probe.LatencyMeasurement
import com.heatnet.measurement.probe.PacketLossMeasurement
import com.heatnet.measurement.probe.ThroughputMeasurement
import com.heatnet.measurement.probe.TransferDirection
import com.heatnet.measurement.radio.RadioSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.TimeoutCancellationException

/** Coordinates one bounded reading. The caller owns the shared [ConnectionSnapshot] monitor. */
class MeasurementEngine(
    private val dependencies: MeasurementDependencies,
) {
    suspend fun takeMeasurement(
        expectedType: ConnectionType,
        onProgress: (MeasurementProgress) -> Unit = {},
    ): MeasurementResult {
        dependencies.ensureMonitoringStarted()
        val measuredAt = dependencies.wallClockMillis()
        val startedAt = dependencies.monotonicNanos()
        val deadlineNanos = deadlineFrom(startedAt, dependencies.config.overallTimeoutMillis)
        onProgress(MeasurementProgress(MeasurementStage.VERIFYING_NETWORK))
        val initialSnapshot = dependencies.connectionState.value
        val captured = if (initialSnapshot.hasTransportObservation) {
            initialSnapshot
        } else {
            val remainingNanos = deadlineNanos - dependencies.monotonicNanos()
            if (remainingNanos <= 0L) {
                initialSnapshot
            } else {
                val waitMillis = ((remainingNanos - 1L) / NANOS_PER_MILLISECOND) + 1L
                withTimeoutOrNull(waitMillis) {
                    dependencies.connectionState.first { it.hasTransportObservation }
                } ?: initialSnapshot
            }
        }
        val issues = mutableListOf<MeasurementIssue>()

        val unsupported = captured.hasTransportObservation && captured.network != null && captured.connectionType == null
        val mismatched = captured.hasTransportObservation &&
            captured.connectionType != null && captured.connectionType != expectedType
        if (unsupported) issues += MeasurementIssue(IssueCode.UNSUPPORTED_TRANSPORT, "The active network transport is unsupported")
        else if (mismatched) {
            issues += MeasurementIssue(IssueCode.TRANSPORT_MISMATCH, "The active transport does not match the requested measurement")
        }
        if (captured.network == null) {
            issues += MeasurementIssue(IssueCode.NO_INTERNET, "There is no active default network to measure")
        }
        if (!captured.hasTransportObservation) issues += timeoutIssue()
        if (unsupported || mismatched || captured.network == null || !captured.hasTransportObservation) {
            return result(
                expectedType = expectedType,
                captured = captured,
                measuredAt = measuredAt,
                status = MeasurementStatus.BLOCKED,
                issues = issues,
            )
        }

        val permission = try {
            dependencies.permissionCheck(expectedType)
        } catch (_: SecurityException) {
            PermissionOutcome.DENIED
        }
        if (expectedType == ConnectionType.WIFI && permission != PermissionOutcome.GRANTED) {
            issues += MeasurementIssue(IssueCode.PERMISSION_DENIED, "Fine location permission is required for Wi-Fi radio data")
            return result(
                expectedType = expectedType,
                captured = captured,
                measuredAt = measuredAt,
                status = MeasurementStatus.BLOCKED,
                issues = issues,
            )
        }

        val network = captured.network
        return coroutineScope {
            val routeChanged = CompletableDeferred<Unit>()
            val watcher = launch(start = CoroutineStart.UNDISPATCHED) {
                dependencies.connectionState.first { current -> !sameRoute(captured, current) }
                routeChanged.complete(Unit)
            }
            try {
                var radio = RadioSnapshot()
                var download: ThroughputMeasurement? = null
                var upload: ThroughputMeasurement? = null
                var latency: LatencyMeasurement? = null
                var packetLoss: PacketLossMeasurement? = null
                var haltRemainingStages = false

                onProgress(MeasurementProgress(MeasurementStage.CAPTURING_RADIO))
                when (val outcome = runStage(routeChanged, captured, deadlineNanos) {
                    withContext(Dispatchers.IO) {
                        runInterruptible {
                            if (expectedType == ConnectionType.WIFI) {
                                dependencies.readWifiRadio(network)
                            } else {
                                dependencies.readCellularRadio()
                            }
                        }
                    }
                }) {
                    is StageOutcome.Value -> {
                        radio = outcome.value
                        issues += radio.issues
                    }
                    StageOutcome.RouteChanged -> {
                        issues += networkChangedIssue()
                        haltRemainingStages = true
                    }
                    StageOutcome.TimedOut -> {
                        issues += timeoutIssue()
                        haltRemainingStages = true
                    }
                    is StageOutcome.Failed -> issues += MeasurementIssue(
                        IssueCode.RADIO_UNAVAILABLE,
                        "Radio data could not be read",
                    )
                }

                if (!haltRemainingStages) {
                    onProgress(MeasurementProgress(MeasurementStage.DOWNLOAD))
                    when (val outcome = runStage(routeChanged, captured, deadlineNanos) {
                        dependencies.measureThroughput(
                            network,
                            TransferDirection.DOWNLOAD,
                            dependencies.config.maxBytesPerDirection,
                            deadlineNanos,
                        )
                    }) {
                        is StageOutcome.Value -> {
                            download = outcome.value
                            issues += outcome.value.issues
                        }
                        StageOutcome.RouteChanged -> {
                            issues += networkChangedIssue()
                            haltRemainingStages = true
                        }
                        StageOutcome.TimedOut -> {
                            issues += timeoutIssue()
                            haltRemainingStages = true
                        }
                        is StageOutcome.Failed -> issues += endpointFailureIssue()
                    }
                }

                if (!haltRemainingStages) {
                    onProgress(MeasurementProgress(MeasurementStage.UPLOAD))
                    when (val outcome = runStage(routeChanged, captured, deadlineNanos) {
                        dependencies.measureThroughput(
                            network,
                            TransferDirection.UPLOAD,
                            dependencies.config.maxBytesPerDirection,
                            deadlineNanos,
                        )
                    }) {
                        is StageOutcome.Value -> {
                            upload = outcome.value
                            issues += outcome.value.issues
                        }
                        StageOutcome.RouteChanged -> {
                            issues += networkChangedIssue()
                            haltRemainingStages = true
                        }
                        StageOutcome.TimedOut -> {
                            issues += timeoutIssue()
                            haltRemainingStages = true
                        }
                        is StageOutcome.Failed -> issues += endpointFailureIssue()
                    }
                }

                if (!haltRemainingStages) {
                    onProgress(MeasurementProgress(MeasurementStage.LATENCY))
                    when (val outcome = runStage(routeChanged, captured, deadlineNanos) {
                        dependencies.measureLatency(network, dependencies.config, deadlineNanos)
                    }) {
                        is StageOutcome.Value -> {
                            latency = outcome.value
                            issues += outcome.value.issues
                        }
                        StageOutcome.RouteChanged -> {
                            issues += networkChangedIssue()
                            haltRemainingStages = true
                        }
                        StageOutcome.TimedOut -> {
                            issues += timeoutIssue()
                            haltRemainingStages = true
                        }
                        is StageOutcome.Failed -> issues += endpointFailureIssue()
                    }
                }

                if (!haltRemainingStages) {
                    onProgress(MeasurementProgress(MeasurementStage.PACKET_LOSS))
                    val httpReachable = (download?.successfulStreams ?: 0) > 0 ||
                        (upload?.successfulStreams ?: 0) > 0 ||
                        (latency?.successfulRequests ?: 0) > 0
                    when (val outcome = runStage(routeChanged, captured, deadlineNanos) {
                        dependencies.measurePacketLoss(network, dependencies.config, httpReachable, deadlineNanos)
                    }) {
                        is StageOutcome.Value -> {
                            packetLoss = outcome.value
                            issues += outcome.value.issues
                        }
                        StageOutcome.RouteChanged -> {
                            issues += networkChangedIssue()
                            haltRemainingStages = true
                        }
                        StageOutcome.TimedOut -> {
                            issues += timeoutIssue()
                            haltRemainingStages = true
                        }
                        is StageOutcome.Failed -> issues += endpointFailureIssue()
                    }
                }

                if (!haltRemainingStages) {
                    when {
                        routeChanged.isCompleted || !sameRoute(captured, dependencies.connectionState.value) ->
                            issues += networkChangedIssue()
                        dependencies.monotonicNanos() >= deadlineNanos -> issues += timeoutIssue()
                        else -> onProgress(MeasurementProgress(MeasurementStage.COMPLETE))
                    }
                }

                result(
                    expectedType = expectedType,
                    captured = captured,
                    measuredAt = measuredAt,
                    status = if (issues.isEmpty() && !haltRemainingStages) MeasurementStatus.COMPLETE else MeasurementStatus.PARTIAL,
                    radio = radio,
                    download = download,
                    upload = upload,
                    latency = latency,
                    packetLoss = packetLoss,
                    issues = issues,
                )
            } finally {
                watcher.cancelAndJoin()
            }
        }
    }

    private suspend fun <T> runStage(
        routeChanged: Deferred<Unit>,
        captured: ConnectionSnapshot,
        deadlineNanos: Long,
        action: suspend () -> T,
    ): StageOutcome<T> = coroutineScope {
        if (routeChanged.isCompleted || !sameRoute(captured, dependencies.connectionState.value)) {
            return@coroutineScope StageOutcome.RouteChanged
        }
        val remainingNanos = deadlineNanos - dependencies.monotonicNanos()
        if (remainingNanos <= 0L) return@coroutineScope StageOutcome.TimedOut
        val timeoutMillis = ((remainingNanos - 1L) / NANOS_PER_MILLISECOND) + 1L
        val task = async {
            try {
                StageOutcome.Value(withTimeout(timeoutMillis) { action() })
            } catch (_: TimeoutCancellationException) {
                StageOutcome.TimedOut
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                StageOutcome.Failed(error)
            }
        }
        select<StageOutcome<T>> {
            routeChanged.onAwait {
                task.cancelAndJoin()
                StageOutcome.RouteChanged
            }
            task.onAwait { it }
        }
    }

    private fun result(
        expectedType: ConnectionType,
        captured: ConnectionSnapshot,
        measuredAt: Long,
        status: MeasurementStatus,
        radio: RadioSnapshot = RadioSnapshot(),
        download: ThroughputMeasurement? = null,
        upload: ThroughputMeasurement? = null,
        latency: LatencyMeasurement? = null,
        packetLoss: PacketLossMeasurement? = null,
        issues: List<MeasurementIssue>,
    ): MeasurementResult {
        val wifi = captured.connectionType == ConnectionType.WIFI
        return MeasurementResult(
            expectedConnectionType = expectedType,
            observedConnectionType = captured.connectionType,
            measuredAtEpochMillis = measuredAt,
            status = status,
            downloadMbps = download?.megabitsPerSecond,
            uploadMbps = upload?.megabitsPerSecond,
            latencyMs = latency?.latencyMs,
            jitterMs = latency?.jitterMs,
            packetLossPct = packetLoss?.lossPct,
            packetLossMethod = packetLoss?.method ?: com.heatnet.measurement.model.PacketLossMethod.UNAVAILABLE,
            signalDbm = radio.signalDbm,
            linkSpeedMbps = if (wifi) radio.linkSpeedMbps else null,
            wifiBand = if (wifi) radio.wifiBand else null,
            wifiChannel = if (wifi) radio.wifiChannel else null,
            bssid = if (wifi) radio.bssid else null,
            networkType = if (wifi) null else radio.networkType,
            issues = issues.distinctBy { it.code },
        )
    }

    private fun sameRoute(captured: ConnectionSnapshot, current: ConnectionSnapshot): Boolean =
        sameNetworkHandle(captured.network, current.network) &&
            (!current.hasTransportObservation || captured.connectionType == current.connectionType)

    private fun networkChangedIssue() = MeasurementIssue(
        IssueCode.NETWORK_CHANGED,
        "The active default network changed during the measurement",
    )

    private fun timeoutIssue() = MeasurementIssue(IssueCode.TIMEOUT, "The overall measurement deadline expired")

    private fun endpointFailureIssue() = MeasurementIssue(IssueCode.ENDPOINT_FAILURE, "A measurement probe failed")

    private sealed interface StageOutcome<out T> {
        data class Value<T>(val value: T) : StageOutcome<T>
        data class Failed(val error: Exception) : StageOutcome<Nothing>
        data object RouteChanged : StageOutcome<Nothing>
        data object TimedOut : StageOutcome<Nothing>
    }

    private companion object {
        const val NANOS_PER_MILLISECOND = 1_000_000L

        fun deadlineFrom(start: Long, timeoutMillis: Long): Long {
            if (timeoutMillis <= 0L) return start
            val timeoutNanos = if (timeoutMillis > Long.MAX_VALUE / NANOS_PER_MILLISECOND) {
                Long.MAX_VALUE
            } else {
                timeoutMillis * NANOS_PER_MILLISECOND
            }
            return if (start > Long.MAX_VALUE - timeoutNanos) Long.MAX_VALUE else start + timeoutNanos
        }
    }
}
