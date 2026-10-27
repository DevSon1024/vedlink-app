package com.devson.vedlink.ui.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

data class VedLinkNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

@Composable
fun VedLinkBottomNavBar(
    currentRoute: String?,
    isNavBarTransparent: Boolean,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ModernBoxedBottomNavBar(
        currentRoute = currentRoute,
        isNavBarTransparent = isNavBarTransparent,
        onNavigate = onNavigate,
        modifier = modifier
    )
}
