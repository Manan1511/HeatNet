package com.heatnet.ui.measure

import com.heatnet.measurement.model.ConnectionType
import com.heatnet.measurement.model.MeasurementProgress
import com.heatnet.measurement.model.MeasurementResult
import com.heatnet.measurement.model.MeasurementStage
import com.heatnet.measurement.model.MeasurementStatus
import com.heatnet.measurement.model.PacketLossMethod
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Demo readings that get worse with distance from a pretend router at ([routerX], [routerY]),
 * plus some noise. Lets the map and heatmap be built and shown without Member 1's engine.
 */
class FakeMeasurementSource(
    private val routerX: Float = 0.5f,
    private val routerY: Float = 0.5f,
    private val stageDelayMillis: Long = 300L,
    private val random: Random = Random.Default,
    private val clock: () -> Long = System::currentTimeMillis,
) : MeasurementSource {
    override val isFake = true

    override suspend fun measure(
        type: ConnectionType,
        xMetres: Float,
        yMetres: Float,
        onProgress: (MeasurementProgress) -> Unit,
    ): MeasurementResult {
        val stages = listOf(
            MeasurementStage.VERIFYING_NETWORK,
            MeasurementStage.CAPTURING_RADIO,
            MeasurementStage.DOWNLOAD,
            MeasurementStage.UPLOAD,
            MeasurementStage.LATENCY,
            MeasurementStage.PACKET_LOSS,
        )
        for (stage in stages) {
            onProgress(MeasurementProgress(stage))
            delay(stageDelayMillis)
        }
        onProgress(MeasurementProgress(MeasurementStage.COMPLETE))

        val distance = hypot(xMetres - routerX, yMetres - routerY)
        // Log-distance path loss: about -35 dBm next to the router, losing ~30 dB per decade of distance.
        val signal = (-35f - 30f * log10(distance + 1f) + noise(3f)).coerceIn(-95f, -30f)
        val quality = ((signal + 90f) / 55f).coerceIn(0.05f, 1f)
        val wifi = type == ConnectionType.WIFI
        return MeasurementResult(
            expectedConnectionType = type,
            observedConnectionType = type,
            measuredAtEpochMillis = clock(),
            status = MeasurementStatus.COMPLETE,
            downloadMbps = ((if (wifi) 180f else 60f) * quality + noise(4f)).coerceAtLeast(0.5f),
            uploadMbps = ((if (wifi) 90f else 20f) * quality + noise(2f)).coerceAtLeast(0.2f),
            latencyMs = (12f + 80f * (1f - quality) + noise(3f)).coerceAtLeast(1f),
            jitterMs = 1f + 15f * (1f - quality),
            packetLossPct = if (quality < 0.35f) 5f * (1f - quality) else 0f,
            packetLossMethod = PacketLossMethod.ICMP,
            signalDbm = signal.roundToInt(),
            linkSpeedMbps = if (wifi) (866 * quality).roundToInt() else null,
            wifiBand = if (wifi) "5" else null,
            wifiChannel = if (wifi) 36 else null,
            bssid = if (wifi) "02:00:00:00:00:01" else null,
            networkType = if (wifi) null else "LTE",
        )
    }

    private fun noise(amplitude: Float): Float = (random.nextFloat() * 2f - 1f) * amplitude
}
