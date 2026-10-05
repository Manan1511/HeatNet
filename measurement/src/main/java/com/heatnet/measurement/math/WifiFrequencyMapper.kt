package com.heatnet.measurement.math

data class WifiFrequencyInfo(
    val band: String,
    val channel: Int,
)

fun mapWifiFrequency(frequencyMhz: Int): WifiFrequencyInfo? = when {
    frequencyMhz in WIFI_24_GHZ_FIRST..WIFI_24_GHZ_LAST &&
        (frequencyMhz - WIFI_24_GHZ_FIRST) % WIFI_CHANNEL_SPACING_MHZ == 0 -> {
        WifiFrequencyInfo(
            band = "2.4",
            channel = (frequencyMhz - WIFI_24_GHZ_CHANNEL_OFFSET) / WIFI_CHANNEL_SPACING_MHZ,
        )
    }

    frequencyMhz == WIFI_24_GHZ_CHANNEL_14_FREQUENCY -> WifiFrequencyInfo(band = "2.4", channel = 14)

    frequencyMhz in WIFI_5_GHZ_FIRST..WIFI_5_GHZ_LAST &&
        (frequencyMhz - WIFI_5_GHZ_CHANNEL_OFFSET) % WIFI_CHANNEL_SPACING_MHZ == 0 -> {
        WifiFrequencyInfo(
            band = "5",
            channel = (frequencyMhz - WIFI_5_GHZ_CHANNEL_OFFSET) / WIFI_CHANNEL_SPACING_MHZ,
        )
    }

    frequencyMhz == WIFI_6_GHZ_CHANNEL_2_FREQUENCY -> WifiFrequencyInfo(band = "6", channel = 2)

    frequencyMhz in WIFI_6_GHZ_FIRST..WIFI_6_GHZ_LAST &&
        (frequencyMhz - WIFI_6_GHZ_CHANNEL_OFFSET) % WIFI_CHANNEL_SPACING_MHZ == 0 -> {
        WifiFrequencyInfo(
            band = "6",
            channel = (frequencyMhz - WIFI_6_GHZ_CHANNEL_OFFSET) / WIFI_CHANNEL_SPACING_MHZ,
        )
    }

    else -> null
}

private const val WIFI_CHANNEL_SPACING_MHZ = 5
private const val WIFI_24_GHZ_FIRST = 2_412
private const val WIFI_24_GHZ_LAST = 2_472
private const val WIFI_24_GHZ_CHANNEL_OFFSET = 2_407
private const val WIFI_24_GHZ_CHANNEL_14_FREQUENCY = 2_484
private const val WIFI_5_GHZ_FIRST = 5_160
private const val WIFI_5_GHZ_LAST = 5_885
private const val WIFI_5_GHZ_CHANNEL_OFFSET = 5_000
private const val WIFI_6_GHZ_CHANNEL_2_FREQUENCY = 5_935
private const val WIFI_6_GHZ_FIRST = 5_955
private const val WIFI_6_GHZ_LAST = 7_115
private const val WIFI_6_GHZ_CHANNEL_OFFSET = 5_950
