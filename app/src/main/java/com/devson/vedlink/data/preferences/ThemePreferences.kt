package com.devson.vedlink.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Existing preferences
    private val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
    private val AUTO_FETCH_METADATA = booleanPreferencesKey("auto_fetch_metadata")
    private val IS_GRID_VIEW = booleanPreferencesKey("is_grid_view")
    private val VIEW_MODE = stringPreferencesKey("view_mode")

    // New Look & Feel preferences
    private val THEME_MODE_KEY = intPreferencesKey("theme_mode")
    private val COLOR_SCHEME_KEY = intPreferencesKey("color_scheme")
    private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
    private val AMOLED_MODE_KEY = booleanPreferencesKey("amoled_mode")

    // Existing flows
    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_DARK_MODE] ?: false
    }

    val autoFetchMetadata: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_FETCH_METADATA] ?: true
    }

    val isGridView: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_GRID_VIEW] ?: false
    }

    val viewMode: Flow<ViewMode> = context.dataStore.data.map { preferences ->
        when (preferences[VIEW_MODE]) {
            ViewMode.GRID.name -> ViewMode.GRID
            ViewMode.LIST.name -> ViewMode.LIST
            else -> ViewMode.LIST // Default to list view
        }
    }

    // New Look & Feel flows
    val themeMode: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[THEME_MODE_KEY] ?: 0 } // 0=System, 1=Light, 2=Dark

    val colorScheme: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[COLOR_SCHEME_KEY] ?: 0 } // 0=Blue, 1=Green, etc.

    val dynamicColor: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[DYNAMIC_COLOR_KEY] ?: false }

    val amoledMode: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[AMOLED_MODE_KEY] ?: false }

    // Existing functions
    suspend fun setDarkMode(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_DARK_MODE] = isDark
        }
    }

    suspend fun setAutoFetchMetadata(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_FETCH_METADATA] = enabled
        }
    }

    suspend fun setGridView(isGrid: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_GRID_VIEW] = isGrid
            preferences[VIEW_MODE] = if (isGrid) ViewMode.GRID.name else ViewMode.LIST.name
        }
    }

    suspend fun setViewMode(mode: ViewMode) {
        context.dataStore.edit { preferences ->
            preferences[VIEW_MODE] = mode.name
            preferences[IS_GRID_VIEW] = mode == ViewMode.GRID
        }
    }

    // New Look & Feel functions
    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode
        }
    }

    suspend fun setColorScheme(scheme: Int) {
        context.dataStore.edit { preferences ->
            preferences[COLOR_SCHEME_KEY] = scheme
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR_KEY] = enabled
        }
    }

    suspend fun setAmoledMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AMOLED_MODE_KEY] = enabled
        }
    }
}

enum class ViewMode {
    LIST,
    GRID
}