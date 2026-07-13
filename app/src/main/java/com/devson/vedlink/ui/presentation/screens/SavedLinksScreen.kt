package com.devson.vedlink.ui.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.navigation.compose.hiltViewModel
import com.devson.vedlink.ui.presentation.components.CompactLinkCard
import com.devson.vedlink.ui.presentation.components.CompactShimmerLinkCard
import com.devson.vedlink.ui.presentation.components.EnhancedAddLinkBottomSheet
import com.devson.vedlink.ui.presentation.components.LinkCard
import com.devson.vedlink.ui.presentation.components.LinkViewSettingsBottomSheet
import com.devson.vedlink.ui.presentation.components.MicroLinkCard
import com.devson.vedlink.ui.presentation.components.MicroShimmerLinkCard
import com.devson.vedlink.ui.presentation.components.ShimmerLinkCard
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.devson.vedlink.ui.presentation.helper.*
import com.devson.vedlink.ui.viewmodel.SavedLinksUiEvent
import com.devson.vedlink.ui.viewmodel.SavedLinksViewModel
import com.devson.vedlink.ui.viewmodel.SettingsViewModel
import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SavedLinksScreen(
    isActive: Boolean = false,
    onUpdateTopBarConfig: (com.devson.vedlink.ui.presentation.components.TopBarConfig) -> Unit = {},
    onNavigateToDetails: (linkId: Int, linkIds: List<Int>) -> Unit,
    viewModel: SavedLinksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showViewSettings by rememberSaveable { mutableStateOf(false) }

    // Selection mode state
    var isSelectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedLinksList by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var selectedLinks by remember(selectedLinksList) { mutableStateOf(selectedLinksList.toSet()) }



    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is SavedLinksUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is SavedLinksUiEvent.ShowSuccess -> {
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
        selectedLinksList = emptyList()
    }

    fun handleLongPress(linkId: Int) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (!isSelectionMode) {
            isSelectionMode = true
            selectedLinksList = listOf(linkId)
        }
    }

    fun handleSelectionClick(linkId: Int) {
        selectedLinksList = if (selectedLinksList.contains(linkId)) {
            val newSelection = selectedLinksList - linkId
            if (newSelection.isEmpty()) isSelectionMode = false
            newSelection
        } else {
            selectedLinksList + linkId
        }
    }

    fun handleSelectAll() {
        if (selectedLinksList.size == uiState.links.size) {
            exitSelectionMode()
        } else {
            selectedLinksList = uiState.links.map { it.id }
        }
    }

    val selectedLinksData = uiState.links.filter { it.id in selectedLinks }
    val favoriteStatus = getFavoriteStatus(selectedLinksData)

    LaunchedEffect(isActive) {
        if (!isActive && isSelectionMode) {
            exitSelectionMode()
        }
    }

    LaunchedEffect(isActive, isSelectionMode, selectedLinksList, uiState.links.size) {
        if (!isActive) return@LaunchedEffect
        if (isSelectionMode) {
            onUpdateTopBarConfig(
                com.devson.vedlink.ui.presentation.components.TopBarConfig(
                    title = "${selectedLinksList.size} Selected",
                    navigationIcon = {
                        IconButton(onClick = { exitSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
                        }
                    },
                    actions = {
                        val allSelected = selectedLinksList.size == uiState.links.size
                        IconButton(onClick = { handleSelectAll() }) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Select all",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.toggleFavoriteMultiple(selectedLinksList.toList())
                            exitSelectionMode()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Favorite",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            shareMultipleLinks(context, selectedLinksData)
                            exitSelectionMode()
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
                    title = "Saved Links",
                    actions = {
                        IconButton(onClick = { viewModel.refreshMetadata() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh metadata",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.toggleSearchActive() }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Column {
                    AnimatedVisibility(visible = uiState.isSearchActive && !isSelectionMode) {
                        SearchBar(
                            query = uiState.searchQuery,
                            onQueryChange = { viewModel.onSearchQueryChange(it) },
                            onClose = { viewModel.toggleSearchActive() }
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = paddingValues.calculateTopPadding())
                ) {
                    if (!uiState.isSearchActive && uiState.links.isNotEmpty() && !isSelectionMode) {
                        ItemCountSection(
                            itemCount = uiState.links.size,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    when {
                        !uiState.isPrefsLoaded -> {
                            // Empty box while reading DataStore to avoid flashing the wrong list type
                            Box(modifier = Modifier.fillMaxSize())
                        }
                        uiState.isLoading -> {
                            // Shimmer skeleton based on current grid count
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(uiState.gridCellsCount),
                                contentPadding = PaddingValues(
                                    start = 16.dp, end = 16.dp,
                                    top = 8.dp, bottom = 80.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(if (uiState.gridCellsCount == 1) 0.dp else 10.dp),
                                verticalArrangement = Arrangement.spacedBy(if (uiState.gridCellsCount == 1) 12.dp else 10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                val shimmerCount = when (uiState.gridCellsCount) {
                                    1 -> 6
                                    2 -> 10
                                    else -> 24
                                }
                                items(shimmerCount) {
                                    when {
                                        uiState.gridCellsCount == 1 -> ShimmerLinkCard()
                                        uiState.gridCellsCount == 2 -> CompactShimmerLinkCard()
                                        else -> MicroShimmerLinkCard()
                                    }
                                }
                            }
                        }
                        uiState.links.isEmpty() -> {
                            EmptyState(modifier = Modifier.fillMaxSize())
                        }
                        else -> {
                            //  UNIFIED GRID 
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(uiState.gridCellsCount),
                                contentPadding = PaddingValues(
                                    start = 16.dp, end = 16.dp,
                                    top = 8.dp, bottom = 80.dp
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
                                    items = uiState.links,
                                    key = { index, link -> "${link.id}_$index" }
                                ) { index, link ->
                                    when {
                                        // count == 1 → full LinkCard (list style)
                                        uiState.gridCellsCount == 1 -> {
                                            LinkCard(
                                                modifier = Modifier.animateItem(),
                                                link = link,
                                                onClick = {
                                                    if (isSelectionMode) handleSelectionClick(link.id)
                                                    else onNavigateToDetails(link.id, uiState.links.map { it.id })
                                                },
                                                onLongPress = { handleLongPress(link.id) },
                                                isSelected = selectedLinks.contains(link.id),
                                                isSelectionMode = isSelectionMode,
                                                onFavoriteClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    viewModel.toggleFavorite(link.id, link.isFavorite)
                                                },
                                                onMoreClick = {},
                                                onCopyClick = {
                                                    copyToClipboard(context, link.url)
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Link copied to clipboard", duration = SnackbarDuration.Short)
                                                    }
                                                },
                                                onShareClick = { shareLink(context, link.url, link.title) },
                                                onRefreshClick = { viewModel.refreshLink(link.id) },
                                                onDeleteClick = { viewModel.deleteLink(link) },
                                                onEditTagsClick = { viewModel.editTags(link) },
                                                showFavicon = uiState.viewSettings.showFavicon,
                                                showUrl = uiState.viewSettings.showUrl,
                                                showTags = uiState.viewSettings.showTags,
                                                showDate = uiState.viewSettings.showDateSaved
                                            )
                                        }
                                        // count == 2 → CompactLinkCard (card+image grid)
                                        uiState.gridCellsCount == 2 -> {
                                            CompactLinkCard(
                                                modifier = Modifier.animateItem(),
                                                link = link,
                                                onClick = {
                                                    if (isSelectionMode) handleSelectionClick(link.id)
                                                    else onNavigateToDetails(link.id, uiState.links.map { it.id })
                                                },
                                                onLongPress = { handleLongPress(link.id) },
                                                isSelected = selectedLinks.contains(link.id),
                                                isSelectionMode = isSelectionMode,
                                                onFavoriteClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    viewModel.toggleFavorite(link.id, link.isFavorite)
                                                },
                                                onCopyClick = {
                                                    copyToClipboard(context, link.url)
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Link copied to clipboard", duration = SnackbarDuration.Short)
                                                    }
                                                },
                                                onShareClick = { shareLink(context, link.url, link.title) },
                                                onRefreshClick = { viewModel.refreshLink(link.id) },
                                                onDeleteClick = { viewModel.deleteLink(link) },
                                                onEditTagsClick = { viewModel.editTags(link) },
                                                showFavicon = uiState.viewSettings.showFavicon,
                                                showUrl = uiState.viewSettings.showUrl,
                                                showTags = uiState.viewSettings.showTags,
                                                showDate = uiState.viewSettings.showDateSaved
                                            )
                                        }
                                        // count >= 3 → MicroLinkCard (dense photo grid)
                                        else -> {
                                            MicroLinkCard(
                                                modifier = Modifier.animateItem(),
                                                link = link,
                                                onClick = {
                                                    if (isSelectionMode) handleSelectionClick(link.id)
                                                    else onNavigateToDetails(link.id, uiState.links.map { it.id })
                                                },
                                                onLongPress = { handleLongPress(link.id) },
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

    //  Bottom Sheets 
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

    // Tag Editing dialog with Smart Recommendations
    val editingTagsLink = uiState.editingTagsLink
    if (editingTagsLink != null) {
        val allTags by viewModel.allTags.collectAsState()
        var newTagName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                viewModel.editTags(null)
            },
            title = {
                Text(
                    text = "Edit Tags",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Link: ${editingTagsLink.title ?: "Untitled"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Current tags
                    if (editingTagsLink.tags.isNotEmpty()) {
                        Text(
                            text = "Current Tags (tap to remove):",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            editingTagsLink.tags.forEach { tag ->
                                InputChip(
                                    selected = true,
                                    onClick = {
                                        viewModel.removeTagFromLink(editingTagsLink.id, tag)
                                    },
                                    label = { Text(tag) },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove tag",
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Add new tag input field
                    OutlinedTextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        label = { Text("Add Tag") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Smart recommendations
                    val filteredTags = remember(newTagName, allTags) {
                        if (newTagName.isBlank()) {
                            emptyList()
                        } else {
                            allTags.filter { it.contains(newTagName, ignoreCase = true) }
                        }
                    }

                    if (filteredTags.isNotEmpty()) {
                        Text(
                            text = "Recommendations",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            filteredTags.take(5).forEach { tag ->
                                SuggestionChip(
                                    onClick = {
                                        viewModel.addTagToLink(editingTagsLink.id, tag)
                                        newTagName = ""
                                    },
                                    label = { Text(tag) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTagName.isNotBlank()) {
                            viewModel.addTagToLink(editingTagsLink.id, newTagName.trim())
                            newTagName = ""
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.editTags(null)
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }
}

//  Supporting composables (kept from original) 

@Composable
fun ItemCountSection(
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$itemCount items in total",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search links...") },
        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
        trailingIcon = {
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close search")
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraSmall,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Links Yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Start saving your favorite links\nby tapping the + button below",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}