package com.heatnet.measurement.radio

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import com.heatnet.measurement.model.IssueCode
import com.heatnet.measurement.model.MeasurementIssue
import com.heatnet.measurement.math.mapWifiFrequency

internal data class WifiRadioData(
    val rssiDbm: Int,
    val receiveLinkSpeedMbps: Int,
    val frequencyMhz: Int,
    val bssid: String?,
)

internal fun interface WifiRadioSource {
    fun read(network: Network): WifiRadioData?
}

class WifiRadioReader private constructor(
    private val source: WifiRadioSource,
) {
    constructor(context: Context) : this(AndroidWifiRadioSource(context.applicationContext))

    fun read(network: Network): RadioSnapshot = WifiRadioSnapshotMapper.map(source.read(network))

    companion object {
        const val INVALID_RSSI = -127
        const val MASKED_BSSID = "02:00:00:00:00:00"
    }
}

internal object WifiRadioSnapshotMapper {
    fun map(data: WifiRadioData?): RadioSnapshot {
        if (data == null) {
            return RadioSnapshot(
                issues = listOf(MeasurementIssue(IssueCode.RADIO_UNAVAILABLE, "Wi-Fi radio data is unavailable")),
            )
        }

        val frequency = mapWifiFrequency(data.frequencyMhz)
        val rssi = data.rssiDbm.takeIf { it in -126..-1 }
        val linkSpeed = data.receiveLinkSpeedMbps.takeIf { it > 0 }
        val bssid = data.bssid
            ?.takeIf { it.isNotBlank() && !it.equals(WifiRadioReader.MASKED_BSSID, ignoreCase = true) }
        val fieldsRedacted = data.rssiDbm == WifiRadioReader.INVALID_RSSI ||
            data.receiveLinkSpeedMbps <= 0 ||
            data.frequencyMhz <= 0 ||
            data.bssid.isNullOrBlank() ||
            data.bssid.equals(WifiRadioReader.MASKED_BSSID, ignoreCase = true)
        val issues = buildList {
            if (fieldsRedacted) {
                add(MeasurementIssue(IssueCode.FIELD_REDACTED, "One or more Wi-Fi radio fields were unavailable or redacted"))
            }
            if ((rssi == null && data.rssiDbm != WifiRadioReader.INVALID_RSSI) ||
                (data.frequencyMhz > 0 && frequency == null)
            ) {
                add(MeasurementIssue(IssueCode.RADIO_UNAVAILABLE, "A Wi-Fi radio field had an unsupported value"))
            }
        }

        return RadioSnapshot(
            signalDbm = rssi,
            linkSpeedMbps = linkSpeed,
            wifiBand = frequency?.band,
            wifiChannel = frequency?.channel,
            bssid = bssid,
            issues = issues,
        )
    }
}

private class AndroidWifiRadioSource(context: Context) : WifiRadioSource {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun read(network: Network): WifiRadioData? = runCatching {
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return null
        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null
        val wifiInfo = capabilities.transportInfo as? WifiInfo ?: return null
        WifiRadioData(
            rssiDbm = wifiInfo.rssi,
            receiveLinkSpeedMbps = wifiInfo.rxLinkSpeedMbps,
            frequencyMhz = wifiInfo.frequency,
            bssid = wifiInfo.bssid,
        )
    }.getOrNull()
}
