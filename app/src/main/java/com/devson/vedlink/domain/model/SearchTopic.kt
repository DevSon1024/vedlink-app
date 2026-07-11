package com.devson.vedlink.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class SearchTopic(
    val id: Int = 0,
    val query: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)

// Extension functions for mapping
fun SearchTopic.toEntity() = com.devson.vedlink.data.local.entity.SearchTopicEntity(
    id = id,
    query = query,
    timestamp = timestamp,
    isCompleted = isCompleted
)

fun com.devson.vedlink.data.local.entity.SearchTopicEntity.toDomain() = SearchTopic(
    id = id,
    query = query,
    timestamp = timestamp,
    isCompleted = isCompleted
)
