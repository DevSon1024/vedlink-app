package com.devson.vedlink.domain.model

data class Link(
    val id: Int = 0,
    val url: String,
    val title: String?,
    val description: String?,
    val imageUrl: String?,
    val domain: String?,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val folderId: Int? = null,
    val tags: List<String> = emptyList()
)

// Extension functions for mapping
fun Link.toEntity() = com.devson.vedlink.data.local.entity.LinkEntity(
    id = id,
    url = url,
    title = title,
    description = description,
    imageUrl = imageUrl,
    domain = domain,
    isFavorite = isFavorite,
    createdAt = createdAt,
    updatedAt = updatedAt,
    folderId = folderId,
    tags = tags.joinToString(",")
)

fun com.devson.vedlink.data.local.entity.LinkEntity.toDomain() = Link(
    id = id,
    url = url,
    title = title,
    description = description,
    imageUrl = imageUrl,
    domain = domain,
    isFavorite = isFavorite,
    createdAt = createdAt,
    updatedAt = updatedAt,
    folderId = folderId,
    tags = tags?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
)
