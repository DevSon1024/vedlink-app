package com.devson.vedlink.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.devson.vedlink.data.local.dao.LinkDao
import com.devson.vedlink.data.local.dao.FolderDao
import com.devson.vedlink.data.local.dao.SearchTopicDao
import com.devson.vedlink.data.local.entity.LinkEntity
import com.devson.vedlink.data.local.entity.FolderEntity
import com.devson.vedlink.data.local.entity.SearchTopicEntity

@Database(
    entities = [LinkEntity::class, FolderEntity::class, SearchTopicEntity::class],
    version = 4,
    exportSchema = false
)
abstract class VedLinkDatabase : RoomDatabase() {
    abstract fun linkDao(): LinkDao
    abstract fun folderDao(): FolderDao
    abstract fun searchTopicDao(): SearchTopicDao

    companion object {
        const val DATABASE_NAME = "vedlink_db"
    }
}