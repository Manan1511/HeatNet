package com.heatnet.measurement.permissions

import com.heatnet.measurement.model.ConnectionType
import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionGateTest {
    @Test
    fun wifiDeniedBlocksButMobileDoesNot() {
        val gate = PermissionGate.fromChecker { false }

        assertEquals(PermissionOutcome.DENIED, gate.check(ConnectionType.WIFI))
        assertEquals(PermissionOutcome.GRANTED, gate.check(ConnectionType.MOBILE))
    }

    @Test
    fun wifiPassesWhenFineLocationIsGranted() {
        val gate = PermissionGate.fromChecker { true }

        assertEquals(PermissionOutcome.GRANTED, gate.check(ConnectionType.WIFI))
    }
}
