package com.devson.vedlink.ui.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.domain.usecase.GetAllLinksUseCase
import com.devson.vedlink.domain.usecase.SaveLinkUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentLinks: List<Link> = emptyList(),
    val totalLinks: Int = 0,
    val totalFavorites: Int = 0,
    val isLoading: Boolean = false
)

sealed class HomeUiEvent {
    data class ShowError(val message: String) : HomeUiEvent()
    data class ShowSuccess(val message: String) : HomeUiEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAllLinksUseCase: GetAllLinksUseCase,
    private val saveLinkUseCase: SaveLinkUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<HomeUiEvent>()
    val uiEvent: SharedFlow<HomeUiEvent> = _uiEvent.asSharedFlow()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getAllLinksUseCase().catch { e ->
                _uiState.update { it.copy(isLoading = false) }
                _uiEvent.emit(HomeUiEvent.ShowError(e.message ?: "Unknown error"))
            }.collect { links ->
                val recent = links.sortedByDescending { it.createdAt }.take(4)
                val total = links.size
                val favorites = links.count { it.isFavorite }
                
                _uiState.update {
                    it.copy(
                        recentLinks = recent,
                        totalLinks = total,
                        totalFavorites = favorites,
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
}
