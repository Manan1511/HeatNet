package com.heatnet.measurement.radio

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.heatnet.measurement.model.ConnectionType
import com.heatnet.measurement.permissions.PermissionGate
import com.heatnet.measurement.permissions.PermissionOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        val wifi = WifiRadioReader().read(null)
        assertNull(wifi.signalDbm)
        assertTrue(wifi.issues.any { it.code.name == "RADIO_UNAVAILABLE" })

        val mobile = CellularRadioReader(context).read()
        assertTrue(mobile.signalDbm == null || mobile.signalDbm < 0)
        assertTrue(mobile.networkType != null || mobile.signalDbm == null)
    }
}
