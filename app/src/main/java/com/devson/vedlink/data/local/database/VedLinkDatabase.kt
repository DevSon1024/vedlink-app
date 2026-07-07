package com.devson.vedlink.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.devson.vedlink.data.local.dao.LinkDao
import com.devson.vedlink.data.local.dao.FolderDao
import com.devson.vedlink.data.local.entity.LinkEntity
import com.devson.vedlink.data.local.entity.FolderEntity

@Database(
    entities = [LinkEntity::class, FolderEntity::class],
    version = 3,
    exportSchema = false
)
abstract class VedLinkDatabase : RoomDatabase() {
    abstract fun linkDao(): LinkDao
    abstract fun folderDao(): FolderDao

    companion object {
        const val DATABASE_NAME = "vedlink_db"
    }
}