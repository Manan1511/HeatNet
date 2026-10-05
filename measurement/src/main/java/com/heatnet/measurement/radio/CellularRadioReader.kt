package com.heatnet.measurement.radio

import android.content.Context
import android.content.pm.PackageManager
import android.telephony.CellSignalStrength
import android.telephony.CellInfo
import android.telephony.CellSignalStrengthCdma
import android.telephony.CellSignalStrengthGsm
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.CellSignalStrengthTdscdma
import android.telephony.CellSignalStrengthWcdma
import android.telephony.SignalStrength
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import com.heatnet.measurement.model.IssueCode
import com.heatnet.measurement.model.MeasurementIssue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal enum class CellularTechnology {
    GSM,
    CDMA,
    WCDMA,
    TD_SCDMA,
    LTE,
    NR,
    UNKNOWN,
}

internal enum class CellularDisplayOverride {
    NONE,
    NR_NSA,
    NR_ADVANCED,
}

internal data class CellularSignalSample(
    val technology: CellularTechnology,
    val dbm: Int?,
)

internal data class CellularRadioData(
    val dataTechnology: CellularTechnology,
    val signalSamples: List<CellularSignalSample>,
    val displayOverride: CellularDisplayOverride = CellularDisplayOverride.NONE,
)

internal interface CellularRadioSource {
    fun activeDataSubscriptionId(): Int?
    fun readForSubscription(subscriptionId: Int): CellularRadioData?
}

class CellularRadioReader private constructor(
    private val source: CellularRadioSource,
) {
    constructor(context: Context) : this(AndroidCellularRadioSource(context.applicationContext))

    fun read(): RadioSnapshot {
        val subscriptionId = runCatching { source.activeDataSubscriptionId() }.getOrNull()
            ?: return unavailable("The active data subscription is unavailable")
        val radio = runCatching { source.readForSubscription(subscriptionId) }.getOrNull()
            ?: return unavailable("Cellular radio data is unavailable")

        val matchingTechnology = radio.dataTechnology
        val signal = radio.signalSamples
            .asSequence()
            .filter { it.technology == matchingTechnology }
            .mapNotNull { it.dbm?.takeIf { dbm -> dbm in -200..-1 } }
            .maxOrNull()
        val networkType = when (radio.dataTechnology) {
            CellularTechnology.GSM -> "GSM"
            CellularTechnology.CDMA -> "CDMA"
            CellularTechnology.WCDMA -> "WCDMA"
            CellularTechnology.TD_SCDMA -> "TD-SCDMA"
            CellularTechnology.LTE -> when (radio.displayOverride) {
                CellularDisplayOverride.NR_NSA -> "NR-NSA"
                CellularDisplayOverride.NR_ADVANCED -> "NR-ADVANCED"
                CellularDisplayOverride.NONE -> "LTE"
            }
            CellularTechnology.NR -> "NR"
            CellularTechnology.UNKNOWN -> null
        }

        return RadioSnapshot(
            signalDbm = signal,
            networkType = networkType,
            issues = if (signal == null || networkType == null) {
                listOf(MeasurementIssue(IssueCode.RADIO_UNAVAILABLE, "Matching cellular signal data is unavailable"))
            } else {
                emptyList()
            },
        )
    }

    private fun unavailable(detail: String) =
        RadioSnapshot(issues = listOf(MeasurementIssue(IssueCode.RADIO_UNAVAILABLE, detail)))

    companion object {
        internal fun fromSource(source: CellularRadioSource) = CellularRadioReader(source)
    }
}

private class AndroidCellularRadioSource(context: Context) : CellularRadioSource {
    private val packageManager = context.packageManager
    private val telephonyManager = context.getSystemService(TelephonyManager::class.java)

