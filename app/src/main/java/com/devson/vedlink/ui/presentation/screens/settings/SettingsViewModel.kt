package com.devson.vedlink.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedlink.data.repository.LinkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val repository: LinkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            val total = repository.getLinksCount()
            _uiState.value = _uiState.value.copy(totalLinks = total)
        }
    }

    fun toggleDarkMode() {
        _uiState.value = _uiState.value.copy(
            isDarkMode = !_uiState.value.isDarkMode
        )
    }

    fun toggleAutoFetchMetadata() {
        _uiState.value = _uiState.value.copy(
            autoFetchMetadata = !_uiState.value.autoFetchMetadata
        )
    }
}
