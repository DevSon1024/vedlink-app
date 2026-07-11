package com.devson.vedlink.ui.presentation.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.devson.vedlink.domain.model.Folder
import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.ui.presentation.components.CompactLinkCard
import com.devson.vedlink.ui.presentation.components.EnhancedAddLinkBottomSheet
import com.devson.vedlink.ui.presentation.components.FolderViewSettingsBottomSheet
import com.devson.vedlink.ui.presentation.components.LinkCard
import com.devson.vedlink.ui.presentation.components.MicroLinkCard
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.devson.vedlink.ui.presentation.helper.*
import com.devson.vedlink.ui.viewmodel.FolderItem
import com.devson.vedlink.ui.viewmodel.FoldersUiEvent
import com.devson.vedlink.ui.viewmodel.FoldersViewModel
import com.devson.vedlink.ui.viewmodel.SettingsViewModel
import kotlin.collections.get

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FoldersScreen(
    isActive: Boolean = false,
    onUpdateTopBarConfig: (com.devson.vedlink.ui.presentation.components.TopBarConfig) -> Unit = {},
    onNavigateToDetails: (Int) -> Unit,
    viewModel: FoldersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Smart Domains, 1 = Custom Collections

    // Smart Domain view states
    var selectedFolderDomain by remember { mutableStateOf<String?>(null) }
    var expandedDomain by remember { mutableStateOf<String?>(null) }

    // Custom folder view states
    var selectedCustomFolderId by remember { mutableStateOf<Int?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var folderToRename by remember { mutableStateOf<Folder?>(null) }
    var renameFolderName by remember { mutableStateOf("") }

    // Selection mode states (applies to link browsing inside folders)
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedLinks by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
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
        val currentLinks = if (selectedTab == 0) {
            uiState.linksByDomain[selectedFolderDomain] ?: emptyList()
        } else {
            uiState.linksByFolderId[selectedCustomFolderId] ?: emptyList()
        }

        if (selectedLinks.size == currentLinks.size) {
            exitSelectionMode()
        } else {
            selectedLinks = currentLinks.map { it.id }.toSet()
        }
    }

    // Dynamic Back Navigation
    BackHandler(enabled = selectedFolderDomain != null || selectedCustomFolderId != null || isSelectionMode) {
        when {
            isSelectionMode -> exitSelectionMode()
            selectedFolderDomain != null -> selectedFolderDomain = null
            selectedCustomFolderId != null -> {
                val currentFolder = uiState.customFolders.find { it.id == selectedCustomFolderId }
                selectedCustomFolderId = currentFolder?.parentId
            }
        }
    }

    // Determine current viewed folder links and selected link statuses
    val activeLinks = if (selectedTab == 0) {
        uiState.linksByDomain[selectedFolderDomain] ?: emptyList()
    } else {
        uiState.linksByFolderId[selectedCustomFolderId] ?: emptyList()
    }
    val selectedLinksData = activeLinks.filter { it.id in selectedLinks }
    val favoriteStatus = getFavoriteStatus(selectedLinksData)

    LaunchedEffect(isActive) {
        if (!isActive) {
            isSelectionMode = false
            selectedLinks = emptySet()
            selectedFolderDomain = null
            selectedCustomFolderId = null
        }
    }

    LaunchedEffect(isActive, isSelectionMode, selectedLinks.size, activeLinks.size, selectedTab, selectedFolderDomain, selectedCustomFolderId, uiState.customFolders) {
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
                        val allSelected = selectedLinks.size == activeLinks.size
                        IconButton(onClick = { handleSelectAll() }) {
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
                            shareMultipleLinks(context, selectedLinksData)
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
            val titleText = when {
                selectedTab == 0 && selectedFolderDomain != null -> {
                    getCleanDomainName(selectedFolderDomain!!)
                }
                selectedTab == 1 && selectedCustomFolderId != null -> {
                    uiState.customFolders.find { it.id == selectedCustomFolderId }?.name ?: "Folder"
                }
                else -> "Folders"
            }
            val hasBack = (selectedTab == 0 && selectedFolderDomain != null) || (selectedTab == 1 && selectedCustomFolderId != null)
            onUpdateTopBarConfig(
                com.devson.vedlink.ui.presentation.components.TopBarConfig(
                    title = titleText,
                    navigationIcon = if (hasBack) {
                        {
                            IconButton(onClick = {
                                if (selectedTab == 0 && selectedFolderDomain != null) {
                                    selectedFolderDomain = null
                                    isSelectionMode = false
                                    selectedLinks = emptySet()
                                } else if (selectedTab == 1 && selectedCustomFolderId != null) {
                                    val currentFolder = uiState.customFolders.find { it.id == selectedCustomFolderId }
                                    selectedCustomFolderId = currentFolder?.parentId
                                    isSelectionMode = false
                                    selectedLinks = emptySet()
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    } else {
                        {}
                    },
                    actions = {
                        if (selectedTab == 0 && selectedFolderDomain == null) {
                            IconButton(onClick = { showViewSettings = true }) {
                                Icon(
                                    imageVector = Icons.Default.DisplaySettings,
                                    contentDescription = "View settings",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (selectedTab == 1 && selectedCustomFolderId == null) {
                            IconButton(onClick = { showCreateFolderDialog = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.CreateNewFolder,
                                    contentDescription = "Add Folder",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {},
            floatingActionButton = {
                if (selectedTab == 1 && !isSelectionMode) {
                    ExtendedFloatingActionButton(
                        onClick = { showCreateFolderDialog = true },
                        icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                        text = { Text("New Folder") },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Render TabRow only at the root (when no domain or custom subfolder is open)
                if (selectedFolderDomain == null && selectedCustomFolderId == null) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Smart Domains", fontWeight = FontWeight.Medium) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Custom Collections", fontWeight = FontWeight.Medium) }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    when (selectedTab) {
                        0 -> { // Smart Domains
                            when {
                                uiState.isLoading -> {
                                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                                }
                                uiState.folders.isEmpty() -> {
                                    EmptyFoldersState(modifier = Modifier.align(Alignment.Center))
                                }
                                selectedFolderDomain != null -> {
                                    FolderLinksGrid(
                                        links = activeLinks,
                                        gridCellsCount = uiState.gridCellsCount,
                                        selectedLinks = selectedLinks,
                                        isSelectionMode = isSelectionMode,
                                        onLinkClick = { linkId ->
                                            if (isSelectionMode) handleSelectionClick(linkId)
                                            else onNavigateToDetails(linkId)
                                        },
                                        onLinkLongPress = { handleLongPress(it) },
                                        onFavoriteClick = { viewModel.toggleFavorite(it.id, it.isFavorite) },
                                        onCopyClick = {
                                            copyToClipboard(context, it.url)
                                            scope.launch { snackbarHostState.showSnackbar("Copied link") }
                                        },
                                        onShareClick = { shareLink(context, it.url, it.title) },
                                        onDeleteClick = { viewModel.deleteLink(it) }
                                    )
                                }
                                else -> {
                                    DomainsView(
                                        folders = uiState.folders,
                                        expandedDomain = expandedDomain,
                                        linksByDomain = uiState.linksByDomain,
                                        gridCellsCount = uiState.gridCellsCount,
                                        isSelectionMode = isSelectionMode,
                                        selectedLinks = selectedLinks,
                                        onDomainClick = { selectedFolderDomain = it },
                                        onToggleExpand = { domain ->
                                            expandedDomain = if (expandedDomain == domain) null else domain
                                        },
                                        onLinkClick = { linkId ->
                                            if (isSelectionMode) handleSelectionClick(linkId)
                                            else onNavigateToDetails(linkId)
                                        },
                                        onLinkLongPress = { handleLongPress(it) },
                                        onFavoriteClick = { viewModel.toggleFavorite(it.id, it.isFavorite) },
                                        onCopyClick = {
                                            copyToClipboard(context, it.url)
                                            scope.launch { snackbarHostState.showSnackbar("Copied link") }
                                        },
                                        onShareClick = { shareLink(context, it.url, it.title) },
                                        onDeleteClick = { viewModel.deleteLink(it) }
                                    )
                                }
                            }
                        }
                        1 -> { // Custom Collections
                            val currentFolders = uiState.customFolders.filter { it.parentId == selectedCustomFolderId }
                            val currentLinks = uiState.linksByFolderId[selectedCustomFolderId] ?: emptyList()

                            CustomFoldersView(
                                folders = currentFolders,
                                links = currentLinks,
                                onFolderClick = { selectedCustomFolderId = it.id },
                                onFolderRename = {
                                    folderToRename = it
                                    renameFolderName = it.name
                                },
                                onFolderDelete = { viewModel.deleteFolder(it) },
                                onLinkClick = { linkId ->
                                    if (isSelectionMode) handleSelectionClick(linkId)
                                    else onNavigateToDetails(linkId)
                                },
                                onLinkLongPress = { handleLongPress(it) },
                                isSelectionMode = isSelectionMode,
                                selectedLinks = selectedLinks,
                                onFavoriteClick = { viewModel.toggleFavorite(it.id, it.isFavorite) },
                                onCopyClick = {
                                    copyToClipboard(context, it.url)
                                    scope.launch { snackbarHostState.showSnackbar("Copied link") }
                                },
                                onShareClick = { shareLink(context, it.url, it.title) },
                                onDeleteClick = { viewModel.deleteLink(it) }
                            )
                        }
                    }
                }
            }
        }

        // --- Dialogs & Bottom Sheets ---
        if (showCreateFolderDialog) {
            AlertDialog(
                onDismissRequest = {
                    showCreateFolderDialog = false
                    newFolderName = ""
                },
                title = { Text("Create Folder") },
                text = {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Folder Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newFolderName.isNotBlank()) {
                                viewModel.createFolder(newFolderName.trim(), selectedCustomFolderId)
                                showCreateFolderDialog = false
                                newFolderName = ""
                            }
                        }
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showCreateFolderDialog = false
                        newFolderName = ""
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (folderToRename != null) {
            AlertDialog(
                onDismissRequest = {
                    folderToRename = null
                    renameFolderName = ""
                },
                title = { Text("Rename Folder") },
                text = {
                    OutlinedTextField(
                        value = renameFolderName,
                        onValueChange = { renameFolderName = it },
                        label = { Text("Folder Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (renameFolderName.isNotBlank()) {
                                viewModel.updateFolder(folderToRename!!.copy(name = renameFolderName.trim()))
                                folderToRename = null
                                renameFolderName = ""
                            }
                        }
                    ) {
                        Text("Rename")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        folderToRename = null
                        renameFolderName = ""
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Links") },
                text = { Text("Are you sure you want to delete ${selectedLinks.size} link(s)?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteLinks(selectedLinks.toList())
                            exitSelectionMode()
                            showDeleteDialog = false
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showViewSettings) {
            FolderViewSettingsBottomSheet(
                layoutMode = uiState.layoutMode,
                onLayoutModeChange = { viewModel.setFolderLayoutMode(it) },
                gridColumns = uiState.gridColumns,
                onGridColumnsChange = { viewModel.setFolderGridColumns(it) },
                sortOrder = uiState.sortOrder,
                onSortOrderChange = { viewModel.setSortOrder(it) },
                onDismiss = { showViewSettings = false }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
        )
    }
}

@Composable
fun DomainsView(
    folders: List<FolderItem>,
    expandedDomain: String?,
    linksByDomain: Map<String, List<Link>>,
    gridCellsCount: Int,
    isSelectionMode: Boolean,
    selectedLinks: Set<Int>,
    onDomainClick: (String) -> Unit,
    onToggleExpand: (String) -> Unit,
    onLinkClick: (Int) -> Unit,
    onLinkLongPress: (Int) -> Unit,
    onFavoriteClick: (Link) -> Unit,
    onCopyClick: (Link) -> Unit,
    onShareClick: (Link) -> Unit,
    onDeleteClick: (Link) -> Unit
) {
    if (gridCellsCount > 1) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridCellsCount),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = folders, key = { it.domain }) { folder ->
                FolderGridCard(
                    folder = folder,
                    onClick = { onDomainClick(folder.domain) }
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = folders, key = { it.domain }) { folder ->
                FolderListCard(
                    folder = folder,
                    isExpanded = expandedDomain == folder.domain,
                    onToggleExpand = { onToggleExpand(folder.domain) },
                    links = if (expandedDomain == folder.domain) linksByDomain[folder.domain] ?: emptyList() else emptyList(),
                    isSelectionMode = isSelectionMode,
                    selectedLinks = selectedLinks,
                    onLinkClick = onLinkClick,
                    onLinkLongPress = onLinkLongPress,
                    onFavoriteClick = onFavoriteClick,
                    onCopyClick = onCopyClick,
                    onShareClick = onShareClick,
                    onDeleteClick = onDeleteClick
                )
            }
        }
    }
}

@Composable
fun FolderLinksGrid(
    links: List<Link>,
    gridCellsCount: Int,
    selectedLinks: Set<Int>,
    isSelectionMode: Boolean,
    onLinkClick: (Int) -> Unit,
    onLinkLongPress: (Int) -> Unit,
    onFavoriteClick: (Link) -> Unit,
    onCopyClick: (Link) -> Unit,
    onShareClick: (Link) -> Unit,
    onDeleteClick: (Link) -> Unit
) {
    val context = LocalContext.current
    if (gridCellsCount > 1) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridCellsCount),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = links, key = { it.id }) { link ->
                CompactLinkCard(
                    link = link,
                    onClick = { onLinkClick(link.id) },
                    onLongPress = { onLinkLongPress(link.id) },
                    isSelected = selectedLinks.contains(link.id),
                    isSelectionMode = isSelectionMode,
                    onFavoriteClick = { onFavoriteClick(link) },
                    onCopyClick = { onCopyClick(link) },
                    onShareClick = { onShareClick(link) },
                    onDeleteClick = { onDeleteClick(link) }
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = links, key = { it.id }) { link ->
                LinkCard(
                    link = link,
                    onClick = { onLinkClick(link.id) },
                    onLongPress = { onLinkLongPress(link.id) },
                    isSelected = selectedLinks.contains(link.id),
                    isSelectionMode = isSelectionMode,
                    onFavoriteClick = { onFavoriteClick(link) },
                    onMoreClick = {},
                    onCopyClick = { onCopyClick(link) },
                    onShareClick = { onShareClick(link) },
                    onDeleteClick = { onDeleteClick(link) }
                )
            }
        }
    }
}

@Composable
fun CustomFoldersView(
    folders: List<Folder>,
    links: List<Link>,
    onFolderClick: (Folder) -> Unit,
    onFolderRename: (Folder) -> Unit,
    onFolderDelete: (Folder) -> Unit,
    onLinkClick: (Int) -> Unit,
    onLinkLongPress: (Int) -> Unit,
    isSelectionMode: Boolean,
    selectedLinks: Set<Int>,
    onFavoriteClick: (Link) -> Unit,
    onCopyClick: (Link) -> Unit,
    onShareClick: (Link) -> Unit,
    onDeleteClick: (Link) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (folders.isEmpty() && links.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "This folder is empty.\nClick '+' to add a subfolder.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Subfolders
        items(items = folders, key = { it.id }) { folder ->
            var expandedMenu by remember { mutableStateOf(false) }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onFolderClick(folder) },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box {
                        IconButton(onClick = { expandedMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Folder options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = {
                                    expandedMenu = false
                                    onFolderRename(folder)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    expandedMenu = false
                                    onFolderDelete(folder)
                                }
                            )
                        }
                    }
                }
            }
        }

        if (links.isNotEmpty()) {
            item {
                Text(
                    text = "Saved Links",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            items(items = links, key = { it.id }) { link ->
                LinkCard(
                    link = link,
                    onClick = { onLinkClick(link.id) },
                    onLongPress = { onLinkLongPress(link.id) },
                    isSelected = selectedLinks.contains(link.id),
                    isSelectionMode = isSelectionMode,
                    onFavoriteClick = { onFavoriteClick(link) },
                    onMoreClick = {},
                    onCopyClick = { onCopyClick(link) },
                    onShareClick = { onShareClick(link) },
                    onDeleteClick = { onDeleteClick(link) }
                )
            }
        }
    }
}

// --- Dynamic Domains Rendering Components ---
@Composable
fun FolderGridCard(
    folder: FolderItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = getCleanDomainName(folder.domain),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${folder.linkCount} links",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getCleanDomainName(folder.domain),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${folder.linkCount} saved items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (links.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else {
                    links.forEach { link ->
                        LinkCard(
                            link = link,
                            onClick = { onLinkClick(link.id) },
                            onLongPress = { onLinkLongPress(link.id) },
                            isSelected = selectedLinks.contains(link.id),
                            isSelectionMode = isSelectionMode,
                            onFavoriteClick = { onFavoriteClick(link) },
                            onMoreClick = {},
                            onCopyClick = { onCopyClick(link) },
                            onShareClick = { onShareClick(link) },
                            onDeleteClick = { onDeleteClick(link) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyFoldersState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FolderCopy,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No folders created yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Start adding links to see them\norganized by domain",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

fun getCleanDomainName(domain: String): String {
    return domain
        .removePrefix("www.")
        .substringBeforeLast(".")
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}