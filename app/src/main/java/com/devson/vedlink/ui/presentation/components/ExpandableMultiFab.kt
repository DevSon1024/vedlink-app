package com.devson.vedlink.ui.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.ContentPasteGo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExpandableMultiFab(
    onAddLinkClick: () -> Unit,
    onQuickPasteClick: (() -> Unit)? = null,
    onAddTopicClick: (() -> Unit)? = null,
    onNewFolderClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    // Rotate main FAB icon with spring physics
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fabRotation"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mini Action FABs (Shown when expanded)
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(180)) + slideInVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                initialOffsetY = { it / 2 }
            ),
            exit = fadeOut(tween(140)) + slideOutVertically(
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                targetOffsetY = { it / 2 }
            )
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // "New Folder" option (if supported by active screen)
                if (onNewFolderClick != null) {
                    FabActionItem(
                        label = "New Folder",
                        icon = Icons.Default.CreateNewFolder,
                        onClick = {
                            isExpanded = false
                            onNewFolderClick()
                        }
                    )
                }

                // "Add Topic" option (if provided)
                if (onAddTopicClick != null) {
                    FabActionItem(
                        label = "Add Topic",
                        icon = Icons.Filled.Lightbulb,
                        onClick = {
                            isExpanded = false
                            onAddTopicClick()
                        }
                    )
                }

                // "Quick Paste URL" option (Immediately saves & unfurls clipboard URL)
                if (onQuickPasteClick != null) {
                    FabActionItem(
                        label = "Quick Paste URL",
                        icon = Icons.Default.ContentPasteGo,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = {
                            isExpanded = false
                            onQuickPasteClick()
                        }
                    )
                }

                // "New Link-Note" option (Opens note + link composer)
                FabActionItem(
                    label = "New Link-Note",
                    icon = Icons.AutoMirrored.Filled.NoteAdd,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = {
                        isExpanded = false
                        onAddLinkClick()
                    }
                )
            }
        }

        // Main FAB
        FloatingActionButton(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier
                .size(60.dp)
                .shadow(8.dp, CircleShape),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (isExpanded) "Close options" else "Expand options",
                modifier = Modifier
                    .size(28.dp)
                    .rotate(rotation)
            )
        }
    }
}

@Composable
private fun FabActionItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.shadow(2.dp, RoundedCornerShape(12.dp)),
            tonalElevation = 3.dp
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clickable(onClick = onClick)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = containerColor,
            contentColor = contentColor,
            shape = CircleShape
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
