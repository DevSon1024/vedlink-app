package com.devson.vedlink.ui.presentation.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devson.vedlink.domain.model.SearchTopic
import com.devson.vedlink.domain.usecase.SmartSpaceCategory
import com.devson.vedlink.ui.presentation.components.LinkCard
import com.devson.vedlink.ui.presentation.helper.copyToClipboard
import com.devson.vedlink.ui.presentation.helper.shareLink
import com.devson.vedlink.ui.viewmodel.SearchTopicsUiEvent
import com.devson.vedlink.ui.viewmodel.SearchTopicsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopicsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetails: ((Int, List<Int>) -> Unit)? = null,
    viewModel: SearchTopicsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var newQueryText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is SearchTopicsUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                }
                is SearchTopicsUiEvent.ShowSuccess -> {
                    snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.selectedSpace != null) uiState.selectedSpace!!.title else "Smart Spaces & Topics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (uiState.selectedSpace == null) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Topic")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Input Field
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(
                        text = if (uiState.selectedSpace != null) {
                            "Search ${uiState.selectedSpace!!.title.lowercase()}..."
                        } else {
                            "Search topics or saved queries..."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Horizontal Bento Grid of Smart Spaces
            Text(
                text = "Smart Spaces",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // My Topics Bento Card
                SmartSpaceBentoCard(
                    title = "My Topics",
                    count = uiState.topics.size,
                    icon = Icons.Default.Lightbulb,
                    isSelected = uiState.selectedSpace == null,
                    tintColor = MaterialTheme.colorScheme.primary,
                    onClick = { viewModel.selectSpace(null) }
                )

                // Pinned Notes Bento Card
                SmartSpaceBentoCard(
                    title = "Pinned Notes",
                    count = uiState.pinnedCount,
                    icon = Icons.Default.PushPin,
                    isSelected = uiState.selectedSpace == SmartSpaceCategory.PINNED,
                    tintColor = MaterialTheme.colorScheme.secondary,
                    onClick = { viewModel.selectSpace(SmartSpaceCategory.PINNED) }
                )

                // Rich Notes Bento Card
                SmartSpaceBentoCard(
                    title = "Rich Notes",
                    count = uiState.richNotesCount,
                    icon = Icons.AutoMirrored.Filled.NoteAdd,
                    isSelected = uiState.selectedSpace == SmartSpaceCategory.RICH_NOTES,
                    tintColor = MaterialTheme.colorScheme.tertiary,
                    onClick = { viewModel.selectSpace(SmartSpaceCategory.RICH_NOTES) }
                )

                // Videos & Media Bento Card
                SmartSpaceBentoCard(
                    title = "Videos & Media",
                    count = uiState.videosCount,
                    icon = Icons.Default.PlayCircle,
                    isSelected = uiState.selectedSpace == SmartSpaceCategory.VIDEOS,
                    tintColor = Color(0xFFFF5252),
                    onClick = { viewModel.selectSpace(SmartSpaceCategory.VIDEOS) }
                )

                // Code & Dev Bento Card
                SmartSpaceBentoCard(
                    title = "Code & Dev",
                    count = uiState.codeCount,
                    icon = Icons.Default.Code,
                    isSelected = uiState.selectedSpace == SmartSpaceCategory.CODE,
                    tintColor = Color(0xFF00B0FF),
                    onClick = { viewModel.selectSpace(SmartSpaceCategory.CODE) }
                )

                // Articles & Reads Bento Card
                SmartSpaceBentoCard(
                    title = "Articles & Reads",
                    count = uiState.articlesCount,
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    isSelected = uiState.selectedSpace == SmartSpaceCategory.ARTICLES,
                    tintColor = Color(0xFF00BFA5),
                    onClick = { viewModel.selectSpace(SmartSpaceCategory.ARTICLES) }
                )
            }

            // Tag Filter Chips (shown when a Smart Space is active and tags exist)
            if (uiState.selectedSpace != null && uiState.availableTags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.selectedTag == null,
                        onClick = { viewModel.selectTag(null) },
                        label = { Text("All Tags") },
                        shape = RoundedCornerShape(12.dp)
                    )

                    uiState.availableTags.forEach { tag ->
                        FilterChip(
                            selected = uiState.selectedTag.equals(tag, ignoreCase = true),
                            onClick = { viewModel.selectTag(tag) },
                            label = { Text("#$tag") },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Dynamic Content with AnimatedContent transition
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                AnimatedContent(
                    targetState = uiState.selectedSpace,
                    transitionSpec = {
                        (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                                scaleIn(initialScale = 0.94f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                            .togetherWith(
                                fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                                        scaleOut(targetScale = 0.94f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            )
                    },
                    label = "SmartSpaceContentTransition",
                    modifier = Modifier.fillMaxSize()
                ) { space ->
                    if (space == null) {
                        // User Saved Topics View
                        Column(modifier = Modifier.fillMaxSize()) {
                            TabRow(
                                selectedTabIndex = uiState.selectedFilterTab,
                                containerColor = MaterialTheme.colorScheme.background,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Tab(
                                    selected = uiState.selectedFilterTab == 0,
                                    onClick = { viewModel.setFilterTab(0) },
                                    text = {
                                        Text(
                                            "Active (${uiState.topics.count { !it.isCompleted }})",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                )
                                Tab(
                                    selected = uiState.selectedFilterTab == 1,
                                    onClick = { viewModel.setFilterTab(1) },
                                    text = {
                                        Text(
                                            "Completed (${uiState.topics.count { it.isCompleted }})",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                )
                            }

                            when {
                                uiState.isLoading -> {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
                                }
                                uiState.filteredTopics.isEmpty() -> {
                                    EmptyTopicsState(
                                        selectedFilterTab = uiState.selectedFilterTab,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                else -> {
                                    LazyColumn(
                                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        itemsIndexed(
                                            items = uiState.filteredTopics,
                                            key = { index, topic -> "${topic.id}_$index" }
                                        ) { _, topic ->
                                            SearchTopicCard(
                                                topic = topic,
                                                onClick = {
                                                    val encoded = URLEncoder.encode(topic.query, "UTF-8")
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$encoded"))
                                                    context.startActivity(intent)
                                                },
                                                onToggleCompleted = { viewModel.toggleTopicCompleted(topic) },
                                                onDelete = { viewModel.deleteTopic(topic) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Smart Space Links Staggered Masonry Grid
                        if (uiState.filteredLinks.isEmpty()) {
                            EmptySpaceState(
                                space = space,
                                searchQuery = uiState.searchQuery,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val linkIds = remember(uiState.filteredLinks) {
                                uiState.filteredLinks.map { it.id }
                            }

                            LazyVerticalStaggeredGrid(
                                columns = StaggeredGridCells.Adaptive(minSize = 260.dp),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalItemSpacing = 16.dp,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item(span = StaggeredGridItemSpan.FullLine) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${uiState.filteredLinks.size} ${if (uiState.filteredLinks.size == 1) "link" else "links"} found",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (uiState.selectedTag != null) {
                                            TextButton(onClick = { viewModel.selectTag(null) }) {
                                                Text("Clear tag filter", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }

                                itemsIndexed(
                                    items = uiState.filteredLinks,
                                    key = { _, link -> link.id }
                                ) { _, link ->
                                    LinkCard(
                                        link = link,
                                        onClick = {
                                            if (onNavigateToDetails != null) {
                                                onNavigateToDetails(link.id, linkIds)
                                            } else {
                                                val target = if (link.url.startsWith("http://") || link.url.startsWith("https://")) link.url else "https://${link.url}"
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                })
                                            }
                                        },
                                        onFavoriteClick = {
                                            viewModel.toggleFavorite(link.id, link.isFavorite)
                                        },
                                        onCopyClick = {
                                            copyToClipboard(context, link.url)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Link copied to clipboard", duration = SnackbarDuration.Short)
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

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = {
                    showAddDialog = false
                    newQueryText = ""
                },
                title = { Text("Save Search Topic") },
                text = {
                    OutlinedTextField(
                        value = newQueryText,
                        onValueChange = { newQueryText = it },
                        label = { Text("Topic or Keyword") },
                        placeholder = { Text("Type query here...") },
                        leadingIcon = {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newQueryText.isNotBlank()) {
                                viewModel.addTopic(newQueryText.trim())
                                showAddDialog = false
                                newQueryText = ""
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showAddDialog = false
                            newQueryText = ""
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun SmartSpaceBentoCard(
    title: String,
    count: Int,
    icon: ImageVector,
    isSelected: Boolean,
    tintColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val surfaceColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = modifier
            .width(132.dp)
            .height(86.dp)
            .clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = surfaceColor,
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        },
        tonalElevation = if (isSelected) 3.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = tintColor.copy(alpha = if (isSelected) 0.22f else 0.12f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else tintColor,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SearchTopicCard(
    topic: SearchTopic,
    onClick: () -> Unit,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault()) }
    val timeString = remember(topic.timestamp) { dateFormat.format(Date(topic.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (topic.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (topic.isCompleted) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleCompleted) {
                Icon(
                    imageVector = if (topic.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (topic.isCompleted) "Mark Active" else "Mark Completed",
                    tint = if (topic.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = topic.query,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (topic.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (topic.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun EmptyTopicsState(
    selectedFilterTab: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selectedFilterTab == 0) Icons.Default.Lightbulb else Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (selectedFilterTab == 0) "No active search topics" else "No completed search topics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (selectedFilterTab == 0) {
                "Tap '+' below to save a topic query\nto search later."
            } else {
                "Topics you mark as completed will\nappear here."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EmptySpaceState(
    space: SmartSpaceCategory,
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    val (icon, title, subtitle) = when (space) {
        SmartSpaceCategory.PINNED -> Triple(
            Icons.Default.PushPin,
            "No Pinned Notes",
            if (searchQuery.isNotBlank()) "No pinned notes match \"$searchQuery\"." else "Pin important links or notes to keep them accessible here."
        )
        SmartSpaceCategory.RICH_NOTES -> Triple(
            Icons.AutoMirrored.Filled.NoteAdd,
            "No Rich Notes",
            if (searchQuery.isNotBlank()) "No notes match \"$searchQuery\"." else "Add thoughts, summaries, or Markdown notes to any saved link."
        )
        SmartSpaceCategory.VIDEOS -> Triple(
            Icons.Default.PlayCircle,
            "No Videos or Media",
            if (searchQuery.isNotBlank()) "No media links match \"$searchQuery\"." else "Save YouTube, streaming, or video links to browse here."
        )
        SmartSpaceCategory.CODE -> Triple(
            Icons.Default.Code,
            "No Code or Dev References",
            if (searchQuery.isNotBlank()) "No developer links match \"$searchQuery\"." else "GitHub repos, StackOverflow questions, and dev blogs will appear here."
        )
        SmartSpaceCategory.ARTICLES -> Triple(
            Icons.AutoMirrored.Filled.MenuBook,
            "No Articles or Reads",
            if (searchQuery.isNotBlank()) "No articles match \"$searchQuery\"." else "Articles and long-form reads will automatically categorize here."
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

