package com.devson.vedlink.domain.usecase

import com.devson.vedlink.domain.repository.LinkRepository
import com.devson.vedlink.domain.model.Link
import javax.inject.Inject

class UpdateLinkUseCase @Inject constructor(
    private val repository: LinkRepository
) {
    suspend operator fun invoke(link: Link) {
        repository.updateLink(link)
    }

    suspend fun updateNote(
        linkId: Int,
        noteTitle: String?,
        noteBodyMarkdown: String?,
        isPinned: Boolean? = null,
        accentColorHex: Int? = null,
        readingTimeMinutes: Int? = null
    ): Result<Link> {
        val existing = repository.getLinkById(linkId)
            ?: return Result.failure(IllegalArgumentException("Link not found with id $linkId"))
        val now = System.currentTimeMillis()
        val updated = existing.copy(
            noteTitle = noteTitle,
            noteBodyMarkdown = noteBodyMarkdown,
            notes = noteBodyMarkdown ?: existing.notes,
            notesUpdatedAt = now,
            isPinned = isPinned ?: existing.isPinned,
            accentColorHex = accentColorHex ?: existing.accentColorHex,
            readingTimeMinutes = readingTimeMinutes ?: existing.readingTimeMinutes,
            updatedAt = now
        )
        repository.updateLink(updated)
        return Result.success(updated)
    }

    suspend fun togglePin(linkId: Long, isPinned: Boolean): Result<Unit> {
        return try {
            repository.togglePin(linkId, isPinned, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}