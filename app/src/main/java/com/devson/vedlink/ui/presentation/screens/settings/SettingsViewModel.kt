package com.devson.vedlink.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedlink.data.preferences.ThemePreferences
import com.devson.vedlink.data.repository.LinkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val totalLinks: Int = 0,
    val favoriteLinks: Int = 0,
    val isDarkMode: Boolean = false,
    val autoFetchMetadata: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: LinkRepository,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
        loadPreferences()
    }

    private fun loadStats() {
        viewModelScope.launch {
            val total = repository.getLinksCount()
            _uiState.update { it.copy(totalLinks = total) }
        }
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            themePreferences.isDarkMode.collect { isDark ->
                _uiState.update { it.copy(isDarkMode = isDark) }
            }
        }

        viewModelScope.launch {
            themePreferences.autoFetchMetadata.collect { autoFetch ->
                _uiState.update { it.copy(autoFetchMetadata = autoFetch) }
            }
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            val newValue = !_uiState.value.isDarkMode
            themePreferences.setDarkMode(newValue)
        }
    }

    fun toggleAutoFetchMetadata() {
        viewModelScope.launch {
            val newValue = !_uiState.value.autoFetchMetadata
            themePreferences.setAutoFetchMetadata(newValue)
        }
    }
}