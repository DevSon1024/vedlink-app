package com.devson.vedlink.ui.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun VedLinkTheme(
    themeMode: Int = 0, // 0=System, 1=Light, 2=Dark
    colorSchemeIndex: Int = 0,
    useDynamicColor: Boolean = false,
    useAmoledMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemInDarkTheme = isSystemInDarkTheme()

    // Determine if dark theme should be used
    val darkTheme = when (themeMode) {
        1 -> false // Light mode
        2 -> true  // Dark mode
        else -> systemInDarkTheme // System default
    }

    val context = LocalContext.current

    val colorScheme = when {
        // Dynamic color (Android 12+)
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        // Custom color schemes
        else -> {
            val selectedScheme = availableColorSchemes.getOrNull(colorSchemeIndex)
                ?: availableColorSchemes[0]

            if (darkTheme) {
                createDarkColorScheme(selectedScheme, useAmoledMode)
            } else {
                createLightColorScheme(selectedScheme)
            }
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bgColor = if (useAmoledMode && darkTheme) {
                Color.Black
            } else {
                colorScheme.background
            }

            window.statusBarColor = bgColor.toArgb()
            window.navigationBarColor = bgColor.toArgb()

            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            windowInsetsController.isAppearanceLightStatusBars = !darkTheme
            windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
private fun createLightColorScheme(scheme: AppColorScheme): ColorScheme {
    return lightColorScheme(
        primary = scheme.lightPrimary,
        onPrimary = scheme.lightOnPrimary,
        primaryContainer = scheme.lightPrimaryContainer,
        onPrimaryContainer = scheme.lightOnPrimaryContainer,
        secondary = scheme.lightPrimary.copy(alpha = 0.7f),
        onSecondary = scheme.lightOnPrimary,
        secondaryContainer = scheme.lightPrimaryContainer.copy(alpha = 0.7f),
        onSecondaryContainer = scheme.lightOnPrimaryContainer,
        tertiary = scheme.lightPrimary.copy(alpha = 0.5f),
        onTertiary = scheme.lightOnPrimary,
        tertiaryContainer = scheme.lightPrimaryContainer.copy(alpha = 0.5f),
        onTertiaryContainer = scheme.lightOnPrimaryContainer,
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFFFBFF),
        onBackground = Color(0xFF1C1B1F),
        surface = Color(0xFFFFFBFF),
        onSurface = Color(0xFF1C1B1F),
        surfaceVariant = Color(0xFFE7E0EC),
        onSurfaceVariant = Color(0xFF49454F),
        outline = Color(0xFF79747E),
        outlineVariant = Color(0xFFCAC4D0)
    )
}

@Composable
private fun createDarkColorScheme(scheme: AppColorScheme, useAmoled: Boolean): ColorScheme {
    val backgroundColor = if (useAmoled) Color.Black else Color(0xFF1C1B1F)
    val surfaceColor = if (useAmoled) Color(0xFF0A0A0A) else Color(0xFF1C1B1F)

    return darkColorScheme(
        primary = scheme.darkPrimary,
        onPrimary = scheme.darkOnPrimary,
        primaryContainer = scheme.darkPrimaryContainer,
        onPrimaryContainer = scheme.darkOnPrimaryContainer,
        secondary = scheme.darkPrimary.copy(alpha = 0.7f),
        onSecondary = scheme.darkOnPrimary,
        secondaryContainer = scheme.darkPrimaryContainer.copy(alpha = 0.7f),
        onSecondaryContainer = scheme.darkOnPrimaryContainer,
        tertiary = scheme.darkPrimary.copy(alpha = 0.5f),
        onTertiary = scheme.darkOnPrimary,
        tertiaryContainer = scheme.darkPrimaryContainer.copy(alpha = 0.5f),
        onTertiaryContainer = scheme.darkOnPrimaryContainer,
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = backgroundColor,
        onBackground = Color(0xFFE6E1E5),
        surface = surfaceColor,
        onSurface = Color(0xFFE6E1E5),
        surfaceVariant = if (useAmoled) Color(0xFF1A1A1A) else Color(0xFF49454F),
        onSurfaceVariant = Color(0xFFCAC4D0),
        outline = Color(0xFF938F99),
        outlineVariant = Color(0xFF49454F)
    )
}