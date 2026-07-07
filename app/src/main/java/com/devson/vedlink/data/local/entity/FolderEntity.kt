package com.devson.vedlink.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "folders",
    indices = [Index(value = ["parentId"])]
)
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val parentId: Int?,
    val createdAt: Long,
    val updatedAt: Long
)
