package com.heatnet.data.db

import com.heatnet.data.model.Point
import com.heatnet.data.model.Reading
import com.heatnet.data.model.Session
import com.heatnet.data.model.SessionSummary
import com.heatnet.data.model.SessionWithReadings
import com.heatnet.measurement.model.ConnectionType
import com.heatnet.measurement.model.MeasurementResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The single entry point the UI uses for saving, loading and deleting sessions (F6). */
class SessionRepository(private val database: HeatNetDatabase) {
    private val sessions get() = database.sessionDao()
    private val readings get() = database.readingDao()

    /** Newest first, with a reading count per session, for the session list screen. */
    fun observeSummaries(): Flow<List<SessionSummary>> =
        sessions.observeSummaries().map { rows -> rows.map { it.toDomain() } }

    /** Width and height come from the outline's bounding box. Returns the new session id. */
    suspend fun createSession(
        name: String,
        connectionType: ConnectionType,
        outline: List<Point>,
        createdAt: Long = System.currentTimeMillis(),
    ): Long {
        require(outline.size >= 3) { "A room outline needs at least 3 corners" }
        val width = outline.maxOf { it.x } - outline.minOf { it.x }
        val height = outline.maxOf { it.y } - outline.minOf { it.y }
        return sessions.insert(
            Session(0, name, createdAt, connectionType, outline, width, height).toEntity(),
        )
    }

    suspend fun addReading(sessionId: Long, x: Float, y: Float, result: MeasurementResult): Long =
        readings.insert(result.toReading(sessionId, x, y).toEntity())

    /** Lets the user remove a wrongly placed dot (PRD risk: manual tapping gives wrong positions). */
    suspend fun deleteReading(readingId: Long) = readings.delete(readingId)

    suspend fun load(sessionId: Long): SessionWithReadings? {
        val session = sessions.get(sessionId) ?: return null
        return SessionWithReadings(session.toDomain(), readings.forSession(sessionId).map { it.toDomain() })
    }

    /** Live readings so the map updates after each Measure. */
    fun observeReadings(sessionId: Long): Flow<List<Reading>> =
        readings.observeForSession(sessionId).map { rows -> rows.map { it.toDomain() } }

    suspend fun rename(sessionId: Long, name: String) = sessions.rename(sessionId, name)

    /** Also removes the session's readings (foreign key CASCADE). */
    suspend fun delete(sessionId: Long) = sessions.delete(sessionId)
}
