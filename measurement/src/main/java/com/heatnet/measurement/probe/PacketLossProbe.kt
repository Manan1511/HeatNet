package com.heatnet.measurement.probe

import android.net.Network
import com.heatnet.measurement.model.IssueCode
import com.heatnet.measurement.model.MeasurementConfig
import com.heatnet.measurement.model.MeasurementIssue
import com.heatnet.measurement.model.PacketLossMethod

data class PacketLossMeasurement(
    val lossPct: Float?,
    val method: PacketLossMethod,
    val attemptedProbes: Int,
    val successfulProbes: Int,
    val issues: List<MeasurementIssue> = emptyList(),
)

class PacketLossProbe internal constructor(
    private val icmpProbe: IcmpProbe,
    private val httpLossProbe: HttpLossProbe,
) {
    constructor() : this(IcmpProbe(), HttpLossProbe())

    internal constructor(icmpProbe: IcmpProbe) : this(icmpProbe, HttpLossProbe())

    suspend fun measure(
        network: Network,
        config: MeasurementConfig,
        httpReachable: Boolean,
        deadlineNanos: Long,
    ): PacketLossMeasurement = measureInternal(config, httpReachable, deadlineNanos) {
        httpLossProbe.measure(network, config, deadlineNanos)
    }

    internal suspend fun measureWithFallback(
        config: MeasurementConfig,
        httpReachable: Boolean,
        deadlineNanos: Long,
        fallback: suspend () -> PacketLossMeasurement,
    ): PacketLossMeasurement = measureInternal(config, httpReachable, deadlineNanos, fallback)

    private suspend fun measureInternal(
        config: MeasurementConfig,
        httpReachable: Boolean,
        deadlineNanos: Long,
        fallback: suspend () -> PacketLossMeasurement,
    ): PacketLossMeasurement {
        val icmp = icmpProbe.measure(config, deadlineNanos)
        if (icmp.method == PacketLossMethod.ICMP && icmp.successfulProbes > 0) return icmp

        if (!httpReachable) {
            return PacketLossMeasurement(
                lossPct = null,
                method = PacketLossMethod.UNAVAILABLE,
                attemptedProbes = icmp.attemptedProbes,
                successfulProbes = icmp.successfulProbes,
                issues = icmp.issues + MeasurementIssue(IssueCode.NO_INTERNET, "HTTP fallback requires an earlier successful HTTP probe"),
            )
        }

        val http = fallback()
        val issues = buildList {
            addAll(icmp.issues)
            if (icmp.method == PacketLossMethod.ICMP && icmp.successfulProbes == 0) {
                add(MeasurementIssue(IssueCode.ICMP_UNAVAILABLE, "ICMP received no replies; HTTP request failures were used as fallback"))
            }
            addAll(http.issues)
        }
        return if (http.method == PacketLossMethod.HTTP_PROBE_FAILURES) {
            http.copy(issues = issues.distinctBy { it.code })
        } else {
            PacketLossMeasurement(
                lossPct = null,
                method = PacketLossMethod.UNAVAILABLE,
                attemptedProbes = http.attemptedProbes,
                successfulProbes = http.successfulProbes,
                issues = issues.distinctBy { it.code },
            )
        }
    }
}
