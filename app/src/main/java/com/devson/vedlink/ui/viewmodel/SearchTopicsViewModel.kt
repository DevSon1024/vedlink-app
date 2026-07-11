package com.devson.vedlink.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedlink.domain.model.SearchTopic
import com.devson.vedlink.domain.usecase.AddSearchTopicUseCase
import com.devson.vedlink.domain.usecase.DeleteSearchTopicUseCase
import com.devson.vedlink.domain.usecase.GetSearchTopicsUseCase
import com.devson.vedlink.domain.usecase.ToggleSearchTopicUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchTopicsUiState(
    val topics: List<SearchTopic> = emptyList(),
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
    private val toggleSearchTopicUseCase: ToggleSearchTopicUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchTopicsUiState())
    val uiState: StateFlow<SearchTopicsUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<SearchTopicsUiEvent>()
    val uiEvent: SharedFlow<SearchTopicsUiEvent> = _uiEvent.asSharedFlow()

    init {
        loadSearchTopics()
    }

    private fun loadSearchTopics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getSearchTopicsUseCase()
                .catch { exception ->
                    _uiState.update { it.copy(isLoading = false, error = exception.message) }
                    _uiEvent.emit(SearchTopicsUiEvent.ShowError(exception.message ?: "Failed to load topics"))
                }
                .collect { topics ->
                    _uiState.update { it.copy(topics = topics, isLoading = false, error = null) }
                }
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
}
