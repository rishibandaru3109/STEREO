package com.nova.music.database

import androidx.sqlite.db.SupportSQLiteDatabase
import com.nova.music.core.database.StereoDatabase
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class RecommendationDatabaseMigrationTest {

    @Test
    fun testMigration1To2CreatesExpectedTables() {
        val executedSqlStatements = mutableListOf<String>()

        val fakeDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedSqlStatements.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        StereoDatabase.MIGRATION_1_2.migrate(fakeDb)

        assertTrue("Should execute at least 2 SQL statements", executedSqlStatements.size >= 2)
        val sqlCombined = executedSqlStatements.joinToString(" ")
        assertTrue("Must create recommendation_cache table", sqlCombined.contains("recommendation_cache"))
        assertTrue("Must have sectionKey primary key", sqlCombined.contains("sectionKey"))
        assertTrue("Must create explicit_feedback table", sqlCombined.contains("explicit_feedback"))
        assertTrue("Must have trackId primary key", sqlCombined.contains("trackId"))
    }
}
