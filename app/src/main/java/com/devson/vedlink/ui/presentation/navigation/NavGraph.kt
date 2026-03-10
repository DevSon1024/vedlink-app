package com.devson.vedlink.ui.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.devson.vedlink.ui.presentation.screens.about.AboutScreen
import com.devson.vedlink.ui.presentation.screens.details.LinkDetailsScreen
import com.devson.vedlink.ui.presentation.screens.details.LinkDetailsPagerScreen
import com.devson.vedlink.ui.presentation.screens.favorites.FavoritesScreen
import com.devson.vedlink.ui.presentation.screens.folders.FoldersScreen
import com.devson.vedlink.ui.presentation.screens.home.HomeScreen
import com.devson.vedlink.ui.presentation.screens.savedlinks.SavedLinksScreen
import com.devson.vedlink.ui.presentation.screens.settings.SettingsScreen
import com.devson.vedlink.ui.presentation.screens.lookandfeel.LookAndFeelScreen
import com.devson.vedlink.ui.presentation.screens.customizehome.CustomizeHomeScreen

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
                onNavigateToSavedLinks = {
                    navController.navigate(Screen.SavedLinks.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToFavorites = {
                    navController.navigate(Screen.Favorites.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToFolders = {
                    navController.navigate(Screen.Folders.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToDetails = onNavigateToDetails
            )
        }

        composable(Screen.SavedLinks.route) {
            SavedLinksScreen(
                onNavigateToDetails = { linkId, linkIds ->
                    navController.navigate(Screen.LinkDetailsPager.createRoute(linkId, linkIds))
                }
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(onNavigateToDetails = onNavigateToDetails)
        }

        composable(Screen.Folders.route) {
            FoldersScreen(onNavigateToDetails = onNavigateToDetails)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onNavigateToLookAndFeel = { navController.navigate(Screen.LookAndFeel.route) },
                onNavigateToCustomizeHome = { navController.navigate(Screen.CustomizeHome.route) }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LookAndFeel.route) {
            LookAndFeelScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CustomizeHome.route) {
            CustomizeHomeScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Legacy single-link details (used by FavoritesScreen / FoldersScreen)
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

        // Swipeable pager details (used by SavedLinksScreen / HomeScreen)
        composable(
            route = Screen.LinkDetailsPager.route,
            arguments = listOf(
                navArgument("initialLinkId") { type = NavType.IntType },
                navArgument("linkIds") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val initialLinkId = backStackEntry.arguments?.getInt("initialLinkId")
                ?: return@composable
            val linkIdsRaw = backStackEntry.arguments?.getString("linkIds") ?: return@composable
            val linkIds = linkIdsRaw
                .split(",")
                .mapNotNull { it.trim().toIntOrNull() }
            if (linkIds.isEmpty()) return@composable

            LinkDetailsPagerScreen(
                initialLinkId = initialLinkId,
                linkIds = linkIds,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}