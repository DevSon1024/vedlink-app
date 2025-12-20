package com.devson.vedlink.ui.presentation.screens.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.domain.usecase.GetAllLinksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FolderItem(
    val domain: String,
    val linkCount: Int,
    val favicon: String? = null
)

data class FoldersUiState(
    val folders: List<FolderItem> = emptyList(),
    val linksByDomain: Map<String, List<Link>> = emptyMap(),
    val isLoading: Boolean = false,
    val isGridView: Boolean = true // Default to Grid View
)

@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val getAllLinksUseCase: GetAllLinksUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoldersUiState())
    val uiState: StateFlow<FoldersUiState> = _uiState.asStateFlow()

    init {
        loadFolders()
    }

    private fun loadFolders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            getAllLinksUseCase()
                .catch { exception ->
                    _uiState.update { it.copy(isLoading = false) }
                }
                .collect { links ->
                    val linksByDomain = links.groupBy { it.domain ?: "Unknown" }
                    val folders = linksByDomain.map { (domain, domainLinks) ->
                        FolderItem(
                            domain = domain,
                            linkCount = domainLinks.size
                        )
                    }.sortedByDescending { it.linkCount }

                    _uiState.update {
                        it.copy(
                            folders = folders,
                            linksByDomain = linksByDomain,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun toggleViewMode() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }
}