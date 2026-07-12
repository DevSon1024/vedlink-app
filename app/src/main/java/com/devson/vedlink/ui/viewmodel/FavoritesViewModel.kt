package com.devson.vedlink.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.domain.usecase.DeleteLinkUseCase
import com.devson.vedlink.domain.usecase.SaveLinkUseCase
import com.devson.vedlink.domain.usecase.GetFavoriteLinksUseCase
import com.devson.vedlink.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.devson.vedlink.domain.model.ScrapedMetadata
import com.devson.vedlink.data.preferences.ThemePreferences
import com.devson.vedlink.ui.presentation.components.LinkViewSettings
import javax.inject.Inject

data class FavoritesUiState(
    val favoriteLinks: List<Link> = emptyList(),
    val isLoading: Boolean = false,
    val isGridView: Boolean = false,
    val gridCellsCount: Int = 1,
    val sortOrder: String = "DESC",
    val layoutMode: String = "list",
    val gridColumns: Int = 2,
    val viewSettings: LinkViewSettings = LinkViewSettings(),
    val isPrefsLoaded: Boolean = false
)

sealed class FavoritesUiEvent {
    data class ShowError(val message: String) : FavoritesUiEvent()
    data class ShowSuccess(val message: String) : FavoritesUiEvent()
}

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val getFavoriteLinksUseCase: GetFavoriteLinksUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deleteLinkUseCase: DeleteLinkUseCase,
    private val saveLinkUseCase: SaveLinkUseCase,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<FavoritesUiEvent>()
    val uiEvent: SharedFlow<FavoritesUiEvent> = _uiEvent.asSharedFlow()

    private val _rawFavorites = MutableStateFlow<List<Link>>(emptyList())

    init {
        loadPreferences()
        loadFavorites()
        observeSortedFavorites()
    }

    private fun observeSortedFavorites() {
        viewModelScope.launch {
            combine(_rawFavorites, themePreferences.favoriteSortOrder) { raw, order ->
                if (order == "ASC") raw.sortedBy { it.createdAt }
                else raw.sortedByDescending { it.createdAt }
            }.collect { sorted ->
                _uiState.update { it.copy(favoriteLinks = sorted) }
            }
        }
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            val initialLayoutMode = themePreferences.favoriteLayoutMode.first()
            val initialGridColumns = themePreferences.favoriteGridColumns.first()
            val initialSort = themePreferences.favoriteSortOrder.first()
            val initialShowFavicon = themePreferences.favoriteShowFavicon.first()
            val initialShowUrl = themePreferences.favoriteShowUrl.first()
            val initialShowTags = themePreferences.favoriteShowTags.first()
            val initialShowDateSaved = themePreferences.favoriteShowDateSaved.first()

            _uiState.update {
                it.copy(
                    layoutMode = initialLayoutMode,
                    gridColumns = initialGridColumns,
                    gridCellsCount = if (initialLayoutMode.equals("list", ignoreCase = true)) 1 else initialGridColumns,
                    sortOrder = initialSort,
                    viewSettings = LinkViewSettings(
                        showFavicon = initialShowFavicon,
                        showUrl = initialShowUrl,
                        showTags = initialShowTags,
                        showDateSaved = initialShowDateSaved
                    ),
                    isGridView = initialLayoutMode.equals("grid", ignoreCase = true),
                    isPrefsLoaded = true
                )
            }

            launch {
                combine(
                    themePreferences.favoriteLayoutMode,
                    themePreferences.favoriteGridColumns
                ) { mode, cols ->
                    Pair(mode, cols)
                }.collect { (mode, cols) ->
                    _uiState.update {
                        it.copy(
                            layoutMode = mode,
                            gridColumns = cols,
                            gridCellsCount = if (mode.equals("list", ignoreCase = true)) 1 else cols,
                            isGridView = mode.equals("grid", ignoreCase = true)
                        )
                    }
                }
            }

            launch {
                themePreferences.favoriteSortOrder.collect { order ->
                    _uiState.update { it.copy(sortOrder = order) }
                }
            }

            launch {
                combine(
                    themePreferences.favoriteShowFavicon,
                    themePreferences.favoriteShowUrl,
                    themePreferences.favoriteShowTags,
                    themePreferences.favoriteShowDateSaved
                ) { favicon, url, tags, date ->
                    LinkViewSettings(
                        showFavicon = favicon,
                        showUrl = url,
                        showTags = tags,
                        showDateSaved = date
                    )
                }.collect { settings ->
                    _uiState.update { it.copy(viewSettings = settings) }
                }
            }
        }
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            getFavoriteLinksUseCase()
                .catch { exception ->
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEvent.emit(
                        FavoritesUiEvent.ShowError(
                            exception.message ?: "Failed to load favorites"
                        )
                    )
                }
                .collect { links ->
                    _rawFavorites.value = links
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    fun setLayoutMode(mode: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    layoutMode = mode,
                    gridCellsCount = if (mode.equals("list", ignoreCase = true)) 1 else it.gridColumns,
                    isGridView = mode.equals("grid", ignoreCase = true)
                )
            }
            themePreferences.setFavoriteLayoutMode(mode)
        }
    }

    fun setGridColumns(columns: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    gridColumns = columns,
                    gridCellsCount = if (it.layoutMode.equals("list", ignoreCase = true)) 1 else columns
                )
            }
            themePreferences.setFavoriteGridColumns(columns)
        }
    }

    fun setViewSettings(settings: LinkViewSettings) {
        viewModelScope.launch {
            _uiState.update { it.copy(viewSettings = settings) }
            themePreferences.setFavoriteShowFavicon(settings.showFavicon)
            themePreferences.setFavoriteShowUrl(settings.showUrl)
            themePreferences.setFavoriteShowTags(settings.showTags)
            themePreferences.setFavoriteShowDateSaved(settings.showDateSaved)
        }
    }

    fun setSortOrder(order: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(sortOrder = order) }
            themePreferences.setFavoriteSortOrder(order)
        }
    }

    fun toggleFavorite(id: Int, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                toggleFavoriteUseCase(id, isFavorite)
                val message = if (!isFavorite) "Added to favorites" else "Removed from favorites"
                _uiEvent.emit(FavoritesUiEvent.ShowSuccess(message))
            } catch (e: Exception) {
                _uiEvent.emit(
                    FavoritesUiEvent.ShowError(
                        e.message ?: "Failed to update favorite"
                    )
                )
            }
        }
    }

    fun toggleFavoriteMultiple(linkIds: List<Int>) {
        viewModelScope.launch {
            try {
                val selectedLinksData = _uiState.value.favoriteLinks.filter { it.id in linkIds }
                // In favorites screen, all selected are favorites, so we remove them
                val shouldBeFavorite = false

                linkIds.forEach { linkId ->
                    toggleFavoriteUseCase(linkId, !shouldBeFavorite)
                }

                _uiEvent.emit(
                    FavoritesUiEvent.ShowSuccess(
                        "${linkIds.size} link(s) removed from favorites"
                    )
                )
            } catch (e: Exception) {
                _uiEvent.emit(
                    FavoritesUiEvent.ShowError(
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
                _uiEvent.emit(FavoritesUiEvent.ShowSuccess("Link deleted"))
            } catch (e: Exception) {
                _uiEvent.emit(
                    FavoritesUiEvent.ShowError(
                        e.message ?: "Failed to delete link"
                    )
                )
            }
        }
    }

    fun deleteLinks(linkIds: List<Int>) {
        viewModelScope.launch {
            try {
                val linksToDelete = _uiState.value.favoriteLinks.filter { it.id in linkIds }
                linksToDelete.forEach { link ->
                    deleteLinkUseCase(link)
                }
                _uiEvent.emit(
                    FavoritesUiEvent.ShowSuccess(
                        "${linkIds.size} link(s) deleted"
                    )
                )
            } catch (e: Exception) {
                _uiEvent.emit(
                    FavoritesUiEvent.ShowError(
                        e.message ?: "Failed to delete links"
                    )
                )
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
                    _uiEvent.emit(FavoritesUiEvent.ShowSuccess("Link saved successfully"))
                }
                .onFailure { exception ->
                    _uiEvent.emit(FavoritesUiEvent.ShowError(exception.message ?: "Failed to save link"))
                }
        }
    }
}