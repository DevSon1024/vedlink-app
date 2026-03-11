package com.devson.vedlink.ui.presentation.screens.folders

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.ui.presentation.components.CompactLinkCard
import com.devson.vedlink.ui.presentation.components.EnhancedAddLinkBottomSheet
import com.devson.vedlink.ui.presentation.components.LinkCard
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.devson.vedlink.ui.presentation.helper.*

fun getCleanDomainName(domain: String): String {
    return domain
        .removePrefix("www.")
        .substringBeforeLast(".")
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FoldersScreen(
    onNavigateToDetails: (Int) -> Unit,
    viewModel: FoldersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var expandedDomain by remember { mutableStateOf<String?>(null) }
    var selectedFolderDomain by remember { mutableStateOf<String?>(null) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedLinks by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showViewSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is FoldersUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is FoldersUiEvent.ShowSuccess -> {
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
        val currentDomain = selectedFolderDomain ?: expandedDomain
        val folderLinks = uiState.linksByDomain[currentDomain] ?: emptyList()

        if (selectedLinks.size == folderLinks.size) {
            exitSelectionMode()
        } else {
            selectedLinks = folderLinks.map { it.id }.toSet()
        }
    }

    BackHandler(enabled = selectedFolderDomain != null || isSelectionMode) {
        when {
            isSelectionMode -> exitSelectionMode()
            selectedFolderDomain != null -> selectedFolderDomain = null
        }
    }

    // Calculate favorite status
    val currentDomain = selectedFolderDomain ?: expandedDomain
    val folderLinks = uiState.linksByDomain[currentDomain] ?: emptyList()
    val selectedLinksData = folderLinks.filter { it.id in selectedLinks }
    val favoriteStatus = getFavoriteStatus(selectedLinksData)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (isSelectionMode && (selectedFolderDomain != null || expandedDomain != null)) {
                    SelectionTopBar(
                        selectedCount = selectedLinks.size,
                        totalCount = folderLinks.size,
                        allSelected = selectedLinks.size == folderLinks.size,
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
                                text = if (selectedFolderDomain != null)
                                    getCleanDomainName(selectedFolderDomain!!)
                                else "Folders",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        navigationIcon = {
                            if (selectedFolderDomain != null) {
                                IconButton(onClick = {
                                    selectedFolderDomain = null
                                    exitSelectionMode()
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        },
                        actions = {
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
                    uiState.folders.isEmpty() -> {
                        EmptyFoldersState(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    selectedFolderDomain != null -> {
                        val folderLinks = uiState.linksByDomain[selectedFolderDomain] ?: emptyList()

                        if (folderLinks.isNotEmpty()) {
                            AnimatedVisibility(
                                visible = uiState.gridCellsCount > 1,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(uiState.gridCellsCount),
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
                                        items = folderLinks,
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
                                visible = uiState.gridCellsCount == 1,
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
                                        items = folderLinks,
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
                                            onMoreClick = { },
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
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No links in this folder")
                            }
                        }
                    }
                    else -> {
                        AnimatedVisibility(
                            visible = uiState.gridCellsCount > 1,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(uiState.gridCellsCount),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 16.dp,
                                    bottom = 120.dp  // ← Updated
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = uiState.folders,
                                    key = { it.domain }
                                ) { folder ->
                                    FolderGridCard(
                                        folder = folder,
                                        onClick = {
                                            selectedFolderDomain = folder.domain
                                            expandedDomain = null
                                        }
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = uiState.gridCellsCount == 1,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 12.dp,
                                    bottom = 120.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = uiState.folders,
                                    key = { it.domain }
                                ) { folder ->
                                    FolderListCard(
                                        folder = folder,
                                        isExpanded = expandedDomain == folder.domain,
                                        onToggleExpand = {
                                            expandedDomain = if (expandedDomain == folder.domain) {
                                                null
                                            } else {
                                                folder.domain
                                            }
                                        },
                                        links = if (expandedDomain == folder.domain) {
                                            uiState.linksByDomain[folder.domain] ?: emptyList()
                                        } else {
                                            emptyList()
                                        },
                                        isSelectionMode = isSelectionMode,
                                        selectedLinks = selectedLinks,
                                        onLinkClick = { linkId ->
                                            if (isSelectionMode) {
                                                handleSelectionClick(linkId)
                                            } else {
                                                onNavigateToDetails(linkId)
                                            }
                                        },
                                        onLinkLongPress = { linkId ->
                                            handleLongPress(linkId)
                                        },
                                        onFavoriteClick = { link ->
                                            viewModel.toggleFavorite(link.id, link.isFavorite)
                                        },
                                        onCopyClick = { link ->
                                            copyToClipboard(context, link.url)
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = "Link copied to clipboard",
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        },
                                        onShareClick = { link ->
                                            shareLink(context, link.url, link.title)
                                        },
                                        onDeleteClick = { link ->
                                            viewModel.deleteLink(link)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Animated FAB
                AnimatedVisibility(
                    visible = !isSelectionMode && !showAddDialog,
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
                            defaultElevation = 8.dp,
                            pressedElevation = 12.dp
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

    if (showAddDialog) {
        EnhancedAddLinkBottomSheet(
            recentLinks = uiState.linksByDomain.values.flatten().sortedByDescending { it.createdAt }.take(10),
            onDismiss = { showAddDialog = false },
            onConfirm = { url, metadata ->
                viewModel.saveLink(url, metadata)
                showAddDialog = false
            },
            onAutoPaste = {}
        )
    }

    if (showViewSettings) {
        ModalBottomSheet(
            onDismissRequest = { showViewSettings = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            FoldersViewSettingsMenu(
                gridCellsCount = uiState.gridCellsCount,
                sortOrder = uiState.sortOrder,
                onGridCellsCountChange = { viewModel.setGridCellsCount(it) },
                onSortOrderChange = { viewModel.setSortOrder(it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderGridCard(
    folder: FolderItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface
            ) {
                val faviconUrl = "https://www.google.com/s2/favicons?domain=${folder.domain}&sz=128"
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(faviconUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = getCleanDomainName(folder.domain),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${folder.linkCount} Links",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderListCard(
    folder: FolderItem,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    links: List<Link>,
    isSelectionMode: Boolean,
    selectedLinks: Set<Int>,
    onLinkClick: (Int) -> Unit,
    onLinkLongPress: (Int) -> Unit,
    onFavoriteClick: (Link) -> Unit,
    onCopyClick: (Link) -> Unit,
    onShareClick: (Link) -> Unit,
    onDeleteClick: (Link) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = getCleanDomainName(folder.domain),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${folder.linkCount} link${if (folder.linkCount != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isExpanded && links.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                links.forEach { link ->
                    LinkCard(
                        link = link,
                        onClick = {
                            onLinkClick(link.id)
                        },
                        onLongPress = {
                            onLinkLongPress(link.id)
                        },
                        isSelected = selectedLinks.contains(link.id),
                        isSelectionMode = isSelectionMode,
                        onFavoriteClick = { onFavoriteClick(link) },
                        onMoreClick = { },
                        onCopyClick = { onCopyClick(link) },
                        onShareClick = { onShareClick(link) },
                        onDeleteClick = { onDeleteClick(link) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun EmptyFoldersState(modifier: Modifier = Modifier) {
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
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No Folders Yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Start adding links to see them\norganized by domain",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FoldersViewSettingsMenu(
    gridCellsCount: Int,
    sortOrder: String,
    onGridCellsCountChange: (Int) -> Unit,
    onSortOrderChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp, start = 24.dp, end = 24.dp, top = 8.dp)
    ) {
        Text(
            text = "View Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        //  Grid Layout Option 
        Text(
            text = "Layout",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = gridCellsCount.toFloat(),
                onValueChange = { onGridCellsCountChange(it.toInt()) },
                valueRange = 1f..6f,
                steps = 4,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
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
        }

        Spacer(modifier = Modifier.height(28.dp))

        //  Sort Order 
        Text(
            text = "Sort Folders (A-Z)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FoldersSortChip(
                label = "A - Z",
                selected = sortOrder == "ASC",
                onClick = { onSortOrderChange("ASC") },
                modifier = Modifier.weight(1f)
            )
            FoldersSortChip(
                label = "Z - A",
                selected = sortOrder == "DESC",
                onClick = { onSortOrderChange("DESC") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FoldersSortChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        tonalElevation = if (selected) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp).padding(end = 0.dp),
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