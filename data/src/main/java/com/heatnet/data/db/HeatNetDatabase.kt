package com.heatnet.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SessionEntity::class, ReadingEntity::class], version = 1, exportSchema = false)
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
                ).build().also { instance = it }
            }
    }
}
