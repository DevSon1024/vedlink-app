package com.devson.vedlink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.devson.vedlink.data.preferences.ThemePreferences
import com.devson.vedlink.data.network.scraper.MetadataPipeline
import com.devson.vedlink.domain.repository.LinkRepository
import com.devson.vedlink.domain.usecase.SaveLinkUseCase
import com.devson.vedlink.ui.presentation.components.EnhancedAddLinkBottomSheet
import com.devson.vedlink.ui.presentation.navigation.NavGraph
import com.devson.vedlink.ui.presentation.navigation.Screen
import com.devson.vedlink.ui.theme.VedLinkTheme
import com.devson.vedlink.ui.theme.AppThemePalette
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themePreferences: ThemePreferences

    @Inject
    lateinit var linkRepository: LinkRepository

    @Inject
    lateinit var saveLinkUseCase: SaveLinkUseCase

    @Inject
    lateinit var metadataPipeline: MetadataPipeline

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            // Collect all theme preferences
            val themeMode by themePreferences.themeMode.collectAsState(initial = 0)
            val colorSchemePref by themePreferences.colorScheme.collectAsState(initial = 0)
            val dynamicColor by themePreferences.dynamicColor.collectAsState(initial = false)
            val isNavBarTransparent by themePreferences.navBarTransparent.collectAsState(initial = false)
            val isBackgroundBlurEnabled by themePreferences.isBackgroundBlurEnabled.collectAsState(initial = true)

            val forceDark = when (themeMode) {
                1 -> false
                2 -> true
                else -> null
            }
            
            val palette = AppThemePalette.entries.getOrElse(colorSchemePref) {
                AppThemePalette.CINEMATIC
            }

            VedLinkTheme(
                forceDark = forceDark,
                dynamicColor = dynamicColor,
                palette = palette,
                isNavBarTransparent = isNavBarTransparent
            ) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val scope = rememberCoroutineScope()
                val snackbarHostState = remember { SnackbarHostState() }
                var showAddDialog by remember { mutableStateOf(false) }

                // Gather recent links reactively for the AddLink Bottom Sheet
                val allLinks by linkRepository.getAllLinks().collectAsState(initial = emptyList())
                val recentLinks = remember(allLinks) {
                    allLinks.sortedByDescending { it.createdAt }.take(10)
                }

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            // Show navigation bar only on primary screens
                            val showBottomBar = currentRoute in listOf(
                                Screen.Home.route,
                                Screen.SavedLinks.route,
                                Screen.Folders.route,
                                Screen.Favorites.route,
                                Screen.Settings.route
                            )
                            if (showBottomBar) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    tonalElevation = 3.dp,
                                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                                ) {
                                    val items = listOf(
                                        NavigationItem(Screen.Home.route, "Home", Icons.Outlined.Home, Icons.Filled.Home),
                                        NavigationItem(Screen.SavedLinks.route, "Saved", Icons.Outlined.Bookmarks, Icons.Filled.Bookmarks),
                                        NavigationItem(Screen.Folders.route, "Folders", Icons.Outlined.Folder, Icons.Filled.Folder),
                                        NavigationItem(Screen.Favorites.route, "Favorites", Icons.Outlined.StarBorder, Icons.Filled.Star),
                                        NavigationItem(Screen.Settings.route, "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
                                    )
                                    items.forEach { item ->
                                        val isSelected = currentRoute == item.route
                                        NavigationBarItem(
                                            selected = isSelected,
                                            onClick = {
                                                if (currentRoute != item.route) {
                                                    navController.navigate(item.route) {
                                                        popUpTo(navController.graph.startDestinationId) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = if (isSelected) item.selectedIcon else item.icon,
                                                    contentDescription = item.label
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = item.label,
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        floatingActionButton = {
                            // Show floating action button only on main grid screens
                            val showFAB = currentRoute in listOf(
                                Screen.Home.route,
                                Screen.SavedLinks.route,
                                Screen.Folders.route,
                                Screen.Favorites.route
                            )
                            AnimatedVisibility(
                                visible = showFAB && !showAddDialog,
                                enter = scaleIn(animationSpec = tween(durationMillis = 400)) + fadeIn(),
                                exit = scaleOut(animationSpec = tween(durationMillis = 300)) + fadeOut()
                            ) {
                                FloatingActionButton(
                                    onClick = { showAddDialog = true },
                                    modifier = Modifier
                                        .size(64.dp)
                                        .shadow(12.dp, CircleShape),
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    shape = CircleShape
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Add Link",
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    ) { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = paddingValues.calculateBottomPadding())
                        ) {
                            NavGraph(
                                navController = navController,
                                onNavigateToDetails = { linkId ->
                                    navController.navigate(Screen.LinkDetails.createRoute(linkId))
                                }
                            )
                        }
                    }

                    // Unified Add Link Dialog Interface
                    if (showAddDialog) {
                        EnhancedAddLinkBottomSheet(
                            recentLinks = recentLinks,
                            onDismiss = { showAddDialog = false },
                            onConfirm = { url, metadata ->
                                showAddDialog = false
                                scope.launch {
                                    saveLinkUseCase(
                                        url = url,
                                        title = metadata?.title,
                                        description = metadata?.description,
                                        imageUrl = metadata?.imageUrl
                                    ).onSuccess {
                                        snackbarHostState.showSnackbar("Link saved successfully")
                                    }.onFailure { e ->
                                        snackbarHostState.showSnackbar("Error: ${e.message ?: "Failed to save link"}")
                                    }
                                }
                            },
                            onAutoPaste = {},
                            onFetchPreview = { url ->
                                try {
                                    metadataPipeline.resolveMetadata(url)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}