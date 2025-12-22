package com.devson.vedlink.ui.presentation.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedlink.data.preferences.ThemePreferences
import com.devson.vedlink.data.repository.LinkRepository
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
import coil.imageLoader
import com.google.gson.annotations.SerializedName

data class SettingsUiState(
    val totalLinks: Int = 0,
    val favoriteLinks: Int = 0,
    val isDarkMode: Boolean = false,
    val autoFetchMetadata: Boolean = true,
    val cacheSize: String = "Calculating..."
)

// Minimal backup structure - only essential fields
data class BackupLinkItem(
    @SerializedName("url")
    val url: String,

    @SerializedName("saved_at")
    val savedAt: Long,

    @SerializedName("is_favorite")
    val isFavorite: Boolean
)

// Root backup structure
data class BackupData(
    @SerializedName("version")
    val version: Int = 1,

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

    /**
     * Calculate cache size with proper formatting
     */
    fun calculateCacheSize(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val size = getCacheSize(context)
            _uiState.update { it.copy(cacheSize = size) }
        }
    }

    /**
     * Get cache size in human-readable format
     */
    private fun getCacheSize(context: Context): String {
        return try {
            val cacheDir = context.cacheDir
            val totalSize = calculateDirectorySize(cacheDir)
            formatFileSize(totalSize)
        } catch (e: Exception) {
            "0 B"
        }
    }

    private fun calculateDirectorySize(directory: java.io.File): Long {
        var size: Long = 0
        if (directory.exists()) {
            val files = directory.listFiles()
            if (files != null) {
                for (file in files) {
                    size += if (file.isDirectory) {
                        calculateDirectorySize(file)
                    } else {
                        file.length()
                    }
                }
            }
        }
        return size
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> String.format("%.2f KB", size / 1024.0)
            size < 1024 * 1024 * 1024 -> String.format("%.2f MB", size / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
        }
    }

    fun clearCache(context: Context) {
        viewModelScope.launch {
            try {
                // Clear app cache
                context.cacheDir.deleteRecursively()

                // Clear Coil image cache
                context.imageLoader.memoryCache?.clear()
                context.imageLoader.diskCache?.clear()

                _toastMessage.emit("Cache cleared successfully")

                // Recalculate cache size after clearing
                calculateCacheSize(context)
            } catch (e: Exception) {
                _toastMessage.emit("Failed to clear cache: ${e.message}")
            }
        }
    }

    fun exportData(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get all links from repository
                val allLinks = repository.getAllLinks().first()

                // Map to minimal backup structure (only url, saved_at, is_favorite)
                val backupLinks = allLinks.map { link ->
                    BackupLinkItem(
                        url = link.url,
                        savedAt = link.createdAt,
                        isFavorite = link.isFavorite
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
                        // Check if link already exists
                        val existingLink = repository.getLinkByUrl(item.url)

                        if (existingLink == null) {
                            // Extract domain from URL
                            val domain = extractDomain(item.url)

                            // Create new link with timestamp from backup
                            val newLink = Link(
                                url = item.url,
                                isFavorite = item.isFavorite,
                                title = domain, // Temporary until metadata fetched
                                description = null,
                                imageUrl = null,
                                domain = domain,
                                createdAt = item.savedAt // Restore original timestamp
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

    // Helper to extract clean domain from URL
    private fun extractDomain(url: String): String {
        return try {
            val uri = java.net.URI(url)
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