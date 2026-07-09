package com.devson.vedlink.ui.presentation.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.devson.vedlink.ui.presentation.components.SettingsCard
import com.devson.vedlink.ui.presentation.components.SettingsDivider
import com.devson.vedlink.ui.presentation.components.SettingsNavRow
import com.devson.vedlink.ui.presentation.components.SettingsSectionLabel
import com.devson.vedlink.ui.presentation.components.SettingsSwitchItem
import com.devson.vedlink.ui.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToCustomizeHome: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isNavBarTransparent by viewModel.isNavBarTransparent.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isDebug = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
    }
    val versionText = "v$versionName" + if (isDebug) " (Debug)" else " (Stable)"

    // Status bar color handling
    val view = LocalView.current
    if (!view.isInEditMode) {
        val backgroundColor = MaterialTheme.colorScheme.background
        val darkTheme = isDark ?: isSystemInDarkTheme()
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = backgroundColor.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
        }
    }

    // Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportData(context, it) }
    }

    // Import Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importData(context, it) }
    }

    // Observe toast messages
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                )
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // App Info Card (Pixchive Style)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primary,
                        tonalElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "VedLink",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Smart Link Manager • $versionText",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Customize UI Section
            SettingsSectionLabel("Customize UI")
            SettingsCard {
                SettingsNavRow(
                    icon = Icons.Default.Home,
                    title = "Customize Home",
                    subtitle = "Toggle home page sections",
                    onClick = onNavigateToCustomizeHome
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Default.Palette,
                    title = "Appearance",
                    subtitle = "Theme, colours & fonts",
                    onClick = onNavigateToAppearance
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Data Management Section
            SettingsSectionLabel("Data Management")
            SettingsCard {
                SettingsSwitchItem(
                    icon = Icons.Default.Cloud,
                    title = "Auto Fetch Metadata",
                    subtitle = "Automatically fetch link previews",
                    checked = uiState.autoFetchMetadata,
                    onCheckedChange = { viewModel.toggleAutoFetchMetadata() }
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Default.FileDownload,
                    title = "Export Data",
                    subtitle = "Backup links to JSON",
                    onClick = {
                        val timeStamp =
                            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        exportLauncher.launch("vedlink_backup_$timeStamp.json")
                    }
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Default.FileUpload,
                    title = "Import Data",
                    subtitle = "Restore links from backup",
                    onClick = {
                        importLauncher.launch(arrayOf("application/json"))
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // About Section
            SettingsSectionLabel("About")
            SettingsCard {
                SettingsNavRow(
                    icon = Icons.Default.Info,
                    title = "About VedLink",
                    subtitle = "Version, Credits & More",
                    onClick = onNavigateToAbout
                )
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}