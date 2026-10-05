package com.heatnet.measurement.probe

import android.content.Context
import android.net.Network
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
    private val routeResolver: IcmpRouteResolver?,
    @Suppress("UNUSED_PARAMETER") marker: Unit,
) {
    constructor() : this(AndroidPingProcess(), SystemProbeClock, null, Unit)

    constructor(context: Context) : this(
        AndroidPingProcess(),
        SystemProbeClock,
        AndroidIcmpRouteResolver(context.applicationContext),
        Unit,
    )

    internal constructor(pingProcess: PingProcess) : this(pingProcess, SystemProbeClock, null, Unit)

    internal constructor(pingProcess: PingProcess, clock: ProbeClock) : this(pingProcess, clock, null, Unit)

    internal constructor(
        pingProcess: PingProcess,
        clock: ProbeClock,
        routeResolver: IcmpRouteResolver,
    ) : this(pingProcess, clock, routeResolver, Unit)

    suspend fun measure(network: Network, config: MeasurementConfig, deadlineNanos: Long): PacketLossMeasurement =
        measureWithRoute(config, deadlineNanos) {
            routeResolver?.resolve(network, config.icmpHost)
        }

    internal suspend fun measure(config: MeasurementConfig, deadlineNanos: Long): PacketLossMeasurement =
        measureWithRoute(config, deadlineNanos) {
            // Unit tests that exercise parsing/process behavior use loopback as an explicit test route.
            IcmpRoute(config.icmpHost, "lo")
        }

    private suspend fun measureWithRoute(
        config: MeasurementConfig,
        deadlineNanos: Long,
        resolveRoute: () -> IcmpRoute?,
    ): PacketLossMeasurement {
        if (config.icmpProbeCount <= 0) return unavailable("ICMP probe count is not positive")
        val remainingNanos = deadlineNanos - clock.nowNanos()
        if (remainingNanos <= 0L) return unavailable("ICMP probe deadline has expired", IssueCode.TIMEOUT)
        val remainingMillis = (remainingNanos / NANOS_PER_MILLISECOND).coerceAtLeast(1L)
        val countBoundMillis = config.icmpProbeCount.toLong() * ICMP_INTERVAL_MILLIS + ICMP_COMPLETION_BUFFER_MILLIS
        val processTimeoutMillis = minOf(remainingMillis, countBoundMillis, MAX_ICMP_PROCESS_MILLIS).coerceAtLeast(1L)

        val execution = try {
            runInterruptible(Dispatchers.IO) {
                val route = resolveRoute() ?: return@runInterruptible null
                pingProcess.run(
                    route = route,
                    count = config.icmpProbeCount,
                    intervalMillis = ICMP_INTERVAL_MILLIS,
                    timeoutMillis = processTimeoutMillis,
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            return unavailable("ICMP process or captured network route could not run")
        } catch (_: RuntimeException) {
            return unavailable("ICMP process could not run")
        }

        if (execution == null) return unavailable("ICMP route for the captured network is unavailable")
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
private const val ICMP_COMPLETION_BUFFER_MILLIS = 500L
private const val MAX_ICMP_PROCESS_MILLIS = 4_500L
