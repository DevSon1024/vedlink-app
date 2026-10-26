package com.devson.vedlink.ui.presentation.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.devson.vedlink.domain.model.Folder
import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.domain.model.ScrapedMetadata
import com.devson.vedlink.domain.util.LinkExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EnhancedAddLinkBottomSheet(
    recentLinks: List<Link>,
    onDismiss: () -> Unit,
    onConfirm: (String, ScrapedMetadata?) -> Unit,
    onAutoPaste: () -> Unit = {},
    onFetchPreview: suspend (String) -> ScrapedMetadata?,
    urlInputValue: String = "",
    onUrlChange: (String) -> Unit = {},
    folders: List<Folder> = emptyList(),
    onSaveLinkNote: ((
        url: String,
        noteTitle: String?,
        noteBody: String?,
        isPinned: Boolean,
        folderId: Int?,
        tags: List<String>,
        metadata: ScrapedMetadata?
    ) -> Unit)? = null
) {
    var noteTitle by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf(urlInputValue) }
    var localUrl by remember { mutableStateOf(urlInputValue) }

    val updateUrl: (String) -> Unit = {
        localUrl = it
        onUrlChange(it)
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isValidUrl by remember { mutableStateOf(true) }
    var isPinned by remember { mutableStateOf(false) }
    var selectedFolderId by remember { mutableStateOf<Int?>(null) }
    var folderMenuExpanded by remember { mutableStateOf(false) }
    var selectedTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }
    var showRecentLinks by remember { mutableStateOf(false) }

    // Clipboard auto-detection state
    var clipboardUrl by remember { mutableStateOf<String?>(null) }
    var clipboardDomain by remember { mutableStateOf<String?>(null) }

    // Detected URL & Domain from note text or URL input
    var detectedUrl by remember { mutableStateOf<String?>(null) }
    var detectedDomain by remember { mutableStateOf<String?>(null) }
    val linkExtractor = remember { LinkExtractor() }

    // Setup state and scope for animation
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Internal Preview State
    var previewMetadata by remember { mutableStateOf<ScrapedMetadata?>(null) }
    var isPreviewLoading by remember { mutableStateOf(false) }

    // Check clipboard safely on mount
    LaunchedEffect(Unit) {
        try {
            val clipText = clipboardManager.getText()?.text
            if (!clipText.isNullOrBlank()) {
                val extracted = linkExtractor.extractUrls(clipText).firstOrNull()
                if (extracted != null && extracted != urlInputValue) {
                    clipboardUrl = extracted
                    clipboardDomain = linkExtractor.extractDomain(extracted)
                }
            }
        } catch (e: Exception) {
            // Ignore clipboard errors safely
        }
    }

    // 350ms debounced live URL detection from noteText and localUrl
    LaunchedEffect(noteText, localUrl) {
        delay(350)
        val textToScan = when {
            noteText.isNotBlank() && localUrl.isNotBlank() && !noteText.contains(localUrl) -> "$localUrl\n$noteText"
            noteText.isNotBlank() -> noteText
            else -> localUrl
        }
        val extracted = linkExtractor.extractUrls(textToScan).firstOrNull()
        if (extracted != null) {
            detectedUrl = extracted
            detectedDomain = linkExtractor.extractDomain(extracted)
            isValidUrl = true
        } else {
            detectedUrl = null
            detectedDomain = null
        }
    }

    // Fetch preview when detectedUrl is resolved
    LaunchedEffect(detectedUrl) {
        val target = detectedUrl
        if (!target.isNullOrBlank() && isValidUrl(target)) {
            isPreviewLoading = true
            previewMetadata = null
            previewMetadata = withContext(Dispatchers.IO) {
                onFetchPreview(target)
            }
            isPreviewLoading = false
        } else {
            previewMetadata = null
            isPreviewLoading = false
        }
    }

    // Animation state for FAB (Plus -> Cross)
    var isLaunched by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isLaunched = true
    }

    val rotation by animateFloatAsState(
        targetValue = if (isLaunched) 45f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 100)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color.Transparent,
        dragHandle = null
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Main Content Surface
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp)
                ) {
                    CustomDragHandle()

                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Quick Link-Note",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Capture thoughts & save links in real time",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Top Paste Button
                        FloatingActionButton(
                            onClick = {
                                val clipText = clipboardManager.getText()?.text
                                if (!clipText.isNullOrBlank()) {
                                    val extracted = linkExtractor.extractUrls(clipText).firstOrNull() ?: clipText
                                    val newText = if (noteText.isBlank()) {
                                        extracted
                                    } else {
                                        "$noteText\n$extracted"
                                    }
                                    noteText = newText
                                    updateUrl(extracted)
                                    onAutoPaste()
                                }
                            },
                            modifier = Modifier.size(44.dp),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Auto Paste",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // One-Tap Clipboard Chip (Animated)
                    AnimatedVisibility(
                        visible = clipboardUrl != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        clipboardUrl?.let { clipUrl ->
                            AssistChip(
                                onClick = {
                                    val newText = if (noteText.isBlank()) {
                                        clipUrl
                                    } else if (noteText.contains(clipUrl)) {
                                        noteText
                                    } else {
                                        "$noteText\n$clipUrl"
                                    }
                                    noteText = newText
                                    updateUrl(clipUrl)
                                    clipboardUrl = null
                                    onAutoPaste()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                label = {
                                    Text(
                                        text = "Paste link from clipboard: ${clipboardDomain ?: "link"}",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                        }
                    }

                    // Note Composer Surface (Title + Multi-line Input)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            // 1. Note Title Field (Optional, bold titleLarge borderless)
                            TextField(
                                value = noteTitle,
                                onValueChange = { noteTitle = it },
                                placeholder = {
                                    Text(
                                        text = "Note title (optional)...",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    )
                                },
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                thickness = 0.8.dp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            // 2. Note & Link Input Field (Multi-line borderless)
                            TextField(
                                value = noteText,
                                onValueChange = {
                                    noteText = it
                                    updateUrl(it)
                                    isValidUrl = true
                                },
                                placeholder = {
                                    Text(
                                        text = "Paste a link or jot down thoughts with a URL...",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    )
                                },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                minLines = 3,
                                maxLines = 6,
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        }
                    }

                    // Validation Error Text
                    AnimatedVisibility(visible = !isValidUrl) {
                        Text(
                            text = "Please include a valid URL in your note or link input",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 6.dp, top = 4.dp)
                        )
                    }

                    // Live Detected Link Pill (Animated)
                    AnimatedVisibility(
                        visible = detectedUrl != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        detectedUrl?.let { url ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(32.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "URL Detected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = detectedDomain ?: "Detected URL",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    text = "Normalized",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = url,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Pin toggle button
                                    FilterChip(
                                        selected = isPinned,
                                        onClick = { isPinned = !isPinned },
                                        label = {
                                            Text(
                                                text = if (isPinned) "Pinned" else "Pin",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                                contentDescription = "Pin Note",
                                                modifier = Modifier.size(14.dp)
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Folder & Tag Selector Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Folder Dropdown Button
                        Box {
                            val selectedFolderName = folders.find { it.id == selectedFolderId }?.name ?: "No Folder"
                            AssistChip(
                                onClick = { folderMenuExpanded = true },
                                label = {
                                    Text(
                                        text = selectedFolderName,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (selectedFolderId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (selectedFolderId != null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            )

                            DropdownMenu(
                                expanded = folderMenuExpanded,
                                onDismissRequest = { folderMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("None (No Folder)") },
                                    onClick = {
                                        selectedFolderId = null
                                        folderMenuExpanded = false
                                    }
                                )
                                folders.forEach { folder ->
                                    DropdownMenuItem(
                                        text = { Text(folder.name) },
                                        onClick = {
                                            selectedFolderId = folder.id
                                            folderMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Add Tag Button
                        AssistChip(
                            onClick = { showAddTagDialog = true },
                            label = { Text("Add Tag") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        )
                    }

                    // Display Selected Tags (if any)
                    if (selectedTags.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            selectedTags.forEach { tag ->
                                InputChip(
                                    selected = true,
                                    onClick = {},
                                    label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { selectedTags = selectedTags - tag },
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove tag",
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Preview Area (Skeleton or Card)
                    AnimatedVisibility(
                        visible = isPreviewLoading,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        LinkPreviewSkeleton(modifier = Modifier.padding(bottom = 12.dp))
                    }

                    val currentMetadata = previewMetadata
                    AnimatedVisibility(
                        visible = !isPreviewLoading && currentMetadata != null && (!currentMetadata.title.isNullOrBlank() || !currentMetadata.imageUrl.isNullOrBlank()),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        currentMetadata?.let {
                            LinkPreviewCard(metadata = it, modifier = Modifier.padding(bottom = 12.dp))
                        }
                    }

                    // Save Link & Note Button
                    Button(
                        onClick = {
                            val candidateUrl = detectedUrl
                                ?: (if (isValidUrl(noteText.trim())) noteText.trim() else null)
                                ?: (if (isValidUrl(localUrl.trim())) localUrl.trim() else null)

                            if (candidateUrl != null && isValidUrl(candidateUrl)) {
                                val titleToSave = noteTitle.trim().takeIf { it.isNotBlank() }
                                val bodyToSave = noteText.trim().takeIf { it.isNotBlank() }
                                if (onSaveLinkNote != null) {
                                    onSaveLinkNote(
                                        candidateUrl,
                                        titleToSave,
                                        bodyToSave,
                                        isPinned,
                                        selectedFolderId,
                                        selectedTags,
                                        previewMetadata
                                    )
                                } else {
                                    onConfirm(candidateUrl, previewMetadata)
                                }
                            } else {
                                isValidUrl = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = noteText.isNotBlank() || localUrl.isNotBlank() || detectedUrl != null,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdded,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (noteTitle.isNotBlank() || noteText.lines().size > 1) "Save Link & Note" else "Save Link",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Recent Links
                    if (recentLinks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent ${recentLinks.size} Links",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            TextButton(
                                onClick = { showRecentLinks = !showRecentLinks }
                            ) {
                                Text(if (showRecentLinks) "Hide" else "Show")
                            }
                        }

                        AnimatedVisibility(visible = showRecentLinks) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(recentLinks.take(10)) { link ->
                                    RecentLinkItem(link = link)
                                }
                            }
                        }
                    }
                }
            }

            // Close FAB - Animates from Add (Plus) to Close (X)
            FloatingActionButton(
                onClick = {
                    previewMetadata = null
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            onDismiss()
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(64.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                ),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Close",
                    modifier = Modifier
                        .size(32.dp)
                        .rotate(rotation)
                )
            }
        }
    }

    // Add Tag Dialog
    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddTagDialog = false
                newTagName = ""
            },
            title = { Text("Add Tag") },
            text = {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    label = { Text("Tag name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = newTagName.trim()
                        if (trimmed.isNotBlank() && !selectedTags.contains(trimmed)) {
                            selectedTags = selectedTags + trimmed
                        }
                        newTagName = ""
                        showAddTagDialog = false
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddTagDialog = false
                        newTagName = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CustomDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        )
    }
}

@Composable
private fun RecentLinkItem(link: Link) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = link.title ?: "No Title",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = link.domain ?: "Unknown",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatTimeAgo(link.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "now"
        diff < 3_600_000 -> "${diff / 60_000}m"
        diff < 86_400_000 -> "${diff / 3_600_000}h"
        else -> "${diff / 86_400_000}d"
    }
}

private fun isValidUrl(url: String): Boolean {
    if (url.isBlank()) return false
    return try {
        val patternMatches = try {
            android.util.Patterns.WEB_URL.matcher(url).matches()
        } catch (e: Exception) {
            url.matches(Regex("""https?://[a-zA-Z0-9.\-]+(?::\d+)?(?:/[^\s]*)?"""))
        }
        patternMatches || url.startsWith("http://") || url.startsWith("https://")
    } catch (e: Exception) {
        false
    }
}

@Composable
fun LinkPreviewCard(
    metadata: ScrapedMetadata,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            if (!metadata.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = metadata.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .width(100.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!metadata.faviconUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = metadata.faviconUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = metadata.websiteName ?: extractDomain(metadata.imageUrl ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = metadata.title ?: "No Title",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = metadata.description ?: "No description available",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun extractDomain(url: String): String {
    return try {
        val uri = java.net.URI(url)
        uri.host?.removePrefix("www.") ?: url
    } catch (e: Exception) {
        url
    }
}

@Composable
fun LinkPreviewSkeleton(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        start = Offset(shimmerTranslate - 200f, shimmerTranslate - 200f),
        end = Offset(shimmerTranslate, shimmerTranslate)
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight()
                    .background(shimmerBrush)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(14.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(10.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(shimmerBrush)
                )
            }
        }
    }
}