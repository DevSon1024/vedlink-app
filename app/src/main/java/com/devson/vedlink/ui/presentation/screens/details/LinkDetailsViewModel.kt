package com.devson.vedlink.presentation.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.domain.usecase.DeleteLinkUseCase
import com.devson.vedlink.domain.usecase.GetLinkByIdUseCase
import com.devson.vedlink.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LinkDetailsUiState(
    val link: Link? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LinkDetailsViewModel @Inject constructor(
    private val getLinkByIdUseCase: GetLinkByIdUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deleteLinkUseCase: DeleteLinkUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LinkDetailsUiState())
    val uiState: StateFlow<LinkDetailsUiState> = _uiState.asStateFlow()

    fun loadLink(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            getLinkByIdUseCase(id)
                .onSuccess { link ->
                    _uiState.update {
                        it.copy(
                            link = link,
                            isLoading = false,
                            error = null
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
}