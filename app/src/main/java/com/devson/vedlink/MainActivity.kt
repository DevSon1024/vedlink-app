package com.devson.vedlink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.devson.vedlink.presentation.components.BottomNavBar
import com.devson.vedlink.presentation.navigation.NavGraph
import com.devson.vedlink.presentation.navigation.Screen
import com.devson.vedlink.presentation.theme.VedLinkTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VedLinkTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // Show bottom nav only on main screens
                        if (currentRoute in listOf(
                                Screen.Home.route,
                                Screen.Favorites.route,
                                Screen.Settings.route
                            )
                        ) {
                            BottomNavBar(
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
                ) { paddingValues ->
                    NavGraph(
                        navController = navController,
                        onNavigateToDetails = { linkId ->
                            navController.navigate(Screen.LinkDetails.createRoute(linkId))
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}
