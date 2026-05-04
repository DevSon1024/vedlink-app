package com.devson.vedlink.ui.presentation.screens

import android.app.Activity
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.devson.vedlink.ui.presentation.components.SettingsCard
import com.devson.vedlink.ui.presentation.components.SettingsDivider
import com.devson.vedlink.ui.presentation.components.SettingsSectionLabel
import com.devson.vedlink.ui.presentation.components.SettingsSwitchItem
import com.devson.vedlink.ui.viewmodel.CustomizeHomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeHomeScreen(
    onNavigateBack: () -> Unit,
    viewModel: CustomizeHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Customize Home",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
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

            // Info chip (Pixchive Style)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Toggle which sections show up on your Home screen. Changes take effect immediately.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Section header
            SettingsSectionLabel("Home Sections")

            SettingsCard {
                SettingsSwitchItem(
                    icon = Icons.Default.BarChart,
                    title = "Stats",
                    subtitle = "Show total saved links and favourites",
                    checked = uiState.showStats,
                    onCheckedChange = { viewModel.toggleShowStats() }
                )
                SettingsDivider()
                SettingsSwitchItem(
                    icon = Icons.Default.Bolt,
                    title = "Quick Actions",
                    subtitle = "Show Favorites, Folders and Search shortcuts",
                    checked = uiState.showQuickActions,
                    onCheckedChange = { viewModel.toggleShowQuickActions() }
                )
                SettingsDivider()
                SettingsSwitchItem(
                    icon = Icons.Default.History,
                    title = "Recently Saved",
                    subtitle = "Show Jump Back In carousel of recent links",
                    checked = uiState.showRecentLinks,
                    onCheckedChange = { viewModel.toggleShowRecentLinks() }
                )
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}
