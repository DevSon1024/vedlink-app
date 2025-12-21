package com.devson.vedlink.ui.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.devson.vedlink.ui.presentation.screens.about.AboutScreen
import com.devson.vedlink.ui.presentation.screens.details.LinkDetailsScreen
import com.devson.vedlink.ui.presentation.screens.favorites.FavoritesScreen
import com.devson.vedlink.ui.presentation.screens.folders.FoldersScreen
import com.devson.vedlink.ui.presentation.screens.home.HomeScreen
import com.devson.vedlink.ui.presentation.screens.settings.SettingsScreen
import com.devson.vedlink.ui.presentation.screens.lookandfeel.LookAndFeelScreen

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
            HomeScreen(onNavigateToDetails = onNavigateToDetails)
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(onNavigateToDetails = onNavigateToDetails)
        }

        composable(Screen.Folders.route) {
            FoldersScreen(onNavigateToDetails = onNavigateToDetails)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToAbout = { navController.navigate(Screen.About.route) }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onNavigateToLookAndFeel = { navController.navigate(Screen.LookAndFeel.route) }
            )
        }

        composable(Screen.LookAndFeel.route) {
            LookAndFeelScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.LinkDetails.route,
            arguments = listOf(navArgument("linkId") { type = NavType.IntType })
        ) { backStackEntry ->
            val linkId = backStackEntry.arguments?.getInt("linkId") ?: return@composable
            LinkDetailsScreen(
                linkId = linkId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}