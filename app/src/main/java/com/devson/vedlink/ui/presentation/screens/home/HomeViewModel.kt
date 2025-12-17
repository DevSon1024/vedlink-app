package com.devson.vedlink.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.domain.usecase.DeleteLinkUseCase
import com.devson.vedlink.domain.usecase.GetLinksUseCase
import com.devson.vedlink.domain.usecase.SaveLinkUseCase
import com.devson.vedlink.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val links: List<Link> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isGridView: Boolean = false,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false
)

sealed class HomeUiEvent {
    data class ShowError(val message: String) : HomeUiEvent()
    data class ShowSuccess(val message: String) : HomeUiEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getLinksUseCase: GetLinksUseCase,
    private val saveLinkUseCase: SaveLinkUseCase,
    private val deleteLinkUseCase: DeleteLinkUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<HomeUiEvent>()
    val uiEvent: SharedFlow<HomeUiEvent> = _uiEvent.asSharedFlow()

    init {
        loadLinks()
    }

    private fun loadLinks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            getLinksUseCase()
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message
                        )
                    }
                }
                .collect { links ->
                    _uiState.update {
                        it.copy(
                            links = filterLinks(links, it.searchQuery),
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                links = filterLinks(it.links, query)
            )
        }
    }

    fun toggleSearchActive() {
        _uiState.update {
            it.copy(
                isSearchActive = !it.isSearchActive,
                searchQuery = if (it.isSearchActive) "" else it.searchQuery
            )
        }
    }

    fun toggleViewMode() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun saveLink(url: String) {
        if (url.isBlank()) {
            viewModelScope.launch {
                _uiEvent.emit(HomeUiEvent.ShowError("URL cannot be empty"))
            }
            return
        }

        // Show loading state
        viewModelScope.launch {
            saveLinkUseCase(url)
                .onSuccess {
                    _uiEvent.emit(HomeUiEvent.ShowSuccess("Link saved! Fetching details..."))
                }
                .onFailure { exception ->
                    _uiEvent.emit(HomeUiEvent.ShowError(exception.message ?: "Failed to save link"))
                }
        }
    }

    fun toggleFavorite(linkId: Int, isFavorite: Boolean) {
        viewModelScope.launch {
            toggleFavoriteUseCase(linkId, !isFavorite)
                .onFailure { exception ->
                    _uiEvent.emit(HomeUiEvent.ShowError(exception.message ?: "Failed to update favorite"))
                }
        }
    }

    fun deleteLink(link: Link) {
        viewModelScope.launch {
            deleteLinkUseCase(link)
                .onSuccess {
                    _uiEvent.emit(HomeUiEvent.ShowSuccess("Link deleted"))
                }
                .onFailure { exception ->
                    _uiEvent.emit(HomeUiEvent.ShowError(exception.message ?: "Failed to delete link"))
                }
        }
    }

    private fun filterLinks(links: List<Link>, query: String): List<Link> {
        if (query.isBlank()) return links

        return links.filter { link ->
            link.title?.contains(query, ignoreCase = true) == true ||
                    link.url.contains(query, ignoreCase = true) ||
                    link.description?.contains(query, ignoreCase = true) == true
        }
    }
}
