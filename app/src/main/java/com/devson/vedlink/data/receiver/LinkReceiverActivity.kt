package com.devson.vedlink.data.receiver

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.devson.vedlink.data.network.scraper.MetadataPipeline
import com.devson.vedlink.data.preferences.ThemePreferences
import com.devson.vedlink.data.service.LinkSaveService
import com.devson.vedlink.domain.model.Folder
import com.devson.vedlink.domain.model.ScrapedMetadata
import com.devson.vedlink.domain.repository.FolderRepository
import com.devson.vedlink.domain.usecase.SaveLinkUseCase
import com.devson.vedlink.domain.util.LinkExtractor
import com.devson.vedlink.ui.theme.AppThemePalette
import com.devson.vedlink.ui.theme.VedLinkTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LinkReceiverActivity : ComponentActivity() {

    @Inject
    lateinit var linkExtractor: LinkExtractor

    @Inject
    lateinit var folderRepository: FolderRepository

    @Inject
    lateinit var themePreferences: ThemePreferences

    @Inject
    lateinit var metadataPipeline: MetadataPipeline

    @Inject
    lateinit var saveLinkUseCase: SaveLinkUseCase

    private var isProcessed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        val sharedIntent = intent
        val (detectedUrl, initialText) = parseSharedIntent(sharedIntent)

        if (detectedUrl == null) {
            Toast.makeText(this, "No valid link found in shared text", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            val themeMode by themePreferences.themeMode.collectAsState(initial = 0)
            val colorSchemePref by themePreferences.colorScheme.collectAsState(initial = 0)
            val dynamicColor by themePreferences.dynamicColor.collectAsState(initial = false)
            val allFolders by folderRepository.getAllFolders().collectAsState(initial = emptyList())

            val forceDark = when (themeMode) {
                1 -> false
                2 -> true
                else -> null
            }

            val palette = AppThemePalette.entries.getOrElse(colorSchemePref) {
                AppThemePalette.CINEMATIC
            }

            VedLinkTheme(
                forceDark = forceDark,
                dynamicColor = dynamicColor,
                palette = palette,
                isNavBarTransparent = true
            ) {
                ShareSheetMicroNoteOverlay(
                    url = detectedUrl,
                    initialNote = initialText,
                    folders = allFolders,
                    onSaveAndFinish = { noteBody, isPinned, folderId ->
                        commitSaveAndFinish(detectedUrl, noteBody, isPinned, folderId)
                    },
                    onDismiss = { noteBody, isPinned, folderId ->
                        // Even on dismiss / back, reliably persist link + note
                        commitSaveAndFinish(detectedUrl, noteBody, isPinned, folderId)
                    },
                    onFetchMetadata = { url ->
                        try {
                            metadataPipeline.resolveMetadata(url)
                        } catch (e: Exception) {
                            null
                        }
                    }
                )
            }
        }
    }

    private fun parseSharedIntent(intent: Intent?): Pair<String?, String> {
        if (intent == null || intent.action != Intent.ACTION_SEND) {
            return Pair(null, "")
        }

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        val sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            ?: intent.getStringExtra(Intent.EXTRA_TITLE)
            ?: ""

        val urls = linkExtractor.extractUrls(sharedText)
        val targetUrl = urls.firstOrNull() ?: if (sharedText.startsWith("http://") || sharedText.startsWith("https://")) sharedText else null

        // Remove the detected URL from the shared text to leave any user-written caption / note
        val remainingText = if (targetUrl != null) {
            sharedText.replace(targetUrl, "").trim()
        } else ""

        val initialNote = when {
            remainingText.isNotBlank() -> remainingText
            sharedSubject.isNotBlank() && !sharedSubject.startsWith("http") -> sharedSubject.trim()
            else -> ""
        }

        return Pair(targetUrl, initialNote)
    }

    private fun commitSaveAndFinish(
        url: String,
        noteBody: String?,
        isPinned: Boolean,
        folderId: Int?
    ) {
        if (isProcessed) return
        isProcessed = true

        val serviceIntent = Intent(this, LinkSaveService::class.java).apply {
            putExtra(LinkSaveService.EXTRA_TEXT, url)
            if (!noteBody.isNullOrBlank()) {
                putExtra(LinkSaveService.EXTRA_NOTE_BODY, noteBody.trim())
            }
            putExtra(LinkSaveService.EXTRA_IS_PINNED, isPinned)
            if (folderId != null) {
                putExtra(LinkSaveService.EXTRA_FOLDER_ID, folderId)
            }
        }

        try {
            startService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        finish()
    }
}

@Composable
private fun ShareSheetMicroNoteOverlay(
    url: String,
    initialNote: String,
    folders: List<Folder>,
    onSaveAndFinish: (noteBody: String?, isPinned: Boolean, folderId: Int?) -> Unit,
    onDismiss: (noteBody: String?, isPinned: Boolean, folderId: Int?) -> Unit,
    onFetchMetadata: suspend (String) -> ScrapedMetadata?
) {
    var noteBody by remember { mutableStateOf(initialNote) }
    var isPinned by remember { mutableStateOf(false) }
    var selectedFolderId by remember { mutableStateOf<Int?>(null) }
    var hasInteracted by remember { mutableStateOf(initialNote.isNotBlank()) }
    var countdown by remember { mutableIntStateOf(4) }
    var metadata by remember { mutableStateOf<ScrapedMetadata?>(null) }
    var isMetadataLoading by remember { mutableStateOf(true) }

    val domain = remember(url) {
        try {
            Uri.parse(url).host?.removePrefix("www.") ?: url
        } catch (e: Exception) {
            url
        }
    }

    // Resolve preview metadata in background
    LaunchedEffect(url) {
        isMetadataLoading = true
        metadata = onFetchMetadata(url)
        isMetadataLoading = false
    }

    // Auto-save countdown loop (pauses as soon as user types or interacts)
    LaunchedEffect(hasInteracted) {
        if (!hasInteracted) {
            while (countdown > 0 && !hasInteracted) {
                delay(1000)
                countdown--
            }
            if (countdown <= 0 && !hasInteracted) {
                onSaveAndFinish(noteBody.takeIf { it.isNotBlank() }, isPinned, selectedFolderId)
            }
        }
    }

    BackHandler {
        onDismiss(noteBody.takeIf { it.isNotBlank() }, isPinned, selectedFolderId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onDismiss(noteBody.takeIf { it.isNotBlank() }, isPinned, selectedFolderId)
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Consume clicks inside sheet */ }
                .shadow(16.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Header Row: Domain Badge, Countdown / Status, Pin & Dismiss
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = domain,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (!hasInteracted) "Auto-saving in ${countdown}s..." else "Quick Note Draft",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (!hasInteracted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pin Toggle
                        IconButton(
                            onClick = {
                                hasInteracted = true
                                isPinned = !isPinned
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin Link",
                                tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Close / Save Button
                        IconButton(
                            onClick = {
                                onDismiss(noteBody.takeIf { it.isNotBlank() }, isPinned, selectedFolderId)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Live Preview Card (if resolved or loading)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (metadata?.imageUrl != null) {
                            AsyncImage(
                                model = metadata?.imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        } else if (isMetadataLoading) {
                            Box(
                                modifier = Modifier.size(44.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = metadata?.title ?: url,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = metadata?.description ?: url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 2-Line Note Input Field
                OutlinedTextField(
                    value = noteBody,
                    onValueChange = {
                        noteBody = it
                        hasInteracted = true
                    },
                    placeholder = {
                        Text(
                            "Add a quick note...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp, max = 120.dp),
                    maxLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Row of Folder Chips
                if (folders.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedFolderId == null,
                            onClick = {
                                hasInteracted = true
                                selectedFolderId = null
                            },
                            label = { Text("No Folder", style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(12.dp)
                        )

                        folders.forEach { folder ->
                            FilterChip(
                                selected = selectedFolderId == folder.id,
                                onClick = {
                                    hasInteracted = true
                                    selectedFolderId = folder.id
                                },
                                label = { Text(folder.name, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = if (selectedFolderId == folder.id) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                } else null,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onDismiss(noteBody.takeIf { it.isNotBlank() }, isPinned, selectedFolderId)
                        },
                        modifier = Modifier.weight(0.4f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Dismiss")
                    }

                    Button(
                        onClick = {
                            onSaveAndFinish(noteBody.takeIf { it.isNotBlank() }, isPinned, selectedFolderId)
                        },
                        modifier = Modifier.weight(0.6f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (noteBody.isNotBlank()) "Save Note" else "Save Link",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}