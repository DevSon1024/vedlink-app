package com.devson.vedlink.ui.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.devson.vedlink.ui.presentation.navigation.Screen

@Composable
fun ModernBoxedBottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(30.dp),
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
                .clip(RoundedCornerShape(30.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModernBoxedNavItem(
                icon = Icons.Outlined.Home,
                selectedIcon = Icons.Filled.Home,
                label = "Home",
                isSelected = currentRoute == Screen.Home.route,
                onClick = { onNavigate(Screen.Home.route) }
            )

            ModernBoxedNavItem(
                icon = Icons.Outlined.FavoriteBorder,
                selectedIcon = Icons.Filled.Favorite,
                label = "Favourite",
                isSelected = currentRoute == Screen.Favorites.route,
                onClick = { onNavigate(Screen.Favorites.route) }
            )

            ModernBoxedNavItem(
                icon = Icons.Outlined.Folder,
                selectedIcon = Icons.Filled.Folder,
                label = "Folders",
                isSelected = currentRoute == Screen.Folders.route,
                onClick = { onNavigate(Screen.Folders.route) }
            )

            ModernBoxedNavItem(
                icon = Icons.Outlined.Settings,
                selectedIcon = Icons.Filled.Settings,
                label = "Settings",
                isSelected = currentRoute == Screen.Settings.route,
                onClick = { onNavigate(Screen.Settings.route) }
            )
        }
    }
}

@Composable
private fun ModernBoxedNavItem(
    icon: ImageVector,
    selectedIcon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {

    val containerColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            Color.Transparent,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "container"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.onPrimary
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 300),
        label = "content"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .height(50.dp),
        shape = RoundedCornerShape(20.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSelected) selectedIcon else icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = contentColor
            )

            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(tween(200)) + expandHorizontally(),
                exit = fadeOut(tween(200)) + shrinkHorizontally()
            ) {
                Row {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor
                    )
                }
            }
        }
    }
}