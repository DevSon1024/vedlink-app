package com.devson.vedlink.ui.presentation.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.devson.vedlink.data.network.scraper.orchestrator.ProviderDetector
import com.devson.vedlink.domain.model.Folder
import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.domain.model.WebsiteProvider
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LinkDetailsScreen(
    link: Link,
    noteTitle: String = link.noteTitle ?: "",
    noteBodyMarkdown: String = link.noteBodyMarkdown ?: link.notes ?: "",
    onNoteTitleChange: (String) -> Unit = {},
    onNoteBodyChange: (String) -> Unit = {},
    onAppendToNote: (String) -> Unit = {},
    folders: List<Folder> = emptyList(),
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onAssignFolder: (Int?) -> Unit = {},
    onAddTag: (String) -> Unit = {},
    onRemoveTag: (String) -> Unit = {},
    onUpdateNotes: (String) -> Unit = {},
    onUrlCardClick: () -> Unit = {},
    onImageClick: () -> Unit = {},
    onImageLongClick: () -> Unit = {},
    onRefreshMetadata: () -> Unit = {},
    onTogglePin: () -> Unit = {},
    onOpenUrl: () -> Unit = {},
    onShareUrl: () -> Unit = {},
    onCopyUrl: () -> Unit = {},
    allTags: List<String> = emptyList()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showFullTitleDialog by remember { mutableStateOf(false) }
    var showFullDescDialog by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var isDescExpanded by remember { mutableStateOf(false) }
    var folderMenuExpanded by remember { mutableStateOf(false) }

    val provider = remember(link.provider, link.url) {
        if (link.provider != WebsiteProvider.GENERIC) {
            link.provider
        } else {
            ProviderDetector().detectProvider(link.url)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding())
    ) {
        // --- Main Scrollable Note Canvas ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = paddingValues.calculateBottomPadding() + 90.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // ==========================================
            // 1. COLLAPSIBLE PRIME LINK CARD (24.dp)
            // ==========================================
            val primeCardShape = RoundedCornerShape(24.dp)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(primeCardShape)
                    .animateContentSize(),
                shape = primeCardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Hero Image Banner
                    if (!link.imageUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                                .combinedClickable(
                                    onClick = onImageClick,
                                    onLongClick = onImageLongClick
                                )
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(link.imageUrl)
                                    .crossfade(true)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .build(),
                                contentDescription = link.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Provider badge on top-right of image
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                shadowElevation = 2.dp
                            ) {
                                Text(
                                    text = provider.name.replace("_", " "),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Card Body Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header row: Favicon + Domain badge + Reading time + Date
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                val faviconUrl = remember(link.faviconUrl, link.domain) {
                                    link.faviconUrl?.takeIf { it.isNotBlank() }
                                        ?: "https://www.google.com/s2/favicons?sz=64&domain=${link.domain ?: ""}"
                                }
                                Surface(
                                    modifier = Modifier.size(18.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(faviconUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }

                                Text(
                                    text = link.domain ?: "Unknown domain",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // Reading time pill if available
                                if (link.readingTimeMinutes != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                                    ) {
                                        Text(
                                            text = "${link.readingTimeMinutes} min read",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                            }

                            Text(
                                text = formatShortDate(link.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        // Scraped Title
                        Text(
                            text = link.title ?: link.url,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { showFullTitleDialog = true },
                                    onLongClick = {
                                        copyToClipboard(clipboardManager, context, link.title ?: link.url, "Title")
                                    }
                                )
                        )

                        // Collapsible Scraped Description
                        if (!link.description.isNullOrBlank()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isDescExpanded = !isDescExpanded }
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = link.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        lineHeight = 18.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                    maxLines = if (isDescExpanded) 20 else 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(
                                        text = if (isDescExpanded) "Show less" else "Show more",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        imageVector = if (isDescExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )

                        // ------------------------------------------
                        // Provider-Aware Action Buttons
                        // ------------------------------------------
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when {
                                // YouTube / Video Provider
                                provider == WebsiteProvider.YOUTUBE || provider.isVideo -> {
                                    FilledTonalButton(
                                        onClick = onOpenUrl,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Watch Video", style = MaterialTheme.typography.labelMedium)
                                    }

                                    OutlinedButton(
                                        onClick = { onAppendToNote("\n- ⏱️ [00:00] ") },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Timestamp", style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                // GitHub / Code Provider
                                provider == WebsiteProvider.GITHUB || provider.isCode -> {
                                    FilledTonalButton(
                                        onClick = onOpenUrl,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("View Repo", style = MaterialTheme.typography.labelMedium)
                                    }

                                    OutlinedButton(
                                        onClick = { onAppendToNote("\n```kotlin\n\n```\n") },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Code,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Code Block", style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                // Article / Blog Provider
                                provider.isArticle || link.readingTimeMinutes != null -> {
                                    FilledTonalButton(
                                        onClick = onOpenUrl,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Read Article", style = MaterialTheme.typography.labelMedium)
                                    }

                                    if (!link.description.isNullOrBlank()) {
                                        OutlinedButton(
                                            onClick = { onAppendToNote("\n> ${link.description.trim()}\n\n") },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FormatQuote,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("+ Quote Summary", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }

                                // Default / Other Provider
                                else -> {
                                    FilledTonalButton(
                                        onClick = onOpenUrl,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open Link", style = MaterialTheme.typography.labelMedium)
                                    }

                                    OutlinedButton(
                                        onClick = onRefreshMetadata,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Refresh", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }

                            // Universal Quick Action Chips
                            OutlinedButton(
                                onClick = onCopyUrl,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy", style = MaterialTheme.typography.labelMedium)
                            }

                            OutlinedButton(
                                onClick = onShareUrl,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // 2. FOLDER & TAGS SELECTOR ROW
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Folder Selector Chip
                val currentFolder = folders.find { it.id == link.folderId }
                val folderTitle = currentFolder?.name ?: "No Folder"
                Box {
                    AssistChip(
                        onClick = { folderMenuExpanded = true },
                        label = { Text(folderTitle) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    DropdownMenu(
                        expanded = folderMenuExpanded,
                        onDismissRequest = { folderMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (Remove Folder)") },
                            onClick = {
                                onAssignFolder(null)
                                folderMenuExpanded = false
                            }
                        )
                        folders.forEach { f ->
                            DropdownMenuItem(
                                text = { Text(f.name) },
                                onClick = {
                                    onAssignFolder(f.id)
                                    folderMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Tags Chips
                link.tags.forEach { tag ->
                    InputChip(
                        selected = true,
                        onClick = {},
                        label = { Text("#$tag") },
                        trailingIcon = {
                            IconButton(
                                onClick = { onRemoveTag(tag) },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove tag",
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Add Tag Chip
                AssistChip(
                    onClick = { showAddTagDialog = true },
                    label = { Text("Tag") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // 3. EXPRESSIVE NOTE TITLE & MARKDOWN CANVAS
            // ==========================================
            // Note Title
            TextField(
                value = noteTitle,
                onValueChange = onNoteTitleChange,
                placeholder = {
                    Text(
                        text = "Note Title...",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Note Body Markdown
            TextField(
                value = noteBodyMarkdown,
                onValueChange = onNoteBodyChange,
                placeholder = {
                    Text(
                        text = "Write your thoughts, markdown notes, code snippets, or checklists here...",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 260.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Subtle Metadata Footer
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Updated: ${formatFullDate(link.updatedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Created: ${formatShortDate(link.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // ==========================================
        // 4. FLOATING FORMATTING MINI-TOOLBAR
        // ==========================================
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = paddingValues.calculateBottomPadding() + 8.dp)
                .imePadding()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checklist
                IconButton(
                    onClick = { onAppendToNote("\n- [ ] ") },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckBox,
                        contentDescription = "Checklist item",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Bullet List
                IconButton(
                    onClick = { onAppendToNote("\n- ") },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                        contentDescription = "Bullet list",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Bold
                IconButton(
                    onClick = { onAppendToNote(" **bold** ") },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatBold,
                        contentDescription = "Bold",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Quote
                IconButton(
                    onClick = { onAppendToNote("\n> ") },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = "Quote",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Heading
                IconButton(
                    onClick = { onAppendToNote("\n### ") },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Title,
                        contentDescription = "Heading",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Code block
                IconButton(
                    onClick = { onAppendToNote("\n```kotlin\n\n```\n") },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Code block",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Timestamp
                IconButton(
                    onClick = { onAppendToNote("\n- ⏱️ [00:00] ") },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Timestamp",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Add Tag
                IconButton(
                    onClick = { showAddTagDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = "Add tag",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // --- Dialogs ---
    var newTagName by remember { mutableStateOf("") }
    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddTagDialog = false
                newTagName = ""
            },
            title = { Text("Add Tag") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        label = { Text("Tag Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val filteredTags = remember(newTagName, allTags) {
                        if (newTagName.isBlank()) emptyList()
                        else allTags.filter { it.contains(newTagName, ignoreCase = true) }
                    }

                    if (filteredTags.isNotEmpty()) {
                        Text(
                            text = "Recommendations",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            filteredTags.take(5).forEach { tag ->
                                SuggestionChip(
                                    onClick = {
                                        onAddTag(tag)
                                        showAddTagDialog = false
                                        newTagName = ""
                                    },
                                    label = { Text(tag) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTagName.isNotBlank()) {
                            onAddTag(newTagName.trim())
                            showAddTagDialog = false
                            newTagName = ""
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddTagDialog = false
                    newTagName = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showFullTitleDialog && !link.title.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showFullTitleDialog = false },
            title = {
                Text(
                    text = "Full Title",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = link.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        copyToClipboard(clipboardManager, context, link.title, "Title")
                        showFullTitleDialog = false
                    }
                ) {
                    Text("Copy")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFullTitleDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showFullDescDialog && !link.description.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showFullDescDialog = false },
            title = {
                Text(
                    text = "Full Description",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = link.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        copyToClipboard(clipboardManager, context, link.description, "Description")
                        showFullDescDialog = false
                    }
                ) {
                    Text("Copy")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFullDescDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

private fun formatFullDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatShortDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun copyToClipboard(
    clipboardManager: ClipboardManager,
    context: Context,
    text: String,
    label: String
) {
    clipboardManager.setText(AnnotatedString(text))
    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
}