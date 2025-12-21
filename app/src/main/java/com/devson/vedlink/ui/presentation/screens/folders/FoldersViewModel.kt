package com.devson.vedlink.ui.presentation.screens.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.domain.usecase.GetAllLinksUseCase
import com.devson.vedlink.domain.usecase.ToggleFavoriteUseCase
import com.devson.vedlink.domain.usecase.DeleteLinkUseCase
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
    val folders: List<FolderItem> = emptyList(),
    val linksByDomain: Map<String, List<Link>> = emptyMap(),
    val isLoading: Boolean = false,
    val isGridView: Boolean = true, // Default to Grid View
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
    private val deleteLinkUseCase: DeleteLinkUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoldersUiState())
    val uiState: StateFlow<FoldersUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<FoldersUiEvent>()
    val uiEvent: SharedFlow<FoldersUiEvent> = _uiEvent.asSharedFlow()

    init {
        loadFolders()
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
                    val linksByDomain = links.groupBy { it.domain ?: "Unknown" }
                    val folders = linksByDomain.map { (domain, domainLinks) ->
                        FolderItem(
                            domain = domain,
                            linkCount = domainLinks.size
                        )
                    }.sortedByDescending { it.linkCount }

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

    fun toggleViewMode() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
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
}