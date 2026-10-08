package com.heatnet

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.heatnet.data.db.HeatNetDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReadingProvenanceMigrationTest {
    @Test
    fun versionOneRowsMigrateAsRealMeasurements() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "heatnet-v1-migration-test.db"
        context.deleteDatabase(databaseName)
        createVersionOneDatabase(context, databaseName)

        val migrated = Room.databaseBuilder(context, HeatNetDatabase::class.java, databaseName)
            .addMigrations(HeatNetDatabase.MIGRATION_1_2)
            .build()
        try {
            val cursor = migrated.openHelper.writableDatabase.query("SELECT isDemo FROM readings WHERE id = 1")
            cursor.use {
                assertEquals(1, it.count)
                it.moveToFirst()
                assertEquals(0, it.getInt(0))
            }
        } finally {
            migrated.close()
            context.deleteDatabase(databaseName)
        }
    }

    private fun createVersionOneDatabase(context: Context, databaseName: String) {
        val file = context.getDatabasePath(databaseName).apply { parentFile?.mkdirs() }
        val database = SQLiteDatabase.openOrCreateDatabase(file, null)
        database.use {
            it.execSQL(
                """CREATE TABLE sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    connectionType TEXT NOT NULL,
                    outline TEXT NOT NULL,
                    widthMetres REAL NOT NULL,
                    heightMetres REAL NOT NULL
                )""".trimIndent(),
            )
            it.execSQL(
                """CREATE TABLE readings (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    sessionId INTEGER NOT NULL,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    timestamp INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    downloadMbps REAL,
                    uploadMbps REAL,
                    latencyMs REAL,
                    jitterMs REAL,
                    packetLossPct REAL,
                    packetLossMethod TEXT NOT NULL,
                    signalDbm INTEGER,
                    linkSpeedMbps INTEGER,
                    wifiBand TEXT,
                    wifiChannel INTEGER,
                    bssid TEXT,
                    networkType TEXT,
                    FOREIGN KEY(sessionId) REFERENCES sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )""".trimIndent(),
            )
            it.execSQL("CREATE INDEX index_readings_sessionId ON readings(sessionId)")
            it.execSQL("INSERT INTO sessions VALUES (1, 'Room', 1, 'WIFI', '[]', 10, 8)")
            it.execSQL("INSERT INTO readings VALUES (1, 1, 1, 2, 1, 'COMPLETE', 25, 10, 30, 2, 0, 'ICMP', -60, 400, '5', 36, NULL, NULL)")
            it.execSQL("PRAGMA user_version = 1")
        }
    }
}
