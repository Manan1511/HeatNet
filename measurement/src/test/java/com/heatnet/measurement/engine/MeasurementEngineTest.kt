package com.heatnet.measurement.engine

import android.net.Network
import com.heatnet.measurement.model.ConnectionType
import com.heatnet.measurement.model.IssueCode
import com.heatnet.measurement.model.MeasurementConfig
import com.heatnet.measurement.model.MeasurementIssue
import com.heatnet.measurement.model.MeasurementProgress
import com.heatnet.measurement.model.MeasurementStage
import com.heatnet.measurement.model.MeasurementStatus
import com.heatnet.measurement.model.PacketLossMethod
import com.heatnet.measurement.network.ConnectionSnapshot
import com.heatnet.measurement.permissions.PermissionOutcome
import com.heatnet.measurement.probe.LatencyMeasurement
import com.heatnet.measurement.probe.PacketLossMeasurement
import com.heatnet.measurement.probe.ThroughputMeasurement
import com.heatnet.measurement.probe.TransferDirection
import com.heatnet.measurement.radio.RadioSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.yield
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class MeasurementEngineTest {
    @Test
    fun wifiRadioIsReadForTheCapturedNetworkDuringTheMeasurement() = runBlocking {
        val network = fakeNetwork()
        val sampledRadio = RadioSnapshot(signalDbm = -61, linkSpeedMbps = 400, wifiBand = "5", wifiChannel = 36)
        val harness = Harness(
            ConnectionSnapshot(network, ConnectionType.WIFI, hasValidatedInternet = true),
            wifiRead = { sampledRadio },
        )

        val result = MeasurementEngine(harness.dependencies()).takeMeasurement(ConnectionType.WIFI)

        assertEquals(-61, result.signalDbm)
        assertEquals(400, result.linkSpeedMbps)
        assertEquals(1, harness.wifiNetworks.size)
        assertSame(network, harness.wifiNetworks.single())
    }

    @Test
    fun successfulReadingEmitsTheApprovedProgressOrderAndCapturesAllFields() = runBlocking {
        val network = fakeNetwork()
        val harness = Harness(snapshot(network, ConnectionType.WIFI, validated = true))
        val progress = mutableListOf<MeasurementProgress>()

        val result = MeasurementEngine(harness.dependencies()).takeMeasurement(ConnectionType.WIFI, progress::add)

        assertEquals(MeasurementStatus.COMPLETE, result.status)
        assertEquals(-54, result.signalDbm)
        assertEquals(866, result.linkSpeedMbps)
        assertEquals("6", result.wifiBand)
        assertEquals(1, result.wifiChannel)
        assertEquals("00:11:22:33:44:55", result.bssid)
        assertEquals(42f, result.downloadMbps!!, 0.001f)
        assertEquals(31f, result.uploadMbps!!, 0.001f)
        assertEquals(12f, result.latencyMs!!, 0.001f)
        assertEquals(2f, result.jitterMs!!, 0.001f)
        assertEquals(5f, result.packetLossPct!!, 0.001f)
        assertEquals(PacketLossMethod.ICMP, result.packetLossMethod)
        assertEquals(
            listOf(
                MeasurementStage.VERIFYING_NETWORK,
                MeasurementStage.CAPTURING_RADIO,
                MeasurementStage.DOWNLOAD,
                MeasurementStage.UPLOAD,
                MeasurementStage.LATENCY,
                MeasurementStage.PACKET_LOSS,
                MeasurementStage.COMPLETE,
            ),
            progress.map { it.stage },
        )
        assertEquals(1, harness.downloadCalls.get())
        assertEquals(1, harness.uploadCalls.get())
        assertEquals(1, harness.latencyCalls.get())
        assertEquals(1, harness.lossCalls.get())
        assertTrue(harness.httpReachablePassedToLoss)
        assertEquals(4, harness.capturedNetworks.size)
        assertTrue(harness.capturedNetworks.all { it === network })
    }

    @Test
    fun unsupportedOrMismatchedTransportBlocksWithoutStartingProbes() = runBlocking {
        val network = fakeNetwork()
        val unsupported = Harness(snapshot(network, type = null, validated = true))
        val unsupportedResult = MeasurementEngine(unsupported.dependencies()).takeMeasurement(ConnectionType.WIFI)
        assertEquals(MeasurementStatus.BLOCKED, unsupportedResult.status)
        assertTrue(unsupportedResult.issues.any { it.code == IssueCode.UNSUPPORTED_TRANSPORT })
        assertEquals(0, unsupported.totalProbeCalls())

        val mismatch = Harness(snapshot(network, ConnectionType.MOBILE, validated = true))
        val mismatchResult = MeasurementEngine(mismatch.dependencies()).takeMeasurement(ConnectionType.WIFI)
        assertEquals(MeasurementStatus.BLOCKED, mismatchResult.status)
        assertTrue(mismatchResult.issues.any { it.code == IssueCode.TRANSPORT_MISMATCH })
        assertEquals(0, mismatch.totalProbeCalls())
    }

    @Test
    fun deniedWifiBlocksButMobileDoesNotNeedWifiLocation() = runBlocking {
        val deniedWifi = Harness(
            snapshot(fakeNetwork(), ConnectionType.WIFI, validated = true),
            permission = { PermissionOutcome.DENIED },
        )
        val wifiResult = MeasurementEngine(deniedWifi.dependencies()).takeMeasurement(ConnectionType.WIFI)
        assertEquals(MeasurementStatus.BLOCKED, wifiResult.status)
        assertTrue(wifiResult.issues.any { it.code == IssueCode.PERMISSION_DENIED })
        assertEquals(0, deniedWifi.totalProbeCalls())

        val mobile = Harness(
            snapshot(fakeNetwork(), ConnectionType.MOBILE, validated = true),
            permission = { type -> if (type == ConnectionType.WIFI) PermissionOutcome.DENIED else PermissionOutcome.GRANTED },
        )
        val mobileResult = MeasurementEngine(mobile.dependencies()).takeMeasurement(ConnectionType.MOBILE)
        assertEquals(MeasurementStatus.COMPLETE, mobileResult.status)
        assertEquals("NR", mobileResult.networkType)
        assertEquals(-91, mobileResult.signalDbm)
    }

    @Test
    fun waitsForTheFirstCompleteDefaultNetworkObservation() = runBlocking {
        val network = fakeNetwork()
        val pending = ConnectionSnapshot(
            network = network,
            connectionType = null,
            hasValidatedInternet = false,
            hasTransportObservation = false,
        )
        val harness = Harness(pending)
        val active = async(Dispatchers.Default) {
            MeasurementEngine(harness.dependencies()).takeMeasurement(ConnectionType.WIFI)
        }
        withTimeout(2_000) {
            while (harness.state.subscriptionCount.value == 0) yield()
        }

        harness.state.value = snapshot(network, ConnectionType.WIFI, validated = true)

        val result = active.await()
        assertEquals(MeasurementStatus.COMPLETE, result.status)
        assertEquals(0, harness.state.subscriptionCount.value)
    }

    @Test
    fun unvalidatedOfflineNetworkKeepsRadioDataAndReportsPartialMetrics() = runBlocking {
        val harness = Harness(
            snapshot(fakeNetwork(), ConnectionType.WIFI, validated = false),
            download = { _, budget, _ ->
                ThroughputMeasurement(null, 0L, budget, isPartial = true, issues = listOf(MeasurementIssue(IssueCode.ENDPOINT_FAILURE)))
            },
            upload = { _, budget, _ ->
                ThroughputMeasurement(null, 0L, budget, isPartial = true, issues = listOf(MeasurementIssue(IssueCode.ENDPOINT_FAILURE)))
            },
            latency = {
                LatencyMeasurement(null, null, attemptedRequests = 10, successfulRequests = 0, issues = listOf(MeasurementIssue(IssueCode.ENDPOINT_FAILURE)))
            },
            loss = {
                PacketLossMeasurement(null, PacketLossMethod.UNAVAILABLE, 0, 0, listOf(MeasurementIssue(IssueCode.NO_INTERNET)))
            },
        )

        val result = MeasurementEngine(harness.dependencies()).takeMeasurement(ConnectionType.WIFI)

        assertEquals(MeasurementStatus.PARTIAL, result.status)
        assertEquals(-54, result.signalDbm)
        assertEquals(866, result.linkSpeedMbps)
        assertNull(result.downloadMbps)
        assertNull(result.uploadMbps)
        assertNull(result.latencyMs)
        assertNull(result.packetLossPct)
        assertEquals(PacketLossMethod.UNAVAILABLE, result.packetLossMethod)
        assertEquals(1, harness.downloadCalls.get())
        assertEquals(1, harness.uploadCalls.get())
    }

    @Test
    fun oneProbeFailureDoesNotDiscardCompletedMetrics() = runBlocking {
        val harness = Harness(
            snapshot(fakeNetwork(), ConnectionType.WIFI, validated = true),
            download = { _, budget, _ ->
                ThroughputMeasurement(null, 0L, budget, isPartial = true, issues = listOf(MeasurementIssue(IssueCode.ENDPOINT_FAILURE)))
            },
        )

        val result = MeasurementEngine(harness.dependencies()).takeMeasurement(ConnectionType.WIFI)

        assertEquals(MeasurementStatus.PARTIAL, result.status)
        assertNull(result.downloadMbps)
        assertEquals(31f, result.uploadMbps!!, 0.001f)
        assertEquals(12f, result.latencyMs!!, 0.001f)
        assertEquals(5f, result.packetLossPct!!, 0.001f)
        assertTrue(result.issues.any { it.code == IssueCode.ENDPOINT_FAILURE })
    }

    @Test
    fun networkLossReturnsPartialAndCallerCancellationCleansUp() = runBlocking {
        val firstNetwork = fakeNetwork()
        val replacementNetwork = fakeNetwork()
        val harness = Harness(snapshot(firstNetwork, ConnectionType.WIFI, validated = true))
        val downloadStarted = CountDownLatch(1)
        val downloadCleanedUp = AtomicInteger()
        harness.download = { _, _, _ ->
            downloadStarted.countDown()
            try {
                awaitCancellation()
            } finally {
                downloadCleanedUp.incrementAndGet()
            }
        }
        val state = harness.state
        val active = async(Dispatchers.Default) {
            MeasurementEngine(harness.dependencies()).takeMeasurement(ConnectionType.WIFI)
        }
        assertTrue(downloadStarted.await(2, TimeUnit.SECONDS))
        withTimeout(2_000) {
            while (state.subscriptionCount.value == 0) yield()
        }

        state.value = snapshot(replacementNetwork, ConnectionType.MOBILE, validated = true)
        val result = active.await()

        assertEquals(MeasurementStatus.PARTIAL, result.status)
        assertTrue(result.issues.any { it.code == IssueCode.NETWORK_CHANGED })
        assertEquals(1, downloadCleanedUp.get())
        assertEquals(0, harness.uploadCalls.get())
        assertSame(replacementNetwork, state.value.network)
        assertEquals(0, state.subscriptionCount.value)

        val cancellationHarness = Harness(snapshot(fakeNetwork(), ConnectionType.WIFI, validated = true))
        val requestStarted = CountDownLatch(1)
        val requestClosed = AtomicInteger()
        cancellationHarness.download = { _, _, _ ->
            requestStarted.countDown()
            try {
                awaitCancellation()
            } finally {
                requestClosed.incrementAndGet()
            }
        }
        val cancelled = async(Dispatchers.Default) {
            MeasurementEngine(cancellationHarness.dependencies()).takeMeasurement(ConnectionType.WIFI)
        }
        assertTrue(requestStarted.await(2, TimeUnit.SECONDS))
        withTimeout(2_000) {
            while (cancellationHarness.state.subscriptionCount.value == 0) yield()
        }

        cancelled.cancelAndJoin()

        assertEquals(1, requestClosed.get())
        assertEquals(0, cancellationHarness.state.subscriptionCount.value)
    }

    @Test
    fun sharedTenSecondDeadlineStopsStartingLaterStagesAndKeepsCompletedDownload() = runBlocking {
        val harness = Harness(snapshot(fakeNetwork(), ConnectionType.WIFI, validated = true))
        harness.nowNanos = 1L
        harness.download = { _, budget, _ ->
            harness.nowNanos = 10_000_000_002L
            ThroughputMeasurement(42f, budget, budget, isPartial = false, successfulStreams = 2)
        }

        val result = MeasurementEngine(harness.dependencies()).takeMeasurement(ConnectionType.WIFI)

        assertEquals(MeasurementStatus.PARTIAL, result.status)
        assertEquals(42f, result.downloadMbps!!, 0.001f)
        assertNull(result.uploadMbps)
        assertNull(result.latencyMs)
        assertTrue(result.issues.any { it.code == IssueCode.TIMEOUT })
        assertEquals(0, harness.uploadCalls.get())
        assertEquals(0, harness.latencyCalls.get())
        assertEquals(0, harness.lossCalls.get())
    }

    private fun snapshot(network: Network?, type: ConnectionType?, validated: Boolean) =
        ConnectionSnapshot(network, type, validated)

    private fun fakeNetwork(): Network {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val field = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = field.get(null)
        return unsafeClass.getMethod("allocateInstance", Class::class.java)
            .invoke(unsafe, Network::class.java) as Network
    }

    private class Harness(
        initialSnapshot: ConnectionSnapshot,
        private val permission: (ConnectionType) -> PermissionOutcome = { PermissionOutcome.GRANTED },
        private val wifiRead: (Network) -> RadioSnapshot = {
            RadioSnapshot(signalDbm = -54, linkSpeedMbps = 866, wifiBand = "6", wifiChannel = 1, bssid = "00:11:22:33:44:55")
        },
        private val radioMobile: RadioSnapshot = RadioSnapshot(signalDbm = -91, networkType = "NR"),
        private val latency: () -> LatencyMeasurement = {
            LatencyMeasurement(12f, 2f, attemptedRequests = 10, successfulRequests = 10)
        },
        private val loss: (Boolean) -> PacketLossMeasurement = {
            PacketLossMeasurement(5f, PacketLossMethod.ICMP, 20, 19)
        },
        private val config: MeasurementConfig = MeasurementConfig(maxBytesPerDirection = 100L),
        download: suspend (Network, Long, Long) -> ThroughputMeasurement = { _, budget, _ ->
            ThroughputMeasurement(42f, budget, budget, isPartial = false, successfulStreams = 2)
        },
        upload: suspend (Network, Long, Long) -> ThroughputMeasurement = { _, budget, _ ->
            ThroughputMeasurement(31f, budget, budget, isPartial = false, successfulStreams = 2)
        },
    ) {
        val state = MutableStateFlow(initialSnapshot)
        val downloadCalls = AtomicInteger()
        val uploadCalls = AtomicInteger()
        val latencyCalls = AtomicInteger()
        val lossCalls = AtomicInteger()
        val wifiNetworks = mutableListOf<Network>()
        val capturedNetworks = mutableListOf<Network>()
        var httpReachablePassedToLoss = false
        var nowNanos = 100L
        var download = download
        var upload = upload

        fun totalProbeCalls() = downloadCalls.get() + uploadCalls.get() + latencyCalls.get() + lossCalls.get()

        fun dependencies(): MeasurementDependencies = MeasurementDependencies(
            connectionState = state,
            permissionCheck = permission,
            readWifiRadio = { network ->
                wifiNetworks += network
                wifiRead(network)
            },
            readCellularRadio = { radioMobile },
            measureThroughput = { network, direction, budget, deadline ->
                capturedNetworks += network
                when (direction) {
                    TransferDirection.DOWNLOAD -> {
                        downloadCalls.incrementAndGet()
                        download(network, budget, deadline)
                    }
                    TransferDirection.UPLOAD -> {
                        uploadCalls.incrementAndGet()
                        upload(network, budget, deadline)
                    }
                }
            },
            measureLatency = { network, _, deadline ->
                capturedNetworks += network
                latencyCalls.incrementAndGet()
                latency()
            },
            measurePacketLoss = { network, _, httpReachable, _ ->
                capturedNetworks += network
                httpReachablePassedToLoss = httpReachable
                lossCalls.incrementAndGet()
                loss(httpReachable)
            },
            config = config,
            wallClockMillis = { 1234L },
            monotonicNanos = { nowNanos },
        )
    }
}
