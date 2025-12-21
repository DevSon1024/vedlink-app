package com.devson.vedlink.ui.presentation.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import com.devson.vedlink.ui.presentation.components.EnhancedAddLinkBottomSheet
import com.devson.vedlink.ui.presentation.components.LinkCard
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.devson.vedlink.ui.presentation.helper.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigateToDetails: (Int) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Link?>(null) }

    // Selection mode state
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedLinks by remember { mutableStateOf<Set<Int>>(emptySet()) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is HomeUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is HomeUiEvent.ShowSuccess -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    // Function to exit selection mode
    fun exitSelectionMode() {
        isSelectionMode = false
        selectedLinks = emptySet()
    }

    // Function to handle long press
    fun handleLongPress(linkId: Int) {
        if (!isSelectionMode) {
            isSelectionMode = true
            selectedLinks = setOf(linkId)
        }
    }

    // Function to handle click in selection mode
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Column {
                    if (isSelectionMode) {
                        // Selection Mode Top Bar
                        SelectionTopBar(
                            selectedCount = selectedLinks.size,
                            onClose = { exitSelectionMode() },
                            onShare = {
                                val selectedLinksData = uiState.links.filter { it.id in selectedLinks }
                                shareMultipleLinks(context, selectedLinksData)
                                exitSelectionMode()
                            },
                            onFavorite = {
                                selectedLinks.forEach { linkId ->
                                    val link = uiState.links.find { it.id == linkId }
                                    link?.let {
                                        viewModel.toggleFavorite(it.id, it.isFavorite)
                                    }
                                }
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "${selectedLinks.size} link(s) updated",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                exitSelectionMode()
                            },
                            onDelete = {
                                val selectedLinksData = uiState.links.filter { it.id in selectedLinks }
                                showDeleteDialog = selectedLinksData.firstOrNull()
                            }
                        )
                    } else {
                        // Normal Top Bar
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
                        uiState.isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        uiState.links.isEmpty() -> {
                            EmptyState(
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> {
                            LinksList(
                                links = uiState.links,
                                isGridView = uiState.isGridView,
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
                                onDeleteClick = { link ->
                                    showDeleteDialog = link
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
                                }
                            )
                        }
                    }
                }

                if (!uiState.isSearchActive && !isSelectionMode) {
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 100.dp)
                            .size(64.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
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

    showDeleteDialog?.let { link ->
        if (isSelectionMode && selectedLinks.isNotEmpty()) {
            MultiDeleteConfirmationDialog(
                count = selectedLinks.size,
                onDismiss = {
                    showDeleteDialog = null
                },
                onConfirm = {
                    selectedLinks.forEach { linkId ->
                        val linkToDelete = uiState.links.find { it.id == linkId }
                        linkToDelete?.let { viewModel.deleteLink(it) }
                    }
                    showDeleteDialog = null
                    exitSelectionMode()
                }
            )
        } else {
            DeleteConfirmationDialog(
                onDismiss = { showDeleteDialog = null },
                onConfirm = {
                    viewModel.deleteLink(link)
                    showDeleteDialog = null
                }
            )
        }
    }
}

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
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search"
            )
        },
        trailingIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close search"
                )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LinksList(
    links: List<Link>,
    isGridView: Boolean,
    isSelectionMode: Boolean,
    selectedLinks: Set<Int>,
    onLinkClick: (Int) -> Unit,
    onLinkLongPress: (Int) -> Unit,
    onFavoriteClick: (Link) -> Unit,
    onDeleteClick: (Link) -> Unit,
    onCopyClick: (Link) -> Unit,
    onShareClick: (Link) -> Unit
) {
    if (isGridView) {
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
                items = links,
                key = { it.id }
            ) { link ->
                CompactLinkCard(
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
                    onCopyClick = { onCopyClick(link) },
                    onShareClick = { onShareClick(link) },
                    onDeleteClick = { onDeleteClick(link) }
                )
            }
        }
    } else {
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
                items = links,
                key = { it.id }
            ) { link ->
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
                    onMoreClick = { onDeleteClick(link) },
                    onCopyClick = { onCopyClick(link) },
                    onShareClick = { onShareClick(link) },
                    onDeleteClick = { onDeleteClick(link) }
                )
            }
        }
    }
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
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
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

@Composable
fun DeleteConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Delete Link?",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete this link? This action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}