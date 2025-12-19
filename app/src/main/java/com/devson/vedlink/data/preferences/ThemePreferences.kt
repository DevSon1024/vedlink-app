package com.devson.vedlink.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
    private val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
    private val AUTO_FETCH_METADATA = booleanPreferencesKey("auto_fetch_metadata")
    private val IS_GRID_VIEW = booleanPreferencesKey("is_grid_view")
    private val VIEW_MODE = stringPreferencesKey("view_mode")

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
}

enum class ViewMode {
    LIST,
    GRID
}