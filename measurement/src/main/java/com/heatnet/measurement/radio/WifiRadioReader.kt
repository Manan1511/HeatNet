package com.heatnet.measurement.radio

import android.net.wifi.WifiInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.heatnet.measurement.model.IssueCode
import com.heatnet.measurement.model.MeasurementIssue
import com.heatnet.measurement.math.mapWifiFrequency
import com.heatnet.measurement.network.sameNetworkHandle
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

internal data class WifiRadioData(
    val rssiDbm: Int,
    val receiveLinkSpeedMbps: Int,
    val frequencyMhz: Int,
    val bssid: String?,
)

/** Maps location-aware [WifiInfo] from the network-specific callback used for each reading. */
class WifiRadioReader {
    fun read(callbackWifiInfo: WifiInfo?): RadioSnapshot = WifiRadioSnapshotMapper.map(
        runCatching {
            callbackWifiInfo?.let { info ->
                WifiRadioData(
                    rssiDbm = info.rssi,
                    receiveLinkSpeedMbps = info.rxLinkSpeedMbps,
                    frequencyMhz = info.frequency,
                    bssid = info.bssid,
                )
            }
        }.getOrNull(),
    )

    /**
     * Registers a location-aware callback for the captured Wi-Fi network and reads its first fresh
     * capabilities event. Synchronous `getNetworkCapabilities` redacts `WifiInfo` on modern Android.
     */
    suspend fun readForNetwork(connectivityManager: ConnectivityManager, capturedNetwork: Network): RadioSnapshot =
        suspendCancellableCoroutine { continuation ->
            val finished = AtomicBoolean(false)
            val callbackRef = AtomicReference<ConnectivityManager.NetworkCallback?>()
            fun unregister() {
                callbackRef.get()?.let { callback ->
                    runCatching { connectivityManager.unregisterNetworkCallback(callback) }
                }
            }
            fun finish(snapshot: RadioSnapshot) {
                if (!finished.compareAndSet(false, true)) return
                unregister()
                if (continuation.isActive) continuation.resume(snapshot)
            }

            val callback = object : ConnectivityManager.NetworkCallback(
                ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO,
            ) {
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    if (!sameNetworkHandle(capturedNetwork, network)) return
                    val snapshot = runCatching {
                        val info = networkCapabilities.transportInfo as? WifiInfo
                        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) read(info) else read(null)
                    }.getOrElse { read(null) }
                    finish(snapshot)
                }

                override fun onLost(network: Network) {
                    if (sameNetworkHandle(capturedNetwork, network)) finish(read(null))
                }
            }
            callbackRef.set(callback)
            continuation.invokeOnCancellation {
                if (finished.compareAndSet(false, true)) unregister()
            }

            if (!continuation.isActive) {
                unregister()
                return@suspendCancellableCoroutine
            }
            try {
                connectivityManager.registerNetworkCallback(
                    NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),
                    callback,
                )
                if (!continuation.isActive) unregister()
            } catch (_: SecurityException) {
                finish(read(null))
            } catch (_: RuntimeException) {
                finish(read(null))
            }
        }

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
