package com.devson.vedlink.ui.presentation.screens.lookandfeel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedlink.data.preferences.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LookAndFeelUiState(
    val themeMode: Int = 0, // 0=System, 1=Light, 2=Dark
    val colorScheme: Int = 0,
    val dynamicColor: Boolean = false,
    val amoledMode: Boolean = false
)

@HiltViewModel
class LookAndFeelViewModel @Inject constructor(
    private val themePreferences: ThemePreferences
) : ViewModel() {

    val uiState: StateFlow<LookAndFeelUiState> = combine(
        themePreferences.themeMode,
        themePreferences.colorScheme,
        themePreferences.dynamicColor,
        themePreferences.amoledMode
    ) { themeMode, colorScheme, dynamicColor, amoledMode ->
        LookAndFeelUiState(
            themeMode = themeMode,
            colorScheme = colorScheme,
            dynamicColor = dynamicColor,
            amoledMode = amoledMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LookAndFeelUiState()
    )

    fun setThemeMode(mode: Int) {
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
        }
    }

    fun setColorScheme(scheme: Int) {
        viewModelScope.launch {
            themePreferences.setColorScheme(scheme)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.setDynamicColor(enabled)
        }
    }

    fun setAmoledMode(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.setAmoledMode(enabled)
        }
    }
}