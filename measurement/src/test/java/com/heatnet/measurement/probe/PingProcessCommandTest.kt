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
        assertEquals("--", command[command.size - 2])
        assertEquals("192.0.2.1", command.last())
    }

    @Test
    fun separatesHostAddressWithOptionDelimiterToPreventArgumentInjection() {
        val command = buildPingCommand(
            route = IcmpRoute("-4", "wlan0"),
            count = 1,
            intervalMillis = 200,
        )

        val delimiterIndex = command.indexOf("--")
        assertTrue("Command should contain '--' option delimiter", delimiterIndex != -1)
        assertEquals("-4", command[delimiterIndex + 1])
    }
}
