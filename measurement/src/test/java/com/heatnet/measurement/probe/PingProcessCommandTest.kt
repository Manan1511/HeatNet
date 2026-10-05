package com.heatnet.measurement.probe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PingProcessCommandTest {
    @Test
    fun bindsToCapturedInterfaceAndUsesRequestedCountAsItsOnlyPingLimit() {
        val command = buildPingCommand(
            route = IcmpRoute("192.0.2.1", "wlan0"),
            count = 2,
            intervalMillis = 200,
        )

        assertTrue(command.contains("-I"))
        assertEquals("wlan0", command[command.indexOf("-I") + 1])
        assertEquals("2", command[command.indexOf("-c") + 1])
        assertFalse("-w can override ping's requested count", command.contains("-w"))
        assertEquals("192.0.2.1", command.last())
    }
}
