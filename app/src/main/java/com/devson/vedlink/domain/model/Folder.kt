package com.devson.vedlink.domain.model

data class Folder(
    val id: Int = 0,
    val name: String,
    val parentId: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
