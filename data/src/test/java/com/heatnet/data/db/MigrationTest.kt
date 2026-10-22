package com.heatnet.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.reflect.Proxy

class MigrationTest {

    @Test
    fun migration12ExecutesCorrectSql() {
        val executedSql = mutableListOf<String>()

        val db = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedSql.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        HeatNetDatabase.MIGRATION_1_2.migrate(db)

        assertEquals(1, executedSql.size)
        assertEquals(
            "ALTER TABLE readings ADD COLUMN isDemo INTEGER NOT NULL DEFAULT 0",
            executedSql[0]
        )
    }
}
