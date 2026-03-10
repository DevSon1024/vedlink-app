package com.devson.vedlink.ui.presentation.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object SavedLinks : Screen("saved_links")
    object Folders : Screen("folders")
    object Favorites : Screen("favorites")
    object Settings : Screen("settings")
    object About : Screen("about")
    object LookAndFeel : Screen("look_and_feel")
    object CustomizeHome : Screen("customize_home")

    // Legacy single-link details route kept for back-compat from HomeScreen / FavoritesScreen
    object LinkDetails : Screen("link_details/{linkId}") {
        fun createRoute(linkId: Int) = "link_details/$linkId"
    }

    // New swipeable pager route — carries the tapped ID and the full ordered list
    object LinkDetailsPager : Screen("link_details_pager/{initialLinkId}/{linkIds}") {
        fun createRoute(initialLinkId: Int, linkIds: List<Int>): String {
            val encoded = linkIds.joinToString(",")
            return "link_details_pager/$initialLinkId/$encoded"
        }
    }
}