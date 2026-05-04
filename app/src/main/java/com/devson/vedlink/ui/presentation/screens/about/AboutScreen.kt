package com.devson.vedlink.ui.presentation.screens.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.devson.vedlink.ui.presentation.components.SettingsCard
import com.devson.vedlink.ui.presentation.components.SettingsDivider
import com.devson.vedlink.ui.presentation.components.SettingsNavRow
import com.devson.vedlink.ui.presentation.components.SettingsSectionLabel
import com.devson.vedlink.ui.presentation.screens.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val isDark by settingsViewModel.isDarkTheme.collectAsState()

    // Status bar color handling
    val view = LocalView.current
    if (!view.isInEditMode) {
        val backgroundColor = MaterialTheme.colorScheme.background
        val darkTheme = isDark ?: isSystemInDarkTheme()
        SideEffect {
            val window = (view.context as android.app.Activity).window
            window.statusBarColor = backgroundColor.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
        }
    }

    // Get version info from context
    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
    }

    val versionCode = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toString()
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toString()
        }
    } catch (e: Exception) {
        "1"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleLarge
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
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(bottom = 16.dp)
        ) {
            // App Info Header (Pixchive Style)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primary,
                        tonalElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "VedLink",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Smart Link Manager",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Version $versionName",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Repository Section
            SettingsSectionLabel("Open Source")
            SettingsCard {
                SettingsNavRow(
                    icon = Icons.Default.Description,
                    title = "README",
                    subtitle = "View project documentation",
                    onClick = {
                        openUrl(context, "https://github.com/DevSon1024/vedlink-app/blob/main/README.md")
                    }
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Default.NewReleases,
                    title = "Latest Release",
                    subtitle = "Check for updates",
                    onClick = {
                        openUrl(context, "https://github.com/DevSon1024/vedlink-app/releases/latest")
                    }
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.AutoMirrored.Filled.Send,
                    title = "Telegram Channel",
                    subtitle = "Join our community",
                    onClick = {
                        openUrl(context, "https://t.me/vedlink_app")
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Legal Section
            SettingsSectionLabel("Legal")
            SettingsCard {
                SettingsNavRow(
                    icon = Icons.Default.Policy,
                    title = "Privacy Policy",
                    subtitle = "How we handle your data",
                    onClick = {
                        openUrl(context, "https://sites.google.com/view/vedlink-privacy-policy-page")
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Credits Section
            SettingsSectionLabel("Credits")
            SettingsCard {
                CreditItem("Jetpack Compose", "Modern Android UI toolkit", "https://developer.android.com/jetpack/compose", context)
                SettingsDivider()
                CreditItem("Kotlin", "Programming language", "https://kotlinlang.org/", context)
                SettingsDivider()
                CreditItem("Material Design 3", "Design system", "https://m3.material.io/", context)
                SettingsDivider()
                CreditItem("Hilt", "Dependency injection", "https://dagger.dev/hilt/", context)
                SettingsDivider()
                CreditItem("Room Database", "Local data persistence", "https://developer.android.com/training/data-storage/room", context)
                SettingsDivider()
                CreditItem("OkHttp", "HTTP client", "https://square.github.io/okhttp/", context)
                SettingsDivider()
                CreditItem("Jsoup", "HTML parsing & web scraping", "https://jsoup.org/", context)
                SettingsDivider()
                CreditItem("Coil", "Image loading library", "https://coil-kt.github.io/coil/", context)
                SettingsDivider()
                CreditItem("Gson", "JSON serialization/deserialization", "https://github.com/google/gson", context)
                SettingsDivider()
                CreditItem("Kotlin Coroutines", "Asynchronous programming", "https://kotlinlang.org/docs/coroutines-overview.html", context)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Device Info Section
            SettingsSectionLabel("Device Information")
            DeviceInfoCard(versionName = versionName, versionCode = versionCode)

            // Footer
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Made with ❤️ by DevSon1024",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
fun AboutSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp)
    )
}

@Composable
fun AboutItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = tint
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CreditItem(
    title: String,
    subtitle: String,
    url: String,
    context: Context
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { openUrl(context, url) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun DeviceInfoCard(versionName: String, versionCode: String) {
    val supportedAbis = Build.SUPPORTED_ABIS.joinToString(", ")
    val manufacturer = Build.MANUFACTURER ?: "Unknown"
    val model = Build.MODEL ?: "Unknown"

    SettingsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            DeviceInfoRow("App Version", "$versionName ($versionCode)")
            SettingsDivider(modifier = Modifier.padding(vertical = 12.dp))
            DeviceInfoRow("Android Version", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            SettingsDivider(modifier = Modifier.padding(vertical = 12.dp))
            DeviceInfoRow("Device", "$manufacturer $model")
            SettingsDivider(modifier = Modifier.padding(vertical = 12.dp))
            DeviceInfoRow("Supported ABIs", supportedAbis)
        }
    }
}

@Composable
fun DeviceInfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}