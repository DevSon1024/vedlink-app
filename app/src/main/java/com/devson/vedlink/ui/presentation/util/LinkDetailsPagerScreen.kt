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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.devson.vedlink.ui.presentation.screens.LinkDetailsScreen
import com.devson.vedlink.ui.viewmodel.LinkDetailsViewModel
import com.devson.vedlink.ui.viewmodel.LinkDetailsUiState
import com.devson.vedlink.ui.viewmodel.SettingsViewModel
import com.devson.vedlink.domain.model.Folder
import com.devson.vedlink.domain.model.Link
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LinkDetailsPagerScreen(
    initialLinkId: Int,
    linkIds: List<Int>,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val initialPage = linkIds.indexOf(initialLinkId).coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { linkIds.size }
    )

    // ViewModels for dark mode setting and folders
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val isDark by settingsViewModel.isDarkTheme.collectAsState()

    // Dialog trigger states
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf(false) }
    var showUrlActionsSheet by remember { mutableStateOf(false) }
    var showEditUrlDialog by remember { mutableStateOf(false) }

    // Derive active page configurations to minimize redundant recompositions
    val activeLinkId by remember(pagerState) {
        derivedStateOf { linkIds[pagerState.currentPage] }
    }
    
    val activeViewModel: LinkDetailsViewModel = hiltViewModel(key = "details_$activeLinkId")
    val uiState by activeViewModel.uiState.collectAsState()
    val folders by activeViewModel.folders.collectAsState()
    
    val currentUrl = remember(uiState.link?.url) { uiState.link?.url }
    val pageText by remember(pagerState) {
        derivedStateOf { "${pagerState.currentPage + 1} / ${linkIds.size}" }
    }

    val onPreviousPage = if (pagerState.currentPage > 0) {
        { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }
    } else null

    val onNextPage = if (pagerState.currentPage < linkIds.lastIndex) {
        { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }
    } else null

    // Status bar color handling
    val view = LocalView.current
    if (!view.isInEditMode) {
        val backgroundColor = MaterialTheme.colorScheme.background
        val darkTheme = isDark ?: androidx.compose.foundation.isSystemInDarkTheme()
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = backgroundColor.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Link Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { uiState.link?.url?.let { activeViewModel.extractReadabilityText(it) } }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "Readability Mode"
                        )
                    }
                    IconButton(
                        onClick = { activeViewModel.showQrCodeDialog(true) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "QR Code"
                        )
                    }
                    IconButton(
                        onClick = { activeViewModel.toggleFavorite() }
                    ) {
                        Icon(
                            imageVector = if (uiState.link?.isFavorite == true)
                                Icons.Filled.Favorite
                            else
                                Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (uiState.link?.isFavorite == true)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onPreviousPage?.invoke() },
                        enabled = onPreviousPage != null
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous link",
                            tint = if (onPreviousPage != null)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    Row(
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { currentUrl?.let { openInBrowser(context, it) } },
                            enabled = currentUrl != null,
                            modifier = Modifier
                                .height(40.dp)
                                .weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        Text(
                            text = pageText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Visible
                        )

                        FilledTonalButton(
                            onClick = { currentUrl?.let { shareLink(context, it) } },
                            enabled = currentUrl != null,
                            modifier = Modifier
                                .height(40.dp)
                                .weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    IconButton(
                        onClick = { onNextPage?.invoke() },
                        enabled = onNextPage != null
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next link",
                            tint = if (onNextPage != null)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fill,
            beyondViewportPageCount = 1,          // Preload 1 adjacent page only
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val linkId = linkIds[pageIndex]
            
            // Only actively load data when the page is settled or ±1 adjacent.
            val shouldLoad by remember(pagerState) {
                derivedStateOf {
                    abs(pagerState.settledPage - pageIndex) <= 1
                }
            }

            // Each page gets its own ViewModel instance keyed to linkId
            val pageViewModel: LinkDetailsViewModel =
                hiltViewModel(key = "details_$linkId")

            LaunchedEffect(linkId, shouldLoad) {
                if (shouldLoad) {
                    pageViewModel.loadLink(linkId)
                }
            }

            val pageUiState by pageViewModel.uiState.collectAsState()
            val allTags by pageViewModel.allTags.collectAsState()

            if (pageUiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                pageUiState.link?.let { link ->
                    LinkDetailsScreen(
                        link = link,
                        folders = folders,
                        paddingValues = paddingValues,
                        onAssignFolder = { folderId -> pageViewModel.assignFolder(folderId); Unit },
                        onAddTag = { tag -> pageViewModel.addTag(tag); Unit },
                        onRemoveTag = { tag -> pageViewModel.removeTag(tag); Unit },
                        onUpdateNotes = { notes -> pageViewModel.updateNotes(notes); Unit },
                        onUrlCardClick = { showUrlActionsSheet = true; Unit },
                        onImageClick = { showImageDialog = true; Unit },
                        onImageLongClick = {
                            scope.launch {
                                downloadImage(
                                    context = context,
                                    imageUrl = link.imageUrl ?: "",
                                    fileName = link.title ?: "image"
                                )
                            }
                            Unit
                        },
                        allTags = allTags
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Link") },
            text = { Text("Are you sure you want to delete this link? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        activeViewModel.deleteLink()
                        showDeleteDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Full Image Dialog with Download Option
    if (showImageDialog && !uiState.link?.imageUrl.isNullOrBlank()) {
        Dialog(onDismissRequest = { showImageDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 600.dp)
                    ) {
                        coil.compose.AsyncImage(
                            model = coil.request.ImageRequest.Builder(context)
                                .data(uiState.link?.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Full Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.extraSmall),
                            contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
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
                                    uiState.link?.let { link ->
                                        downloadImage(
                                            context = context,
                                            imageUrl = link.imageUrl!!,
                                            fileName = link.title ?: "image"
                                        )
                                    }
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

    // URL Actions Bottom Sheet
    if (showUrlActionsSheet && uiState.link != null) {
        ModalBottomSheet(
            onDismissRequest = { showUrlActionsSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 24.dp, start = 24.dp, end = 24.dp)
            ) {
                Text(
                    text = "Link Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                ListItem(
                    headlineContent = { Text("Copy Link") },
                    leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                    modifier = Modifier.clickable {
                        copyToClipboard(
                            clipboardManager,
                            context,
                            uiState.link!!.url,
                            "URL"
                        )
                        showUrlActionsSheet = false
                    }
                )
                ListItem(
                    headlineContent = { Text("Edit Link") },
                    leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showUrlActionsSheet = false
                        showEditUrlDialog = true
                    }
                )
            }
        }
    }

    // Edit URL Dialog
    if (showEditUrlDialog && uiState.link != null) {
        val originalUrl = uiState.link!!.url
        var urlInput by remember { mutableStateOf(originalUrl) }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditUrlDialog = false },
            title = { Text("Edit Link URL") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = {
                            urlInput = it
                            isError = it.isBlank()
                        },
                        label = { Text("URL") },
                        isError = isError,
                        supportingText = {
                            if (isError) {
                                Text("URL cannot be empty", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        trailingIcon = {
                            if (urlInput != originalUrl) {
                                IconButton(
                                    onClick = { urlInput = originalUrl }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = "Reset to Original"
                                    )
                                }
                            }
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
                            activeViewModel.updateUrl(urlInput)
                            showEditUrlDialog = false
                        } else {
                            isError = true
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

    // QR Code Dialog
    if (uiState.showQrCodeDialog && uiState.link != null) {
        val qrBitmap = remember(uiState.link!!.url) {
            generateQrCode(uiState.link!!.url, 512)
        }

        AlertDialog(
            onDismissRequest = { activeViewModel.showQrCodeDialog(false) },
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
                        text = uiState.link!!.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { activeViewModel.showQrCodeDialog(false) }) {
                    Text("Close")
                }
            }
        )
    }

    // Readability Mode Overlay
    if (uiState.isReadingModeActive && uiState.link != null) {
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
                    IconButton(onClick = { activeViewModel.exitReadingMode() }) {
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
                            text = uiState.link?.title ?: "No Title",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.link?.domain ?: "",
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

// Suspend function to download the image file to external downloads folder
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
    } catch (e: Exception) {
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
    } catch (e: Exception) {
        null
    }
}
