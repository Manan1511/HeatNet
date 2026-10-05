package com.heatnet.measurement.probe

import android.net.Network
import com.heatnet.measurement.math.calculateLossPercentage
import com.heatnet.measurement.model.IssueCode
import com.heatnet.measurement.model.MeasurementConfig
import com.heatnet.measurement.model.MeasurementIssue
import com.heatnet.measurement.model.PacketLossMethod
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okhttp3.Request

class HttpLossProbe private constructor(
    private val clientFactory: NetworkBoundHttpClientFactory,
    private val clock: ProbeClock,
) {
    constructor() : this(NetworkBoundHttpClientFactory(), SystemProbeClock)

    internal constructor(clock: ProbeClock) : this(NetworkBoundHttpClientFactory(), clock)

    suspend fun measure(network: Network, config: MeasurementConfig, deadlineNanos: Long): PacketLossMeasurement =
        measureWithClient(clientFactory.create(network), config, deadlineNanos)

    internal suspend fun measureWithClient(
        client: OkHttpClient,
        config: MeasurementConfig,
        deadlineNanos: Long,
    ): PacketLossMeasurement {
        var attempted = 0
        var successful = 0
        val issues = mutableListOf<MeasurementIssue>()

        for (probeIndex in 0 until config.httpFallbackProbeCount.coerceAtLeast(0)) {
            if (clock.nowNanos() >= deadlineNanos) {
                addIssueOnce(issues, MeasurementIssue(IssueCode.TIMEOUT, "HTTP fallback reached the measurement deadline"))
                break
            }

            val request = try {
                Request.Builder().url(withByteCount(config.latencyUrl, 0L)).get().build()
            } catch (_: IllegalArgumentException) {
                addIssueOnce(issues, MeasurementIssue(IssueCode.ENDPOINT_FAILURE, "HTTP fallback endpoint is invalid"))
                break
            }
            attempted += 1
            try {
                val succeeded = client.newCall(request).awaitResponse(deadlineNanos) { response -> response.isSuccessful }
                if (succeeded) {
                    successful += 1
                } else {
                    addIssueOnce(issues, MeasurementIssue(IssueCode.ENDPOINT_FAILURE, "HTTP fallback endpoint returned a non-success status"))
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                addIssueOnce(issues, error.toMeasurementIssue(System.nanoTime(), deadlineNanos))
            }
        }

        if (attempted == 0) {
            return PacketLossMeasurement(
                lossPct = null,
                method = PacketLossMethod.UNAVAILABLE,
                attemptedProbes = 0,
                successfulProbes = 0,
                issues = issues,
            )
        }
        return PacketLossMeasurement(
            lossPct = calculateLossPercentage(attempted - successful, attempted),
            method = PacketLossMethod.HTTP_PROBE_FAILURES,
            attemptedProbes = attempted,
            successfulProbes = successful,
            issues = issues,
        )
    }
}

internal fun addIssueOnce(issues: MutableList<MeasurementIssue>, issue: MeasurementIssue) {
    if (issues.none { it.code == issue.code }) issues += issue
}
