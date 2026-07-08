package com.devson.vedlink.ui.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.devson.vedlink.ui.presentation.screens.AboutScreen
import com.devson.vedlink.ui.presentation.screens.LinkDetailsScreen
import com.devson.vedlink.ui.presentation.util.LinkDetailsPagerScreen
import com.devson.vedlink.ui.presentation.screens.FavoritesScreen
import com.devson.vedlink.ui.presentation.screens.FoldersScreen
import com.devson.vedlink.ui.presentation.screens.HomeScreen
import com.devson.vedlink.ui.presentation.screens.SavedLinksScreen
import com.devson.vedlink.ui.presentation.screens.SettingsScreen
import com.devson.vedlink.ui.presentation.screens.AppearanceSettingsScreen
import com.devson.vedlink.ui.presentation.screens.CustomizeHomeScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    onNavigateToDetails: (Int) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = {
            val targetRoute = targetState.destination.route
            if (targetRoute != null && (targetRoute.startsWith("link_details") || targetRoute == Screen.About.route || targetRoute == Screen.Appearance.route || targetRoute == Screen.CustomizeHome.route)) {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(350)
                ) + fadeIn(animationSpec = tween(350))
            } else {
                scaleIn(
                    initialScale = 0.96f,
                    animationSpec = tween(250)
                ) + fadeIn(animationSpec = tween(250))
            }
        },
        exitTransition = {
            val targetRoute = targetState.destination.route
            if (targetRoute != null && (targetRoute.startsWith("link_details") || targetRoute == Screen.About.route || targetRoute == Screen.Appearance.route || targetRoute == Screen.CustomizeHome.route)) {
                fadeOut(animationSpec = tween(250))
            } else {
                scaleOut(
                    targetScale = 0.96f,
                    animationSpec = tween(250)
                ) + fadeOut(animationSpec = tween(250))
            }
        },
        popEnterTransition = {
            val initialRoute = initialState.destination.route
            if (initialRoute != null && (initialRoute.startsWith("link_details") || initialRoute == Screen.About.route || initialRoute == Screen.Appearance.route || initialRoute == Screen.CustomizeHome.route)) {
                fadeIn(animationSpec = tween(250))
            } else {
                scaleIn(
                    initialScale = 0.96f,
                    animationSpec = tween(250)
                ) + fadeIn(animationSpec = tween(250))
            }
        },
        popExitTransition = {
            val initialRoute = initialState.destination.route
            if (initialRoute != null && (initialRoute.startsWith("link_details") || initialRoute == Screen.About.route || initialRoute == Screen.Appearance.route || initialRoute == Screen.CustomizeHome.route)) {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(350)
                ) + fadeOut(animationSpec = tween(350))
            } else {
                scaleOut(
                    targetScale = 0.96f,
                    animationSpec = tween(250)
                ) + fadeOut(animationSpec = tween(250))
            }
        }
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
                onNavigateToDetails = onNavigateToDetails,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
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
                onNavigateToAppearance = { navController.navigate(Screen.Appearance.route) },
                onNavigateToCustomizeHome = { navController.navigate(Screen.CustomizeHome.route) }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Appearance.route) {
            AppearanceSettingsScreen(
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