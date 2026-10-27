package com.devson.vedlink.domain.usecase

import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.domain.model.WebsiteProvider
import com.devson.vedlink.domain.repository.LinkRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

enum class SmartSpaceCategory(
    val title: String,
    val description: String
) {
    PINNED("Pinned Notes", "Important pinned notes & links"),
    RICH_NOTES("Rich Notes", "Links with personal Markdown notes"),
    VIDEOS("Videos & Media", "YouTube, streaming, and media links"),
    CODE("Code & Dev", "GitHub, dev tools, and technical references"),
    ARTICLES("Articles & Reads", "Long-form articles and reading lists")
}

class SearchLinksUseCase @Inject constructor(
    private val repository: LinkRepository
) {
    operator fun invoke(query: String): Flow<List<Link>> {
        return repository.searchLinks(query)
    }

    /**
     * Filters links reactively according to Smart Space category criteria.
     */
    fun filterBySmartSpace(links: List<Link>, space: SmartSpaceCategory): List<Link> {
        return when (space) {
            SmartSpaceCategory.PINNED -> links.filter { it.isPinned || it.isPinnedNotes }
            SmartSpaceCategory.RICH_NOTES -> links.filter {
                !it.noteBodyMarkdown.isNullOrBlank() || !it.notes.isNullOrBlank()
            }
            SmartSpaceCategory.VIDEOS -> links.filter {
                it.provider.isVideo ||
                it.provider == WebsiteProvider.YOUTUBE ||
                it.provider == WebsiteProvider.NETFLIX ||
                it.provider == WebsiteProvider.PRIME_VIDEO ||
                it.url.contains("youtube.com", ignoreCase = true) ||
                it.url.contains("youtu.be", ignoreCase = true) ||
                it.url.contains("vimeo.com", ignoreCase = true)
            }
            SmartSpaceCategory.CODE -> links.filter {
                it.provider.isCode ||
                it.provider == WebsiteProvider.GITHUB ||
                it.provider == WebsiteProvider.STACK_OVERFLOW ||
                it.provider == WebsiteProvider.DEV_TO ||
                it.url.contains("github.com", ignoreCase = true) ||
                it.url.contains("gitlab.com", ignoreCase = true) ||
                it.url.contains("stackoverflow.com", ignoreCase = true)
            }
            SmartSpaceCategory.ARTICLES -> links.filter {
                it.readingTimeMinutes != null ||
                it.provider.isArticle ||
                it.provider == WebsiteProvider.MEDIUM ||
                it.provider == WebsiteProvider.WIKIPEDIA ||
                it.url.contains("medium.com", ignoreCase = true) ||
                it.url.contains("substack.com", ignoreCase = true)
            }
        }
    }

    /**
     * In-memory search query matching across all text metadata fields.
     */
    fun searchInMemory(links: List<Link>, query: String): List<Link> {
        if (query.isBlank()) return links
        val clean = query.trim()
        return links.filter { link ->
            link.title?.contains(clean, ignoreCase = true) == true ||
            link.description?.contains(clean, ignoreCase = true) == true ||
            link.url.contains(clean, ignoreCase = true) ||
            link.domain?.contains(clean, ignoreCase = true) == true ||
            link.noteTitle?.contains(clean, ignoreCase = true) == true ||
            link.noteBodyMarkdown?.contains(clean, ignoreCase = true) == true ||
            link.notes?.contains(clean, ignoreCase = true) == true ||
            link.tags.any { it.contains(clean, ignoreCase = true) }
        }
    }
}

