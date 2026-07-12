package com.devson.vedlink.ui.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
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
    onAddTopicClick: () -> Unit,
    onNewFolderClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    // Rotate main FAB icon
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rotation"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mini FABs (Shown when expanded)
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(200)) + slideInVertically(
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                initialOffsetY = { it / 2 }
            ),
            exit = fadeOut(tween(150)) + slideOutVertically(
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                targetOffsetY = { it / 2 }
            )
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // "New Folder" option (Label + Mini FAB)
                if (onNewFolderClick != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.shadow(2.dp, RoundedCornerShape(8.dp)),
                            tonalElevation = 2.dp
                        ) {
                            Text(
                                text = "New Folder",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .clickable {
                                        isExpanded = false
                                        onNewFolderClick()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        SmallFloatingActionButton(
                            onClick = {
                                isExpanded = false
                                onNewFolderClick()
                            },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = "New Folder",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // "Add Topic" option (Label + Mini FAB)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.shadow(2.dp, RoundedCornerShape(8.dp)),
                        tonalElevation = 2.dp
                    ) {
                        Text(
                            text = "Add Topic",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clickable {
                                    isExpanded = false
                                    onAddTopicClick()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            isExpanded = false
                            onAddTopicClick()
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lightbulb,
                            contentDescription = "Add Search Topic",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // "Add Link" option (Label + Mini FAB)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.shadow(2.dp, RoundedCornerShape(8.dp)),
                        tonalElevation = 2.dp
                    ) {
                        Text(
                            text = "Add Link",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clickable {
                                    isExpanded = false
                                    onAddLinkClick()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            isExpanded = false
                            onAddLinkClick()
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Link,
                            contentDescription = "Add Link",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Main FAB
        FloatingActionButton(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier
                .size(64.dp)
                .shadow(12.dp, CircleShape),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Expand Options",
                modifier = Modifier
                    .size(32.dp)
                    .rotate(rotation)
            )
        }
    }
}

