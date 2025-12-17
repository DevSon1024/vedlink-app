package com.devson.vedlink.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.devson.vedlink.presentation.screens.favorites.FavoritesScreen
import com.devson.vedlink.presentation.screens.home.HomeScreen
import com.devson.vedlink.presentation.screens.settings.SettingsScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    onNavigateToDetails: (Int) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToDetails = onNavigateToDetails
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onNavigateToDetails = onNavigateToDetails
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }

        composable(
            route = Screen.LinkDetails.route,
            arguments = listOf(
                navArgument("linkId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val linkId = backStackEntry.arguments?.getInt("linkId") ?: return@composable
            // LinkDetailsScreen will be implemented later
        }
    }
}
