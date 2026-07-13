package com.converty.app.data.history.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ConversionJobEntity::class,
        ConversionItemEntity::class,
        ConversionOutputEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@TypeConverters(HistoryTypeConverters::class)
abstract class ConvertyDatabase : RoomDatabase() {
    abstract fun conversionHistoryDao(): ConversionHistoryDao

    companion object {
        private const val DATABASE_NAME = "converty_history.db"

        @Volatile
        private var instance: ConvertyDatabase? = null

        fun getInstance(context: Context): ConvertyDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                ConvertyDatabase::class.java,
                DATABASE_NAME,
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .also { instance = it }
        }

        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE conversion_jobs ADD COLUMN dpi INTEGER NOT NULL DEFAULT 300",
                )
                db.execSQL(
                    "ALTER TABLE conversion_jobs ADD COLUMN lossless INTEGER NOT NULL DEFAULT 1",
                )
            }
        }

        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS conversion_outputs_new (
                        item_id TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        uri TEXT NOT NULL,
                        display_name TEXT NOT NULL,
                        mime_type TEXT NOT NULL,
                        size_bytes INTEGER,
                        created_at INTEGER NOT NULL,
                        is_readable INTEGER NOT NULL,
                        PRIMARY KEY(item_id, position),
                        FOREIGN KEY(item_id) REFERENCES conversion_items(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO conversion_outputs_new (
                        item_id, position, uri, display_name, mime_type, size_bytes, created_at, is_readable
                    )
                    SELECT item_id, 0, uri, display_name, mime_type, size_bytes, created_at, is_readable
                    FROM conversion_outputs
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE conversion_outputs")
                db.execSQL("ALTER TABLE conversion_outputs_new RENAME TO conversion_outputs")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_conversion_outputs_item_id ON conversion_outputs(item_id)",
                )
            }
        }
    }
}
