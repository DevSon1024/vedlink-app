package com.devson.vedlink.ui.presentation.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devson.vedlink.domain.model.SearchTopic
import com.devson.vedlink.ui.viewmodel.SearchTopicsUiEvent
import com.devson.vedlink.ui.viewmodel.SearchTopicsViewModel
import kotlinx.coroutines.flow.collectLatest
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopicsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SearchTopicsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedFilterTab by remember { mutableStateOf(0) } // 0 = Active, 1 = Completed

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

    val filteredTopics = remember(uiState.topics, selectedFilterTab) {
        uiState.topics.filter {
            if (selectedFilterTab == 0) !it.isCompleted else it.isCompleted
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Search Topics",
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
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Topic")
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
            // Filter Tabs
            TabRow(
                selectedTabIndex = selectedFilterTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedFilterTab == 0,
                    onClick = { selectedFilterTab = 0 },
                    text = { Text("Active (${uiState.topics.count { !it.isCompleted }})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedFilterTab == 1,
                    onClick = { selectedFilterTab = 1 },
                    text = { Text("Completed (${uiState.topics.count { it.isCompleted }})", fontWeight = FontWeight.Bold) }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    filteredTopics.isEmpty() -> {
                        EmptyTopicsState(
                            selectedFilterTab = selectedFilterTab,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                             itemsIndexed(
                                 items = filteredTopics,
                                 key = { index, topic -> "${topic.id}_$index" }
                             ) { index, topic ->
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
            // Checkbox/CheckCircle complete icon
            IconButton(onClick = onToggleCompleted) {
                Icon(
                    imageVector = if (topic.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (topic.isCompleted) "Mark Active" else "Mark Completed",
                    tint = if (topic.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Query & Time Info
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

            // Search Icon
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Delete Icon
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
                "Tap '+' below to save a random query\nto search later."
            } else {
                "Topics you mark as completed will\nappear here."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
