package com.devson.vedlink.domain.model

enum class MetadataState {
    QUEUED,     // Waiting in WorkManager queue
    FETCHING,   // Actively fetching URL contents / resolving redirects
    PROCESSING, // Running extraction pipeline and downloading assets
    COMPLETED,  // Metadata, favicon, and preview image are fully resolved and stored
    PARTIAL,    // Fetched text metadata, but failed to cache favicon or preview image
    RETRYING,   // Encountered transient network issues, rescheduled with backoff
    FAILED,     // Non-recoverable error (e.g. 404, malformed URL, SSL failure)
    OFFLINE     // Saved offline directly by user
}
