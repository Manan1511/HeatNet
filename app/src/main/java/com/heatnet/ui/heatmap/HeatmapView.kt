package com.heatnet.ui.heatmap

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.heatnet.data.model.Metric
import com.heatnet.data.model.Point
import com.heatnet.data.model.Reading
import com.heatnet.ui.geometry.MapTransform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * The room map: outline, IDW heatmap for [metric] (faded far from real readings) and the real
 * readings as dots on top. Reusable as-is for compare mode: pass a shared [scale] to both maps.
 *
 * Taps report positions in metres via [onTap]; a tap on a dot goes to [onReadingTap] instead.
 */
@Composable
fun HeatmapView(
    outline: List<Point>,
    widthMetres: Float,
    heightMetres: Float,
    readings: List<Reading>,
    metric: Metric,
    scale: MetricScale,
    modifier: Modifier = Modifier,
    config: HeatmapConfig = HeatmapConfig(),
    selectedPosition: Point? = null,
    highlightedReadingId: Long? = null,
    onTap: ((Point) -> Unit)? = null,
    onReadingTap: ((Reading) -> Unit)? = null,
) {
    val heatmap by produceState<HeatmapImage?>(null, outline, widthMetres, heightMetres, readings, metric, scale, config) {
        value = withContext(Dispatchers.Default) {
            val samples = readings.mapNotNull { r -> metric.valueOf(r)?.let { Sample(r.x, r.y, it) } }
            Idw.computeGrid(outline, widthMetres, heightMetres, samples, config)?.let { renderGrid(it, scale) }
        }
    }

    val density = LocalDensity.current
    val padding = with(density) { 16.dp.toPx() }
    val dotRadius = with(density) { 9.dp.toPx() }
    val hitRadius = with(density) { 24.dp.toPx() }
    val colors = MaterialTheme.colorScheme
    val roomFill = colors.surfaceVariant
    val outlineColor = colors.onSurface
    val selectionColor = colors.primary

    // Latest values for the gesture handler, which is set up once.
    val latestReadings = rememberUpdatedState(readings)
    val latestOnTap = rememberUpdatedState(onTap)
    val latestOnReadingTap = rememberUpdatedState(onReadingTap)

    Canvas(
        modifier = modifier.pointerInput(widthMetres, heightMetres) {
            detectTapGestures { tap ->
                val transform = MapTransform(widthMetres, heightMetres, size.width.toFloat(), size.height.toFloat(), padding)
                val hit = latestReadings.value.minByOrNull { r ->
                    val p = transform.toPx(Point(r.x, r.y))
                    hypot(p.x - tap.x, p.y - tap.y)
                }?.takeIf { r ->
                    val p = transform.toPx(Point(r.x, r.y))
                    hypot(p.x - tap.x, p.y - tap.y) <= hitRadius
                }
                val readingTap = latestOnReadingTap.value
                if (hit != null && readingTap != null) {
                    readingTap(hit)
                } else {
                    latestOnTap.value?.invoke(transform.toMetres(Point(tap.x, tap.y)))
                }
            }
        },
    ) {
        if (outline.size < 3) return@Canvas
        val transform = MapTransform(widthMetres, heightMetres, size.width, size.height, padding)
        val path = Path().apply {
            outline.forEachIndexed { i, p ->
                val px = transform.toPx(p)
                if (i == 0) moveTo(px.x, px.y) else lineTo(px.x, px.y)
            }
            close()
        }

        drawPath(path, roomFill)
        heatmap?.let { image ->
            val origin = transform.toPx(Point(0f, 0f))
            clipPath(path) {
                drawImage(
                    image = image.bitmap,
                    dstOffset = IntOffset(origin.x.roundToInt(), origin.y.roundToInt()),
                    dstSize = IntSize(
                        ceil(image.widthMetres * transform.scale).toInt(),
                        ceil(image.heightMetres * transform.scale).toInt(),
                    ),
                    filterQuality = FilterQuality.Low,
                )
            }
        }
        drawPath(path, outlineColor, style = Stroke(width = 3.dp.toPx()))

        readings.forEach { r ->
            val p = transform.toPx(Point(r.x, r.y)).toOffset()
            val value = metric.valueOf(r)
            val highlighted = r.id == highlightedReadingId
            val radius = if (highlighted) dotRadius * 1.4f else dotRadius
            if (value != null) {
                drawCircle(Color.White, radius + 2.dp.toPx(), p)
                drawCircle(Color(scale.colorOf(value)), radius, p)
                drawCircle(Color.Black.copy(alpha = 0.7f), radius, p, style = Stroke(1.5.dp.toPx()))
            } else {
                drawFailedDot(p, radius)
            }
        }

        selectedPosition?.let { drawSelection(transform.toPx(it).toOffset(), dotRadius, selectionColor) }
    }
}

/** A failed (or missing for this metric) reading: hollow grey circle with a cross. */
private fun DrawScope.drawFailedDot(center: Offset, radius: Float) {
    val grey = Color(0xFF757575)
    drawCircle(Color.White, radius, center)
    drawCircle(grey, radius, center, style = Stroke(2.dp.toPx()))
    val d = radius * 0.5f
    drawLine(grey, Offset(center.x - d, center.y - d), Offset(center.x + d, center.y + d), 2.dp.toPx(), StrokeCap.Round)
    drawLine(grey, Offset(center.x - d, center.y + d), Offset(center.x + d, center.y - d), 2.dp.toPx(), StrokeCap.Round)
}

/** The "you are here" marker for the position about to be measured. */
private fun DrawScope.drawSelection(center: Offset, radius: Float, color: Color) {
    drawCircle(color.copy(alpha = 0.25f), radius * 2.2f, center)
    drawCircle(color, radius * 2.2f, center, style = Stroke(2.dp.toPx()))
    drawCircle(color, radius * 0.45f, center)
}

private fun Point.toOffset() = Offset(x, y)

private class HeatmapImage(val bitmap: ImageBitmap, val widthMetres: Float, val heightMetres: Float)

private fun renderGrid(grid: HeatmapGrid, scale: MetricScale): HeatmapImage {
    val pixels = IntArray(grid.cols * grid.rows)
    for (i in pixels.indices) {
        val v = grid.values[i]
        if (v.isNaN()) continue
        val alpha = (grid.alphas[i] * 255).roundToInt().coerceIn(0, 255)
        pixels[i] = (alpha shl 24) or (scale.colorOf(v) and 0x00FFFFFF)
    }
    val bitmap = Bitmap.createBitmap(pixels, grid.cols, grid.rows, Bitmap.Config.ARGB_8888)
    return HeatmapImage(bitmap.asImageBitmap(), grid.cols * grid.cellMetres, grid.rows * grid.cellMetres)
}

/** True when there are enough readings with a value for [metric] to draw a heatmap. */
fun hasEnoughForHeatmap(readings: List<Reading>, metric: Metric, config: HeatmapConfig = HeatmapConfig()): Boolean =
    readings.count { metric.valueOf(it) != null } >= config.minReadings
