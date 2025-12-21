package com.devson.vedlink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.devson.vedlink.data.preferences.ThemePreferences
import com.devson.vedlink.ui.presentation.components.ModernBoxedBottomNavBar
import com.devson.vedlink.ui.presentation.navigation.NavGraph
import com.devson.vedlink.ui.presentation.navigation.Screen
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
            // Collect all theme preferences
            val themeMode by themePreferences.themeMode.collectAsState(initial = 0)
            val colorScheme by themePreferences.colorScheme.collectAsState(initial = 0)
            val dynamicColor by themePreferences.dynamicColor.collectAsState(initial = false)
            val amoledMode by themePreferences.amoledMode.collectAsState(initial = false)

            VedLinkTheme(
                themeMode = themeMode,
                colorSchemeIndex = colorScheme,
                useDynamicColor = dynamicColor,
                useAmoledMode = amoledMode
            ) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0)
                    ) { paddingValues ->
                        NavGraph(
                            navController = navController,
                            onNavigateToDetails = { linkId ->
                                navController.navigate(Screen.LinkDetails.createRoute(linkId))
                            }
                        )
                    }

                    // Show bottom bar only on main screens
                    if (currentRoute in listOf(
                            Screen.Home.route,
                            Screen.Favorites.route,
                            Screen.Folders.route,
                            Screen.Settings.route
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars)
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
                    }
                }
            }
        }
    }
}