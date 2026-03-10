package com.devson.vedlink.ui.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.devson.vedlink.domain.model.Link

/**
 * A dense, full-bleed portrait card for 2-column grid layouts.
 * It features a background image (or placeholder) with a dark bottom
 * gradient for text readability, plus a 3-dot context menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactLinkCard(
    link: Link,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onFavoriteClick: () -> Unit,
    onCopyClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onRefreshClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                }
            )
            .then(
                if (isSelected) Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                ) else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // ── Background: image or placeholder ──
            if (!link.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(link.imageUrl)
                        .size(400, 400)
                        .scale(Scale.FILL)
                        .crossfade(true)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .networkCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = link.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = if (isSelectionMode && !isSelected) 0.6f else 1f
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    )
                }
            }

            // ── Bottom gradient overlay ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                        )
                    )
            )

            // ── Top-Left: Favourite badge ──
            if (!isSelectionMode && link.isFavorite) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFF4081).copy(alpha = 0.92f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Favourite",
                        modifier = Modifier.padding(4.dp).size(10.dp),
                        tint = Color.White
                    )
                }
            }

            // ── Top-Right: Selection checkbox ──
            if (isSelectionMode) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ) {
                    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                            contentDescription = if (isSelected) "Selected" else "Not selected",
                            modifier = Modifier.size(18.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // ── Selection dim overlay ──
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
                )
            }

            // ── Bottom Left: Title & Domain text ──
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(end = 36.dp) // Leave space for the 3-dot menu
                    .padding(start = 8.dp, bottom = 8.dp, top = 8.dp)
            ) {
                Text(
                    text = link.title ?: "Untitled Link",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 16.sp
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = Color.White.copy(alpha = 0.75f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = link.domain ?: "Unknown",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.White.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // ── Bottom Right: Action Menu ──
            if (!isSelectionMode) {
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                    }

                    LinkOptionsMenu(
                        expanded = showMenu,
                        onDismiss = { showMenu = false },
                        isFavorite = link.isFavorite,
                        onFavoriteClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onFavoriteClick()
                            showMenu = false
                        },
                        onCopyClick = { onCopyClick(); showMenu = false },
                        onShareClick = { onShareClick(); showMenu = false },
                        onRefreshClick = { onRefreshClick(); showMenu = false },
                        onDeleteClick = { onDeleteClick(); showMenu = false }
                    )
                }
            }
        }
    }
}