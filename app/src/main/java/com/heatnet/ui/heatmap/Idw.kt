package com.heatnet.ui.heatmap

import com.heatnet.data.model.Point
import com.heatnet.ui.geometry.Geometry
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.pow

/**
 * Tunable heatmap constants (PRD section 7). Member 3 tunes these after real-room testing.
 * Fade distances are fractions of the room's diagonal, so they scale with room size.
 */
data class HeatmapConfig(
    /** IDW power: higher means each reading's influence drops off faster with distance. */
    val power: Float = 2f,
    /** Grid cells along the room's longer side. The bitmap is smoothed when drawn. */
    val gridResolution: Int = 96,
    /** Up to this distance from the nearest reading the colour is fully bright ("measured"). */
    val fadeStartFraction: Float = 0.08f,
    /** At and beyond this distance the colour is fully washed out ("estimated"). */
    val fadeEndFraction: Float = 0.35f,
    val maxAlpha: Float = 0.85f,
    val minAlpha: Float = 0.12f,
    /** Below this many readings with a value for the metric, no heatmap is drawn (PRD 5). */
    val minReadings: Int = 3,
)

/** One real reading's position (metres) and its value for the selected metric. */
data class Sample(val x: Float, val y: Float, val value: Float)

/**
 * The interpolated room: [values] and [alphas] are row-major, [cols] x [rows], one cell per
 * [cellMetres] square. Cells outside the outline hold NaN and alpha 0.
 */
class HeatmapGrid(
    val cols: Int,
    val rows: Int,
    val cellMetres: Float,
    val values: FloatArray,
    val alphas: FloatArray,
)

object Idw {
    /**
     * Inverse distance weighting: a weighted average of all samples, each weighted by
     * 1 / distance^power. Standing exactly on a sample returns that sample's value.
     */
    fun interpolate(samples: List<Sample>, x: Float, y: Float, power: Float): Float {
        var weightedSum = 0.0
        var weightTotal = 0.0
        val powerDouble = power.toDouble()
        for (s in samples) {
            val d = hypot(s.x - x, s.y - y)
            if (d < EXACT_HIT_METRES) return s.value
            val w = 1.0 / d.toDouble().pow(powerDouble)
            weightedSum += w * s.value
            weightTotal += w
        }
        return (weightedSum / weightTotal).toFloat()
    }

    /** Opacity from distance to the nearest real reading: bright near data, washed out far away. */
    fun fadeAlpha(nearestMetres: Float, roomDiagonalMetres: Float, config: HeatmapConfig): Float {
        val start = config.fadeStartFraction * roomDiagonalMetres
        val end = max(config.fadeEndFraction * roomDiagonalMetres, start + 1e-3f)
        val t = ((nearestMetres - start) / (end - start)).coerceIn(0f, 1f)
        return config.maxAlpha + (config.minAlpha - config.maxAlpha) * t
    }

    /** Null when there are too few samples to draw an honest heatmap. */
    fun computeGrid(
        outline: List<Point>,
        widthMetres: Float,
        heightMetres: Float,
        samples: List<Sample>,
        config: HeatmapConfig = HeatmapConfig(),
    ): HeatmapGrid? {
        if (samples.size < config.minReadings || outline.size < 3) return null
        val longer = max(widthMetres, heightMetres).coerceAtLeast(0.01f)
        val cell = longer / config.gridResolution
        val cols = max(1, ceil(widthMetres / cell).toInt())
        val rows = max(1, ceil(heightMetres / cell).toInt())
        val diagonal = hypot(widthMetres, heightMetres)
        val values = FloatArray(cols * rows) { Float.NaN }
        val alphas = FloatArray(cols * rows)
        val powerDouble = config.power.toDouble()
        for (r in 0 until rows) {
            val y = (r + 0.5f) * cell
            for (c in 0 until cols) {
                val x = (c + 0.5f) * cell
                if (!Geometry.contains(outline, Point(x, y))) continue
                val i = r * cols + c

                var weightedSum = 0.0
                var weightTotal = 0.0
                var minDistance = Float.MAX_VALUE
                var exactHit = false
                var exactHitValue = 0f

                for (s in samples) {
                    val d = hypot(s.x - x, s.y - y)
                    if (d < minDistance) minDistance = d
                    if (!exactHit) {
                        if (d < EXACT_HIT_METRES) {
                            exactHit = true
                            exactHitValue = s.value
                        } else {
                            val w = 1.0 / d.toDouble().pow(powerDouble)
                            weightedSum += w * s.value
                            weightTotal += w
                        }
                    }
                }

                values[i] = if (exactHit) exactHitValue else (weightedSum / weightTotal).toFloat()
                alphas[i] = fadeAlpha(minDistance, diagonal, config)
            }
        }
        return HeatmapGrid(cols, rows, cell, values, alphas)
    }

    private const val EXACT_HIT_METRES = 1e-4f
}
