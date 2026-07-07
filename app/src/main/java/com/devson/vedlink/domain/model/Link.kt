package com.devson.vedlink.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Link(
    val id: Int = 0,
    val url: String,
    val canonicalUrl: String = url,
    val title: String?,
    val description: String?,
    val imageUrl: String?,
    val faviconUrl: String? = null,
    val domain: String?,
    val provider: WebsiteProvider = WebsiteProvider.GENERIC,
    val folderId: Int? = null,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isUnread: Boolean = true,
    val notes: String? = null,
    val isPinnedNotes: Boolean = false,
    val notesUpdatedAt: Long? = null,
    val metadataState: MetadataState = MetadataState.QUEUED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val eTag: String? = null,
    val lastModified: String? = null,
    val tags: List<String> = emptyList()
)

// Extension functions for mapping
fun Link.toEntity() = com.devson.vedlink.data.local.entity.LinkEntity(
    id = id,
    url = url,
    canonicalUrl = canonicalUrl,
    title = title,
    description = description,
    imageUrl = imageUrl,
    faviconUrl = faviconUrl,
    domain = domain,
    provider = provider.name,
    folderId = folderId,
    isFavorite = isFavorite,
    isPinned = isPinned,
    isArchived = isArchived,
    isUnread = isUnread,
    notes = notes,
    isPinnedNotes = isPinnedNotes,
    notesUpdatedAt = notesUpdatedAt,
    metadataState = metadataState.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastUpdated = lastUpdated,
    eTag = eTag,
    lastModified = lastModified,
    tags = if (tags.isEmpty()) null else tags.joinToString(",")
)

fun com.devson.vedlink.data.local.entity.LinkEntity.toDomain() = Link(
    id = id,
    url = url,
    canonicalUrl = canonicalUrl,
    title = title,
    description = description,
    imageUrl = imageUrl,
    faviconUrl = faviconUrl,
    domain = domain,
    provider = try {
        WebsiteProvider.valueOf(provider)
    } catch (e: Exception) {
        WebsiteProvider.GENERIC
    },
    folderId = folderId,
    isFavorite = isFavorite,
    isPinned = isPinned,
    isArchived = isArchived,
    isUnread = isUnread,
    notes = notes,
    isPinnedNotes = isPinnedNotes,
    notesUpdatedAt = notesUpdatedAt,
    metadataState = try {
        MetadataState.valueOf(metadataState)
    } catch (e: Exception) {
        MetadataState.QUEUED
    },
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastUpdated = lastUpdated,
    eTag = eTag,
    lastModified = lastModified,
    tags = tags?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
)
