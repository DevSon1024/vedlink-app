package com.devson.vedlink.ui.presentation.screens.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

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
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(bottom = 16.dp)
        ) {
            // App Info Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "VedLink",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Link Manager & Downloader",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Version $versionName ($versionCode)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }

            // Repository Section
            AboutSection(title = "Open Source")

            AboutItem(
                icon = Icons.Default.Description,
                title = "README",
                subtitle = "View project documentation",
                onClick = {
                    openUrl(context, "https://github.com/DevSon1024/vedlink-app/blob/main/README.md")
                }
            )

            AboutItem(
                icon = Icons.Default.NewReleases,
                title = "Latest Release",
                subtitle = "Check for updates",
                onClick = {
                    openUrl(context, "https://github.com/DevSon1024/vedlink-app/releases/latest")
                }
            )

            AboutItem(
                icon = Icons.Default.Send,
                title = "Telegram Channel",
                subtitle = "Join our community",
                onClick = {
                    openUrl(context, "https://t.me/vedlink_app")
                }
            )

            // Legal Section
            AboutSection(title = "Legal")

            AboutItem(
                icon = Icons.Default.Policy,
                title = "Privacy Policy",
                subtitle = "How we handle your data",
                onClick = {
                    openUrl(context, "https://sites.google.com/view/vedlink-privacy-policy-page")
                }
            )

            // Credits Section
            AboutSection(title = "Credits")

            CreditItem(
                title = "Jetpack Compose",
                subtitle = "Modern Android UI toolkit",
                url = "https://developer.android.com/jetpack/compose",
                context = context
            )

            CreditItem(
                title = "Kotlin",
                subtitle = "Programming language",
                url = "https://kotlinlang.org/",
                context = context
            )

            CreditItem(
                title = "Material Design 3",
                subtitle = "Design system",
                url = "https://m3.material.io/",
                context = context
            )

            CreditItem(
                title = "Hilt",
                subtitle = "Dependency injection",
                url = "https://dagger.dev/hilt/",
                context = context
            )

            CreditItem(
                title = "Room Database",
                subtitle = "Local data persistence",
                url = "https://developer.android.com/training/data-storage/room",
                context = context
            )

            CreditItem(
                title = "OkHttp",
                subtitle = "HTTP client",
                url = "https://square.github.io/okhttp/",
                context = context
            )

            CreditItem(
                title = "Jsoup",
                subtitle = "HTML parsing & web scraping",
                url = "https://jsoup.org/",
                context = context
            )

            CreditItem(
                title = "Coil",
                subtitle = "Image loading library",
                url = "https://coil-kt.github.io/coil/",
                context = context
            )

            CreditItem(
                title = "Gson",
                subtitle = "JSON serialization/deserialization",
                url = "https://github.com/google/gson",
                context = context
            )

            CreditItem(
                title = "Kotlin Coroutines",
                subtitle = "Asynchronous programming",
                url = "https://kotlinlang.org/docs/coroutines-overview.html",
                context = context
            )

            // Device Info Section
            AboutSection(title = "Device Information")

            DeviceInfoCard(versionName = versionName, versionCode = versionCode)

            // Footer
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Made with ❤️ by DevSon1024",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
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
                imageVector = Icons.Default.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CreditItem(
    title: String,
    subtitle: String,
    url: String,
    context: Context
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { openUrl(context, url) },
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
                    color = MaterialTheme.colorScheme.onSurface
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DeviceInfoCard(versionName: String, versionCode: String) {
    val supportedAbis = Build.SUPPORTED_ABIS.joinToString(", ")
    val manufacturer = Build.MANUFACTURER ?: "Unknown"
    val model = Build.MODEL ?: "Unknown"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            DeviceInfoRow("App Version", "$versionName ($versionCode)")
            Spacer(modifier = Modifier.height(8.dp))
            DeviceInfoRow("Android Version", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            Spacer(modifier = Modifier.height(8.dp))
            DeviceInfoRow("Device", "$manufacturer $model")
            Spacer(modifier = Modifier.height(8.dp))
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