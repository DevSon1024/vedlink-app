package com.devson.vedlink.ui.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devson.vedlink.ui.presentation.navigation.Screen

data class ModernNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

@Composable
fun ModernBoxedBottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    ModernBoxedBottomNavBar(
        currentRoute = currentRoute,
        isNavBarTransparent = false,
        onNavigate = onNavigate
    )
}

@Composable
fun ModernBoxedBottomNavBar(
    currentRoute: String?,
    isNavBarTransparent: Boolean = false,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember {
        listOf(
            ModernNavItem(Screen.Home.route, "Home", Icons.Outlined.Home, Icons.Filled.Home),
            ModernNavItem(Screen.SavedLinks.route, "Saved", Icons.Outlined.Bookmarks, Icons.Filled.Bookmarks),
            ModernNavItem(Screen.Folders.route, "Folders", Icons.Outlined.Folder, Icons.Filled.Folder),
            ModernNavItem(Screen.Favorites.route, "Favorites", Icons.Outlined.StarBorder, Icons.Filled.Star),
            ModernNavItem(Screen.Settings.route, "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
        )
    }

    val dockShape = RoundedCornerShape(32.dp)
    val backgroundColor = if (isNavBarTransparent) {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 10.dp,
                    shape = dockShape,
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ),
            shape = dockShape,
            color = backgroundColor,
            tonalElevation = 4.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    ModernBoxedNavItem(
                        item = item,
                        isSelected = isSelected,
                        onClick = {
                            if (currentRoute != item.route) {
                                onNavigate(item.route)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernBoxedNavItem(
    item: ModernNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val pillContainerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pillContainerColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "contentColor"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "iconScale"
    )

    val horizontalPadding by animateDpAsState(
        targetValue = if (isSelected) 14.dp else 10.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "horizontalPadding"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(24.dp),
        color = pillContainerColor,
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.icon,
                contentDescription = item.label,
                modifier = Modifier
                    .size(22.dp)
                    .scale(iconScale),
                tint = contentColor
            )

            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(spring(dampingRatio = Spring.DampingRatioLowBouncy)) +
                        expandHorizontally(spring(dampingRatio = Spring.DampingRatioLowBouncy)),
                exit = fadeOut(tween(150)) + shrinkHorizontally(tween(150))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = contentColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}