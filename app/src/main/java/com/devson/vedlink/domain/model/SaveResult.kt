package com.devson.vedlink.domain.model

// Result status for save operations
enum class SaveStatus {
    NEWLY_SAVED,
    ALREADY_EXISTS
}

data class SaveResult(
    val linkId: Long,
    val status: SaveStatus
)