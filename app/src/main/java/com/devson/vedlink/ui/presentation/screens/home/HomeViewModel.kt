package com.devson.vedlink.ui.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedlink.data.preferences.ThemePreferences
import com.devson.vedlink.data.worker.WorkManagerHelper
import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val links: List<Link> = emptyList(),
    val isLoading: Boolean = false,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val isGridView: Boolean = false
)

sealed class HomeUiEvent {
    data class ShowError(val message: String) : HomeUiEvent()
    data class ShowSuccess(val message: String) : HomeUiEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAllLinksUseCase: GetAllLinksUseCase,
    private val saveLinkUseCase: SaveLinkUseCase,
    private val deleteLinkUseCase: DeleteLinkUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val searchLinksUseCase: SearchLinksUseCase,
    private val workManagerHelper: WorkManagerHelper,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<HomeUiEvent>()
    val uiEvent: SharedFlow<HomeUiEvent> = _uiEvent.asSharedFlow()

    init {
        loadLinks()
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            themePreferences.isGridView.collect { isGrid ->
                _uiState.update { it.copy(isGridView = isGrid) }
            }
        }
    }

    private fun loadLinks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            getAllLinksUseCase()
                .catch { exception ->
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEvent.emit(HomeUiEvent.ShowError(exception.message ?: "Unknown error"))
                }
                .collect { links ->
                    _uiState.update {
                        it.copy(
                            links = links,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun saveLink(url: String) {
        viewModelScope.launch {
            saveLinkUseCase(url)
                .onSuccess {
                    _uiEvent.emit(HomeUiEvent.ShowSuccess("Link saved successfully"))
                }
                .onFailure { exception ->
                    _uiEvent.emit(HomeUiEvent.ShowError(exception.message ?: "Failed to save link"))
                }
        }
    }

    fun deleteLink(link: Link) {
        viewModelScope.launch {
            try {
                deleteLinkUseCase(link)
                _uiEvent.emit(HomeUiEvent.ShowSuccess("Link deleted"))
            } catch (e: Exception) {
                _uiEvent.emit(HomeUiEvent.ShowError(e.message ?: "Failed to delete link"))
            }
        }
    }

    fun deleteLinks(linkIds: List<Int>) {
        viewModelScope.launch {
            try {
                val linksToDelete = _uiState.value.links.filter { it.id in linkIds }
                linksToDelete.forEach { link ->
                    deleteLinkUseCase(link)
                }
                _uiEvent.emit(HomeUiEvent.ShowSuccess("${linkIds.size} link(s) deleted"))
            } catch (e: Exception) {
                _uiEvent.emit(HomeUiEvent.ShowError(e.message ?: "Failed to delete links"))
            }
        }
    }

    fun toggleFavorite(id: Int, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                toggleFavoriteUseCase(id, isFavorite)
                val message = if (!isFavorite) "Added to favorites" else "Removed from favorites"
                _uiEvent.emit(HomeUiEvent.ShowSuccess(message))
            } catch (e: Exception) {
                _uiEvent.emit(HomeUiEvent.ShowError(e.message ?: "Failed to update favorite"))
            }
        }
    }

    fun toggleFavoriteMultiple(linkIds: List<Int>) {
        viewModelScope.launch {
            try {
                val selectedLinksData = _uiState.value.links.filter { it.id in linkIds }
                val shouldBeFavorite = selectedLinksData.any { !it.isFavorite }

                linkIds.forEach { linkId ->
                    val link = _uiState.value.links.find { it.id == linkId }
                    link?.let {
                        toggleFavoriteUseCase(linkId, !shouldBeFavorite)
                    }
                }

                val message = if (shouldBeFavorite) {
                    "${linkIds.size} link(s) added to favorites"
                } else {
                    "${linkIds.size} link(s) removed from favorites"
                }
                _uiEvent.emit(HomeUiEvent.ShowSuccess(message))
            } catch (e: Exception) {
                _uiEvent.emit(HomeUiEvent.ShowError(e.message ?: "Failed to update favorites"))
            }
        }
    }

    fun toggleSearchActive() {
        _uiState.update {
            it.copy(
                isSearchActive = !it.isSearchActive,
                searchQuery = if (!it.isSearchActive) "" else it.searchQuery
            )
        }
        if (!_uiState.value.isSearchActive) {
            loadLinks()
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isNotEmpty()) {
            searchLinks(query)
        } else {
            loadLinks()
        }
    }

    private fun searchLinks(query: String) {
        viewModelScope.launch {
            searchLinksUseCase(query)
                .catch { exception ->
                    _uiEvent.emit(HomeUiEvent.ShowError(exception.message ?: "Search failed"))
                }
                .collect { links ->
                    _uiState.update { it.copy(links = links) }
                }
        }
    }

    fun toggleViewMode() {
        viewModelScope.launch {
            val newGridValue = !_uiState.value.isGridView
            _uiState.update { it.copy(isGridView = newGridValue) }
            themePreferences.setGridView(newGridValue)
        }
    }

    fun refreshMetadata() {
        viewModelScope.launch {
            _uiState.value.links.forEach { link ->
                workManagerHelper.enqueueLinkMetadataFetch(link.id)
            }
            _uiEvent.emit(HomeUiEvent.ShowSuccess("Refreshing metadata..."))
        }
    }
}