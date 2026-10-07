package com.heatnet.ui.screens

import com.heatnet.data.model.Point

/** Maps stored canvas pixels into a resized canvas without stretching or scaling them above 1:1. */
internal object OutlineCanvasTransform {
    fun scale(storedWidth: Float, storedHeight: Float, canvasWidth: Float, canvasHeight: Float): Float {
        if (listOf(storedWidth, storedHeight, canvasWidth, canvasHeight).any { !it.isFinite() || it <= 0f }) {
            return 1f
        }
        return minOf(canvasWidth / storedWidth, canvasHeight / storedHeight, 1f)
    }

    fun toCanvas(point: Point, scale: Float): Point = Point(point.x * safeScale(scale), point.y * safeScale(scale))

    fun toStored(point: Point, scale: Float): Point = Point(point.x / safeScale(scale), point.y / safeScale(scale))

    private fun safeScale(scale: Float): Float = scale.takeIf { it.isFinite() && it > 0f } ?: 1f
}
