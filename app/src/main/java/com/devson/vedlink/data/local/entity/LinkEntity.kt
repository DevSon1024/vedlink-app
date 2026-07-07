package com.devson.vedlink.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "links",
    indices = [
        Index(value = ["url"], unique = true),
        Index(value = ["canonicalUrl"], unique = true),
        Index(value = ["folderId"])
    ]
)
data class LinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val canonicalUrl: String,
    val title: String?,
    val description: String?,
    val imageUrl: String?,
    val faviconUrl: String?,
    val domain: String?,
    val provider: String,
    val folderId: Int?,
    val isFavorite: Boolean,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val isUnread: Boolean,
    val notes: String?,
    val isPinnedNotes: Boolean,
    val notesUpdatedAt: Long?,
    val metadataState: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastUpdated: Long,
    val eTag: String?,
    val lastModified: String?,
    val tags: String? // Comma-separated
)
