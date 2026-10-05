package com.heatnet.measurement.probe

import android.net.Network
import com.heatnet.measurement.math.calculateLatencySummary
import com.heatnet.measurement.model.IssueCode
import com.heatnet.measurement.model.MeasurementConfig
import com.heatnet.measurement.model.MeasurementIssue
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException

class LatencyProbe private constructor(
    private val clientFactory: NetworkBoundHttpClientFactory,
    private val clock: ProbeClock,
) {
    constructor() : this(NetworkBoundHttpClientFactory(), SystemProbeClock)

    internal constructor(clock: ProbeClock) : this(NetworkBoundHttpClientFactory(), clock)

    suspend fun measure(network: Network, config: MeasurementConfig, deadlineNanos: Long): LatencyMeasurement =
        measureWithClient(clientFactory.create(network), config, deadlineNanos)

    internal suspend fun measureWithClient(
        client: OkHttpClient,
        config: MeasurementConfig,
        deadlineNanos: Long,
    ): LatencyMeasurement {
        val samples = mutableListOf<Float>()
        val issues = mutableListOf<MeasurementIssue>()
        var attempted = 0

        for (sampleIndex in 0 until config.latencySampleCount.coerceAtLeast(0)) {
            if (clock.nowNanos() >= deadlineNanos) {
                issues += MeasurementIssue(IssueCode.TIMEOUT, "Latency requests reached the measurement deadline")
                break
            }
            attempted += 1
            val startedAt = clock.nowNanos()
            try {
                val url = withByteCount(config.latencyUrl, 0)
                val request = Request.Builder().url(url).get().build()
                val successful = client.newCall(request).awaitResponse(deadlineNanos) { response ->
                    if (response.isSuccessful) {
                        val elapsedNanos = (clock.nowNanos() - startedAt).coerceAtLeast(0L)
                        samples += elapsedNanos / NANOS_PER_MILLISECOND
                        true
                    } else {
                        false
                    }
                }
                if (!successful) {
                    issues += MeasurementIssue(IssueCode.ENDPOINT_FAILURE, "Latency endpoint returned a non-success status")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                issues += error.toMeasurementIssue(clock.nowNanos(), deadlineNanos)
            }
        }

        val summary = calculateLatencySummary(samples)
        return LatencyMeasurement(
            latencyMs = summary?.meanMs,
            jitterMs = summary?.jitterMs,
            attemptedRequests = attempted,
            successfulRequests = samples.size,
            issues = issues,
        )
    }
}

internal fun withByteCount(url: String, bytes: Long): String {
    val parsed = requireNotNull(url.toHttpUrlOrNull()) { "Probe endpoint must be an HTTP URL" }
    return parsed.newBuilder().setQueryParameter("bytes", bytes.toString()).build().toString()
}

internal fun Exception.toMeasurementIssue(nowNanos: Long, deadlineNanos: Long): MeasurementIssue =
    if (this is SocketTimeoutException || nowNanos >= deadlineNanos) {
        MeasurementIssue(IssueCode.TIMEOUT, "Probe reached its time limit")
    } else {
        MeasurementIssue(IssueCode.ENDPOINT_FAILURE, "Probe request failed")
    }

private const val NANOS_PER_MILLISECOND = 1_000_000f
