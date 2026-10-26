package com.devson.vedlink.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.devson.vedlink.data.local.dao.LinkDao
import com.devson.vedlink.data.local.dao.FolderDao
import com.devson.vedlink.data.local.dao.SearchTopicDao
import com.devson.vedlink.data.local.entity.LinkEntity
import com.devson.vedlink.data.local.entity.FolderEntity
import com.devson.vedlink.data.local.entity.SearchTopicEntity

@Database(
    entities = [LinkEntity::class, FolderEntity::class, SearchTopicEntity::class],
    version = 5,
    exportSchema = false
)
abstract class VedLinkDatabase : RoomDatabase() {
    abstract fun linkDao(): LinkDao
    abstract fun folderDao(): FolderDao
    abstract fun searchTopicDao(): SearchTopicDao

    companion object {
        const val DATABASE_NAME = "vedlink_db"

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Dynamically inspect existing columns in 'links' table to ensure safe, idempotent migration
                val existingColumns = mutableSetOf<String>()
                db.query("PRAGMA table_info(links)").use { cursor ->
                    val nameIndex = cursor.getColumnIndex("name")
                    while (cursor.moveToNext()) {
                        if (nameIndex != -1) {
                            existingColumns.add(cursor.getString(nameIndex))
                        }
                    }
                }

                if (!existingColumns.contains("isPinned")) {
                    db.execSQL("ALTER TABLE links ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
                }
                if (!existingColumns.contains("updatedAt")) {
                    db.execSQL("ALTER TABLE links ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                }
                if (!existingColumns.contains("noteTitle")) {
                    db.execSQL("ALTER TABLE links ADD COLUMN noteTitle TEXT DEFAULT NULL")
                }
                if (!existingColumns.contains("noteBodyMarkdown")) {
                    db.execSQL("ALTER TABLE links ADD COLUMN noteBodyMarkdown TEXT DEFAULT NULL")
                }
                if (!existingColumns.contains("accentColorHex")) {
                    db.execSQL("ALTER TABLE links ADD COLUMN accentColorHex INTEGER DEFAULT NULL")
                }
                if (!existingColumns.contains("readingTimeMinutes")) {
                    db.execSQL("ALTER TABLE links ADD COLUMN readingTimeMinutes INTEGER DEFAULT NULL")
                }
            }
        }
    }
}