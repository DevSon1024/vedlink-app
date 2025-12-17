package com.devson.vedlink.presentation.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Favorites : Screen("favorites")
    object Settings : Screen("settings")
    object LinkDetails : Screen("link_details/{linkId}") {
        fun createRoute(linkId: Int) = "link_details/$linkId"
    }
}
