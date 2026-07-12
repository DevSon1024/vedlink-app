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
    // Legacy keys (kept for compatibility)
    private val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
    private val AUTO_FETCH_METADATA = booleanPreferencesKey("auto_fetch_metadata")
    private val IS_GRID_VIEW = booleanPreferencesKey("is_grid_view")
    private val VIEW_MODE = stringPreferencesKey("view_mode")

    // Look & Feel preferences
    private val THEME_MODE_KEY = intPreferencesKey("theme_mode")
    private val COLOR_SCHEME_KEY = intPreferencesKey("color_scheme")
    private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
    private val AMOLED_MODE_KEY = booleanPreferencesKey("amoled_mode")

    // View Settings preferences
    private val GRID_CELLS_COUNT_KEY = intPreferencesKey("grid_cells_count")
    private val SORT_ORDER_KEY = stringPreferencesKey("sort_order")
    private val LINK_SHOW_FAVICON_KEY = booleanPreferencesKey("link_show_favicon")
    private val LINK_SHOW_URL_KEY = booleanPreferencesKey("link_show_url")
    private val LINK_SHOW_TAGS_KEY = booleanPreferencesKey("link_show_tags")
    private val LINK_SHOW_DATE_SAVED_KEY = booleanPreferencesKey("link_show_date_saved")
    private val LINK_GRID_COLUMNS_KEY = intPreferencesKey("link_grid_columns")
    private val LINK_LAYOUT_MODE_KEY = stringPreferencesKey("link_layout_mode")

    // Folder View Settings preferences
    private val FOLDER_GRID_CELLS_COUNT_KEY = intPreferencesKey("folder_grid_cells_count")
    private val FOLDER_SORT_ORDER_KEY = stringPreferencesKey("folder_sort_order")
    private val FOLDER_LAYOUT_MODE_KEY = stringPreferencesKey("folder_layout_mode")
    private val FOLDER_GRID_COLUMNS_KEY = intPreferencesKey("folder_grid_columns")

    // Favorite View Settings preferences
    private val FAVORITE_GRID_CELLS_COUNT_KEY = intPreferencesKey("favorite_grid_cells_count")
    private val FAVORITE_SORT_ORDER_KEY = stringPreferencesKey("favorite_sort_order")
    private val FAVORITE_LAYOUT_MODE_KEY = stringPreferencesKey("favorite_layout_mode")
    private val FAVORITE_GRID_COLUMNS_KEY = intPreferencesKey("favorite_grid_columns")
    private val FAVORITE_SHOW_FAVICON_KEY = booleanPreferencesKey("favorite_show_favicon")
    private val FAVORITE_SHOW_URL_KEY = booleanPreferencesKey("favorite_show_url")
    private val FAVORITE_SHOW_TAGS_KEY = booleanPreferencesKey("favorite_show_tags")
    private val FAVORITE_SHOW_DATE_SAVED_KEY = booleanPreferencesKey("favorite_show_date_saved")

    // Home section visibility preferences
    private val HOME_SHOW_STATS_KEY = booleanPreferencesKey("home_show_stats")
    private val HOME_SHOW_QUICK_ACTIONS_KEY = booleanPreferencesKey("home_show_quick_actions")
    private val HOME_SHOW_RECENT_LINKS_KEY = booleanPreferencesKey("home_show_recent_links")

    // Additional Appearance Preferences
    private val NAV_BAR_TRANSPARENT_KEY = booleanPreferencesKey("nav_bar_transparent")

    // Flows
    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_DARK_MODE] ?: false
    }

    val autoFetchMetadata: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_FETCH_METADATA] ?: true
    }

    val viewMode: Flow<ViewMode> = context.dataStore.data.map { preferences ->
        when (preferences[VIEW_MODE]) {
            ViewMode.GRID.name -> ViewMode.GRID
            ViewMode.LIST.name -> ViewMode.LIST
            else -> ViewMode.LIST // Default to list view
        }
    }

    // New View Settings flows
    /** Number of columns in the grid. Range: 1..6. Default: 1 (list-style). */
    val gridCellsCount: Flow<Int> = context.dataStore.data.map { preferences ->
        (preferences[GRID_CELLS_COUNT_KEY] ?: 1).coerceIn(1, 6)
    }

    /** Sort order for links. "DESC" = Latest first, "ASC" = Oldest first. */
    val sortOrder: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SORT_ORDER_KEY] ?: "DESC"
    }

    val linkShowFavicon: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[LINK_SHOW_FAVICON_KEY] ?: true
    }

    val linkShowUrl: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[LINK_SHOW_URL_KEY] ?: true
    }

    val linkShowTags: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[LINK_SHOW_TAGS_KEY] ?: true
    }

    val linkShowDateSaved: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[LINK_SHOW_DATE_SAVED_KEY] ?: true
    }

    val linkGridColumns: Flow<Int> = context.dataStore.data.map { preferences ->
        (preferences[LINK_GRID_COLUMNS_KEY] ?: 2).coerceIn(2, 4)
    }

    val linkLayoutMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LINK_LAYOUT_MODE_KEY] ?: "list"
    }

    // Folder View Settings flows
    val folderGridCellsCount: Flow<Int> = context.dataStore.data.map { preferences ->
        (preferences[FOLDER_GRID_CELLS_COUNT_KEY] ?: 2).coerceIn(1, 6) // Default 2 for folders
    }

    /** Sort order for folders. "ASC" = A-Z, "DESC" = Z-A. */
    val folderSortOrder: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[FOLDER_SORT_ORDER_KEY] ?: "ASC" // Default A-Z
    }

    val folderLayoutMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[FOLDER_LAYOUT_MODE_KEY] ?: "list"
    }

    val folderGridColumns: Flow<Int> = context.dataStore.data.map { preferences ->
        (preferences[FOLDER_GRID_COLUMNS_KEY] ?: 2).coerceIn(2, 4)
    }

    // Favorite View Settings flows
    val favoriteGridCellsCount: Flow<Int> = context.dataStore.data.map { preferences ->
        (preferences[FAVORITE_GRID_CELLS_COUNT_KEY] ?: 1).coerceIn(1, 6)
    }

    val favoriteSortOrder: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[FAVORITE_SORT_ORDER_KEY] ?: "DESC"
    }

    val favoriteLayoutMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[FAVORITE_LAYOUT_MODE_KEY] ?: "list"
    }

    val favoriteGridColumns: Flow<Int> = context.dataStore.data.map { preferences ->
        (preferences[FAVORITE_GRID_COLUMNS_KEY] ?: 2).coerceIn(2, 4)
    }

    val favoriteShowFavicon: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FAVORITE_SHOW_FAVICON_KEY] ?: true
    }

    val favoriteShowUrl: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FAVORITE_SHOW_URL_KEY] ?: true
    }

    val favoriteShowTags: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FAVORITE_SHOW_TAGS_KEY] ?: true
    }

    val favoriteShowDateSaved: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FAVORITE_SHOW_DATE_SAVED_KEY] ?: true
    }

    // Home section visibility flows
    val homeShowStats: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HOME_SHOW_STATS_KEY] ?: true
    }
    val homeShowQuickActions: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HOME_SHOW_QUICK_ACTIONS_KEY] ?: true
    }
    val homeShowRecentLinks: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HOME_SHOW_RECENT_LINKS_KEY] ?: true
    }

    // Look & Feel flows
    val themeMode: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[THEME_MODE_KEY] ?: 0 } // 0=System, 1=Light, 2=Dark

    val colorScheme: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[COLOR_SCHEME_KEY] ?: 0 } // 0=Blue, 1=Green, etc.

    val dynamicColor: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[DYNAMIC_COLOR_KEY] ?: false }

    val amoledMode: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[AMOLED_MODE_KEY] ?: false }

    // Setter functions
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

    suspend fun setViewMode(mode: ViewMode) {
        context.dataStore.edit { preferences ->
            preferences[VIEW_MODE] = mode.name
            preferences[IS_GRID_VIEW] = mode == ViewMode.GRID
        }
    }

    /** Set the number of columns (1–6). */
    suspend fun setGridCellsCount(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[GRID_CELLS_COUNT_KEY] = count.coerceIn(1, 6)
        }
    }

    /** Set sort order: "DESC" for Latest-Oldest, "ASC" for Oldest-Latest. */
    suspend fun setSortOrder(order: String) {
        context.dataStore.edit { preferences ->
            preferences[SORT_ORDER_KEY] = if (order == "ASC") "ASC" else "DESC"
        }
    }

    suspend fun setLinkShowFavicon(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LINK_SHOW_FAVICON_KEY] = show
        }
    }

    suspend fun setLinkShowUrl(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LINK_SHOW_URL_KEY] = show
        }
    }

    suspend fun setLinkShowTags(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LINK_SHOW_TAGS_KEY] = show
        }
    }

    suspend fun setLinkShowDateSaved(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LINK_SHOW_DATE_SAVED_KEY] = show
        }
    }

    suspend fun setLinkGridColumns(columns: Int) {
        context.dataStore.edit { preferences ->
            preferences[LINK_GRID_COLUMNS_KEY] = columns.coerceIn(2, 4)
        }
    }

    suspend fun setLinkLayoutMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[LINK_LAYOUT_MODE_KEY] = mode
        }
    }

    suspend fun setFolderGridCellsCount(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[FOLDER_GRID_CELLS_COUNT_KEY] = count.coerceIn(1, 6)
        }
    }

    suspend fun setFolderSortOrder(order: String) {
        context.dataStore.edit { preferences ->
            preferences[FOLDER_SORT_ORDER_KEY] = if (order == "DESC") "DESC" else "ASC"
        }
    }

    suspend fun setFolderLayoutMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[FOLDER_LAYOUT_MODE_KEY] = mode
        }
    }

    suspend fun setFolderGridColumns(columns: Int) {
        context.dataStore.edit { preferences ->
            preferences[FOLDER_GRID_COLUMNS_KEY] = columns.coerceIn(2, 4)
        }
    }

    suspend fun setFavoriteGridCellsCount(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[FAVORITE_GRID_CELLS_COUNT_KEY] = count.coerceIn(1, 6)
        }
    }

    suspend fun setFavoriteSortOrder(order: String) {
        context.dataStore.edit { preferences ->
            preferences[FAVORITE_SORT_ORDER_KEY] = if (order == "ASC") "ASC" else "DESC"
        }
    }

    suspend fun setFavoriteLayoutMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[FAVORITE_LAYOUT_MODE_KEY] = mode
        }
    }

    suspend fun setFavoriteGridColumns(columns: Int) {
        context.dataStore.edit { preferences ->
            preferences[FAVORITE_GRID_COLUMNS_KEY] = columns.coerceIn(2, 4)
        }
    }

    suspend fun setFavoriteShowFavicon(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FAVORITE_SHOW_FAVICON_KEY] = show
        }
    }

    suspend fun setFavoriteShowUrl(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FAVORITE_SHOW_URL_KEY] = show
        }
    }

    suspend fun setFavoriteShowTags(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FAVORITE_SHOW_TAGS_KEY] = show
        }
    }

    suspend fun setFavoriteShowDateSaved(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FAVORITE_SHOW_DATE_SAVED_KEY] = show
        }
    }

    // Look & Feel setters
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

    // Home section visibility setters
    suspend fun setHomeShowStats(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HOME_SHOW_STATS_KEY] = show
        }
    }

    suspend fun setHomeShowQuickActions(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HOME_SHOW_QUICK_ACTIONS_KEY] = show
        }
    }

    suspend fun setHomeShowRecentLinks(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HOME_SHOW_RECENT_LINKS_KEY] = show
        }
    }

    // Additional Appearance
    val navBarTransparent: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NAV_BAR_TRANSPARENT_KEY] ?: false
    }

    suspend fun setNavBarTransparent(transparent: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NAV_BAR_TRANSPARENT_KEY] = transparent
        }
    }
}

enum class ViewMode {
    LIST,
    GRID
}