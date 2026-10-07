package com.heatnet.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [SessionEntity::class, ReadingEntity::class], version = 2, exportSchema = false)
abstract class HeatNetDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun readingDao(): ReadingDao

    companion object {
        @Volatile
        private var instance: HeatNetDatabase? = null

        fun get(context: Context): HeatNetDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HeatNetDatabase::class.java,
                    "heatnet.db",
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE readings ADD COLUMN isDemo INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
