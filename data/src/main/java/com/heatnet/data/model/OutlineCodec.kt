package com.heatnet.data.model

/** Stores a room outline as one text column: "x,y;x,y;...". Floats round-trip exactly via toString. */
object OutlineCodec {
    fun encode(points: List<Point>): String =
        points.joinToString(";") { "${it.x},${it.y}" }

    fun decode(text: String): List<Point> {
        if (text.isBlank()) return emptyList()
        return text.split(';').map { pair ->
            val parts = pair.split(',')
            require(parts.size == 2) { "Bad outline point: '$pair'" }
            Point(parts[0].toFloat(), parts[1].toFloat())
        }
    }
}
