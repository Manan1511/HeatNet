package com.heatnet.measurement.probe

import android.net.Network
import com.heatnet.measurement.math.calculatePayloadMbps
import com.heatnet.measurement.model.IssueCode
import com.heatnet.measurement.model.MeasurementConfig
import com.heatnet.measurement.model.MeasurementIssue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class ThroughputProbe private constructor(
    private val config: MeasurementConfig,
    private val clientFactory: NetworkBoundHttpClientFactory,
    private val clock: ProbeClock,
) {
    constructor(config: MeasurementConfig = MeasurementConfig()) :
        this(config, NetworkBoundHttpClientFactory(), SystemProbeClock)

    internal constructor(config: MeasurementConfig, clock: ProbeClock) :
        this(config, NetworkBoundHttpClientFactory(), clock)

    suspend fun measure(
        network: Network,
        direction: TransferDirection,
        byteBudget: Long,
        deadlineNanos: Long,
    ): ThroughputMeasurement = measureWithClient(
        client = clientFactory.create(network),
        direction = direction,
        byteBudget = byteBudget,
        deadlineNanos = deadlineNanos,
    )

    internal suspend fun measureWithClient(
        client: OkHttpClient,
        direction: TransferDirection,
        byteBudget: Long,
        deadlineNanos: Long,
    ): ThroughputMeasurement {
        val effectiveBudget = minOf(byteBudget.coerceAtLeast(0L), config.maxBytesPerDirection.coerceAtLeast(0L))
        if (effectiveBudget == 0L) {
            return ThroughputMeasurement(null, 0L, 0L, isPartial = false)
        }

        val streamCount = minOf(config.parallelStreams.coerceIn(1, MAX_PARALLEL_STREAMS), effectiveBudget.toIntOrMax())
        val allocations = allocateBudget(effectiveBudget, streamCount)
        val startedAt = clock.nowNanos()
        val outcomes = coroutineScope {
            allocations.map { allocation ->
                async(Dispatchers.IO) {
                    transferStream(client, direction, allocation, deadlineNanos)
                }
            }.awaitAll()
        }
        val elapsedNanos = (clock.nowNanos() - startedAt).coerceAtLeast(0L)
        val payloadBytes = outcomes.sumOf { it.payloadBytes }.coerceAtMost(effectiveBudget)
        val issues = outcomes.flatMap { it.issues }
        val partial = payloadBytes < effectiveBudget || outcomes.any { it.partial } || issues.isNotEmpty()

        return ThroughputMeasurement(
            megabitsPerSecond = calculatePayloadMbps(payloadBytes, elapsedNanos),
            payloadBytes = payloadBytes,
            budgetBytes = effectiveBudget,
            isPartial = partial,
            issues = issues,
            successfulStreams = outcomes.sumOf { it.successfulResponses },
        )
    }

    private suspend fun transferStream(
        client: OkHttpClient,
        direction: TransferDirection,
        allocation: Long,
        deadlineNanos: Long,
    ): StreamOutcome {
        val uploadedBytes = AtomicLong(0L)
        val successfulResponses = AtomicInteger(0)
        return try {
            val builder = Request.Builder()
            val request = when (direction) {
                TransferDirection.DOWNLOAD -> builder
                    .url(withByteCount(config.downloadUrl, allocation))
                    .get()
                    .build()
                TransferDirection.UPLOAD -> builder
                    .url(config.uploadUrl)
                    .post(FixedPayloadRequestBody(allocation, deadlineNanos, uploadedBytes))
                    .build()
            }
            val responseOutcome = client.newCall(request).awaitResponse(deadlineNanos) { response ->
                if (response.isSuccessful) successfulResponses.incrementAndGet()
                if (direction == TransferDirection.UPLOAD) {
                    ResponseOutcome(
                        payloadBytes = uploadedBytes.get(),
                        successful = response.isSuccessful,
                        bodyEndedEarly = uploadedBytes.get() < allocation,
                    )
                } else if (!response.isSuccessful) {
                    ResponseOutcome(0L, successful = false, bodyEndedEarly = true)
                } else {
                    val body = response.body
                    val read = body.byteStream().readAtMost(allocation, deadlineNanos)
                    ResponseOutcome(read.bytesRead, successful = true, bodyEndedEarly = read.endedBeforeLimit)
                }
            }
            val issues = buildList {
                if (!responseOutcome.successful) {
                    add(MeasurementIssue(IssueCode.ENDPOINT_FAILURE, "Throughput endpoint returned a non-success status"))
                } else if (responseOutcome.bodyEndedEarly) {
                    add(MeasurementIssue(IssueCode.ENDPOINT_FAILURE, "Throughput response ended before its byte allocation"))
                }
            }
            StreamOutcome(
                payloadBytes = responseOutcome.payloadBytes.coerceAtMost(allocation),
                partial = responseOutcome.bodyEndedEarly || !responseOutcome.successful,
                successfulResponses = successfulResponses.get(),
                issues = issues,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val issue = error.toMeasurementIssue(System.nanoTime(), deadlineNanos)
            StreamOutcome(
                payloadBytes = uploadedBytes.get().coerceAtMost(allocation),
                partial = true,
                successfulResponses = successfulResponses.get(),
                issues = listOf(issue),
            )
        }
    }
}

private data class ResponseOutcome(
    val payloadBytes: Long,
    val successful: Boolean,
    val bodyEndedEarly: Boolean,
)

private data class StreamOutcome(
    val payloadBytes: Long,
    val partial: Boolean,
    val successfulResponses: Int,
    val issues: List<MeasurementIssue>,
)

private data class BodyRead(
    val bytesRead: Long,
    val endedBeforeLimit: Boolean,
)

private class FixedPayloadRequestBody(
    private val byteCount: Long,
    private val deadlineNanos: Long,
    private val bytesWritten: AtomicLong,
) : RequestBody() {
    override fun contentType() = BINARY_MEDIA_TYPE

    override fun contentLength(): Long = byteCount

    override fun writeTo(sink: BufferedSink) {
        val chunk = ByteArray(TRANSFER_CHUNK_BYTES)
        var remaining = byteCount
        while (remaining > 0L) {
            if (System.nanoTime() >= deadlineNanos) throw SocketTimeoutException("Upload reached the measurement deadline")
            val count = minOf(remaining, chunk.size.toLong()).toInt()
            sink.write(chunk, 0, count)
            bytesWritten.addAndGet(count.toLong())
            remaining -= count
        }
    }
}

private fun java.io.InputStream.readAtMost(limit: Long, deadlineNanos: Long): BodyRead {
    val buffer = ByteArray(TRANSFER_CHUNK_BYTES)
    var bytesRead = 0L
    while (bytesRead < limit) {
        if (System.nanoTime() >= deadlineNanos) throw SocketTimeoutException("Download reached the measurement deadline")
        val remaining = minOf(buffer.size.toLong(), limit - bytesRead).toInt()
        val count = read(buffer, 0, remaining)
        if (count == -1) return BodyRead(bytesRead, endedBeforeLimit = true)
        if (count == 0) continue
        bytesRead += count
    }
    return BodyRead(bytesRead, endedBeforeLimit = false)
}

private fun allocateBudget(total: Long, streams: Int): List<Long> {
    val each = total / streams
    val remainder = (total % streams).toInt()
    return List(streams) { index -> each + if (index < remainder) 1L else 0L }
}

private fun Long.toIntOrMax(): Int = if (this > Int.MAX_VALUE) Int.MAX_VALUE else toInt()

private const val MAX_PARALLEL_STREAMS = 2
private const val TRANSFER_CHUNK_BYTES = 8 * 1024
private val BINARY_MEDIA_TYPE = "application/octet-stream".toMediaType()
