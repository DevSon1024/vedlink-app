package com.devson.vedlink.ui.presentation.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devson.vedlink.ui.presentation.components.CompactLinkCard
import com.devson.vedlink.ui.presentation.components.EnhancedAddLinkBottomSheet
import kotlinx.coroutines.flow.collectLatest
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSavedLinks: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToDetails: (Int) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    val greeting = remember {
        val calendar = Calendar.getInstance()
        when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 120.dp)
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -it / 2 }
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "$greeting 👋",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Here's your link collection at a glance",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = !uiState.isLoading,
                    enter = fadeIn(tween(600)) + scaleIn(tween(600), initialScale = 0.9f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Saved Links",
                            count = uiState.totalLinks,
                            icon = Icons.Default.Bookmarks,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Favorites",
                            count = uiState.totalFavorites,
                            icon = Icons.Default.Favorite,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(700)) + slideInHorizontally(tween(700)) { it / 2 }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(
                            text = "Quick Actions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ActionCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Favorite,
                                title = "Favorites",
                                onClick = onNavigateToFavorites
                            )
                            ActionCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Folder,
                                title = "Folders",
                                onClick = onNavigateToFolders
                            )
                            ActionCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Search,
                                title = "Search",
                                onClick = onNavigateToSavedLinks
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                AnimatedVisibility(
                    visible = uiState.recentLinks.isNotEmpty(),
                    enter = fadeIn(tween(800)) + slideInVertically(tween(800)) { it / 2 }
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recently Saved",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "See All",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onNavigateToSavedLinks() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val listState = rememberLazyListState()
                        LazyRow(
                            state = listState,
                            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(uiState.recentLinks, key = { it.id }) { link ->
                                Box(modifier = Modifier.width(300.dp)) {
                                    CompactLinkCard(
                                        link = link,
                                        onClick = { onNavigateToDetails(link.id) },
                                        onLongPress = { },
                                        isSelected = false,
                                        isSelectionMode = false,
                                        onFavoriteClick = { } // Keep it simple on home
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Animated FAB
            AnimatedVisibility(
                visible = !showAddDialog,
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

            if (showAddDialog) {
                EnhancedAddLinkBottomSheet(
                    recentLinks = uiState.recentLinks,
                    onDismiss = { showAddDialog = false },
                    onConfirm = { url ->
                        viewModel.saveLink(url)
                        showAddDialog = false
                    },
                    onAutoPaste = {}
                )
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun ActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}