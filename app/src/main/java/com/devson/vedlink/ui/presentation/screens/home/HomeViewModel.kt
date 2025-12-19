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
            // Load view mode preference
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
            deleteLinkUseCase(link)
            _uiEvent.emit(HomeUiEvent.ShowSuccess("Link deleted"))
        }
    }

    fun toggleFavorite(id: Int, isFavorite: Boolean) {
        viewModelScope.launch {
            toggleFavoriteUseCase(id, isFavorite)
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
            // Persist the preference
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