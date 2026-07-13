package com.converty.app

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.converty.app.data.history.local.ConvertyDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationSmokeTest {
    @Test
    fun v1ToV3PreservesOldOutputAndEnablesMultiplePositions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "migration-${System.nanoTime()}.db"
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            createVersionOneFixture(db)
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                )
                .build(),
        )
        try {
            val db = helper.writableDatabase
            ConvertyDatabase.MIGRATION_1_2.migrate(db)
            db.query("SELECT dpi, lossless FROM conversion_jobs WHERE id = 'job'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(300, cursor.getInt(0))
                assertEquals(1, cursor.getInt(1))
            }

            ConvertyDatabase.MIGRATION_2_3.migrate(db)
            db.query(
                "SELECT position, display_name FROM conversion_outputs WHERE item_id = 'item' ORDER BY position",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
                assertEquals("old.pdf", cursor.getString(1))
            }
            db.execSQL(
                """
                INSERT INTO conversion_outputs (
                    item_id, position, uri, display_name, mime_type, size_bytes, created_at, is_readable
                ) VALUES ('item', 1, 'content://new', 'new.pdf', 'application/pdf', 20, 3, 1)
                """.trimIndent(),
            )
            db.query("SELECT COUNT(*) FROM conversion_outputs WHERE item_id = 'item'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(2, cursor.getInt(0))
            }
        } finally {
            helper.close()
            context.deleteDatabase(databaseName)
        }
    }

    private fun createVersionOneFixture(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE conversion_jobs (id TEXT NOT NULL PRIMARY KEY)")
        db.execSQL("CREATE TABLE conversion_items (id TEXT NOT NULL PRIMARY KEY)")
        db.execSQL(
            """
            CREATE TABLE conversion_outputs (
                item_id TEXT NOT NULL PRIMARY KEY,
                uri TEXT NOT NULL,
                display_name TEXT NOT NULL,
                mime_type TEXT NOT NULL,
                size_bytes INTEGER,
                created_at INTEGER NOT NULL,
                is_readable INTEGER NOT NULL,
                FOREIGN KEY(item_id) REFERENCES conversion_items(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("INSERT INTO conversion_jobs (id) VALUES ('job')")
        db.execSQL("INSERT INTO conversion_items (id) VALUES ('item')")
        db.execSQL(
            """
            INSERT INTO conversion_outputs (
                item_id, uri, display_name, mime_type, size_bytes, created_at, is_readable
            ) VALUES ('item', 'content://old', 'old.pdf', 'application/pdf', 10, 2, 1)
            """.trimIndent(),
        )
    }
}
