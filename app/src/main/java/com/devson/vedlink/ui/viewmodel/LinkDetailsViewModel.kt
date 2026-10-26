package com.devson.vedlink.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedlink.data.worker.WorkManagerHelper
import com.devson.vedlink.domain.model.Folder
import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.domain.usecase.DeleteLinkUseCase
import com.devson.vedlink.domain.usecase.GetAllTagsUseCase
import com.devson.vedlink.domain.usecase.GetFoldersUseCase
import com.devson.vedlink.domain.usecase.GetLinkByIdUseCase
import com.devson.vedlink.domain.usecase.ToggleFavoriteUseCase
import com.devson.vedlink.domain.usecase.UpdateLinkUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

enum class SaveState {
    Saved,
    Saving
}

data class LinkDetailsUiState(
    val link: Link? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showQrCodeDialog: Boolean = false,
    val readabilityText: String? = null,
    val isReadingModeActive: Boolean = false,
    val isExtractingReaderText: Boolean = false,
    val noteTitle: String = "",
    val noteBodyMarkdown: String = "",
    val isPinned: Boolean = false,
    val tags: List<String> = emptyList(),
    val folderId: Int? = null,
    val saveState: SaveState = SaveState.Saved
)

@OptIn(FlowPreview::class)
@HiltViewModel
class LinkDetailsViewModel @Inject constructor(
    private val getLinkByIdUseCase: GetLinkByIdUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deleteLinkUseCase: DeleteLinkUseCase,
    private val updateLinkUseCase: UpdateLinkUseCase,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val workManagerHelper: WorkManagerHelper,
    getFoldersUseCase: GetFoldersUseCase
) : ViewModel() {

    val folders: StateFlow<List<Folder>> = getFoldersUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags: StateFlow<List<String>> = getAllTagsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(LinkDetailsUiState())
    val uiState: StateFlow<LinkDetailsUiState> = _uiState.asStateFlow()

    private val noteEditFlow = MutableSharedFlow<Pair<String, String>>(replay = 0, extraBufferCapacity = 1)

    init {
        viewModelScope.launch {
            noteEditFlow
                .debounce(500)
                .distinctUntilChanged()
                .collect { (title, body) ->
                    persistNoteChanges(title, body)
                }
        }
    }

    fun loadLink(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            getLinkByIdUseCase(id)
                .onSuccess { link ->
                    _uiState.update {
                        it.copy(
                            link = link,
                            isLoading = false,
                            error = null,
                            noteTitle = link.noteTitle ?: "",
                            noteBodyMarkdown = link.noteBodyMarkdown ?: link.notes ?: "",
                            isPinned = link.isPinned,
                            tags = link.tags,
                            folderId = link.folderId,
                            saveState = SaveState.Saved
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message
                        )
                    }
                }
        }
    }

    fun onNoteTitleChanged(newTitle: String) {
        _uiState.update { it.copy(noteTitle = newTitle, saveState = SaveState.Saving) }
        noteEditFlow.tryEmit(newTitle to _uiState.value.noteBodyMarkdown)
    }

    fun onNoteBodyChanged(newBody: String) {
        _uiState.update { it.copy(noteBodyMarkdown = newBody, saveState = SaveState.Saving) }
        noteEditFlow.tryEmit(_uiState.value.noteTitle to newBody)
    }

    fun appendToNoteBody(textToAppend: String) {
        val currentBody = _uiState.value.noteBodyMarkdown
        val newBody = if (currentBody.isBlank()) {
            textToAppend
        } else if (currentBody.endsWith("\n\n") || currentBody.endsWith("\n")) {
            currentBody + textToAppend
        } else {
            currentBody + "\n" + textToAppend
        }
        onNoteBodyChanged(newBody)
    }

    private suspend fun persistNoteChanges(title: String, body: String) {
        val currentLink = _uiState.value.link ?: return
        _uiState.update { it.copy(saveState = SaveState.Saving) }
        try {
            updateLinkUseCase.updateNote(
                linkId = currentLink.id,
                noteTitle = title,
                noteBodyMarkdown = body
            ).onSuccess { updatedLink ->
                _uiState.update {
                    it.copy(
                        link = updatedLink,
                        saveState = SaveState.Saved
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(saveState = SaveState.Saved) }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(saveState = SaveState.Saved) }
        }
    }

    fun flushPendingSave() {
        val currentLink = _uiState.value.link ?: return
        val title = _uiState.value.noteTitle
        val body = _uiState.value.noteBodyMarkdown
        if (title != (currentLink.noteTitle ?: "") || body != (currentLink.noteBodyMarkdown ?: currentLink.notes ?: "")) {
            viewModelScope.launch {
                persistNoteChanges(title, body)
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCleared() {
        super.onCleared()
        val currentLink = _uiState.value.link ?: return
        val title = _uiState.value.noteTitle
        val body = _uiState.value.noteBodyMarkdown
        if (title != (currentLink.noteTitle ?: "") || body != (currentLink.noteBodyMarkdown ?: currentLink.notes ?: "")) {
            GlobalScope.launch(Dispatchers.IO + NonCancellable) {
                try {
                    updateLinkUseCase.updateNote(
                        linkId = currentLink.id,
                        noteTitle = title,
                        noteBodyMarkdown = body
                    )
                } catch (_: Exception) {}
            }
        }
    }

    fun togglePin() {
        val currentLink = _uiState.value.link ?: return
        val newPinned = !_uiState.value.isPinned
        _uiState.update { it.copy(isPinned = newPinned) }
        viewModelScope.launch {
            updateLinkUseCase.togglePin(currentLink.id.toLong(), newPinned)
            _uiState.update { it.copy(link = currentLink.copy(isPinned = newPinned)) }
        }
    }

    fun refreshMetadata() {
        val currentLink = _uiState.value.link ?: return
        workManagerHelper.enqueueMetadataFetch(
            url = currentLink.url,
            linkId = currentLink.id,
            isForcedRefresh = true
        )
    }

    fun toggleFavorite() {
        val currentLink = _uiState.value.link ?: return
        viewModelScope.launch {
            toggleFavoriteUseCase(currentLink.id, currentLink.isFavorite)
            _uiState.update {
                it.copy(
                    link = currentLink.copy(isFavorite = !currentLink.isFavorite)
                )
            }
        }
    }

    fun deleteLink() {
        val currentLink = _uiState.value.link ?: return
        viewModelScope.launch {
            deleteLinkUseCase(currentLink)
        }
    }

    fun updateNotes(notes: String) {
        onNoteBodyChanged(notes)
    }

    fun assignFolder(folderId: Int?) {
        val currentLink = _uiState.value.link ?: return
        _uiState.update { it.copy(folderId = folderId) }
        val updated = currentLink.copy(
            folderId = folderId,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            updateLinkUseCase(updated)
            _uiState.update { it.copy(link = updated) }
        }
    }

    fun addTag(tag: String) {
        val currentLink = _uiState.value.link ?: return
        val cleanTag = tag.trim().lowercase().removePrefix("#")
        if (cleanTag.isBlank() || _uiState.value.tags.contains(cleanTag)) return
        val updatedTags = _uiState.value.tags + cleanTag
        _uiState.update { it.copy(tags = updatedTags) }
        val updated = currentLink.copy(
            tags = updatedTags,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            updateLinkUseCase(updated)
            _uiState.update { it.copy(link = updated) }
        }
    }

    fun removeTag(tag: String) {
        val currentLink = _uiState.value.link ?: return
        val updatedTags = _uiState.value.tags - tag
        _uiState.update { it.copy(tags = updatedTags) }
        val updated = currentLink.copy(
            tags = updatedTags,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            updateLinkUseCase(updated)
            _uiState.update { it.copy(link = updated) }
        }
    }

    fun updateUrl(newUrl: String) {
        val currentLink = _uiState.value.link ?: return
        val trimmedUrl = newUrl.trim()
        val domain = try {
            val uri = java.net.URI(trimmedUrl)
            uri.host?.removePrefix("www.") ?: currentLink.domain
        } catch (e: Exception) {
            currentLink.domain
        }
        val updated = currentLink.copy(
            url = trimmedUrl,
            canonicalUrl = trimmedUrl,
            domain = domain,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            updateLinkUseCase(updated)
            _uiState.update { it.copy(link = updated) }
        }
    }

    fun showQrCodeDialog(show: Boolean) {
        _uiState.update { it.copy(showQrCodeDialog = show) }
    }

    fun extractReadabilityText(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExtractingReaderText = true, isReadingModeActive = true) }
            kotlinx.coroutines.delay(1000)
            _uiState.update {
                it.copy(
                    isExtractingReaderText = false,
                    readabilityText = """
                        Reader View Mode
                        URL: ${url}
                        
                        This is a reader-friendly representation of the web article. Readability mode strips away extraneous styling, advertisements, layout constraints, and script elements to expose clean text optimized for user reading.
                    """.trimIndent()
                )
            }
        }
    }

    fun exitReadingMode() {
        _uiState.update {
            it.copy(
                isReadingModeActive = false,
                readabilityText = null
            )
        }
    }
}