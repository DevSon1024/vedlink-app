package com.devson.vedlink.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.domain.model.SearchTopic
import com.devson.vedlink.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchTopicsUiState(
    val topics: List<SearchTopic> = emptyList(),
    val filteredTopics: List<SearchTopic> = emptyList(),
    val allLinks: List<Link> = emptyList(),
    val filteredLinks: List<Link> = emptyList(),
    val selectedSpace: SmartSpaceCategory? = null,
    val selectedFilterTab: Int = 0, // 0 = Active, 1 = Completed
    val searchQuery: String = "",
    val selectedTag: String? = null,
    val availableTags: List<String> = emptyList(),
    val pinnedCount: Int = 0,
    val richNotesCount: Int = 0,
    val videosCount: Int = 0,
    val codeCount: Int = 0,
    val articlesCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class SearchTopicsUiEvent {
    data class ShowError(val message: String) : SearchTopicsUiEvent()
    data class ShowSuccess(val message: String) : SearchTopicsUiEvent()
}

@HiltViewModel
class SearchTopicsViewModel @Inject constructor(
    private val getSearchTopicsUseCase: GetSearchTopicsUseCase,
    private val addSearchTopicUseCase: AddSearchTopicUseCase,
    private val deleteSearchTopicUseCase: DeleteSearchTopicUseCase,
    private val toggleSearchTopicUseCase: ToggleSearchTopicUseCase,
    private val getAllLinksUseCase: GetAllLinksUseCase,
    private val searchLinksUseCase: SearchLinksUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deleteLinkUseCase: DeleteLinkUseCase
) : ViewModel() {

    private val _topics = MutableStateFlow<List<SearchTopic>>(emptyList())
    private val _allLinks = MutableStateFlow<List<Link>>(emptyList())
    private val _selectedSpace = MutableStateFlow<SmartSpaceCategory?>(null)
    private val _selectedFilterTab = MutableStateFlow(0)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedTag = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SearchTopicsUiState> = combine(
        combine(_topics, _allLinks, _selectedSpace, _selectedFilterTab) { topics, allLinks, selectedSpace, tab ->
            StateQuad1(topics, allLinks, selectedSpace, tab)
        },
        combine(_searchQuery, _selectedTag, _isLoading, _error) { query, tag, loading, err ->
            StateQuad2(query, tag, loading, err)
        }
    ) { quad1, quad2 ->
        val topics = quad1.topics
        val allLinks = quad1.allLinks
        val selectedSpace = quad1.selectedSpace
        val tab = quad1.tab
        val query = quad2.query
        val tag = quad2.tag
        val loading = quad2.loading
        val err = quad2.err

        val pinnedCount = searchLinksUseCase.filterBySmartSpace(allLinks, SmartSpaceCategory.PINNED).size
        val richNotesCount = searchLinksUseCase.filterBySmartSpace(allLinks, SmartSpaceCategory.RICH_NOTES).size
        val videosCount = searchLinksUseCase.filterBySmartSpace(allLinks, SmartSpaceCategory.VIDEOS).size
        val codeCount = searchLinksUseCase.filterBySmartSpace(allLinks, SmartSpaceCategory.CODE).size
        val articlesCount = searchLinksUseCase.filterBySmartSpace(allLinks, SmartSpaceCategory.ARTICLES).size
        val availableTags = allLinks.flatMap { it.tags }.distinct().sorted()

        // Filter links if a space is selected
        val spaceLinks = if (selectedSpace != null) {
            searchLinksUseCase.filterBySmartSpace(allLinks, selectedSpace)
        } else {
            emptyList()
        }

        val tagFilteredLinks = if (tag != null) {
            spaceLinks.filter { it.tags.any { t -> t.equals(tag, ignoreCase = true) } }
        } else {
            spaceLinks
        }

        val finalFilteredLinks = if (query.isNotBlank()) {
            searchLinksUseCase.searchInMemory(tagFilteredLinks, query)
        } else {
            tagFilteredLinks
        }

        // Filter topics
        val tabFilteredTopics = topics.filter {
            if (tab == 0) !it.isCompleted else it.isCompleted
        }
        val finalFilteredTopics = if (query.isNotBlank()) {
            tabFilteredTopics.filter { it.query.contains(query, ignoreCase = true) }
        } else {
            tabFilteredTopics
        }

        SearchTopicsUiState(
            topics = topics,
            filteredTopics = finalFilteredTopics,
            allLinks = allLinks,
            filteredLinks = finalFilteredLinks,
            selectedSpace = selectedSpace,
            selectedFilterTab = tab,
            searchQuery = query,
            selectedTag = tag,
            availableTags = availableTags,
            pinnedCount = pinnedCount,
            richNotesCount = richNotesCount,
            videosCount = videosCount,
            codeCount = codeCount,
            articlesCount = articlesCount,
            isLoading = loading,
            error = err
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchTopicsUiState()
    )

    private val _uiEvent = MutableSharedFlow<SearchTopicsUiEvent>()
    val uiEvent: SharedFlow<SearchTopicsUiEvent> = _uiEvent.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            getSearchTopicsUseCase()
                .catch { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                    _uiEvent.emit(SearchTopicsUiEvent.ShowError(exception.message ?: "Failed to load topics"))
                }
                .collect { topics ->
                    _topics.value = topics
                    _isLoading.value = false
                }
        }

        viewModelScope.launch {
            getAllLinksUseCase()
                .catch { exception ->
                    _error.value = exception.message
                }
                .collect { links ->
                    _allLinks.value = links
                }
        }
    }

    fun selectSpace(space: SmartSpaceCategory?) {
        if (_selectedSpace.value == space) {
            _selectedSpace.value = null
        } else {
            _selectedSpace.value = space
        }
        _selectedTag.value = null
    }

    fun setFilterTab(tab: Int) {
        _selectedFilterTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectTag(tag: String?) {
        if (_selectedTag.value == tag) {
            _selectedTag.value = null
        } else {
            _selectedTag.value = tag
        }
    }

    fun addTopic(query: String) {
        viewModelScope.launch {
            try {
                addSearchTopicUseCase(query)
                _uiEvent.emit(SearchTopicsUiEvent.ShowSuccess("Topic saved"))
            } catch (e: Exception) {
                _uiEvent.emit(SearchTopicsUiEvent.ShowError(e.message ?: "Failed to save topic"))
            }
        }
    }

    fun deleteTopic(topic: SearchTopic) {
        viewModelScope.launch {
            try {
                deleteSearchTopicUseCase(topic)
                _uiEvent.emit(SearchTopicsUiEvent.ShowSuccess("Topic deleted"))
            } catch (e: Exception) {
                _uiEvent.emit(SearchTopicsUiEvent.ShowError(e.message ?: "Failed to delete topic"))
            }
        }
    }

    fun toggleTopicCompleted(topic: SearchTopic) {
        viewModelScope.launch {
            try {
                toggleSearchTopicUseCase(topic)
                val status = if (!topic.isCompleted) "marked as completed" else "marked as active"
                _uiEvent.emit(SearchTopicsUiEvent.ShowSuccess("Topic $status"))
            } catch (e: Exception) {
                _uiEvent.emit(SearchTopicsUiEvent.ShowError(e.message ?: "Failed to update topic"))
            }
        }
    }

    fun toggleFavorite(linkId: Int, currentFavorite: Boolean) {
        viewModelScope.launch {
            try {
                toggleFavoriteUseCase(linkId, currentFavorite)
            } catch (e: Exception) {
                _uiEvent.emit(SearchTopicsUiEvent.ShowError(e.message ?: "Failed to update favorite"))
            }
        }
    }

    fun deleteLink(link: Link) {
        viewModelScope.launch {
            try {
                deleteLinkUseCase(link)
                _uiEvent.emit(SearchTopicsUiEvent.ShowSuccess("Link deleted"))
            } catch (e: Exception) {
                _uiEvent.emit(SearchTopicsUiEvent.ShowError(e.message ?: "Failed to delete link"))
            }
        }
    }
}

private data class StateQuad1(
    val topics: List<SearchTopic>,
    val allLinks: List<Link>,
    val selectedSpace: SmartSpaceCategory?,
    val tab: Int
)

private data class StateQuad2(
    val query: String,
    val tag: String?,
    val loading: Boolean,
    val err: String?
)

