package com.devson.vedlink.ui.presentation.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.domain.usecase.DeleteLinkUseCase
import com.devson.vedlink.domain.usecase.GetFavoriteLinksUseCase
import com.devson.vedlink.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val favoriteLinks: List<Link> = emptyList(),
    val isLoading: Boolean = false,
    val isGridView: Boolean = false
)

sealed class FavoritesUiEvent {
    data class ShowError(val message: String) : FavoritesUiEvent()
    data class ShowSuccess(val message: String) : FavoritesUiEvent()
}

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val getFavoriteLinksUseCase: GetFavoriteLinksUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deleteLinkUseCase: DeleteLinkUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<FavoritesUiEvent>()
    val uiEvent: SharedFlow<FavoritesUiEvent> = _uiEvent.asSharedFlow()

    init {
        loadFavorites()
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
                    _uiState.update {
                        it.copy(
                            favoriteLinks = links,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun toggleViewMode() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
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
}