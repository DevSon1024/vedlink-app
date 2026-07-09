package com.devson.vedlink.ui.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class LinkViewSettings(
    val showFavicon: Boolean = true,
    val showUrl: Boolean = true,
    val showTags: Boolean = true,
    val showDateSaved: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkViewSettingsBottomSheet(
    layoutMode: String,
    onLayoutModeChange: (String) -> Unit,
    gridColumns: Int,
    onGridColumnsChange: (Int) -> Unit,
    viewSettings: LinkViewSettings,
    onViewSettingsChange: (LinkViewSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Sheet Title
            Text(
                text = "View Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // Layout Mode Section
            LinkSettingsSectionLabel("Layout")
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LinkIconToggleButton(
                    label = "List",
                    selected = layoutMode.equals("list", ignoreCase = true),
                    selectedIcon = Icons.Filled.ViewAgenda,
                    unselectedIcon = Icons.Outlined.ViewAgenda,
                    modifier = Modifier.weight(1f),
                    onClick = { onLayoutModeChange("list") }
                )
                LinkIconToggleButton(
                    label = "Grid",
                    selected = layoutMode.equals("grid", ignoreCase = true),
                    selectedIcon = Icons.Filled.GridView,
                    unselectedIcon = Icons.Outlined.GridView,
                    modifier = Modifier.weight(1f),
                    onClick = { onLayoutModeChange("grid") }
                )
            }

            // Animated Grid Columns Slider
            AnimatedVisibility(
                visible = layoutMode.equals("grid", ignoreCase = true),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinkSettingsSectionLabel("Grid Columns")
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "$gridColumns Columns",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = gridColumns.toFloat(),
                        onValueChange = { onGridColumnsChange(it.toInt()) },
                        valueRange = 2f..4f,
                        steps = 1,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("2 Cols", "3 Cols", "4 Cols").forEachIndexed { index, label ->
                            val columnsVal = index + 2
                            val isSelected = gridColumns == columnsVal
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            // Metadata Fields Section
            LinkSettingsSectionLabel("Link Metadata Fields")
            Spacer(modifier = Modifier.height(8.dp))

            val fieldItems = listOf(
                Triple("Favicon", viewSettings.showFavicon) { checked: Boolean ->
                    onViewSettingsChange(viewSettings.copy(showFavicon = checked))
                },
                Triple("URL", viewSettings.showUrl) { checked: Boolean ->
                    onViewSettingsChange(viewSettings.copy(showUrl = checked))
                },
                Triple("Tags", viewSettings.showTags) { checked: Boolean ->
                    onViewSettingsChange(viewSettings.copy(showTags = checked))
                },
                Triple("Date Saved", viewSettings.showDateSaved) { checked: Boolean ->
                    onViewSettingsChange(viewSettings.copy(showDateSaved = checked))
                }
            )

            // Chunked by 2 to display 2 rows of 2 columns
            val chunked = fieldItems.chunked(2)
            Column(modifier = Modifier.fillMaxWidth()) {
                chunked.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rowItems.forEach { (label, checked, onChange) ->
                            Box(Modifier.weight(1f)) {
                                LinkCompactMetadataToggle(
                                    label = label,
                                    checked = checked,
                                    onCheckedChange = onChange
                                )
                            }
                        }
                        // Fill empty slots if any (won't happen with 4 items chunked by 2)
                        repeat(2 - rowItems.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkSettingsSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun LinkIconToggleButton(
    label: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon = if (selected) selectedIcon else unselectedIcon
    val fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = fontWeight,
            color = contentColor,
            maxLines = 1
        )
    }
}

@Composable
private fun LinkCompactMetadataToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
