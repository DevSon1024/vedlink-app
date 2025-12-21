package com.devson.vedlink.ui.presentation.screens.folders

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.ui.presentation.screens.home.LinksList
import kotlinx.coroutines.launch

// Helper to clean domain names (e.g. "www.instagram.com" -> "Instagram")
fun getCleanDomainName(domain: String): String {
    return domain
        .removePrefix("www.")
        .substringBeforeLast(".") // Removes .com, .net etc roughly
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    onNavigateToDetails: (Int) -> Unit,
    viewModel: FoldersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // State for Accordion in List View
    var expandedDomain by remember { mutableStateOf<String?>(null) }

    // State for navigation in Grid View (Drill down into folder)
    var selectedFolderDomain by remember { mutableStateOf<String?>(null) }

    // Selection mode state
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedLinks by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

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

    // Handle Back press when inside a folder in Grid View or selection mode
    BackHandler(enabled = selectedFolderDomain != null || isSelectionMode) {
        when {
            isSelectionMode -> exitSelectionMode()
            selectedFolderDomain != null -> selectedFolderDomain = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (isSelectionMode && selectedFolderDomain != null) {
                    // Selection Mode Top Bar
                    SelectionTopBar(
                        selectedCount = selectedLinks.size,
                        onClose = { exitSelectionMode() },
                        onShare = {
                            val folderLinks = uiState.linksByDomain[selectedFolderDomain] ?: emptyList()
                            val selectedLinksData = folderLinks.filter { it.id in selectedLinks }
                            shareMultipleLinks(context, selectedLinksData)
                            exitSelectionMode()
                        },
                        onFavorite = {
                            // Handle favorite toggle for selected links
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "${selectedLinks.size} link(s) updated",
                                    duration = SnackbarDuration.Short
                                )
                            }
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
                                IconButton(onClick = { selectedFolderDomain = null }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            }
                        },
                        actions = {
                            // View Toggle Button (Grid/List) - Only show on main Folders view
                            if (selectedFolderDomain == null) {
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
                    // Show links for a specific folder (Grid View Click Action)
                    selectedFolderDomain != null -> {
                        val folderLinks = uiState.linksByDomain[selectedFolderDomain] ?: emptyList()

                        if (folderLinks.isNotEmpty()) {
                            // Reuse LinksList from Home Screen for consistency
                            LinksList(
                                links = folderLinks,
                                isGridView = false,
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
                                    // Handle favorite toggle
                                },
                                onDeleteClick = { },
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
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No links in this folder")
                            }
                        }
                    }
                    // Main Folders View
                    else -> {
                        AnimatedVisibility(
                            visible = uiState.isGridView,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            // Grid View
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = uiState.folders,
                                    key = { it.domain }
                                ) { folder ->
                                    FolderGridCard(
                                        folder = folder,
                                        onClick = { selectedFolderDomain = folder.domain }
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = !uiState.isGridView,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            // List View (Accordion)
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 12.dp,
                                    bottom = 100.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = uiState.folders,
                                    key = { it.domain }
                                ) { folder ->
                                    FolderCard(
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
                                        onLinkClick = onNavigateToDetails
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
    if (showDeleteDialog && selectedLinks.isNotEmpty()) {
        MultiDeleteConfirmationDialog(
            count = selectedLinks.size,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                // Handle deletion of selected links
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "${selectedLinks.size} link(s) deleted",
                        duration = SnackbarDuration.Short
                    )
                }
                showDeleteDialog = false
                exitSelectionMode()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "$selectedCount selected",
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit selection mode"
                )
            }
        },
        actions = {
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share selected",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Toggle favorite",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete selected",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

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
            // Favicon with fallback
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
                    contentScale = ContentScale.Fit,
                    error = null // Could add error icon
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

@Composable
fun FolderCard(
    folder: FolderItem,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    links: List<Link>,
    onLinkClick: (Int) -> Unit
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

            // Expanded links list
            if (isExpanded && links.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                links.forEach { link ->
                    LinkItemInFolder(
                        link = link,
                        onClick = { onLinkClick(link.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun LinkItemInFolder(
    link: Link,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = link.title ?: "Untitled",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (link.description != null) {
                    Text(
                        text = link.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (link.isFavorite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorite",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
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
fun MultiDeleteConfirmationDialog(
    count: Int,
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
                text = "Delete $count Link${if (count > 1) "s" else ""}?",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete $count selected link${if (count > 1) "s" else ""}? This action cannot be undone.",
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
                Text("Delete All")
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

// Helper functions
private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Link", text)
    clipboard.setPrimaryClip(clip)
}

private fun shareLink(context: Context, url: String, title: String?) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
        putExtra(Intent.EXTRA_TITLE, title ?: "Check out this link")
    }
    context.startActivity(Intent.createChooser(intent, "Share link via"))
}

private fun shareMultipleLinks(context: Context, links: List<Link>) {
    val shareText = buildString {
        appendLine("Check out these links:")
        appendLine()
        links.forEach { link ->
            if (!link.title.isNullOrBlank()) {
                appendLine(link.title)
            }
            appendLine(link.url)
            appendLine()
        }
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_TITLE, "Shared Links from VedLink")
    }
    context.startActivity(Intent.createChooser(intent, "Share links via"))
}