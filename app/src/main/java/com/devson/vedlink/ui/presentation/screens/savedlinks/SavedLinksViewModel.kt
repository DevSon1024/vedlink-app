package com.devson.vedlink.ui.presentation.screens.savedlinks

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

data class SavedLinksUiState(
    val links: List<Link> = emptyList(),
    val isLoading: Boolean = false,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val gridCellsCount: Int = 1,
    val sortOrder: String = "DESC",  // "DESC" = Latest first, "ASC" = Oldest first
    val isPrefsLoaded: Boolean = false
)

sealed class SavedLinksUiEvent {
    data class ShowError(val message: String) : SavedLinksUiEvent()
    data class ShowSuccess(val message: String) : SavedLinksUiEvent()
}

@HiltViewModel
class SavedLinksViewModel @Inject constructor(
    private val getAllLinksUseCase: GetAllLinksUseCase,
    private val saveLinkUseCase: SaveLinkUseCase,
    private val deleteLinkUseCase: DeleteLinkUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val searchLinksUseCase: SearchLinksUseCase,
    private val workManagerHelper: WorkManagerHelper,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavedLinksUiState())
    val uiState: StateFlow<SavedLinksUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<SavedLinksUiEvent>()
    val uiEvent: SharedFlow<SavedLinksUiEvent> = _uiEvent.asSharedFlow()

    @kotlin.OptIn(kotlinx.coroutines.FlowPreview::class)
    private val rawSearchQuery = MutableStateFlow("")

    // Internal flow for raw links (before sorting)
    private val _rawLinks = MutableStateFlow<List<Link>>(emptyList())

    init {
        loadPreferences()
        loadLinks()
        setupSearchDebouncing()
        observeSortedLinks()
    }

    /** Observe raw links + sortOrder together so the list re-sorts whenever either changes. */
    private fun observeSortedLinks() {
        viewModelScope.launch {
            combine(_rawLinks, themePreferences.sortOrder) { raw, order ->
                if (order == "ASC") raw.sortedBy { it.createdAt }
                else raw.sortedByDescending { it.createdAt }
            }.collect { sorted ->
                _uiState.update { it.copy(links = sorted) }
            }
        }
    }

    @kotlin.OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun setupSearchDebouncing() {
        viewModelScope.launch {
            rawSearchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isNotEmpty()) {
                        searchLinks(query)
                    } else if (_uiState.value.isSearchActive) {
                        loadLinks()
                    }
                }
        }
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            val initialGrid = themePreferences.gridCellsCount.first()
            val initialSort = themePreferences.sortOrder.first()
            _uiState.update {
                it.copy(
                    gridCellsCount = initialGrid,
                    sortOrder = initialSort,
                    isPrefsLoaded = true
                )
            }

            launch {
                themePreferences.gridCellsCount.collect { count ->
                    _uiState.update { it.copy(gridCellsCount = count) }
                }
            }
            launch {
                themePreferences.sortOrder.collect { order ->
                    _uiState.update { it.copy(sortOrder = order) }
                }
            }
        }
    }

    private fun loadLinks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            getAllLinksUseCase()
                .catch { exception ->
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEvent.emit(SavedLinksUiEvent.ShowError(exception.message ?: "Unknown error"))
                }
                .collect { links ->
                    _rawLinks.value = links
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    fun saveLink(url: String) {
        viewModelScope.launch {
            saveLinkUseCase(url)
                .onSuccess {
                    _uiEvent.emit(SavedLinksUiEvent.ShowSuccess("Link saved successfully"))
                }
                .onFailure { exception ->
                    _uiEvent.emit(SavedLinksUiEvent.ShowError(exception.message ?: "Failed to save link"))
                }
        }
    }

    fun deleteLink(link: Link) {
        viewModelScope.launch {
            try {
                deleteLinkUseCase(link)
                _uiEvent.emit(SavedLinksUiEvent.ShowSuccess("Link deleted"))
            } catch (e: Exception) {
                _uiEvent.emit(SavedLinksUiEvent.ShowError(e.message ?: "Failed to delete link"))
            }
        }
    }

    fun deleteLinks(linkIds: List<Int>) {
        viewModelScope.launch {
            try {
                val linksToDelete = _uiState.value.links.filter { it.id in linkIds }
                linksToDelete.forEach { link -> deleteLinkUseCase(link) }
                _uiEvent.emit(SavedLinksUiEvent.ShowSuccess("${linkIds.size} link(s) deleted"))
            } catch (e: Exception) {
                _uiEvent.emit(SavedLinksUiEvent.ShowError(e.message ?: "Failed to delete links"))
            }
        }
    }

    fun toggleFavorite(id: Int, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                toggleFavoriteUseCase(id, isFavorite)
                val message = if (!isFavorite) "Added to favorites" else "Removed from favorites"
                _uiEvent.emit(SavedLinksUiEvent.ShowSuccess(message))
            } catch (e: Exception) {
                _uiEvent.emit(SavedLinksUiEvent.ShowError(e.message ?: "Failed to update favorite"))
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
                    link?.let { toggleFavoriteUseCase(linkId, !shouldBeFavorite) }
                }
                val message = if (shouldBeFavorite) {
                    "${linkIds.size} link(s) added to favorites"
                } else {
                    "${linkIds.size} link(s) removed from favorites"
                }
                _uiEvent.emit(SavedLinksUiEvent.ShowSuccess(message))
            } catch (e: Exception) {
                _uiEvent.emit(SavedLinksUiEvent.ShowError(e.message ?: "Failed to update favorites"))
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
        rawSearchQuery.value = query
    }

    private fun searchLinks(query: String) {
        viewModelScope.launch {
            searchLinksUseCase(query)
                .catch { exception ->
                    _uiEvent.emit(SavedLinksUiEvent.ShowError(exception.message ?: "Search failed"))
                }
                .collect { links ->
                    _rawLinks.value = links
                }
        }
    }

    /** Update grid columns count (1–6) and persist to DataStore. */
    fun setGridCellsCount(count: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(gridCellsCount = count.coerceIn(1, 6)) }
            themePreferences.setGridCellsCount(count)
        }
    }

    /** Update sort order ("DESC" or "ASC") and persist to DataStore. */
    fun setSortOrder(order: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(sortOrder = order) }
            themePreferences.setSortOrder(order)
        }
    }

    fun refreshMetadata() {
        viewModelScope.launch {
            _uiState.value.links.forEach { link ->
                // User explicitly requested refresh → bypass cache
                workManagerHelper.enqueueLinkMetadataFetch(link.id, isForcedRefresh = true)
            }
            _uiEvent.emit(SavedLinksUiEvent.ShowSuccess("Refreshing metadata..."))
        }
    }

    fun refreshLink(linkId: Int) {
        viewModelScope.launch {
            // User explicitly requested refresh → bypass cache
            workManagerHelper.enqueueLinkMetadataFetch(linkId, isForcedRefresh = true)
            _uiEvent.emit(SavedLinksUiEvent.ShowSuccess("Refreshing link..."))
        }
    }
}