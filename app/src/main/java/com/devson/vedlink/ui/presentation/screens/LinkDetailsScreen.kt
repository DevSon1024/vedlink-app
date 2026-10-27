package com.devson.vedlink.ui.presentation.screens

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.devson.vedlink.data.network.scraper.orchestrator.ProviderDetector
import com.devson.vedlink.domain.model.Folder
import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.domain.model.WebsiteProvider
import com.devson.vedlink.ui.viewmodel.SaveState
import java.text.SimpleDateFormat
import java.util.*

private data class NoteColorOption(
    val name: String,
    val colorHex: Int?
)

private val noteColorPalette = listOf(
    NoteColorOption("Default", null),
    NoteColorOption("Coral", 0xFFFF8A80.toInt()),
    NoteColorOption("Peach", 0xFFFFD180.toInt()),
    NoteColorOption("Sand", 0xFFFFFF8D.toInt()),
    NoteColorOption("Mint", 0xFFCCFF90.toInt()),
    NoteColorOption("Sage", 0xFFA7FFEB.toInt()),
    NoteColorOption("Fog", 0xFF80D8FF.toInt()),
    NoteColorOption("Storm", 0xFF82B1FF.toInt()),
    NoteColorOption("Dusk", 0xFFB388FF.toInt()),
    NoteColorOption("Blossom", 0xFFFF80AB.toInt()),
    NoteColorOption("Clay", 0xFFEA80FC.toInt()),
    NoteColorOption("Chalk", 0xFFCFD8DC.toInt())
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LinkDetailsScreen(
    link: Link,
    noteTitle: String = link.noteTitle ?: "",
    noteBodyMarkdown: String = link.noteBodyMarkdown ?: link.notes ?: "",
    saveState: SaveState = SaveState.Saved,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    folders: List<Folder> = emptyList(),
    allTags: List<String> = emptyList(),
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onNavigateBack: () -> Unit = {},
    onNoteTitleChange: (String) -> Unit = {},
    onNoteBodyChange: (String) -> Unit = {},
    onAppendToNote: (String) -> Unit = {},
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    onAssignFolder: (Int?) -> Unit = {},
    onAddTag: (String) -> Unit = {},
    onRemoveTag: (String) -> Unit = {},
    onTogglePin: () -> Unit = {},
    onToggleArchive: () -> Unit = {},
    onSetAccentColor: (Int?) -> Unit = {},
    onDeleteLink: () -> Unit = {},
    onEditUrl: (String) -> Unit = {},
    onRefreshMetadata: () -> Unit = {},
    onOpenUrl: () -> Unit = {},
    onShareUrl: () -> Unit = {},
    onCopyUrl: () -> Unit = {},
    onImageClick: () -> Unit = {},
    onImageLongClick: () -> Unit = {},
    onReadabilityClick: () -> Unit = {},
    onQrCodeClick: () -> Unit = {},
    onUpdateNotes: (String) -> Unit = onNoteBodyChange,
    onUrlCardClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Modal & Sheet visibility states
    var showRichLinkCardSheet by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showColorPickerSheet by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var showLinkQuickMenu by remember { mutableStateOf(false) }
    var isFormattingToolbarVisible by remember { mutableStateOf(false) }

    // Dialog states
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEditUrlDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var showFullTitleDialog by remember { mutableStateOf(false) }
    var showFullDescDialog by remember { mutableStateOf(false) }
    var isDescExpandedInSheet by remember { mutableStateOf(false) }
    var folderMenuExpandedInSheet by remember { mutableStateOf(false) }

    val provider = remember(link.provider, link.url) {
        if (link.provider != WebsiteProvider.GENERIC) {
            link.provider
        } else {
            ProviderDetector().detectProvider(link.url)
        }
    }

    // Dynamic background color: fully applied if set; theme background as-is when null
    val defaultThemeBg = MaterialTheme.colorScheme.background
    val isDarkTheme = isSystemInDarkTheme()

    val screenBgColor = remember(link.accentColorHex, defaultThemeBg) {
        if (link.accentColorHex != null) {
            Color(link.accentColorHex)
        } else {
            defaultThemeBg
        }
    }

    // Fully apply color to top status bar as well
    val statusBarColorArgb = remember(link.accentColorHex, defaultThemeBg) {
        link.accentColorHex ?: defaultThemeBg.toArgb()
    }

    val isStatusBarLight = remember(statusBarColorArgb, isDarkTheme, link.accentColorHex) {
        if (link.accentColorHex != null) {
            ColorUtils.calculateLuminance(statusBarColorArgb) > 0.45
        } else {
            !isDarkTheme
        }
    }

    val view = LocalView.current
    DisposableEffect(statusBarColorArgb, isStatusBarLight, defaultThemeBg, isDarkTheme) {
        val window = (view.context as? Activity)?.window
        if (window != null && !view.isInEditMode) {
            @Suppress("DEPRECATION")
            window.statusBarColor = statusBarColorArgb
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = isStatusBarLight
        }
        onDispose {
            val window = (view.context as? Activity)?.window
            if (window != null && !view.isInEditMode) {
                @Suppress("DEPRECATION")
                window.statusBarColor = defaultThemeBg.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDarkTheme
            }
        }
    }

    val buttonContainerColor = if (link.accentColorHex != null) {
        if (isDarkTheme) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    Scaffold(
        containerColor = screenBgColor,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            // Google Keep-Style Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back button on the left
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Middle autosave status indicator (unobtrusive)
                if (saveState == SaveState.Saving) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(10.dp),
                            strokeWidth = 1.4.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Saving...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Top-right action buttons in rounded squircles
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Pin Button
                    val isPinned = link.isPinned
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPinned) MaterialTheme.colorScheme.primaryContainer else buttonContainerColor,
                        modifier = Modifier.size(42.dp)
                    ) {
                        IconButton(
                            onClick = onTogglePin,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (isPinned) "Unpin" else "Pin",
                                tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // 2. Reminder / Bell Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = buttonContainerColor,
                        modifier = Modifier.size(42.dp)
                    ) {
                        IconButton(
                            onClick = { showReminderDialog = true },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = "Reminder",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // 3. Archive Button
                    val isArchived = link.isArchived
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isArchived) MaterialTheme.colorScheme.secondaryContainer else buttonContainerColor,
                        modifier = Modifier.size(42.dp)
                    ) {
                        IconButton(
                            onClick = onToggleArchive,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = if (isArchived) "Unarchive" else "Archive",
                                tint = if (isArchived) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Google Keep-Style Bottom Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding()
            ) {
                // Optional Formatting Mini-Toolbar (toggled by [A] button)
                AnimatedVisibility(
                    visible = isFormattingToolbarVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        tonalElevation = 4.dp,
                        shadowElevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onAppendToNote("\n- [ ] ") }) {
                                Icon(Icons.Default.CheckBox, contentDescription = "Checklist", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onAppendToNote("\n- ") }) {
                                Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Bullet", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onAppendToNote(" **bold** ") }) {
                                Icon(Icons.Default.FormatBold, contentDescription = "Bold", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onAppendToNote(" *italic* ") }) {
                                Icon(Icons.Default.FormatItalic, contentDescription = "Italic", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onAppendToNote("\n### ") }) {
                                Icon(Icons.Default.Title, contentDescription = "Heading", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onAppendToNote("\n> ") }) {
                                Icon(Icons.Default.FormatQuote, contentDescription = "Quote", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onAppendToNote("\n```kotlin\n\n```\n") }) {
                                Icon(Icons.Default.Code, contentDescription = "Code", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Bottom Action Bar Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left action icons ([+], [🎨], [A])
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // [+] Add items to note
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = buttonContainerColor,
                            modifier = Modifier.size(44.dp)
                        ) {
                            IconButton(
                                onClick = { showAddSheet = true },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AddBox,
                                    contentDescription = "Add to note",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // [🎨] Palette color picker
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = buttonContainerColor,
                            modifier = Modifier.size(44.dp)
                        ) {
                            IconButton(
                                onClick = { showColorPickerSheet = true },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Palette,
                                    contentDescription = "Note color",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // [A] Formatting mini-toolbar toggle
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isFormattingToolbarVisible) MaterialTheme.colorScheme.primaryContainer else buttonContainerColor,
                            modifier = Modifier.size(44.dp)
                        ) {
                            IconButton(
                                onClick = { isFormattingToolbarVisible = !isFormattingToolbarVisible },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatColorText,
                                    contentDescription = "Format text",
                                    tint = if (isFormattingToolbarVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Right action icons ([↶], [↷], [⋮])
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // [↶] Undo
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = buttonContainerColor,
                            modifier = Modifier.size(44.dp)
                        ) {
                            IconButton(
                                onClick = onUndo,
                                enabled = canUndo,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "Undo",
                                    tint = if (canUndo) MaterialTheme.colorScheme.onSurfaceVariant
                                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // [↷] Redo
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = buttonContainerColor,
                            modifier = Modifier.size(44.dp)
                        ) {
                            IconButton(
                                onClick = onRedo,
                                enabled = canRedo,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Redo,
                                    contentDescription = "Redo",
                                    tint = if (canRedo) MaterialTheme.colorScheme.onSurfaceVariant
                                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // [⋮] More note options
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = buttonContainerColor,
                            modifier = Modifier.size(44.dp)
                        ) {
                            IconButton(
                                onClick = { showMoreSheet = true },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { scaffoldPadding ->
        // Main Scrollable Canvas (Title, Notes, and Link Card at Top)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = scaffoldPadding.calculateTopPadding(), bottom = scaffoldPadding.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. Note Title (Google Keep style at the top)
            TextField(
                value = noteTitle,
                onValueChange = onNoteTitleChange,
                placeholder = {
                    Text(
                        text = "Title",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                },
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                maxLines = 4
            )

            // 2. Note Body (Notes text right below the title)
            TextField(
                value = noteBodyMarkdown,
                onValueChange = onNoteBodyChange,
                placeholder = {
                    Text(
                        text = "Note",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    lineHeight = 23.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 70.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Compact Link Card (Exact matching screenshot layout)
            val compactCardShape = RoundedCornerShape(16.dp)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(compactCardShape)
                    .clickable { showRichLinkCardSheet = true },
                shape = compactCardShape,
                colors = CardDefaults.cardColors(
                    containerColor = if (link.accentColorHex != null) {
                        if (isDarkTheme) Color.Black.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.70f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (link.accentColorHex != null) {
                        if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left image thumbnail
                    Box(
                        modifier = Modifier
                            .width(82.dp)
                            .fillMaxHeight()
                    ) {
                        if (!link.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(link.imageUrl)
                                    .crossfade(true)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .build(),
                                contentDescription = link.title,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Middle title and domain
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = link.title ?: link.url,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = link.domain ?: link.url.removePrefix("https://").removePrefix("http://").split("/").firstOrNull() ?: "",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Right 3-dots menu
                    Box(modifier = Modifier.padding(end = 4.dp)) {
                        IconButton(
                            onClick = { showLinkQuickMenu = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Link options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showLinkQuickMenu,
                            onDismissRequest = { showLinkQuickMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Open Link") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                                onClick = {
                                    showLinkQuickMenu = false
                                    onOpenUrl()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Link") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    showLinkQuickMenu = false
                                    onShareUrl()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Copy Link") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                onClick = {
                                    showLinkQuickMenu = false
                                    onCopyUrl()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("View Full Card") },
                                leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) },
                                onClick = {
                                    showLinkQuickMenu = false
                                    showRichLinkCardSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Link") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showLinkQuickMenu = false
                                    showEditUrlDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Refresh Metadata") },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                onClick = {
                                    showLinkQuickMenu = false
                                    onRefreshMetadata()
                                }
                            )
                        }
                    }
                }
            }

            // 4. Subtle Chips for Folder and Tags (if attached)
            if (link.folderId != null || link.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val assignedFolder = folders.find { it.id == link.folderId }
                    if (assignedFolder != null) {
                        SuggestionChip(
                            onClick = { showAddSheet = true },
                            label = { Text(assignedFolder.name) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

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
                }
            }

            // Free canvas empty space for comfortable typing/scrolling
            Spacer(modifier = Modifier.height(200.dp))
        }
    }

    // ==========================================
    // MODAL BOTTOM SHEETS & INTERACTIVE DIALOGS
    // ==========================================

    // 1. Rich Link Card Modal Bottom Sheet ("Our same link card with open and share buttons")
    if (showRichLinkCardSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRichLinkCardSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Hero Image Banner
                if (!link.imageUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(18.dp))
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

                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
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

                // Favicon, Domain, Reading Time, Date Row
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
                            text = link.domain ?: "Web Link",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (link.readingTimeMinutes != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "${link.readingTimeMinutes} min read",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
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
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = 21.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { showFullTitleDialog = true },
                            onLongClick = { copyToClipboard(clipboardManager, context, link.title ?: link.url, "Title") }
                        )
                )

                // Scraped Description
                if (!link.description.isNullOrBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isDescExpandedInSheet = !isDescExpandedInSheet }
                    ) {
                        Text(
                            text = link.description,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            maxLines = if (isDescExpandedInSheet) 20 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = if (isDescExpandedInSheet) "Show less" else "Show more",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = if (isDescExpandedInSheet) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Prominent Open & Share Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            showRichLinkCardSheet = false
                            onOpenUrl()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                provider == WebsiteProvider.YOUTUBE || provider.isVideo -> Icons.Default.PlayArrow
                                provider == WebsiteProvider.GITHUB || provider.isCode -> Icons.AutoMirrored.Filled.OpenInNew
                                provider.isArticle -> Icons.AutoMirrored.Filled.MenuBook
                                else -> Icons.AutoMirrored.Filled.OpenInNew
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                provider == WebsiteProvider.YOUTUBE || provider.isVideo -> "Watch Video"
                                provider == WebsiteProvider.GITHUB || provider.isCode -> "View Repo"
                                provider.isArticle -> "Read Article"
                                else -> "Open Link"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            showRichLinkCardSheet = false
                            onShareUrl()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                }

                // Secondary Action Buttons Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onCopyUrl,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = onRefreshMetadata,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Refresh", style = MaterialTheme.typography.labelMedium)
                    }

                    // Provider Injections into Note
                    when {
                        provider == WebsiteProvider.YOUTUBE || provider.isVideo -> {
                            OutlinedButton(
                                onClick = {
                                    onAppendToNote("\n- ⏱️ [00:00] ")
                                    showRichLinkCardSheet = false
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Timestamp", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        provider == WebsiteProvider.GITHUB || provider.isCode -> {
                            OutlinedButton(
                                onClick = {
                                    onAppendToNote("\n```kotlin\n\n```\n")
                                    showRichLinkCardSheet = false
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Code Block", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        provider.isArticle && !link.description.isNullOrBlank() -> {
                            OutlinedButton(
                                onClick = {
                                    onAppendToNote("\n> ${link.description.trim()}\n\n")
                                    showRichLinkCardSheet = false
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.FormatQuote, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Quote", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // Folder & Tag Management Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentFolder = folders.find { it.id == link.folderId }
                    Box {
                        AssistChip(
                            onClick = { folderMenuExpandedInSheet = true },
                            label = { Text(currentFolder?.name ?: "Assign Folder") },
                            leadingIcon = {
                                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                        DropdownMenu(
                            expanded = folderMenuExpandedInSheet,
                            onDismissRequest = { folderMenuExpandedInSheet = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None (Remove Folder)") },
                                onClick = {
                                    onAssignFolder(null)
                                    folderMenuExpandedInSheet = false
                                }
                            )
                            folders.forEach { f ->
                                DropdownMenuItem(
                                    text = { Text(f.name) },
                                    onClick = {
                                        onAssignFolder(f.id)
                                        folderMenuExpandedInSheet = false
                                    }
                                )
                            }
                        }
                    }

                    AssistChip(
                        onClick = { showAddTagDialog = true },
                        label = { Text("+ Add Tag") },
                        leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }
    }

    // 2. Add Items to Note Modal Bottom Sheet ([+] button)
    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 20.dp)
            ) {
                Text(
                    text = "Add to note",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                ListItem(
                    headlineContent = { Text("Checklist") },
                    leadingContent = { Icon(Icons.Default.CheckBox, contentDescription = null) },
                    modifier = Modifier.clickable {
                        onAppendToNote("\n- [ ] ")
                        showAddSheet = false
                    }
                )
                ListItem(
                    headlineContent = { Text("Bulleted list") },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = null) },
                    modifier = Modifier.clickable {
                        onAppendToNote("\n- ")
                        showAddSheet = false
                    }
                )
                ListItem(
                    headlineContent = { Text("Code block") },
                    leadingContent = { Icon(Icons.Default.Code, contentDescription = null) },
                    modifier = Modifier.clickable {
                        onAppendToNote("\n```kotlin\n\n```\n")
                        showAddSheet = false
                    }
                )
                ListItem(
                    headlineContent = { Text("Quote block") },
                    leadingContent = { Icon(Icons.Default.FormatQuote, contentDescription = null) },
                    modifier = Modifier.clickable {
                        onAppendToNote("\n> ")
                        showAddSheet = false
                    }
                )
                ListItem(
                    headlineContent = { Text("Heading") },
                    leadingContent = { Icon(Icons.Default.Title, contentDescription = null) },
                    modifier = Modifier.clickable {
                        onAppendToNote("\n### ")
                        showAddSheet = false
                    }
                )
                ListItem(
                    headlineContent = { Text("Add Tag") },
                    leadingContent = { Icon(Icons.Default.Tag, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showAddSheet = false
                        showAddTagDialog = true
                    }
                )
            }
        }
    }

    // 3. Google Keep-Style Color Palette Modal Bottom Sheet ([🎨] button)
    if (showColorPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showColorPickerSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Note Color",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    noteColorPalette.forEach { option ->
                        val isSelected = link.accentColorHex == option.colorHex
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (option.colorHex != null) Color(option.colorHex)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onSetAccentColor(option.colorHex)
                                    showColorPickerSheet = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else if (option.colorHex == null) {
                                Icon(
                                    imageVector = Icons.Default.FormatColorReset,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 4. More Options Modal Bottom Sheet ([⋮] button)
    if (showMoreSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 20.dp)
            ) {
                Text(
                    text = "Note Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                ListItem(
                    headlineContent = { Text("Delete") },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable {
                        showMoreSheet = false
                        showDeleteConfirmDialog = true
                    }
                )
                ListItem(
                    headlineContent = { Text("Share Note Text") },
                    leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showMoreSheet = false
                        val noteFullText = buildString {
                            if (noteTitle.isNotBlank()) append(noteTitle).append("\n\n")
                            if (noteBodyMarkdown.isNotBlank()) append(noteBodyMarkdown).append("\n\n")
                            append(link.url)
                        }
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, noteFullText)
                            type = "text/plain"
                        }
                        context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Note"))
                    }
                )
                ListItem(
                    headlineContent = { Text("Reader View") },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showMoreSheet = false
                        onReadabilityClick()
                    }
                )
                ListItem(
                    headlineContent = { Text("QR Code") },
                    leadingContent = { Icon(Icons.Default.QrCode, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showMoreSheet = false
                        onQrCodeClick()
                    }
                )
                ListItem(
                    headlineContent = { Text("Edit Link URL") },
                    leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showMoreSheet = false
                        showEditUrlDialog = true
                    }
                )
                ListItem(
                    headlineContent = { Text("Refresh Link Metadata") },
                    leadingContent = { Icon(Icons.Default.Refresh, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showMoreSheet = false
                        onRefreshMetadata()
                    }
                )
            }
        }
    }

    // 5. Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Link Note") },
            text = { Text("Are you sure you want to delete this note and link? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteLink()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 6. Reminder Dialog
    if (showReminderDialog) {
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = { Text("Set Reminder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select when you'd like to be reminded:")
                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Reminder set for later today (6:00 PM)", Toast.LENGTH_SHORT).show()
                            showReminderDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Later today (6:00 PM)")
                    }
                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Reminder set for tomorrow (8:00 AM)", Toast.LENGTH_SHORT).show()
                            showReminderDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tomorrow morning (8:00 AM)")
                    }
                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Reminder set for next Monday", Toast.LENGTH_SHORT).show()
                            showReminderDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Next week")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReminderDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // 7. Edit URL Dialog
    if (showEditUrlDialog) {
        var urlInput by remember(link.url) { mutableStateOf(link.url) }
        var isUrlError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditUrlDialog = false },
            title = { Text("Edit Link URL") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = {
                            urlInput = it
                            isUrlError = it.isBlank()
                        },
                        label = { Text("URL") },
                        isError = isUrlError,
                        supportingText = {
                            if (isUrlError) Text("URL cannot be empty", color = MaterialTheme.colorScheme.error)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (urlInput.isNotBlank()) {
                            onEditUrl(urlInput.trim())
                            showEditUrlDialog = false
                        } else {
                            isUrlError = true
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditUrlDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 8. Add Tag Dialog
    var newTagName by remember { mutableStateOf("") }
    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddTagDialog = false
                newTagName = ""
            },
            title = { Text("Add Tag") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
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
                            text = "Suggestions",
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

    // 9. Full Title Dialog
    if (showFullTitleDialog && !link.title.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showFullTitleDialog = false },
            title = { Text("Full Title", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = link.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
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

    // 10. Full Description Dialog
    if (showFullDescDialog && !link.description.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showFullDescDialog = false },
            title = { Text("Full Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = link.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
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