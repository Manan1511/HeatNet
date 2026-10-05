package com.heatnet

import android.Manifest
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.heatnet.measurement.engine.MeasurementDependencies
import com.heatnet.measurement.engine.MeasurementEngine
import com.heatnet.measurement.model.ConnectionType
import com.heatnet.measurement.model.MeasurementConfig
import com.heatnet.measurement.model.MeasurementResult
import com.heatnet.measurement.model.MeasurementStatus
import com.heatnet.measurement.model.PacketLossMethod
import com.heatnet.measurement.network.ConnectionSnapshot
import com.heatnet.measurement.permissions.PermissionOutcome
import com.heatnet.measurement.probe.LatencyMeasurement
import com.heatnet.measurement.probe.PacketLossMeasurement
import com.heatnet.measurement.probe.ThroughputMeasurement
import com.heatnet.measurement.probe.TransferDirection
import com.heatnet.measurement.radio.RadioSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MeasurementManifestIntegrationTest {
    @Test
    fun libraryManifestDeclaresMeasurementPermissions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
        )
        val requestedPermissions = packageInfo.requestedPermissions.orEmpty().toSet()

        listOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_BASIC_PHONE_STATE,
        ).forEach { permission ->
            assertTrue("Missing merged permission: $permission", permission in requestedPermissions)
        }
        assertFalse(Manifest.permission.READ_PHONE_STATE in requestedPermissions)
    }

    @Test
    fun appConsumesMeasurementLibraryAndKeepsApprovedResultLabels() = runBlocking {
        assertNotNull(MeasurementEngine::class.java)
        val dependencies = MeasurementDependencies(
            connectionState = MutableStateFlow(ConnectionSnapshot(null, null, hasValidatedInternet = false)),
            permissionCheck = { PermissionOutcome.GRANTED },
            readCellularRadio = { RadioSnapshot() },
            measureThroughput = { _, _, budget, _ -> ThroughputMeasurement(null, 0L, budget, isPartial = true) },
            measureLatency = { _, _: MeasurementConfig, _ -> LatencyMeasurement(null, null, 0, 0) },
            measurePacketLoss = { _, _, _, _ -> PacketLossMeasurement(null, PacketLossMethod.UNAVAILABLE, 0, 0) },
        )

        val blocked = MeasurementEngine(dependencies).takeMeasurement(ConnectionType.WIFI)
        assertEquals(MeasurementStatus.BLOCKED, blocked.status)

        val handoffExample = MeasurementResult(
            expectedConnectionType = ConnectionType.WIFI,
            observedConnectionType = ConnectionType.WIFI,
            measuredAtEpochMillis = 0L,
            status = MeasurementStatus.PARTIAL,
            packetLossMethod = PacketLossMethod.HTTP_PROBE_FAILURES,
            wifiBand = "6",
        )
        assertEquals(PacketLossMethod.HTTP_PROBE_FAILURES, handoffExample.packetLossMethod)
        assertEquals("6", handoffExample.wifiBand)
    }
}
