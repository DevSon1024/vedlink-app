package com.devson.vedlink.ui.presentation.screens.favorites

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.ui.presentation.components.CompactLinkCard
import com.devson.vedlink.ui.presentation.components.LinkCard
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.devson.vedlink.ui.presentation.helper.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FavoritesScreen(
    onNavigateToDetails: (Int) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Selection mode state
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedLinks by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Event handling
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is FavoritesUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is FavoritesUiEvent.ShowSuccess -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedLinks = emptySet()
    }

    fun handleLongPress(linkId: Int) {
        if (!isSelectionMode) {
            isSelectionMode = true
            selectedLinks = setOf(linkId)
        }
    }

    fun handleSelectionClick(linkId: Int) {
        selectedLinks = if (selectedLinks.contains(linkId)) {
            val newSelection = selectedLinks - linkId
            if (newSelection.isEmpty()) {
                isSelectionMode = false
            }
            newSelection
        } else {
            selectedLinks + linkId
        }
    }

    fun handleSelectAll() {
        if (selectedLinks.size == uiState.favoriteLinks.size) {
            exitSelectionMode()
        } else {
            selectedLinks = uiState.favoriteLinks.map { it.id }.toSet()
        }
    }

    // Calculate favorite status - in favorites screen, all are already favorited
    val selectedLinksData = uiState.favoriteLinks.filter { it.id in selectedLinks }
    val favoriteStatus = if (selectedLinksData.isNotEmpty()) {
        FavoriteStatus.ALL_FAVORITED // Show HeartBroken to remove from favorites
    } else {
        FavoriteStatus.HIDDEN
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (isSelectionMode) {
                    SelectionTopBar(
                        selectedCount = selectedLinks.size,
                        totalCount = uiState.favoriteLinks.size,
                        allSelected = selectedLinks.size == uiState.favoriteLinks.size,
                        favoriteStatus = favoriteStatus,
                        onClose = { exitSelectionMode() },
                        onSelectAll = { handleSelectAll() },
                        onShare = {
                            shareMultipleLinks(context, selectedLinksData)
                            exitSelectionMode()
                        },
                        onFavorite = {
                            viewModel.toggleFavoriteMultiple(selectedLinks.toList())
                            exitSelectionMode()
                        },
                        onDelete = {
                            showDeleteDialog = true
                        }
                    )
                } else {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Favorites",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        actions = {
                            IconButton(onClick = { viewModel.toggleViewMode() }) {
                                Icon(
                                    imageVector = if (uiState.isGridView)
                                        Icons.Default.ViewList
                                    else
                                        Icons.Default.GridView,
                                    contentDescription = if (uiState.isGridView) "List View" else "Grid View",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    uiState.favoriteLinks.isEmpty() -> {
                        EmptyFavoritesState(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        AnimatedVisibility(
                            visible = uiState.isGridView,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 8.dp,
                                    bottom = 120.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = uiState.favoriteLinks,
                                    key = { it.id }
                                ) { link ->
                                    CompactLinkCard(
                                        link = link,
                                        onClick = {
                                            if (isSelectionMode) {
                                                handleSelectionClick(link.id)
                                            } else {
                                                onNavigateToDetails(link.id)
                                            }
                                        },
                                        onLongPress = {
                                            handleLongPress(link.id)
                                        },
                                        isSelected = selectedLinks.contains(link.id),
                                        isSelectionMode = isSelectionMode,
                                        onFavoriteClick = {
                                            viewModel.toggleFavorite(link.id, link.isFavorite)
                                        },
                                        onCopyClick = {
                                            copyToClipboard(context, link.url)
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = "Link copied to clipboard",
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        },
                                        onShareClick = {
                                            shareLink(context, link.url, link.title)
                                        },
                                        onDeleteClick = {
                                            viewModel.deleteLink(link)
                                        }
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = !uiState.isGridView,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 8.dp,
                                    bottom = 120.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = uiState.favoriteLinks,
                                    key = { it.id }
                                ) { link ->
                                    LinkCard(
                                        link = link,
                                        onClick = {
                                            if (isSelectionMode) {
                                                handleSelectionClick(link.id)
                                            } else {
                                                onNavigateToDetails(link.id)
                                            }
                                        },
                                        onLongPress = {
                                            handleLongPress(link.id)
                                        },
                                        isSelected = selectedLinks.contains(link.id),
                                        isSelectionMode = isSelectionMode,
                                        onFavoriteClick = {
                                            viewModel.toggleFavorite(link.id, link.isFavorite)
                                        },
                                        onMoreClick = {
                                            viewModel.deleteLink(link)
                                        },
                                        onCopyClick = {
                                            copyToClipboard(context, link.url)
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = "Link copied to clipboard",
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        },
                                        onShareClick = {
                                            shareLink(context, link.url, link.title)
                                        },
                                        onDeleteClick = {
                                            viewModel.deleteLink(link)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        )
    }

    // Delete Dialog
    if (showDeleteDialog && selectedLinks.isNotEmpty()) {
        MultiDeleteConfirmationDialog(
            count = selectedLinks.size,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                viewModel.deleteLinks(selectedLinks.toList())
                showDeleteDialog = false
                exitSelectionMode()
            }
        )
    }
}

@Composable
fun EmptyFavoritesState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(60.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No Favorites Yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Mark links as favorites to\nsee them here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}