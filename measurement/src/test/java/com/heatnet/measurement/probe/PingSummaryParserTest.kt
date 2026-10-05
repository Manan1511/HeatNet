package com.heatnet.measurement.probe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PingSummaryParserTest {
    @Test
    fun parsesAndroidPingSummary() {
        val output = """
            PING speed.cloudflare.com (104.16.132.229): 56 data bytes
            64 bytes from 104.16.132.229: seq=0 ttl=57 time=12.3 ms
            --- speed.cloudflare.com ping statistics ---
            20 packets transmitted, 18 received, 10% packet loss, time 19016ms
            rtt min/avg/max/mdev = 11.2/12.1/13.0/0.4 ms
        """.trimIndent()

        assertEquals(PingSummary(transmitted = 20, received = 18), PingSummaryParser.parse(output))
    }

    @Test
    fun parsesZeroReplySummaryAndRejectsMalformedOrEmptyOutput() {
        assertEquals(
            PingSummary(transmitted = 20, received = 0),
            PingSummaryParser.parse("20 packets transmitted, 0 packets received, 100% packet loss"),
        )
        assertNull(PingSummaryParser.parse("ping: not found"))
        assertNull(PingSummaryParser.parse(""))
        assertNull(PingSummaryParser.parse("0 packets transmitted, 0 received, 0% packet loss"))
        assertNull(PingSummaryParser.parse("5 packets transmitted, 8 received, 0% packet loss"))
    }
}
