package com.devson.vedlink.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedlink.data.preferences.ThemePreferences
import com.devson.vedlink.domain.model.Folder
import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.domain.model.ScrapedMetadata
import com.devson.vedlink.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FolderItem(
    val domain: String,
    val linkCount: Int,
    val favicon: String? = null
)

data class FoldersUiState(
    val folders: List<FolderItem> = emptyList(), // Dynamic Domain folders
    val customFolders: List<Folder> = emptyList(), // Custom DB folders
    val linksByDomain: Map<String, List<Link>> = emptyMap(),
    val linksByFolderId: Map<Int?, List<Link>> = emptyMap(),
    val isLoading: Boolean = false,
    val layoutMode: String = "list",
    val gridColumns: Int = 2,
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
    private val getFoldersUseCase: GetFoldersUseCase,
    private val createFolderUseCase: CreateFolderUseCase,
    private val updateFolderUseCase: UpdateFolderUseCase,
    private val deleteFolderUseCase: DeleteFolderUseCase,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoldersUiState())
    val uiState: StateFlow<FoldersUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<FoldersUiEvent>()
    val uiEvent: SharedFlow<FoldersUiEvent> = _uiEvent.asSharedFlow()

    init {
        loadPreferences()
        loadFoldersAndLinks()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            combine(
                themePreferences.folderLayoutMode,
                themePreferences.folderGridColumns
            ) { mode, cols ->
                Pair(mode, cols)
            }.collect { (mode, cols) ->
                _uiState.update {
                    it.copy(
                        layoutMode = mode,
                        gridColumns = cols,
                        gridCellsCount = if (mode.equals("list", ignoreCase = true)) 1 else cols
                    )
                }
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

    fun setFolderLayoutMode(mode: String) {
        viewModelScope.launch {
            themePreferences.setFolderLayoutMode(mode)
        }
    }

    fun setFolderGridColumns(columns: Int) {
        viewModelScope.launch {
            themePreferences.setFolderGridColumns(columns)
        }
    }

    fun setSortOrder(order: String) {
        viewModelScope.launch {
            themePreferences.setFolderSortOrder(order)
        }
    }

    private fun loadFoldersAndLinks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Combine links and folders streams
            combine(getAllLinksUseCase(), getFoldersUseCase()) { links, dbFolders ->
                val linksByDomain = links.groupBy { link ->
                    mapToReadableDomain(link.domain ?: "Unknown")
                }

                var domainFolders = linksByDomain.map { (domain, domainLinks) ->
                    FolderItem(
                        domain = domain,
                        linkCount = domainLinks.size
                    )
                }

                domainFolders = if (_uiState.value.sortOrder == "ASC") {
                    domainFolders.sortedBy { it.domain.lowercase() }
                } else {
                    domainFolders.sortedByDescending { it.domain.lowercase() }
                }

                val linksByFolderId = links.groupBy { it.folderId }

                Triple(domainFolders, linksByDomain, dbFolders to linksByFolderId)
            }.catch { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load folders"
                    )
                }
                _uiEvent.emit(FoldersUiEvent.ShowError(exception.message ?: "Failed to load folders"))
            }.collect { (domainFolders, linksByDomain, pair) ->
                val (dbFolders, linksByFolderId) = pair
                _uiState.update {
                    it.copy(
                        folders = domainFolders,
                        linksByDomain = linksByDomain,
                        customFolders = dbFolders,
                        linksByFolderId = linksByFolderId,
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
                val message = if (!currentFavoriteStatus) "Added to favorites" else "Removed from favorites"
                _uiEvent.emit(FoldersUiEvent.ShowSuccess(message))
            } catch (e: Exception) {
                _uiEvent.emit(FoldersUiEvent.ShowError(e.message ?: "Failed to update favorite"))
            }
        }
    }

    fun toggleFavoriteMultiple(linkIds: List<Int>) {
        viewModelScope.launch {
            try {
                val allLinks = _uiState.value.linksByDomain.values.flatten()
                val selectedLinksData = allLinks.filter { it.id in linkIds }
                val shouldBeFavorite = selectedLinksData.any { !it.isFavorite }

                linkIds.forEach { linkId ->
                    toggleFavoriteUseCase(linkId, !shouldBeFavorite)
                }

                val message = if (shouldBeFavorite) {
                    "${linkIds.size} link(s) added to favorites"
                } else {
                    "${linkIds.size} link(s) removed from favorites"
                }
                _uiEvent.emit(FoldersUiEvent.ShowSuccess(message))
            } catch (e: Exception) {
                _uiEvent.emit(FoldersUiEvent.ShowError(e.message ?: "Failed to update favorites"))
            }
        }
    }

    fun deleteLink(link: Link) {
        viewModelScope.launch {
            try {
                deleteLinkUseCase(link)
                _uiEvent.emit(FoldersUiEvent.ShowSuccess("Link deleted successfully"))
            } catch (e: Exception) {
                _uiEvent.emit(FoldersUiEvent.ShowError(e.message ?: "Failed to delete link"))
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

                _uiEvent.emit(FoldersUiEvent.ShowSuccess("${linkIds.size} link(s) deleted successfully"))
            } catch (e: Exception) {
                _uiEvent.emit(FoldersUiEvent.ShowError(e.message ?: "Failed to delete links"))
            }
        }
    }

    // Custom folder CRUD
    fun createFolder(name: String, parentId: Int? = null) {
        viewModelScope.launch {
            try {
                createFolderUseCase(name, parentId)
                _uiEvent.emit(FoldersUiEvent.ShowSuccess("Folder created"))
            } catch (e: Exception) {
                _uiEvent.emit(FoldersUiEvent.ShowError(e.message ?: "Failed to create folder"))
            }
        }
    }

    fun updateFolder(folder: Folder) {
        viewModelScope.launch {
            try {
                updateFolderUseCase(folder)
                _uiEvent.emit(FoldersUiEvent.ShowSuccess("Folder updated"))
            } catch (e: Exception) {
                _uiEvent.emit(FoldersUiEvent.ShowError(e.message ?: "Failed to update folder"))
            }
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            try {
                deleteFolderUseCase(folder)
                _uiEvent.emit(FoldersUiEvent.ShowSuccess("Folder deleted"))
            } catch (e: Exception) {
                _uiEvent.emit(FoldersUiEvent.ShowError(e.message ?: "Failed to delete folder"))
            }
        }
    }

    fun saveLink(url: String, metadata: ScrapedMetadata? = null) {
        viewModelScope.launch {
            saveLinkUseCase(
                url = url,
                title = metadata?.title,
                description = metadata?.description,
                imageUrl = metadata?.imageUrl
            )
                .onSuccess {
                    _uiEvent.emit(FoldersUiEvent.ShowSuccess("Link saved successfully"))
                }
                .onFailure { exception ->
                    _uiEvent.emit(FoldersUiEvent.ShowError(exception.message ?: "Failed to save link"))
                }
        }
    }
}