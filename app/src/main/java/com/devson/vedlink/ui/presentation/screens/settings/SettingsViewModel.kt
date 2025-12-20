package com.devson.vedlink.ui.presentation.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedlink.data.preferences.ThemePreferences
import com.devson.vedlink.data.repository.LinkRepository
import com.devson.vedlink.domain.model.Link
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

data class SettingsUiState(
    val totalLinks: Int = 0,
    val favoriteLinks: Int = 0,
    val isDarkMode: Boolean = false,
    val autoFetchMetadata: Boolean = true
)

// Simplified item for backup (only essential data)
data class BackupLinkItem(
    val url: String,
    val isFavorite: Boolean
)

// The root backup structure
data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
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

    fun clearCache(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.cacheDir.deleteRecursively()
                context.codeCacheDir.deleteRecursively()
                withContext(Dispatchers.Main) {
                    _toastMessage.emit("Cache cleared successfully")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _toastMessage.emit("Failed to clear cache")
                }
            }
        }
    }

    fun exportData(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get all links from repository
                val allLinks = repository.getAllLinks().first()

                // Map to simplified structure (Only URL and isFavorite)
                val simpleLinks = allLinks.map { link ->
                    BackupLinkItem(
                        url = link.url,
                        isFavorite = link.isFavorite
                    )
                }

                val backupData = BackupData(links = simpleLinks)

                // Convert to JSON
                val gson = Gson()
                val jsonString = gson.toJson(backupData)

                // Write to file
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }

                withContext(Dispatchers.Main) {
                    _toastMessage.emit("Backup saved successfully")
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

                val gson = Gson()
                val backupData = gson.fromJson(stringBuilder.toString(), BackupData::class.java)

                if (backupData.links.isNotEmpty()) {
                    var count = 0
                    backupData.links.forEach { item ->
                        // Extract domain immediately during import
                        val domain = extractDomain(item.url)

                        val newLink = Link(
                            url = item.url,
                            isFavorite = item.isFavorite,
                            title = null,
                            description = null,
                            imageUrl = null,
                            domain = domain // Set extracted domain
                        )

                        repository.insertLink(newLink)
                        count++
                    }

                    withContext(Dispatchers.Main) {
                        _toastMessage.emit("Restored $count links. Refresh to fetch metadata.")
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
    private fun extractDomain(url: String): String? {
        return try {
            val uri = java.net.URI(url)
            val host = uri.host
            if (host != null && host.startsWith("www.")) {
                host.substring(4)
            } else {
                host
            }
        } catch (e: Exception) {
            // Fallback for simple parsing if URI fails
            try {
                val domain = url.substringAfter("://").substringBefore("/")
                if (domain.startsWith("www.")) domain.substring(4) else domain
            } catch (e2: Exception) {
                null
            }
        }
    }
}