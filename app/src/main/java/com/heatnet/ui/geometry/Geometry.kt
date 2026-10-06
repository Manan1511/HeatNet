package com.heatnet.ui.geometry

import com.heatnet.data.model.Point
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Polygon helpers for the room outline. Units are whatever the caller uses (pixels or metres). */
object Geometry {
    fun distance(a: Point, b: Point): Float = hypot(a.x - b.x, a.y - b.y)

    /** Ray casting. Points exactly on an edge may go either way, which is fine for taps. */
    fun contains(polygon: List<Point>, p: Point): Boolean {
        if (polygon.size < 3) return false
        var inside = false
        var j = polygon.lastIndex
        for (i in polygon.indices) {
            val a = polygon[i]
            val b = polygon[j]
            if ((a.y > p.y) != (b.y > p.y)) {
                val crossX = (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x
                if (p.x < crossX) inside = !inside
            }
            j = i
        }
        return inside
    }

    fun area(polygon: List<Point>): Float {
        if (polygon.size < 3) return 0f
        var sum = 0f
        for (i in polygon.indices) {
            val a = polygon[i]
            val b = polygon[(i + 1) % polygon.size]
            sum += a.x * b.y - b.x * a.y
        }
        return abs(sum) / 2f
    }

    /** True when segments a1-a2 and b1-b2 cross or touch. */
    fun segmentsIntersect(a1: Point, a2: Point, b1: Point, b2: Point): Boolean {
        val d1 = cross(b1, b2, a1)
        val d2 = cross(b1, b2, a2)
        val d3 = cross(a1, a2, b1)
        val d4 = cross(a1, a2, b2)
        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) return true
        return (d1 == 0f && onSegment(b1, b2, a1)) ||
            (d2 == 0f && onSegment(b1, b2, a2)) ||
            (d3 == 0f && onSegment(a1, a2, b1)) ||
            (d4 == 0f && onSegment(a1, a2, b2))
    }

    /** True when no two non-neighbouring edges of the closed polygon touch. */
    fun isSimple(polygon: List<Point>): Boolean {
        val n = polygon.size
        if (n < 3) return false
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val neighbours = j == i + 1 || (i == 0 && j == n - 1)
                if (neighbours) continue
                if (segmentsIntersect(polygon[i], polygon[(i + 1) % n], polygon[j], polygon[(j + 1) % n])) return false
            }
        }
        return true
    }

    /** Would adding [next] to the open chain [chain] make the new edge cross an earlier edge? */
    fun newEdgeCrosses(chain: List<Point>, next: Point): Boolean {
        if (chain.size < 3) return false
        val last = chain.last()
        // Skip the edge that ends at [last]; it shares a corner with the new edge.
        for (i in 0 until chain.size - 2) {
            if (segmentsIntersect(chain[i], chain[i + 1], last, next)) return true
        }
        return false
    }

    /** Would closing the chain (last corner back to the first) cross an earlier edge? */
    fun closingEdgeCrosses(chain: List<Point>): Boolean {
        if (chain.size < 3) return false
        val first = chain.first()
        val last = chain.last()
        for (i in 1 until chain.size - 2) {
            if (segmentsIntersect(chain[i], chain[i + 1], last, first)) return true
        }
        return false
    }

    fun distanceToSegment(p: Point, a: Point, b: Point): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared == 0f) return distance(p, a)
        val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / lengthSquared).coerceIn(0f, 1f)
        return hypot(p.x - (a.x + t * dx), p.y - (a.y + t * dy))
    }

    /** Index i of the closest edge (polygon[i] to polygon[i+1]) within [maxDistance], or null. */
    fun nearestEdge(polygon: List<Point>, p: Point, maxDistance: Float): Int? {
        var best: Int? = null
        var bestDistance = maxDistance
        for (i in polygon.indices) {
            val d = distanceToSegment(p, polygon[i], polygon[(i + 1) % polygon.size])
            if (d <= bestDistance) {
                best = i
                bestDistance = d
            }
        }
        return best
    }

    /** Smallest distance from [p] to any of [points], or null if there are none. */
    fun nearestDistance(points: List<Point>, p: Point): Float? = points.minOfOrNull { distance(it, p) }

    /**
     * Snaps [p] so the edge from [from] is exactly horizontal or vertical when it is within
     * [toleranceDegrees] of one. Most rooms are rectangles, and hand-tapped corners are never square.
     */
    fun snapToRightAngle(from: Point, p: Point, toleranceDegrees: Float = 12f): Point {
        val angle = Math.toDegrees(atan2((p.y - from.y).toDouble(), (p.x - from.x).toDouble())).toFloat()
        val fromHorizontal = min(abs(angle), 180f - abs(angle))
        val fromVertical = abs(90f - abs(angle))
        return when {
            fromHorizontal <= toleranceDegrees -> Point(p.x, from.y)
            fromVertical <= toleranceDegrees -> Point(from.x, p.y)
            else -> p
        }
    }

    data class Bounds(val minX: Float, val minY: Float, val maxX: Float, val maxY: Float) {
        val width get() = maxX - minX
        val height get() = maxY - minY
    }

    fun bounds(points: List<Point>): Bounds = Bounds(
        points.minOf { it.x },
        points.minOf { it.y },
        points.maxOf { it.x },
        points.maxOf { it.y },
    )

    /**
     * Converts an outline drawn in screen pixels to metres. The user says wall [wallIndex]
     * (corner i to corner i+1) is [wallLengthMetres] long; every other length scales by the same
     * factor. The result is shifted so the bounding box starts at (0, 0), y = 0 being north.
     */
    fun outlineToMetres(pixels: List<Point>, wallIndex: Int, wallLengthMetres: Float): List<Point> {
        require(pixels.size >= 3) { "An outline needs at least 3 corners" }
        require(wallIndex in pixels.indices) { "No wall $wallIndex" }
        require(wallLengthMetres > 0f) { "Wall length must be positive" }
        val wallPixels = distance(pixels[wallIndex], pixels[(wallIndex + 1) % pixels.size])
        require(wallPixels > 0f) { "That wall has zero length" }
        val metresPerPixel = wallLengthMetres / wallPixels
        val b = bounds(pixels)
        return pixels.map { Point((it.x - b.minX) * metresPerPixel, (it.y - b.minY) * metresPerPixel) }
    }

    private fun cross(o: Point, a: Point, b: Point): Float = (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x)

    private fun onSegment(a: Point, b: Point, p: Point): Boolean =
        p.x in min(a.x, b.x)..max(a.x, b.x) && p.y in min(a.y, b.y)..max(a.y, b.y)
}
