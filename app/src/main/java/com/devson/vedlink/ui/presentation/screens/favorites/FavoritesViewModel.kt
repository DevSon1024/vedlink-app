package com.devson.vedlink.presentation.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedlink.data.repository.LinkRepository
import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val favoriteLinks: List<Link> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: LinkRepository,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            repository.getFavoriteLinks()
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
                            favoriteLinks = links,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    fun toggleFavorite(linkId: Int) {
        viewModelScope.launch {
            toggleFavoriteUseCase(linkId, false)
        }
    }
}