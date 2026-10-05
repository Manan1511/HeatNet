package com.heatnet.measurement.radio

import com.heatnet.measurement.model.IssueCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiRadioReaderTest {
    @Test
    fun redactedFieldsBecomeNull() {
        val snapshot = WifiRadioSnapshotMapper.map(
            WifiRadioData(
                rssiDbm = WifiRadioReader.INVALID_RSSI,
                receiveLinkSpeedMbps = 0,
                frequencyMhz = 0,
                bssid = WifiRadioReader.MASKED_BSSID,
            ),
        )

        assertNull(snapshot.signalDbm)
        assertNull(snapshot.linkSpeedMbps)
        assertNull(snapshot.wifiBand)
        assertNull(snapshot.wifiChannel)
        assertNull(snapshot.bssid)
        assertTrue(snapshot.issues.any { it.code == IssueCode.FIELD_REDACTED })
    }

    @Test
    fun maps24FiveAndSixGhzBandsAndChannels() {
        val twoGhz = WifiRadioSnapshotMapper.map(WifiRadioData(-48, 600, 2412, "11:22:33:44:55:66"))
        assertEquals("2.4", twoGhz.wifiBand)
        assertEquals(1, twoGhz.wifiChannel)

        val fiveGhz = WifiRadioSnapshotMapper.map(WifiRadioData(-51, 780, 5180, "11:22:33:44:55:66"))
        assertEquals("5", fiveGhz.wifiBand)
        assertEquals(36, fiveGhz.wifiChannel)

        val sixGhz = WifiRadioSnapshotMapper.map(WifiRadioData(-55, 960, 5955, "11:22:33:44:55:66"))
        assertEquals("6", sixGhz.wifiBand)
        assertEquals(1, sixGhz.wifiChannel)
    }

    @Test
    fun invalidRssiLinkAndUnknownFrequencyBecomeNull() {
        val snapshot = WifiRadioSnapshotMapper.map(
            WifiRadioData(rssiDbm = 9, receiveLinkSpeedMbps = -1, frequencyMhz = 900, bssid = "11:22:33:44:55:66"),
        )

        assertNull(snapshot.signalDbm)
        assertNull(snapshot.linkSpeedMbps)
        assertNull(snapshot.wifiBand)
        assertNull(snapshot.wifiChannel)
        assertEquals("11:22:33:44:55:66", snapshot.bssid)
    }

    @Test
    fun missingWifiInfoReturnsUnavailableFields() {
        val snapshot = WifiRadioSnapshotMapper.map(null)

        assertNull(snapshot.signalDbm)
        assertTrue(snapshot.issues.any { it.code == IssueCode.RADIO_UNAVAILABLE })
    }

}
