package com.heatnet.measurement.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WifiFrequencyMapperTest {

    @Test
    fun maps24GhzBandChannelsCorrectly() {
        // Channel 1
        assertEquals(WifiFrequencyInfo(band = "2.4", channel = 1), mapWifiFrequency(2412))
        // Channel 6
        assertEquals(WifiFrequencyInfo(band = "2.4", channel = 6), mapWifiFrequency(2437))
        // Channel 13 (last standard channel)
        assertEquals(WifiFrequencyInfo(band = "2.4", channel = 13), mapWifiFrequency(2472))
        // Channel 14 (special frequency)
        assertEquals(WifiFrequencyInfo(band = "2.4", channel = 14), mapWifiFrequency(2484))
    }

    @Test
    fun maps5GhzBandChannelsCorrectly() {
        // First 5 GHz channel (channel 32)
        assertEquals(WifiFrequencyInfo(band = "5", channel = 32), mapWifiFrequency(5160))
        // Channel 36
        assertEquals(WifiFrequencyInfo(band = "5", channel = 36), mapWifiFrequency(5180))
        // Channel 149
        assertEquals(WifiFrequencyInfo(band = "5", channel = 149), mapWifiFrequency(5745))
        // Last 5 GHz channel (channel 177)
        assertEquals(WifiFrequencyInfo(band = "5", channel = 177), mapWifiFrequency(5885))
    }

    @Test
    fun maps6GhzBandChannelsCorrectly() {
        // Channel 2 (special frequency)
        assertEquals(WifiFrequencyInfo(band = "6", channel = 2), mapWifiFrequency(5935))
        // First 6 GHz channel (channel 1)
        assertEquals(WifiFrequencyInfo(band = "6", channel = 1), mapWifiFrequency(5955))
        // Channel 31
        assertEquals(WifiFrequencyInfo(band = "6", channel = 31), mapWifiFrequency(6105))
        // Last 6 GHz channel (channel 233)
        assertEquals(WifiFrequencyInfo(band = "6", channel = 233), mapWifiFrequency(7115))
    }

    @Test
    fun returnsNullForFrequenciesNotAlignedTo5MhzChannelSpacing() {
        // Unaligned frequency within 2.4 GHz range
        assertNull(mapWifiFrequency(2413))
        assertNull(mapWifiFrequency(2414))
        // Unaligned frequency within 5 GHz range
        assertNull(mapWifiFrequency(5161))
        assertNull(mapWifiFrequency(5182))
        // Unaligned frequency within 6 GHz range
        assertNull(mapWifiFrequency(5956))
        assertNull(mapWifiFrequency(7114))
    }

    @Test
    fun returnsNullForOutOfRangeOrGapFrequencies() {
        assertNull(mapWifiFrequency(0))
        assertNull(mapWifiFrequency(-2412))

        // Below 2.4 GHz
        assertNull(mapWifiFrequency(2411))
        // Between 2.4 GHz channel 13 and channel 14
        assertNull(mapWifiFrequency(2473))
        assertNull(mapWifiFrequency(2480))
        // Above 2.4 GHz channel 14 and below 5 GHz
        assertNull(mapWifiFrequency(2485))
        assertNull(mapWifiFrequency(3000))
        assertNull(mapWifiFrequency(5000))
        assertNull(mapWifiFrequency(5159))

        // Between 5 GHz and 6 GHz
        assertNull(mapWifiFrequency(5886))
        assertNull(mapWifiFrequency(5900))
        assertNull(mapWifiFrequency(5934))
        assertNull(mapWifiFrequency(5936))
        assertNull(mapWifiFrequency(5954))

        // Above 6 GHz range
        assertNull(mapWifiFrequency(7116))
        assertNull(mapWifiFrequency(8000))
    }
}
