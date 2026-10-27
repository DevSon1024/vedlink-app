package com.devson.vedlink.ui.presentation.util

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.devson.vedlink.ui.presentation.screens.LinkDetailsScreen
import com.devson.vedlink.ui.viewmodel.LinkDetailsViewModel
import com.devson.vedlink.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

@Composable
fun LinkDetailsPagerScreen(
    initialLinkId: Int,
    linkIds: List<Int> = listOf(initialLinkId),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    // ViewModels
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val isDark by settingsViewModel.isDarkTheme.collectAsStateWithLifecycle()

    val viewModel: LinkDetailsViewModel = hiltViewModel(key = "details_$initialLinkId")

    LaunchedEffect(initialLinkId) {
        viewModel.loadLink(initialLinkId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val activeLink = uiState.link

    var showImageDialog by remember { mutableStateOf(false) }

    // Dynamic status bar color handling
    val view = LocalView.current
    val isDarkTheme = isDark ?: androidx.compose.foundation.isSystemInDarkTheme()
    val defaultBg = MaterialTheme.colorScheme.background

    val statusBarColorArgb = remember(activeLink?.accentColorHex, defaultBg) {
        activeLink?.accentColorHex ?: defaultBg.toArgb()
    }

    val isLightStatusBar = remember(statusBarColorArgb, isDarkTheme, activeLink?.accentColorHex) {
        if (activeLink?.accentColorHex != null) {
            androidx.core.graphics.ColorUtils.calculateLuminance(statusBarColorArgb) > 0.45
        } else {
            !isDarkTheme
        }
    }

    DisposableEffect(statusBarColorArgb, isLightStatusBar, defaultBg, isDarkTheme) {
        val window = (view.context as? Activity)?.window
        if (window != null && !view.isInEditMode) {
            @Suppress("DEPRECATION")
            window.statusBarColor = statusBarColorArgb
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = isLightStatusBar
        }
        onDispose {
            val window = (view.context as? Activity)?.window
            if (window != null && !view.isInEditMode) {
                @Suppress("DEPRECATION")
                window.statusBarColor = defaultBg.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDarkTheme
            }
        }
    }

    // Flush on system back navigation
    BackHandler {
        viewModel.flushPendingSave()
        onNavigateBack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading && activeLink == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (activeLink != null) {
            LinkDetailsScreen(
                link = activeLink,
                noteTitle = uiState.noteTitle,
                noteBodyMarkdown = uiState.noteBodyMarkdown,
                saveState = uiState.saveState,
                canUndo = uiState.canUndo,
                canRedo = uiState.canRedo,
                folders = folders,
                allTags = allTags,
                onNavigateBack = {
                    viewModel.flushPendingSave()
                    onNavigateBack()
                },
                onNoteTitleChange = { viewModel.onNoteTitleChanged(it) },
                onNoteBodyChange = { viewModel.onNoteBodyChanged(it) },
                onAppendToNote = { viewModel.appendToNoteBody(it) },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onAssignFolder = { viewModel.assignFolder(it) },
                onAddTag = { viewModel.addTag(it) },
                onRemoveTag = { viewModel.removeTag(it) },
                onTogglePin = { viewModel.togglePin() },
                onToggleArchive = { viewModel.toggleArchive() },
                onSetAccentColor = { viewModel.setAccentColor(it) },
                onDeleteLink = {
                    viewModel.deleteLink()
                    onNavigateBack()
                },
                onEditUrl = { newUrl -> viewModel.updateUrl(newUrl) },
                onRefreshMetadata = { viewModel.refreshMetadata() },
                onOpenUrl = { openInBrowser(context, activeLink.url) },
                onShareUrl = { shareLink(context, activeLink.url) },
                onCopyUrl = { copyToClipboard(clipboardManager, context, activeLink.url, "URL") },
                onReadabilityClick = { viewModel.extractReadabilityText(activeLink.url) },
                onQrCodeClick = { viewModel.showQrCodeDialog(true) },
                onImageClick = { showImageDialog = true },
                onImageLongClick = {
                    scope.launch {
                        downloadImage(
                            context = context,
                            imageUrl = activeLink.imageUrl ?: "",
                            fileName = activeLink.title ?: "image"
                        )
                    }
                }
            )
        }

        // Full Image Dialog with Download Option
        if (showImageDialog && !activeLink?.imageUrl.isNullOrBlank()) {
            val imageUrl = activeLink?.imageUrl ?: ""
            val imageTitle = activeLink?.title ?: "image"
            Dialog(onDismissRequest = { showImageDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 500.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Full Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium),
                                contentScale = ContentScale.FillWidth
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    scope.launch {
                                        downloadImage(
                                            context = context,
                                            imageUrl = imageUrl,
                                            fileName = imageTitle
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download")
                            }

                            OutlinedButton(
                                onClick = { showImageDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Close")
                            }
                        }
                    }
                }
            }
        }

        // QR Code Dialog
        if (uiState.showQrCodeDialog && activeLink != null) {
            val qrUrl = activeLink.url
            val qrBitmap = remember(qrUrl) {
                generateQrCode(qrUrl, 512)
            }

            AlertDialog(
                onDismissRequest = { viewModel.showQrCodeDialog(false) },
                title = {
                    Text(
                        text = "QR Code",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "QR Code for Link URL",
                                modifier = Modifier
                                    .size(240.dp)
                                    .padding(8.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Failed to generate QR Code")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = qrUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.showQrCodeDialog(false) }) {
                        Text("Close")
                    }
                }
            )
        }

        // Readability Mode Overlay
        if (uiState.isReadingModeActive && activeLink != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.exitReadingMode() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit Reading Mode"
                            )
                        }
                        Text(
                            text = "Reader View",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    if (uiState.isExtractingReaderText) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Extracting article text...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 16.dp)
                        ) {
                            Text(
                                text = activeLink.title ?: "No Title",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = activeLink.domain ?: "",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = uiState.readabilityText ?: "",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun downloadImage(
    context: Context,
    imageUrl: String,
    fileName: String
) {
    withContext(Dispatchers.IO) {
        try {
            val url = URL(imageUrl)
            val connection = url.openConnection()
            connection.connect()

            val inputStream = connection.getInputStream()

            val cleanFileName = fileName.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val timestamp = System.currentTimeMillis()
            val finalFileName = "${cleanFileName}_${timestamp}.jpg"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, finalFileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values
                )

                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
                val file = File(downloadsDir, finalFileName)

                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            inputStream.close()

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Image downloaded to Downloads folder",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Failed to download image: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
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

private fun openInBrowser(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Failed to open link", Toast.LENGTH_SHORT).show()
    }
}

private fun shareLink(context: Context, url: String) {
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, url)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share link"))
}

private fun generateQrCode(text: String, size: Int = 512): Bitmap? {
    return try {
        val bitMatrix = com.google.zxing.MultiFormatWriter().encode(
            text,
            com.google.zxing.BarcodeFormat.QR_CODE,
            size,
            size
        )
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        bitmap
    } catch (_: Exception) {
        null
    }
}
