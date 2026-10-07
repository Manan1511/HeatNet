package com.heatnet.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heatnet.data.db.SessionRepository
import com.heatnet.data.model.Point
import com.heatnet.measurement.model.ConnectionType
import com.heatnet.ui.geometry.Geometry
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * F1: the user taps corners (in screen pixels) to draw the room, closes the shape, picks one
 * wall and types its real length. Everything is converted to metres before it is saved.
 */
class OutlineViewModel(
    private val repository: SessionRepository,
    val connectionType: ConnectionType,
) : ViewModel() {
    /** Corners in canvas pixels, in tap order. */
    val corners = mutableStateListOf<Point>()
    var closed by mutableStateOf(false)
        private set
    var snap by mutableStateOf(true)
    var message by mutableStateOf<String?>(null)
        private set
    var selectedWall by mutableStateOf(0)
        private set
    var wallLengthText by mutableStateOf("")
    var name by mutableStateOf(defaultName(connectionType))
    var creating by mutableStateOf(false)
        private set

    // Canvas size when the first corner was placed. If it later shrinks, the drawing scales down
    // to fit instead of overflowing; stored corners stay in the original canvas coordinate space.
    private var drawnWidth = 0f
    private var drawnHeight = 0f

    fun displayScale(canvasWidth: Float, canvasHeight: Float): Float =
        OutlineCanvasTransform.scale(drawnWidth, drawnHeight, canvasWidth, canvasHeight)

    /** A tap in current canvas pixels, mapped back to the stored corner coordinate space. */
    fun onCanvasTap(p: Point, canvasWidth: Float, canvasHeight: Float, closeRadiusPx: Float) {
        if (corners.isEmpty() && !closed) {
            drawnWidth = canvasWidth
            drawnHeight = canvasHeight
        }
        val scale = displayScale(canvasWidth, canvasHeight)
        onTap(OutlineCanvasTransform.toStored(p, scale), closeRadiusPx / scale)
    }

    private fun onTap(p: Point, closeRadiusPx: Float) {
        if (closed) {
            Geometry.nearestEdge(corners, p, closeRadiusPx * 1.5f)?.let { selectedWall = it }
            return
        }
        if (corners.size >= 3 && Geometry.distance(p, corners.first()) <= closeRadiusPx) {
            close()
            return
        }
        val q = if (snap && corners.isNotEmpty()) snapped(p, closeRadiusPx) else p
        if (corners.isNotEmpty() && Geometry.distance(q, corners.last()) < closeRadiusPx / 2f) return
        if (Geometry.newEdgeCrosses(corners, q)) {
            message = "Walls can't cross each other. Tap somewhere else."
            return
        }
        corners.add(q)
        message = null
    }

    /** Right-angle snap from the previous corner, then line up with the first corner if close. */
    private fun snapped(p: Point, tolerancePx: Float): Point {
        var q = Geometry.snapToRightAngle(corners.last(), p)
        if (corners.size >= 2) {
            val first = corners.first()
            // Never undo a vertical (or horizontal) snap from the previous corner.
            if (abs(q.x - first.x) <= tolerancePx / 2f && q.x != corners.last().x) q = Point(first.x, q.y)
            if (abs(q.y - first.y) <= tolerancePx / 2f && q.y != corners.last().y) q = Point(q.x, first.y)
        }
        return q
    }

    fun close() {
        message = when {
            corners.size < 3 -> "A room needs at least 3 corners."
            Geometry.closingEdgeCrosses(corners) -> "Closing the shape here would make walls cross."
            !Geometry.isSimple(corners) -> "Walls can't cross each other."
            Geometry.area(corners) < 100f -> "That shape is too small. Draw the room bigger."
            else -> null
        }
        if (message == null) {
            closed = true
            selectedWall = 0
        }
    }

    fun undo() {
        message = null
        if (closed) closed = false else corners.removeLastOrNull()
    }

    fun clear() {
        message = null
        closed = false
        corners.clear()
        drawnWidth = 0f
        drawnHeight = 0f
    }

    /** The outline in metres, or an error to show under the length field. */
    fun scaledOutline(): Result<List<Point>> {
        val length = OutlineInputValidation.parseWallLength(wallLengthText, MIN_WALL_METRES, MAX_WALL_METRES)
            .getOrElse { return Result.failure(it) }
        val metres = Geometry.outlineToMetres(corners, selectedWall, length)
        val b = Geometry.bounds(metres)
        if (b.width > MAX_ROOM_METRES || b.height > MAX_ROOM_METRES) {
            return Result.failure(IllegalArgumentException("That makes the room over $MAX_ROOM_METRES m across. Check the wall and length."))
        }
        if (Geometry.area(metres) < MIN_AREA_SQ_METRES) {
            return Result.failure(IllegalArgumentException("That makes the room smaller than $MIN_AREA_SQ_METRES m². Check the wall and length."))
        }
        return Result.success(metres)
    }

    fun create(onCreated: (Long) -> Unit) {
        val outline = scaledOutline().getOrNull() ?: return
        if (creating) return
        creating = true
        viewModelScope.launch {
            try {
                val id = repository.createSession(name.trim().ifEmpty { defaultName(connectionType) }, connectionType, outline)
                onCreated(id)
            } finally {
                creating = false
            }
        }
    }

    companion object {
        const val MIN_WALL_METRES = 0.5f
        const val MAX_WALL_METRES = 50f
        const val MAX_ROOM_METRES = 100f
        const val MIN_AREA_SQ_METRES = 1f

        fun defaultName(type: ConnectionType): String =
            "${type.label} · ${SimpleDateFormat("d MMM HH:mm", Locale.getDefault()).format(Date())}"
    }
}
