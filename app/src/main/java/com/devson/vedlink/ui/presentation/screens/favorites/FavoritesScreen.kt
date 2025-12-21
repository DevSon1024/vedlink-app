package com.devson.vedlink.ui.presentation.screens.favorites

import androidx.activity.compose.BackHandler
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
import com.devson.vedlink.ui.presentation.components.LinkCard
import com.devson.vedlink.ui.presentation.components.CompactLinkCard
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

    var showDeleteDialog by remember { mutableStateOf<Link?>(null) }
    var isGridView by remember { mutableStateOf(false) }

    // Selection mode state
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedLinks by remember { mutableStateOf<Set<Int>>(emptySet()) }

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

    // Handle back press in selection mode
    BackHandler(enabled = isSelectionMode) {
        exitSelectionMode()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (isSelectionMode) {
                    // Selection Mode Top Bar
                    SelectionTopBar(
                        selectedCount = selectedLinks.size,
                        onClose = { exitSelectionMode() },
                        onShare = {
                            val selectedLinksData = uiState.favoriteLinks.filter { it.id in selectedLinks }
                            shareMultipleLinks(context, selectedLinksData)
                            exitSelectionMode()
                        },
                        onFavorite = {
                            selectedLinks.forEach { linkId ->
                                val link = uiState.favoriteLinks.find { it.id == linkId }
                                link?.let {
                                    viewModel.toggleFavorite(it.id, it.isFavorite)
                                }
                            }
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "${selectedLinks.size} link(s) removed from favorites",
                                    duration = SnackbarDuration.Short
                                )
                            }
                            exitSelectionMode()
                        },
                        onDelete = {
                            showDeleteDialog = uiState.favoriteLinks.find { it.id in selectedLinks }
                        }
                    )
                } else {
                    // Normal Top Bar
                    TopAppBar(
                        title = {
                            Text(
                                text = "Favorites",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        actions = {
                            // View Toggle Button (Grid/List)
                            if (uiState.favoriteLinks.isNotEmpty()) {
                                IconButton(onClick = { isGridView = !isGridView }) {
                                    Icon(
                                        imageVector = if (isGridView)
                                            Icons.Default.ViewList
                                        else
                                            Icons.Default.GridView,
                                        contentDescription = if (isGridView) "List View" else "Grid View",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
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
                        if (isGridView) {
                            // Grid View
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
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = "Removed from favorites",
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
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
                                            showDeleteDialog = link
                                        }
                                    )
                                }
                            }
                        } else {
                            // List View
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
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = "Removed from favorites",
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        },
                                        onMoreClick = {
                                            showDeleteDialog = link
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
                                            showDeleteDialog = link
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        )
    }

    // Delete Dialog
    showDeleteDialog?.let { link ->
        if (isSelectionMode && selectedLinks.isNotEmpty()) {
            MultiDeleteConfirmationDialog(
                count = selectedLinks.size,
                onDismiss = {
                    showDeleteDialog = null
                },
                onConfirm = {
                    selectedLinks.forEach { linkId ->
                        val linkToDelete = uiState.favoriteLinks.find { it.id == linkId }
                        linkToDelete?.let { viewModel.deleteLink(it) }
                    }
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "${selectedLinks.size} link(s) deleted",
                            duration = SnackbarDuration.Short
                        )
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
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Link deleted",
                            duration = SnackbarDuration.Short
                        )
                    }
                    showDeleteDialog = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
                    imageVector = Icons.Default.Favorite,
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
            text = "Links you mark as favorite\nwill appear here",
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