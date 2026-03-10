package com.devson.vedlink.ui.presentation.screens.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.domain.usecase.GetAllLinksUseCase
import com.devson.vedlink.domain.usecase.ToggleFavoriteUseCase
import com.devson.vedlink.domain.usecase.DeleteLinkUseCase
import com.devson.vedlink.domain.usecase.SaveLinkUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.devson.vedlink.data.preferences.ThemePreferences
import javax.inject.Inject

data class FolderItem(
    val domain: String,
    val linkCount: Int,
    val favicon: String? = null
)

data class FoldersUiState(
    val folders: List<FolderItem> = emptyList(),
    val linksByDomain: Map<String, List<Link>> = emptyMap(),
    val isLoading: Boolean = false,
    val gridCellsCount: Int = 2,
    val sortOrder: String = "ASC",
    val isPrefsLoaded: Boolean = false,
    val error: String? = null
)

sealed class FoldersUiEvent {
    data class ShowError(val message: String) : FoldersUiEvent()
    data class ShowSuccess(val message: String) : FoldersUiEvent()
}

@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val getAllLinksUseCase: GetAllLinksUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deleteLinkUseCase: DeleteLinkUseCase,
    private val saveLinkUseCase: SaveLinkUseCase,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoldersUiState())
    val uiState: StateFlow<FoldersUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<FoldersUiEvent>()
    val uiEvent: SharedFlow<FoldersUiEvent> = _uiEvent.asSharedFlow()

    init {
        loadPreferences()
        loadFolders()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            themePreferences.folderGridCellsCount.collect { count ->
                _uiState.update { it.copy(gridCellsCount = count) }
                // Delay setting isPrefsLoaded until we have both, or just set it here since they're fast
                if (!_uiState.value.isPrefsLoaded) {
                    _uiState.update { it.copy(isPrefsLoaded = true) }
                }
            }
        }
        viewModelScope.launch {
            themePreferences.folderSortOrder.collect { order ->
                val sortingChanged = _uiState.value.sortOrder != order && _uiState.value.isPrefsLoaded
                _uiState.update { it.copy(sortOrder = order) }
                if (sortingChanged) {
                    resortFolders()
                }
            }
        }
    }

    private fun resortFolders() {
        _uiState.update { state ->
            val order = state.sortOrder
            val sorted = if (order == "ASC") {
                state.folders.sortedBy { it.domain.lowercase() }
            } else {
                state.folders.sortedByDescending { it.domain.lowercase() }
            }
            state.copy(folders = sorted)
        }
    }

    fun setGridCellsCount(count: Int) {
        viewModelScope.launch {
            themePreferences.setFolderGridCellsCount(count)
        }
    }

    fun setSortOrder(order: String) {
        viewModelScope.launch {
            themePreferences.setFolderSortOrder(order)
        }
    }

    private fun loadFolders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            getAllLinksUseCase()
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load folders"
                        )
                    }
                    _uiEvent.emit(
                        FoldersUiEvent.ShowError(
                            exception.message ?: "Failed to load folders"
                        )
                    )
                }
                .collect { links ->
                    // Map domains to readable names BEFORE grouping
                    val linksByDomain = links.groupBy { link ->
                        mapToReadableDomain(link.domain ?: "Unknown")
                    }

                    var folders = linksByDomain.map { (domain, domainLinks) ->
                        FolderItem(
                            domain = domain,
                            linkCount = domainLinks.size
                        )
                    }
                    
                    folders = if (_uiState.value.sortOrder == "ASC") {
                        folders.sortedBy { it.domain.lowercase() }
                    } else {
                        folders.sortedByDescending { it.domain.lowercase() }
                    }

                    _uiState.update {
                        it.copy(
                            folders = folders,
                            linksByDomain = linksByDomain,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    private fun mapToReadableDomain(domain: String): String {
        return when (domain.lowercase()) {
            "t.me" -> "Telegram"
            "youtu.be", "youtube.com" -> "YouTube"
            "instagr.am", "instagram.com" -> "Instagram"
            "wa.me" -> "WhatsApp"
            "twitter.com", "x.com" -> "X (Twitter)"
            "linkedin.com", "lnkd.in" -> "LinkedIn"
            "amzn.to", "amazon.com" -> "Amazon"
            "fb.com", "facebook.com", "fb.watch" -> "Facebook"
            "pin.it", "pinterest.com" -> "Pinterest"
            "github.com" -> "GitHub"
            "stackoverflow.com" -> "Stack Overflow"
            "medium.com" -> "Medium"
            else -> domain.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }


    fun toggleFavorite(linkId: Int, currentFavoriteStatus: Boolean) {
        viewModelScope.launch {
            try {
                toggleFavoriteUseCase(linkId, currentFavoriteStatus)

                val message = if (!currentFavoriteStatus) {
                    "Added to favorites"
                } else {
                    "Removed from favorites"
                }
                _uiEvent.emit(FoldersUiEvent.ShowSuccess(message))
            } catch (e: Exception) {
                _uiEvent.emit(
                    FoldersUiEvent.ShowError(
                        e.message ?: "Failed to update favorite"
                    )
                )
            }
        }
    }

    fun toggleFavoriteMultiple(linkIds: List<Int>) {
        viewModelScope.launch {
            try {
                val allLinks = _uiState.value.linksByDomain.values.flatten()
                val selectedLinksData = allLinks.filter { it.id in linkIds }

                // Determine new favorite status - if any is not favorite, make all favorite
                val shouldBeFavorite = selectedLinksData.any { !it.isFavorite }

                linkIds.forEach { linkId ->
                    val link = allLinks.find { it.id == linkId }
                    link?.let {
                        toggleFavoriteUseCase(linkId, !shouldBeFavorite)
                    }
                }

                val message = if (shouldBeFavorite) {
                    "${linkIds.size} link(s) added to favorites"
                } else {
                    "${linkIds.size} link(s) removed from favorites"
                }
                _uiEvent.emit(FoldersUiEvent.ShowSuccess(message))
            } catch (e: Exception) {
                _uiEvent.emit(
                    FoldersUiEvent.ShowError(
                        e.message ?: "Failed to update favorites"
                    )
                )
            }
        }
    }

    fun deleteLink(link: Link) {
        viewModelScope.launch {
            try {
                deleteLinkUseCase(link)
                _uiEvent.emit(FoldersUiEvent.ShowSuccess("Link deleted successfully"))
            } catch (e: Exception) {
                _uiEvent.emit(
                    FoldersUiEvent.ShowError(
                        e.message ?: "Failed to delete link"
                    )
                )
            }
        }
    }

    fun deleteLinks(linkIds: List<Int>) {
        viewModelScope.launch {
            try {
                val allLinks = _uiState.value.linksByDomain.values.flatten()
                val linksToDelete = allLinks.filter { it.id in linkIds }

                linksToDelete.forEach { link ->
                    deleteLinkUseCase(link)
                }

                _uiEvent.emit(
                    FoldersUiEvent.ShowSuccess(
                        "${linkIds.size} link(s) deleted successfully"
                    )
                )
            } catch (e: Exception) {
                _uiEvent.emit(
                    FoldersUiEvent.ShowError(
                        e.message ?: "Failed to delete links"
                    )
                )
            }
        }
    }

    fun refreshFolders() {
        loadFolders()
    }

    fun saveLink(url: String) {
        viewModelScope.launch {
            saveLinkUseCase(url)
                .onSuccess {
                    _uiEvent.emit(FoldersUiEvent.ShowSuccess("Link saved successfully"))
                }
                .onFailure { exception ->
                    _uiEvent.emit(FoldersUiEvent.ShowError(exception.message ?: "Failed to save link"))
                }
        }
    }
}