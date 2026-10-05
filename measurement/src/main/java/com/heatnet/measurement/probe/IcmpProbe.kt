package com.heatnet.measurement.probe

import com.heatnet.measurement.math.calculateLossPercentage
import com.heatnet.measurement.model.IssueCode
import com.heatnet.measurement.model.MeasurementConfig
import com.heatnet.measurement.model.MeasurementIssue
import com.heatnet.measurement.model.PacketLossMethod
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible

class IcmpProbe private constructor(
    private val pingProcess: PingProcess,
    private val clock: ProbeClock,
    @Suppress("UNUSED_PARAMETER") marker: Unit,
) {
    constructor() : this(AndroidPingProcess(), SystemProbeClock, Unit)

    internal constructor(pingProcess: PingProcess) : this(pingProcess, SystemProbeClock, Unit)

    internal constructor(pingProcess: PingProcess, clock: ProbeClock) : this(pingProcess, clock, Unit)

    suspend fun measure(config: MeasurementConfig, deadlineNanos: Long): PacketLossMeasurement {
        if (config.icmpProbeCount <= 0) return unavailable("ICMP probe count is not positive")
        val remainingNanos = deadlineNanos - clock.nowNanos()
        if (remainingNanos <= 0L) return unavailable("ICMP probe deadline has expired", IssueCode.TIMEOUT)

        val execution = try {
            runInterruptible(Dispatchers.IO) {
                pingProcess.run(
                    host = config.icmpHost,
                    count = config.icmpProbeCount,
                    intervalMillis = ICMP_INTERVAL_MILLIS,
                    timeoutMillis = (remainingNanos / NANOS_PER_MILLISECOND).coerceAtLeast(1L),
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            return unavailable("ICMP process could not run")
        } catch (_: RuntimeException) {
            return unavailable("ICMP process could not run")
        }

        if (!execution.started) return unavailable("ICMP process is unavailable")
        val summary = PingSummaryParser.parse(execution.output)
            ?: return unavailable("ICMP process returned an unreadable summary")
        if (summary.transmitted > config.icmpProbeCount) {
            return unavailable("ICMP process returned more probes than requested")
        }

        val loss = calculateLossPercentage(summary.transmitted - summary.received, summary.transmitted)
            ?: return unavailable("ICMP process returned no attempted probes")
        val issues = buildList {
            if (execution.timedOut || summary.transmitted < config.icmpProbeCount) {
                add(MeasurementIssue(IssueCode.TIMEOUT, "ICMP probes stopped before the requested count completed"))
            }
        }
        return PacketLossMeasurement(
            lossPct = loss,
            method = PacketLossMethod.ICMP,
            attemptedProbes = summary.transmitted,
            successfulProbes = summary.received,
            issues = issues,
        )
    }

    private fun unavailable(detail: String, code: IssueCode = IssueCode.ICMP_UNAVAILABLE) =
        PacketLossMeasurement(
            lossPct = null,
            method = PacketLossMethod.UNAVAILABLE,
            attemptedProbes = 0,
            successfulProbes = 0,
            issues = listOf(MeasurementIssue(code, detail)),
        )
}

private const val ICMP_INTERVAL_MILLIS = 200
private const val NANOS_PER_MILLISECOND = 1_000_000L
