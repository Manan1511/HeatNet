package com.heatnet.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query(
        "SELECT s.id AS id, s.name AS name, s.createdAt AS createdAt, s.connectionType AS connectionType, " +
            "COUNT(r.id) AS readingCount FROM sessions s LEFT JOIN readings r ON r.sessionId = s.id " +
            "GROUP BY s.id ORDER BY s.createdAt DESC",
    )
    fun observeSummaries(): Flow<List<SessionSummaryRow>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun get(id: Long): SessionEntity?

    @Query("UPDATE sessions SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ReadingDao {
    @Insert
    suspend fun insert(reading: ReadingEntity): Long

    @Query("SELECT * FROM readings WHERE sessionId = :sessionId ORDER BY timestamp")
    suspend fun forSession(sessionId: Long): List<ReadingEntity>

    @Query("SELECT * FROM readings WHERE sessionId = :sessionId ORDER BY timestamp")
    fun observeForSession(sessionId: Long): Flow<List<ReadingEntity>>

    @Query("DELETE FROM readings WHERE id = :id")
    suspend fun delete(id: Long)
}