    override fun activeDataSubscriptionId(): Int? {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_SUBSCRIPTION)) return null
        return runCatching { SubscriptionManager.getActiveDataSubscriptionId() }
            .getOrNull()
            ?.takeIf { it != SubscriptionManager.INVALID_SUBSCRIPTION_ID }
    }

    override fun readForSubscription(subscriptionId: Int): CellularRadioData? {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_RADIO_ACCESS)) return null
        val subscriptionManager = telephonyManager ?: return null
        return runCatching {
            val manager = subscriptionManager.createForSubscriptionId(subscriptionId)
            val networkType = manager.dataNetworkType
            val signalStrength = manager.signalStrength
            CellularRadioData(
                dataTechnology = networkType.toCellularTechnology(),
                signalSamples = signalStrength.toSamples(),
                displayOverride = if (networkType == TelephonyManager.NETWORK_TYPE_LTE) {
                    manager.readNrDisplayOverride()
                } else {
                    CellularDisplayOverride.NONE
                },
            )
        }.getOrNull()
    }
}

private fun Int.toCellularTechnology(): CellularTechnology = when (this) {
    TelephonyManager.NETWORK_TYPE_GSM,
    TelephonyManager.NETWORK_TYPE_GPRS,
    TelephonyManager.NETWORK_TYPE_EDGE,
    TelephonyManager.NETWORK_TYPE_IDEN -> CellularTechnology.GSM
    TelephonyManager.NETWORK_TYPE_CDMA,
    TelephonyManager.NETWORK_TYPE_1xRTT,
    TelephonyManager.NETWORK_TYPE_EVDO_0,
    TelephonyManager.NETWORK_TYPE_EVDO_A,
    TelephonyManager.NETWORK_TYPE_EVDO_B,
    TelephonyManager.NETWORK_TYPE_EHRPD -> CellularTechnology.CDMA
    TelephonyManager.NETWORK_TYPE_UMTS,
    TelephonyManager.NETWORK_TYPE_HSDPA,
    TelephonyManager.NETWORK_TYPE_HSUPA,
    TelephonyManager.NETWORK_TYPE_HSPA,
    TelephonyManager.NETWORK_TYPE_HSPAP -> CellularTechnology.WCDMA
    TelephonyManager.NETWORK_TYPE_TD_SCDMA -> CellularTechnology.TD_SCDMA
    TelephonyManager.NETWORK_TYPE_LTE -> CellularTechnology.LTE
    TelephonyManager.NETWORK_TYPE_NR -> CellularTechnology.NR
    else -> CellularTechnology.UNKNOWN
}

private fun SignalStrength?.toSamples(): List<CellularSignalSample> {
    if (this == null) return emptyList()
    val allSamples = getCellSignalStrengths()
    return allSamples.mapNotNull { signal ->
        val technology = when (signal) {
            is CellSignalStrengthGsm -> CellularTechnology.GSM
            is CellSignalStrengthCdma -> CellularTechnology.CDMA
            is CellSignalStrengthWcdma -> CellularTechnology.WCDMA
            is CellSignalStrengthTdscdma -> CellularTechnology.TD_SCDMA
            is CellSignalStrengthLte -> CellularTechnology.LTE
            is CellSignalStrengthNr -> CellularTechnology.NR
            else -> null
        } ?: return@mapNotNull null
        CellularSignalSample(technology, signal.dbm.takeUnless { it == CellInfo.UNAVAILABLE })
    }
}

private fun TelephonyManager.readNrDisplayOverride(): CellularDisplayOverride {
    val received = CountDownLatch(1)
    val override = java.util.concurrent.atomic.AtomicReference(CellularDisplayOverride.NONE)
    val executor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "HeatNet-display-info") }
    val callback = object : TelephonyCallback(), TelephonyCallback.DisplayInfoListener {
        override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
            override.set(
                when (telephonyDisplayInfo.overrideNetworkType) {
                    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA -> CellularDisplayOverride.NR_NSA
                    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED -> CellularDisplayOverride.NR_ADVANCED
                    else -> CellularDisplayOverride.NONE
                },
            )
            received.countDown()
        }
    }

    try {
        registerTelephonyCallback(executor, callback)
        received.await(DISPLAY_INFO_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        return override.get()
    } catch (_: RuntimeException) {
        return CellularDisplayOverride.NONE
    } catch (_: InterruptedException) {
        Thread.currentThread().interrupt()
        return CellularDisplayOverride.NONE
    } finally {
        runCatching { unregisterTelephonyCallback(callback) }
        executor.shutdownNow()
    }
}

private const val DISPLAY_INFO_TIMEOUT_MILLIS = 300L
