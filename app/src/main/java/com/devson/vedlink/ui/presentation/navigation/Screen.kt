package com.devson.vedlink.ui.presentation.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Favorites : Screen("favorites")
    object Folders : Screen("folders")
    object Settings : Screen("settings")
    object About : Screen("about")
    object LookAndFeel : Screen("look_and_feel")
    object LinkDetails : Screen("link_details/{linkId}") {
        fun createRoute(linkId: Int) = "link_details/$linkId"
    }
}