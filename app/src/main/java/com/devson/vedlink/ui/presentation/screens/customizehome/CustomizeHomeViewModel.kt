package com.devson.vedlink.ui.presentation.screens.customizehome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedlink.data.preferences.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomizeHomeUiState(
    val showStats: Boolean = true,
    val showQuickActions: Boolean = true,
    val showRecentLinks: Boolean = true
)

@HiltViewModel
class CustomizeHomeViewModel @Inject constructor(
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomizeHomeUiState())
    val uiState: StateFlow<CustomizeHomeUiState> = _uiState.asStateFlow()

    init {
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

    fun toggleShowStats() {
        viewModelScope.launch {
            themePreferences.setHomeShowStats(!_uiState.value.showStats)
        }
    }

    fun toggleShowQuickActions() {
        viewModelScope.launch {
            themePreferences.setHomeShowQuickActions(!_uiState.value.showQuickActions)
        }
    }

    fun toggleShowRecentLinks() {
        viewModelScope.launch {
            themePreferences.setHomeShowRecentLinks(!_uiState.value.showRecentLinks)
        }
    }
}
