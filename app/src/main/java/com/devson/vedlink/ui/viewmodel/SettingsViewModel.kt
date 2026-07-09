package com.devson.vedlink.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedlink.data.preferences.ThemePreferences
import com.devson.vedlink.domain.repository.LinkRepository
import com.devson.vedlink.domain.model.Link
import com.google.gson.GsonBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import com.google.gson.annotations.SerializedName
import com.devson.vedlink.ui.theme.AppThemePalette
import java.net.URI

data class SettingsUiState(
    val totalLinks: Int = 0,
    val favoriteLinks: Int = 0,
    val isDarkMode: Boolean = false,
    val autoFetchMetadata: Boolean = true
)

// Enhanced backup structure containing all database fields
data class BackupLinkItem(
    @SerializedName("url")
    val url: String,

    @SerializedName("canonical_url")
    val canonicalUrl: String? = null,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("image_url")
    val imageUrl: String? = null,

    @SerializedName("favicon_url")
    val faviconUrl: String? = null,

    @SerializedName("domain")
    val domain: String? = null,

    @SerializedName("provider")
    val provider: String? = null,

    @SerializedName("folder_id")
    val folderId: Int? = null,

    @SerializedName("is_favorite")
    val isFavorite: Boolean = false,

    @SerializedName("is_pinned")
    val isPinned: Boolean = false,

    @SerializedName("is_archived")
    val isArchived: Boolean = false,

    @SerializedName("is_unread")
    val isUnread: Boolean = true,

    @SerializedName("notes")
    val notes: String? = null,

    @SerializedName("is_pinned_notes")
    val isPinnedNotes: Boolean = false,

    @SerializedName("notes_updated_at")
    val notesUpdatedAt: Long? = null,

    @SerializedName("metadata_state")
    val metadataState: String? = null,

    @SerializedName("created_at")
    val createdAt: Long? = null,

    @SerializedName("saved_at")
    val savedAt: Long? = null, // legacy fallback

    @SerializedName("updated_at")
    val updatedAt: Long? = null,

    @SerializedName("last_updated")
    val lastUpdated: Long? = null,

    @SerializedName("etag")
    val eTag: String? = null,

    @SerializedName("last_modified")
    val lastModified: String? = null,

    @SerializedName("tags")
    val tags: List<String>? = null
)

