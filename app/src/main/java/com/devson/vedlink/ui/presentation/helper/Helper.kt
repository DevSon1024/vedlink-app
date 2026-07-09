package com.devson.vedlink.ui.presentation.helper

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.devson.vedlink.domain.model.Link
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    selectedCount: Int,
    totalCount: Int = 0,
    allSelected: Boolean = false,
    favoriteStatus: FavoriteStatus = FavoriteStatus.MIXED,
    onClose: () -> Unit,
    onSelectAll: () -> Unit = {},
    onShare: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "$selectedCount selected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit selection mode",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            // Select All / Deselect All
            if (totalCount > 0) {
                IconButton(onClick = onSelectAll) {
                    Icon(
                        imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                        contentDescription = if (allSelected) "Deselect all" else "Select all",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Dynamic Favorite Icon based on status
            if (favoriteStatus != FavoriteStatus.HIDDEN) {
                IconButton(onClick = onFavorite) {
                    Icon(
                        imageVector = when (favoriteStatus) {
                            FavoriteStatus.ALL_FAVORITED -> Icons.Default.Favorite
                            else -> Icons.Default.FavoriteBorder
                        },
                        contentDescription = if (favoriteStatus == FavoriteStatus.ALL_FAVORITED) "Remove from favorites" else "Add to favorites",
                        tint = if (favoriteStatus == FavoriteStatus.ALL_FAVORITED) Color(0xFFFF4081) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share selected",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete selected",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

enum class FavoriteStatus {
    ALL_FAVORITED,      // All selected links are favorited - show HeartBroken icon
    NONE_FAVORITED,     // None of the selected links are favorited - show Favorite icon
    MIXED,              // Some are favorited, some aren't - show Favorite icon
    HIDDEN              // Don't show favorite icon at all
}

@Composable
fun MultiDeleteConfirmationDialog(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Delete $count Link${if (count > 1) "s" else ""}?",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete $count selected link${if (count > 1) "s" else ""}? This action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Delete All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

fun shareMultipleLinks(context: Context, links: List<Link>) {
    val shareText = buildString {
        appendLine("Check out these links:")
        appendLine()
        links.forEach { link ->
            appendLine(link.url)
            appendLine()
        }
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_TITLE, "Shared Links from VedLink")
    }
    context.startActivity(Intent.createChooser(intent, "Share links via"))
}

fun shareLink(context: Context, url: String, title: String?) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url ?: "Check this link")
        putExtra(Intent.EXTRA_TITLE, title ?: "Check out this link")
    }
    context.startActivity(Intent.createChooser(intent, "Share link via"))
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Link", text)
    clipboard.setPrimaryClip(clip)
}

// Helper function to determine favorite status
fun getFavoriteStatus(selectedLinks: List<Link>, hasMixedContext: Boolean = false): FavoriteStatus {
    if (selectedLinks.isEmpty()) return FavoriteStatus.HIDDEN

    val favoritedCount = selectedLinks.count { it.isFavorite }

    return when {
        hasMixedContext && favoritedCount > 0 -> FavoriteStatus.HIDDEN
        favoritedCount == selectedLinks.size -> FavoriteStatus.ALL_FAVORITED
        favoritedCount == 0 -> FavoriteStatus.NONE_FAVORITED
        else -> FavoriteStatus.MIXED
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsDialog(
    tags: List<String>,
    onDismiss: () -> Unit,
    onCopyAll: () -> Unit,
    onCopySingleTag: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Label,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tags Collection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${tags.size} tag(s) associated with this link:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        SuggestionChip(
                            onClick = { onCopySingleTag(tag) },
                            label = { Text(tag) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy tag",
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCopyAll,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CopyAll,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}