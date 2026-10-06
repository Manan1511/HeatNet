package com.heatnet.ui.geometry

import com.heatnet.data.model.Point
import kotlin.math.min

/**
 * Fits a room of [widthMetres] x [heightMetres] into a canvas, keeping its shape and centring it.
 * Readings and outlines are stored in metres (PRD section 8); this is the only place that turns
 * them into pixels, so the same session draws correctly on any screen and in compare mode.
 */
class MapTransform(
    val widthMetres: Float,
    val heightMetres: Float,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    paddingPx: Float,
) {
    /** Pixels per metre. */
    val scale: Float
    val offsetX: Float
    val offsetY: Float

    init {
        val usableW = (canvasWidthPx - 2 * paddingPx).coerceAtLeast(1f)
        val usableH = (canvasHeightPx - 2 * paddingPx).coerceAtLeast(1f)
        scale = min(usableW / widthMetres.coerceAtLeast(MIN_SIZE), usableH / heightMetres.coerceAtLeast(MIN_SIZE))
        offsetX = (canvasWidthPx - widthMetres * scale) / 2f
        offsetY = (canvasHeightPx - heightMetres * scale) / 2f
    }

    fun toPx(metres: Point): Point = Point(offsetX + metres.x * scale, offsetY + metres.y * scale)

    fun toMetres(px: Point): Point = Point((px.x - offsetX) / scale, (px.y - offsetY) / scale)

    private companion object {
        const val MIN_SIZE = 0.01f
    }
}
