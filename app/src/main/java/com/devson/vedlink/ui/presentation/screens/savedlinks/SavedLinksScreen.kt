package com.devson.vedlink.ui.presentation.screens.savedlinks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.navigation.compose.hiltViewModel
import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.ui.presentation.components.CompactLinkCard
import com.devson.vedlink.ui.presentation.components.CompactShimmerLinkCard
import com.devson.vedlink.ui.presentation.components.EnhancedAddLinkBottomSheet
import com.devson.vedlink.ui.presentation.components.LinkCard
import com.devson.vedlink.ui.presentation.components.MicroLinkCard
import com.devson.vedlink.ui.presentation.components.MicroShimmerLinkCard
import com.devson.vedlink.ui.presentation.components.ShimmerLinkCard
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.devson.vedlink.ui.presentation.helper.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SavedLinksScreen(
    onNavigateToDetails: (linkId: Int, linkIds: List<Int>) -> Unit,
    viewModel: SavedLinksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Column {
                    if (isSelectionMode) {
                        SelectionTopBar(
                            selectedCount = selectedLinks.size,
                            totalCount = uiState.links.size,
                            allSelected = selectedLinks.size == uiState.links.size,
                            favoriteStatus = favoriteStatus,
                            onClose = { exitSelectionMode() },
                            onSelectAll = { handleSelectAll() },
                            onShare = {
                                shareMultipleLinks(context, selectedLinksData)
                                exitSelectionMode()
                            },
                            onFavorite = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.toggleFavoriteMultiple(selectedLinks.toList())
                                exitSelectionMode()
                            },
                            onDelete = { showDeleteDialog = true }
                        )
                    } else {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Saved Links",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
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
                                // New: View Settings icon replaces old Grid/List toggle
                                IconButton(onClick = { showViewSettings = true }) {
                                    Icon(
                                        imageVector = Icons.Default.DisplaySettings,
                                        contentDescription = "View settings",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }

                    AnimatedVisibility(visible = uiState.isSearchActive && !isSelectionMode) {
                        SearchBar(
                            query = uiState.searchQuery,
                            onQueryChange = { viewModel.onSearchQueryChange(it) },
                            onClose = { viewModel.toggleSearchActive() }
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
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
                                    top = 8.dp, bottom = 120.dp
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
                            // ── UNIFIED GRID ──────────────────────────────────────
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(uiState.gridCellsCount),
                                contentPadding = PaddingValues(
                                    start = 16.dp, end = 16.dp,
                                    top = 8.dp, bottom = 120.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(
                                    if (uiState.gridCellsCount == 1) 0.dp else 10.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(
                                    if (uiState.gridCellsCount == 1) 12.dp else 10.dp
                                ),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = uiState.links,
                                    key = { it.id }
                                ) { link ->
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
                                                onDeleteClick = { viewModel.deleteLink(link) }
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
                                                onDeleteClick = { viewModel.deleteLink(link) }
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

                // Animated FAB
                AnimatedVisibility(
                    visible = !uiState.isSearchActive && !isSelectionMode && !showAddDialog,
                    enter = scaleIn(animationSpec = tween(durationMillis = 300)),
                    exit = scaleOut(animationSpec = tween(durationMillis = 300)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp)
                ) {
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.size(64.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 8.dp, pressedElevation = 12.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Link",
                            modifier = Modifier.size(32.dp)
                        )
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

    // ── Bottom Sheets ────────────────────────────────────────────────────────
    if (showViewSettings) {
        ViewSettingsBottomSheet(
            gridCellsCount = uiState.gridCellsCount,
            sortOrder = uiState.sortOrder,
            onGridCellsChange = { viewModel.setGridCellsCount(it) },
            onSortOrderChange = { viewModel.setSortOrder(it) },
            onDismiss = { showViewSettings = false }
        )
    }

    if (showAddDialog) {
        EnhancedAddLinkBottomSheet(
            recentLinks = uiState.links.take(10),
            onDismiss = { showAddDialog = false },
            onConfirm = { url ->
                viewModel.saveLink(url)
                showAddDialog = false
            },
            onAutoPaste = {}
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
}

// ── View Settings Bottom Sheet ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewSettingsBottomSheet(
    gridCellsCount: Int,
    sortOrder: String,
    onGridCellsChange: (Int) -> Unit,
    onSortOrderChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .then(Modifier.padding(0.dp))
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                ) {}
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Title
            Text(
                text = "View Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // ── Grid Size Slider ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Grid Size",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = when (gridCellsCount) {
                            1 -> "List"
                            2 -> "2 Columns"
                            3 -> "3 Columns"
                            4 -> "4 Columns"
                            5 -> "5 Columns"
                            else -> "6 Columns"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Slider(
                value = gridCellsCount.toFloat(),
                onValueChange = { onGridCellsChange(it.toInt()) },
                valueRange = 1f..6f,
                steps = 4,  // 5-step slider (1,2,3,4,5,6)
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            // Tick labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("List", "2", "3", "4", "5", "6").forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Sort Order ────────────────────────────────────────────────────
            Text(
                text = "Sort Order",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SortChip(
                    label = "Latest First",
                    selected = sortOrder == "DESC",
                    onClick = { onSortOrderChange("DESC") },
                    modifier = Modifier.weight(1f)
                )
                SortChip(
                    label = "Oldest First",
                    selected = sortOrder == "ASC",
                    onClick = { onSortOrderChange("ASC") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SortChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant

    val contentColor = if (selected)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        tonalElevation = if (selected) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(end = 0.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}

// ── Supporting composables (kept from original) ─────────────────────────────

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
        shape = RoundedCornerShape(12.dp),
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
            shape = RoundedCornerShape(60.dp),
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