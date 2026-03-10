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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.devson.vedlink.domain.model.Link
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
            in 0..11  -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else      -> "Good Evening"
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is HomeUiEvent.ShowError   -> snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                is HomeUiEvent.ShowSuccess -> snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
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
                // ── Greeting ──────────────────────────────────────────────────
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -it / 2 }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
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

                // ── Stats ─────────────────────────────────────────────────────
                AnimatedVisibility(
                    visible = !uiState.isLoading && uiState.showStats,
                    enter = fadeIn(tween(600)) + scaleIn(tween(600), initialScale = 0.9f),
                    exit = fadeOut(tween(400)) + scaleOut(tween(400))
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

                if (uiState.showStats) Spacer(modifier = Modifier.height(24.dp))

                // ── Quick Actions ─────────────────────────────────────────────
                AnimatedVisibility(
                    visible = uiState.showQuickActions,
                    enter = fadeIn(tween(700)) + slideInHorizontally(tween(700)) { it / 2 },
                    exit  = fadeOut(tween(400)) + slideOutHorizontally(tween(400)) { it / 2 }
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

                if (uiState.showQuickActions) Spacer(modifier = Modifier.height(32.dp))

                // ── Jump Back In ──────────────────────────────────────────────
                AnimatedVisibility(
                    visible = uiState.showRecentLinks && uiState.recentLinks.isNotEmpty(),
                    enter = fadeIn(tween(800)) + slideInVertically(tween(800)) { it / 2 },
                    exit  = fadeOut(tween(400)) + slideOutVertically(tween(400)) { it / 2 }
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Jump Back In",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
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

                        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                        val cardWidth = screenWidth * 0.72f

                        val listState = rememberLazyListState()
                        LazyRow(
                            state = listState,
                            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(uiState.recentLinks, key = { it.id }) { link ->
                                JumpBackInCard(
                                    link = link,
                                    cardWidth = cardWidth.value,
                                    onClick = { onNavigateToDetails(link.id) }
                                )
                            }
                        }
                    }
                }
            }

            // ── Animated FAB ──────────────────────────────────────────────────
            AnimatedVisibility(
                visible = !showAddDialog,
                enter = scaleIn(animationSpec = tween(durationMillis = 300)),
                exit  = scaleOut(animationSpec = tween(durationMillis = 300)),
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

// ── Jump Back In Card ─────────────────────────────────────────────────────────

@Composable
private fun JumpBackInCard(
    link: Link,
    cardWidth: Float,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(cardWidth.dp)
            .height(210.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Full-bleed background image
            if (!link.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(link.imageUrl)
                        .size(600, 420)
                        .scale(Scale.FILL)
                        .crossfade(true)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = link.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            // Gradient overlay — bottom 60%
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.65f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Favourite badge (top-start)
            if (link.isFavorite) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFF4081).copy(alpha = 0.92f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favourite",
                        modifier = Modifier.padding(4.dp).size(12.dp),
                        tint = Color.White
                    )
                }
            }

            // Text at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    text = link.title ?: "Untitled",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = link.domain ?: "Unknown",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ── Supporting composables ────────────────────────────────────────────────────

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
                Icon(imageVector = icon, contentDescription = null, tint = contentColor)
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