// Root backup structure
data class BackupData(
    @SerializedName("version")
    val version: Int = 2,

    @SerializedName("app_name")
    val appName: String = "VedLink",

    @SerializedName("export_timestamp")
    val exportTimestamp: Long = System.currentTimeMillis(),

    @SerializedName("total_links")
    val totalLinks: Int,

    @SerializedName("links")
    val links: List<BackupLinkItem>
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: LinkRepository,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    // --- Appearance / Theme Flows ---
    val isDarkTheme: StateFlow<Boolean?> = themePreferences.themeMode.map { mode ->
        when (mode) {
            1 -> false
            2 -> true
            else -> null // System default
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val dynamicColor: StateFlow<Boolean> = themePreferences.dynamicColor
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val selectedPalette: StateFlow<AppThemePalette> = themePreferences.colorScheme.map { schemeIndex ->
        try {
            AppThemePalette.entries[schemeIndex]
        } catch (e: Exception) {
            AppThemePalette.BLUE
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, AppThemePalette.BLUE)

    val isNavBarTransparent: StateFlow<Boolean> = themePreferences.navBarTransparent
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

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

    // --- Theme Setters ---
    fun setDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            themePreferences.setThemeMode(if (isDark) 2 else 1)
        }
    }

    fun resetDarkTheme() {
        viewModelScope.launch {
            themePreferences.setThemeMode(0) // System default
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.setDynamicColor(enabled)
        }
    }

    fun setSelectedPalette(palette: AppThemePalette) {
        viewModelScope.launch {
            themePreferences.setColorScheme(palette.ordinal)
        }
    }

    fun setNavBarTransparent(transparent: Boolean) {
        viewModelScope.launch {
            themePreferences.setNavBarTransparent(transparent)
        }
    }

    // --- Legacy / General Setters ---
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

    fun exportData(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get all links from repository
                val allLinks = repository.getAllLinks().first()

                // Map to complete backup structure
                val backupLinks = allLinks.map { link ->
                    BackupLinkItem(
                        url = link.url,
                        canonicalUrl = link.canonicalUrl,
                        title = link.title,
                        description = link.description,
                        imageUrl = link.imageUrl,
                        faviconUrl = link.faviconUrl,
                        domain = link.domain,
                        provider = link.provider.name,
                        folderId = link.folderId,
                        isFavorite = link.isFavorite,
                        isPinned = link.isPinned,
                        isArchived = link.isArchived,
                        isUnread = link.isUnread,
                        notes = link.notes,
                        isPinnedNotes = link.isPinnedNotes,
                        notesUpdatedAt = link.notesUpdatedAt,
                        metadataState = link.metadataState.name,
                        createdAt = link.createdAt,
                        savedAt = link.createdAt,
                        updatedAt = link.updatedAt,
                        lastUpdated = link.lastUpdated,
                        eTag = link.eTag,
                        lastModified = link.lastModified,
                        tags = link.tags
                    )
                }

                val backupData = BackupData(
                    totalLinks = backupLinks.size,
                    links = backupLinks
                )

                // Convert to pretty-printed JSON
                val gson = GsonBuilder()
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create()

                val jsonString = gson.toJson(backupData)

                // Write to file
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }

                withContext(Dispatchers.Main) {
                    _toastMessage.emit("Exported ${backupLinks.size} links successfully")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _toastMessage.emit("Export failed: ${e.message}")
                }
            }
        }
    }

    fun importData(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val stringBuilder = StringBuilder()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            stringBuilder.append(line)
                            line = reader.readLine()
                        }
                    }
                }

                val gson = GsonBuilder().create()
                val backupData = gson.fromJson(stringBuilder.toString(), BackupData::class.java)

                if (backupData.links.isNotEmpty()) {
                    var newCount = 0
                    var duplicateCount = 0

                    backupData.links.forEach { item ->
                        val normalisedUrl = normaliseUrl(item.url)

                        val existingLink = repository.getLinkByUrl(normalisedUrl)
                            ?: repository.getLinkByUrl(item.url)

                        if (existingLink == null) {
                            val domainName = item.domain ?: extractDomain(normalisedUrl)

                            val newLink = Link(
                                url = normalisedUrl,
                                canonicalUrl = item.canonicalUrl ?: normalisedUrl,
                                title = item.title ?: domainName,
                                description = item.description,
                                imageUrl = item.imageUrl,
                                faviconUrl = item.faviconUrl,
                                domain = domainName,
                                provider = try {
                                    com.devson.vedlink.domain.model.WebsiteProvider.valueOf(item.provider ?: "GENERIC")
                                } catch (e: Exception) {
                                    com.devson.vedlink.domain.model.WebsiteProvider.GENERIC
                                },
                                folderId = item.folderId,
                                isFavorite = item.isFavorite,
                                isPinned = item.isPinned,
                                isArchived = item.isArchived,
                                isUnread = item.isUnread,
                                notes = item.notes,
                                isPinnedNotes = item.isPinnedNotes,
                                notesUpdatedAt = item.notesUpdatedAt,
                                metadataState = try {
                                    com.devson.vedlink.domain.model.MetadataState.valueOf(item.metadataState ?: "QUEUED")
                                } catch (e: Exception) {
                                    com.devson.vedlink.domain.model.MetadataState.QUEUED
                                },
                                createdAt = item.createdAt ?: item.savedAt ?: System.currentTimeMillis(),
                                updatedAt = item.updatedAt ?: System.currentTimeMillis(),
                                lastUpdated = item.lastUpdated ?: System.currentTimeMillis(),
                                eTag = item.eTag,
                                lastModified = item.lastModified,
                                tags = item.tags ?: emptyList()
                            )

                            repository.insertLink(newLink)
                            newCount++
                        } else {
                            duplicateCount++
                        }
                    }

                    withContext(Dispatchers.Main) {
                        val message = when {
                            newCount > 0 && duplicateCount > 0 ->
                                "Imported $newCount links ($duplicateCount duplicates skipped)"
                            newCount > 0 ->
                                "Imported $newCount links successfully"
                            else ->
                                "All ${backupData.links.size} links already exist"
                        }
                        _toastMessage.emit(message)
                        loadStats()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _toastMessage.emit("No links found in backup file")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _toastMessage.emit("Import failed: ${e.message}")
                }
            }
        }
    }

    /**
     * Normalise a URL so that trivially-different variants of the same link
     * (trailing slash, mixed-case scheme/host, extra whitespace) are treated as equal.
     */
    private fun normaliseUrl(url: String): String {
        return try {
            val trimmed = url.trim()
            val uri = URI(trimmed)
            // Lowercase only the scheme and host; keep path/query/fragment as-is
            val normUri = URI(
                uri.scheme?.lowercase(),
                uri.userInfo,
                uri.host?.lowercase(),
                uri.port,
                uri.path?.trimEnd('/')?.ifEmpty { "/" },
                uri.query,
                uri.fragment
            )
            normUri.toString()
        } catch (e: Exception) {
            url.trim().trimEnd('/')
        }
    }

    // Helper to extract clean domain from URL
    private fun extractDomain(url: String): String {
        return try {
            val uri = URI(url)
            val host = uri.host
            if (host != null && host.startsWith("www.")) {
                host.substring(4)
            } else {
                host ?: url
            }
        } catch (e: Exception) {
            try {
                val domain = url.substringAfter("://").substringBefore("/")
                if (domain.startsWith("www.")) domain.substring(4) else domain
            } catch (e2: Exception) {
                url
            }
        }
    }
}