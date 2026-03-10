package com.devson.vedlink.ui.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedlink.data.preferences.ThemePreferences
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
    val isLoading: Boolean = false,
    // Section visibility — driven by ThemePreferences
    val showStats: Boolean = true,
    val showQuickActions: Boolean = true,
    val showRecentLinks: Boolean = true
)

sealed class HomeUiEvent {
    data class ShowError(val message: String) : HomeUiEvent()
    data class ShowSuccess(val message: String) : HomeUiEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAllLinksUseCase: GetAllLinksUseCase,
    private val saveLinkUseCase: SaveLinkUseCase,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<HomeUiEvent>()
    val uiEvent: SharedFlow<HomeUiEvent> = _uiEvent.asSharedFlow()

    init {
        loadHomeData()
        loadSectionPreferences()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getAllLinksUseCase().catch { e ->
                _uiState.update { it.copy(isLoading = false) }
                _uiEvent.emit(HomeUiEvent.ShowError(e.message ?: "Unknown error"))
            }.collect { links ->
                val recent = links.sortedByDescending { it.createdAt }.take(5)
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

    private fun loadSectionPreferences() {
        viewModelScope.launch {
            themePreferences.homeShowStats.collect { v ->
                _uiState.update { it.copy(showStats = v) }
            }
        }
        viewModelScope.launch {
            themePreferences.homeShowQuickActions.collect { v ->
                _uiState.update { it.copy(showQuickActions = v) }
            }
        }
        viewModelScope.launch {
            themePreferences.homeShowRecentLinks.collect { v ->
                _uiState.update { it.copy(showRecentLinks = v) }
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
