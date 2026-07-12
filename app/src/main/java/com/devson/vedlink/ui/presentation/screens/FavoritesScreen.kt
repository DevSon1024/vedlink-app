package com.devson.vedlink.ui.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devson.vedlink.ui.presentation.components.CompactLinkCard
import com.devson.vedlink.ui.presentation.components.EnhancedAddLinkBottomSheet
import com.devson.vedlink.ui.presentation.components.LinkCard
import com.devson.vedlink.ui.presentation.components.MicroLinkCard
import com.devson.vedlink.ui.presentation.components.LinkViewSettingsBottomSheet
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.devson.vedlink.ui.presentation.helper.*
import com.devson.vedlink.ui.viewmodel.FavoritesUiEvent
import com.devson.vedlink.ui.viewmodel.FavoritesViewModel
import com.devson.vedlink.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FavoritesScreen(
    isActive: Boolean = false,
    onUpdateTopBarConfig: (com.devson.vedlink.ui.presentation.components.TopBarConfig) -> Unit = {},
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
    var showViewSettings by remember { mutableStateOf(false) }

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    LaunchedEffect(isActive) {
        if (!isActive && isSelectionMode) {
            isSelectionMode = false
            selectedLinks = emptySet()
        }
    }

    LaunchedEffect(isActive, isSelectionMode, selectedLinks.size, uiState.favoriteLinks.size, uiState.isGridView) {
        if (!isActive) return@LaunchedEffect
        if (isSelectionMode) {
            onUpdateTopBarConfig(
                com.devson.vedlink.ui.presentation.components.TopBarConfig(
                    title = "${selectedLinks.size} Selected",
                    navigationIcon = {
                        IconButton(onClick = { 
                            isSelectionMode = false
                            selectedLinks = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
                        }
                    },
                    actions = {
                        val allSelected = selectedLinks.size == uiState.favoriteLinks.size
                        IconButton(onClick = {
                            if (allSelected) {
                                selectedLinks = emptySet()
                            } else {
                                selectedLinks = uiState.favoriteLinks.map { it.id }.toSet()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Select all",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            viewModel.toggleFavoriteMultiple(selectedLinks.toList())
                            isSelectionMode = false
                            selectedLinks = emptySet()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Favorite",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            shareMultipleLinks(context, uiState.favoriteLinks.filter { selectedLinks.contains(it.id) })
                            isSelectionMode = false
                            selectedLinks = emptySet()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
            )
        } else {
            onUpdateTopBarConfig(
                com.devson.vedlink.ui.presentation.components.TopBarConfig(
                    title = "Favorites",
                    actions = {
                        IconButton(onClick = { showViewSettings = true }) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "View settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            )
        }
    }

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
            topBar = {},
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
            ) {
                if (uiState.favoriteLinks.isNotEmpty() && !isSelectionMode) {
                    ItemCountSection(
                        itemCount = uiState.favoriteLinks.size,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
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
                        if (!uiState.isPrefsLoaded) {
                            Box(modifier = Modifier.fillMaxSize())
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(uiState.gridCellsCount),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 8.dp,
                                    bottom = 80.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(
                                    if (uiState.gridCellsCount == 1) 0.dp else 10.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(
                                    if (uiState.gridCellsCount == 1) 12.dp else 10.dp
                                ),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(
                                    items = uiState.favoriteLinks,
                                    key = { index, link -> "${link.id}_$index" }
                                ) { index, link ->
                                    when {
                                        uiState.gridCellsCount == 1 -> {
                                            LinkCard(
                                                modifier = Modifier.animateItem(),
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
                                                onMoreClick = {},
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
                                                },
                                                showFavicon = uiState.viewSettings.showFavicon,
                                                showUrl = uiState.viewSettings.showUrl,
                                                showTags = uiState.viewSettings.showTags,
                                                showDate = uiState.viewSettings.showDateSaved
                                            )
                                        }
                                        uiState.gridCellsCount == 2 -> {
                                            CompactLinkCard(
                                                modifier = Modifier.animateItem(),
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
                                                },
                                                showFavicon = uiState.viewSettings.showFavicon,
                                                showUrl = uiState.viewSettings.showUrl,
                                                showTags = uiState.viewSettings.showTags,
                                                showDate = uiState.viewSettings.showDateSaved
                                            )
                                        }
                                        else -> {
                                            MicroLinkCard(
                                                modifier = Modifier.animateItem(),
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
                                                isSelectionMode = isSelectionMode
                                            )
                                        }
                                    }
                                }
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

    if (showViewSettings) {
        LinkViewSettingsBottomSheet(
            layoutMode = uiState.layoutMode,
            onLayoutModeChange = { viewModel.setLayoutMode(it) },
            gridColumns = uiState.gridColumns,
            onGridColumnsChange = { viewModel.setGridColumns(it) },
            viewSettings = uiState.viewSettings,
            onViewSettingsChange = { viewModel.setViewSettings(it) },
            sortOrder = uiState.sortOrder,
            onSortOrderChange = { viewModel.setSortOrder(it) },
            onDismiss = { showViewSettings = false }
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