package com.heatnet.measurement.radio

import android.content.Context
import android.net.ConnectivityManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.heatnet.measurement.model.ConnectionType
import com.heatnet.measurement.permissions.PermissionGate
import com.heatnet.measurement.permissions.PermissionOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RadioReadersPlatformTest {
    @Test
    fun permissionGateUsesFineLocationOnlyForWifi() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val actualPermission = context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
        val expectedWifiOutcome = if (actualPermission == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            PermissionOutcome.GRANTED
        } else {
            PermissionOutcome.DENIED
        }

        assertEquals(expectedWifiOutcome, PermissionGate(context).check(ConnectionType.WIFI))
        assertEquals(PermissionOutcome.GRANTED, PermissionGate(context).check(ConnectionType.MOBILE))
    }

    @Test
    fun platformReadersReturnSafeSnapshotsOnApi36() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivity.activeNetwork
        if (network != null) {
            val wifi = WifiRadioReader(context).read(network)
            assertTrue(wifi.signalDbm == null || wifi.signalDbm in -127..0)
            assertTrue(wifi.linkSpeedMbps == null || wifi.linkSpeedMbps > 0)
        }

        val mobile = CellularRadioReader(context).read()
        assertTrue(mobile.signalDbm == null || mobile.signalDbm < 0)
        assertTrue(mobile.networkType != null || mobile.signalDbm == null)
    }
}
