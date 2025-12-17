package com.devson.vedlink.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.devson.vedlink.data.local.dao.LinkDao
import com.devson.vedlink.data.local.entity.LinkEntity

@Database(
    entities = [LinkEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VedLinkDatabase : RoomDatabase() {
    abstract fun linkDao(): LinkDao

    companion object {
        const val DATABASE_NAME = "vedlink_db"
    }
}