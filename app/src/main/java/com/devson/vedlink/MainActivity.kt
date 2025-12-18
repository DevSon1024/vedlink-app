package com.devson.vedlink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.devson.vedlink.data.preferences.ThemePreferences
import com.devson.vedlink.ui.presentation.components.EnhancedAddLinkBottomSheet
import com.devson.vedlink.ui.presentation.components.ModernBoxedBottomNavBar
import com.devson.vedlink.ui.presentation.navigation.NavGraph
import com.devson.vedlink.ui.presentation.navigation.Screen
import com.devson.vedlink.ui.presentation.screens.home.HomeViewModel
import com.devson.vedlink.ui.presentation.theme.VedLinkTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themePreferences: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val isDarkMode by themePreferences.isDarkMode.collectAsState(initial = false)

            VedLinkTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                var showAddSheet by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background
                    ) { paddingValues ->
                        NavGraph(
                            navController = navController,
                            onNavigateToDetails = { linkId ->
                                navController.navigate(Screen.LinkDetails.createRoute(linkId))
                            }
                        )
                    }

                    // Show bottom bar and FAB only on main screens
                    if (currentRoute in listOf(
                            Screen.Home.route,
                            Screen.Favorites.route,
                            Screen.Settings.route
                        )
                    ) {
                        // Modern Bottom Navigation Bar
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                        ) {
                            ModernBoxedBottomNavBar(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        popUpTo(Screen.Home.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }

                        // Centered FAB above navbar (only on Home screen)
                        if (currentRoute == Screen.Home.route) {
                            FloatingActionButton(
                                onClick = { showAddSheet = true },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 100.dp)
                                    .size(64.dp),
                                containerColor = MaterialTheme.colorScheme.primary,
                                elevation = FloatingActionButtonDefaults.elevation(
                                    defaultElevation = 6.dp,
                                    pressedElevation = 12.dp
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add Link",
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    // Add Link Bottom Sheet - Get ViewModel only when needed
                    if (showAddSheet && currentRoute == Screen.Home.route) {
                        val viewModel: HomeViewModel = hiltViewModel()
                        val uiState by viewModel.uiState.collectAsState()

                        EnhancedAddLinkBottomSheet(
                            recentLinks = uiState.links,
                            onDismiss = { showAddSheet = false },
                            onConfirm = { url ->
                                viewModel.saveLink(url)
                                showAddSheet = false
                            },
                            onAutoPaste = { /* Handle auto paste event if needed */ }
                        )
                    }
                }
            }
        }
    }
}