package com.devson.vedlink.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.domain.model.Folder
import com.devson.vedlink.domain.usecase.GetFoldersUseCase
import com.devson.vedlink.domain.usecase.DeleteLinkUseCase
import com.devson.vedlink.domain.usecase.GetLinkByIdUseCase
import com.devson.vedlink.domain.usecase.ToggleFavoriteUseCase
import com.devson.vedlink.domain.usecase.UpdateLinkUseCase
import com.devson.vedlink.domain.usecase.GetAllTagsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LinkDetailsUiState(
    val link: Link? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showQrCodeDialog: Boolean = false,
    val readabilityText: String? = null,
    val isReadingModeActive: Boolean = false,
    val isExtractingReaderText: Boolean = false
)

@HiltViewModel
class LinkDetailsViewModel @Inject constructor(
    private val getLinkByIdUseCase: GetLinkByIdUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deleteLinkUseCase: DeleteLinkUseCase,
    private val updateLinkUseCase: UpdateLinkUseCase,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    getFoldersUseCase: GetFoldersUseCase
) : ViewModel() {

    val folders: StateFlow<List<Folder>> = getFoldersUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags: StateFlow<List<String>> = getAllTagsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(LinkDetailsUiState())
    val uiState: StateFlow<LinkDetailsUiState> = _uiState.asStateFlow()

    fun loadLink(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            getLinkByIdUseCase(id)
                .onSuccess { link ->
                    _uiState.update {
                        it.copy(
                            link = link,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message
                        )
                    }
                }
        }
    }

    fun toggleFavorite() {
        val currentLink = _uiState.value.link ?: return
        viewModelScope.launch {
            toggleFavoriteUseCase(currentLink.id, currentLink.isFavorite)
            _uiState.update {
                it.copy(
                    link = currentLink.copy(isFavorite = !currentLink.isFavorite)
                )
            }
        }
    }

    fun deleteLink() {
        val currentLink = _uiState.value.link ?: return
        viewModelScope.launch {
            deleteLinkUseCase(currentLink)
        }
    }

    fun updateNotes(notes: String) {
        val currentLink = _uiState.value.link ?: return
        val updated = currentLink.copy(
            notes = notes,
            notesUpdatedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            updateLinkUseCase(updated)
            _uiState.update { it.copy(link = updated) }
        }
    }

    fun assignFolder(folderId: Int?) {
        val currentLink = _uiState.value.link ?: return
        val updated = currentLink.copy(
            folderId = folderId,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            updateLinkUseCase(updated)
            _uiState.update { it.copy(link = updated) }
        }
    }

    fun addTag(tag: String) {
        val currentLink = _uiState.value.link ?: return
        val cleanTag = tag.trim().lowercase()
        if (cleanTag.isBlank() || currentLink.tags.contains(cleanTag)) return
        val updatedTags = currentLink.tags + cleanTag
        val updated = currentLink.copy(
            tags = updatedTags,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            updateLinkUseCase(updated)
            _uiState.update { it.copy(link = updated) }
        }
    }

    fun removeTag(tag: String) {
        val currentLink = _uiState.value.link ?: return
        val updatedTags = currentLink.tags - tag
        val updated = currentLink.copy(
            tags = updatedTags,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            updateLinkUseCase(updated)
            _uiState.update { it.copy(link = updated) }
        }
    }

    fun updateUrl(newUrl: String) {
        val currentLink = _uiState.value.link ?: return
        val trimmedUrl = newUrl.trim()
        val domain = try {
            val uri = java.net.URI(trimmedUrl)
            uri.host?.removePrefix("www.") ?: currentLink.domain
        } catch (e: Exception) {
            currentLink.domain
        }
        val updated = currentLink.copy(
            url = trimmedUrl,
            canonicalUrl = trimmedUrl,
            domain = domain,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            updateLinkUseCase(updated)
            _uiState.update { it.copy(link = updated) }
        }
    }

    fun showQrCodeDialog(show: Boolean) {
        _uiState.update { it.copy(showQrCodeDialog = show) }
    }

    fun extractReadabilityText(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExtractingReaderText = true, isReadingModeActive = true) }
            kotlinx.coroutines.delay(1000)
            _uiState.update {
                it.copy(
                    isExtractingReaderText = false,
                    readabilityText = """
                        Reader View Mode
                        URL: ${url}
                        
                        This is a reader-friendly representation of the web article. Readability mode strips away extraneous styling, advertisements, layout constraints, and script elements to expose clean text optimized for user reading.
                        
                        The backend text extraction logic can be integrated into the LinkDetailsViewModel by parsing the document body or utilizing a custom scraper here.
                    """.trimIndent()
                )
            }
        }
    }

    fun exitReadingMode() {
        _uiState.update {
            it.copy(
                isReadingModeActive = false,
                readabilityText = null
            )
        }
    }
}