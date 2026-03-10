package com.devson.vedlink.ui.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
 * A compact, image-first card for dense 3–6 column grid layouts.
 *
 * Performance notes:
 * - Image is downsampled to 200×200 px via [coil.size.Precision.INEXACT] to keep memory low.
 * - Aggressive disk + memory cache policies ensure thumbnails are reused across scrolls.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MicroLinkCard(
    link: Link,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
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
            // --- Background: image or placeholder ---
            if (!link.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(link.imageUrl)
                        // Downsample aggressively for dense grids
                        .size(200, 200)
                        .scale(Scale.FILL)
                        .crossfade(true)
                        // Strict caching – avoid re-downloads on scroll
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
                // Placeholder with domain initial
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            // --- Bottom gradient overlay ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.55f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                        )
                    )
            )

            // --- Title text at bottom ---
            Text(
                text = (link.title ?: "Untitled").take(20),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 6.dp, vertical = 5.dp)
            )

            // --- Selection checkbox overlay ---
            if (isSelectionMode) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
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
            } else if (link.isFavorite) {
                // --- Favourite badge ---
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFF4081).copy(alpha = 0.92f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Favourite",
                        modifier = Modifier
                            .padding(3.dp)
                            .size(10.dp),
                        tint = Color.White
                    )
                }
            }

            // --- Selection dim overlay ---
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
                )
            }
        }
    }
}

/** Shimmer placeholder for a MicroLinkCard. */
@Composable
fun MicroShimmerLinkCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shimmerEffect()
        )
    }
}
