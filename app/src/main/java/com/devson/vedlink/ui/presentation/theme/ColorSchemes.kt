package com.devson.vedlink.ui.presentation.theme

import androidx.compose.ui.graphics.Color

// Blue Theme (Default)
val BlueLightPrimary = Color(0xFF0061A4)
val BlueLightOnPrimary = Color(0xFFFFFFFF)
val BlueLightPrimaryContainer = Color(0xFFD1E4FF)
val BlueLightOnPrimaryContainer = Color(0xFF001D36)

val BlueDarkPrimary = Color(0xFF9ECAFF)
val BlueDarkOnPrimary = Color(0xFF003258)
val BlueDarkPrimaryContainer = Color(0xFF00497D)
val BlueDarkOnPrimaryContainer = Color(0xFFD1E4FF)

// Green Theme
val GreenLightPrimary = Color(0xFF006E1C)
val GreenLightOnPrimary = Color(0xFFFFFFFF)
val GreenLightPrimaryContainer = Color(0xFF96F990)
val GreenLightOnPrimaryContainer = Color(0xFF002204)

val GreenDarkPrimary = Color(0xFF7ADC77)
val GreenDarkOnPrimary = Color(0xFF00390A)
val GreenDarkPrimaryContainer = Color(0xFF005313)
val GreenDarkOnPrimaryContainer = Color(0xFF96F990)

// Purple Theme
val PurpleLightPrimary = Color(0xFF6750A4)
val PurpleLightOnPrimary = Color(0xFFFFFFFF)
val PurpleLightPrimaryContainer = Color(0xFFEADDFF)
val PurpleLightOnPrimaryContainer = Color(0xFF21005D)

val PurpleDarkPrimary = Color(0xFFD0BCFF)
val PurpleDarkOnPrimary = Color(0xFF381E72)
val PurpleDarkPrimaryContainer = Color(0xFF4F378B)
val PurpleDarkOnPrimaryContainer = Color(0xFFEADDFF)

// Red Theme
val RedLightPrimary = Color(0xFFBA1A1A)
val RedLightOnPrimary = Color(0xFFFFFFFF)
val RedLightPrimaryContainer = Color(0xFFFFDAD6)
val RedLightOnPrimaryContainer = Color(0xFF410002)

val RedDarkPrimary = Color(0xFFFFB4AB)
val RedDarkOnPrimary = Color(0xFF690005)
val RedDarkPrimaryContainer = Color(0xFF93000A)
val RedDarkOnPrimaryContainer = Color(0xFFFFDAD6)

// Orange Theme
val OrangeLightPrimary = Color(0xFF8B5000)
val OrangeLightOnPrimary = Color(0xFFFFFFFF)
val OrangeLightPrimaryContainer = Color(0xFFFFDCC2)
val OrangeLightOnPrimaryContainer = Color(0xFF2D1600)

val OrangeDarkPrimary = Color(0xFFFFB870)
val OrangeDarkOnPrimary = Color(0xFF4A2800)
val OrangeDarkPrimaryContainer = Color(0xFF6A3C00)
val OrangeDarkOnPrimaryContainer = Color(0xFFFFDCC2)

// Yellow Theme
val YellowLightPrimary = Color(0xFF6A5F00)
val YellowLightOnPrimary = Color(0xFFFFFFFF)
val YellowLightPrimaryContainer = Color(0xFFF9E287)
val YellowLightOnPrimaryContainer = Color(0xFF201C00)

val YellowDarkPrimary = Color(0xFFDCC66E)
val YellowDarkOnPrimary = Color(0xFF373100)
val YellowDarkPrimaryContainer = Color(0xFF504800)
val YellowDarkOnPrimaryContainer = Color(0xFFF9E287)

// Pink Theme
val PinkLightPrimary = Color(0xFFA23E93)
val PinkLightOnPrimary = Color(0xFFFFFFFF)
val PinkLightPrimaryContainer = Color(0xFFFFD7F2)
val PinkLightOnPrimaryContainer = Color(0xFF3A0034)

val PinkDarkPrimary = Color(0xFFFFABEC)
val PinkDarkOnPrimary = Color(0xFF5F1156)
val PinkDarkPrimaryContainer = Color(0xFF832975)
val PinkDarkOnPrimaryContainer = Color(0xFFFFD7F2)

// Teal Theme
val TealLightPrimary = Color(0xFF006A67)
val TealLightOnPrimary = Color(0xFFFFFFFF)
val TealLightPrimaryContainer = Color(0xFF6FF7F2)
val TealLightOnPrimaryContainer = Color(0xFF00201F)

val TealDarkPrimary = Color(0xFF4CDAD5)
val TealDarkOnPrimary = Color(0xFF003735)
val TealDarkPrimaryContainer = Color(0xFF00504E)
val TealDarkOnPrimaryContainer = Color(0xFF6FF7F2)

data class AppColorScheme(
    val name: String,
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightPrimaryContainer: Color,
    val lightOnPrimaryContainer: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkPrimaryContainer: Color,
    val darkOnPrimaryContainer: Color
)

val availableColorSchemes = listOf(
    AppColorScheme(
        name = "Blue",
        lightPrimary = BlueLightPrimary,
        lightOnPrimary = BlueLightOnPrimary,
        lightPrimaryContainer = BlueLightPrimaryContainer,
        lightOnPrimaryContainer = BlueLightOnPrimaryContainer,
        darkPrimary = BlueDarkPrimary,
        darkOnPrimary = BlueDarkOnPrimary,
        darkPrimaryContainer = BlueDarkPrimaryContainer,
        darkOnPrimaryContainer = BlueDarkOnPrimaryContainer
    ),
    AppColorScheme(
        name = "Green",
        lightPrimary = GreenLightPrimary,
        lightOnPrimary = GreenLightOnPrimary,
        lightPrimaryContainer = GreenLightPrimaryContainer,
        lightOnPrimaryContainer = GreenLightOnPrimaryContainer,
        darkPrimary = GreenDarkPrimary,
        darkOnPrimary = GreenDarkOnPrimary,
        darkPrimaryContainer = GreenDarkPrimaryContainer,
        darkOnPrimaryContainer = GreenDarkOnPrimaryContainer
    ),
    AppColorScheme(
        name = "Purple",
        lightPrimary = PurpleLightPrimary,
        lightOnPrimary = PurpleLightOnPrimary,
        lightPrimaryContainer = PurpleLightPrimaryContainer,
        lightOnPrimaryContainer = PurpleLightOnPrimaryContainer,
        darkPrimary = PurpleDarkPrimary,
        darkOnPrimary = PurpleDarkOnPrimary,
        darkPrimaryContainer = PurpleDarkPrimaryContainer,
        darkOnPrimaryContainer = PurpleDarkOnPrimaryContainer
    ),
    AppColorScheme(
        name = "Red",
        lightPrimary = RedLightPrimary,
        lightOnPrimary = RedLightOnPrimary,
        lightPrimaryContainer = RedLightPrimaryContainer,
        lightOnPrimaryContainer = RedLightOnPrimaryContainer,
        darkPrimary = RedDarkPrimary,
        darkOnPrimary = RedDarkOnPrimary,
        darkPrimaryContainer = RedDarkPrimaryContainer,
        darkOnPrimaryContainer = RedDarkOnPrimaryContainer
    ),
    AppColorScheme(
        name = "Orange",
        lightPrimary = OrangeLightPrimary,
        lightOnPrimary = OrangeLightOnPrimary,
        lightPrimaryContainer = OrangeLightPrimaryContainer,
        lightOnPrimaryContainer = OrangeLightOnPrimaryContainer,
        darkPrimary = OrangeDarkPrimary,
        darkOnPrimary = OrangeDarkOnPrimary,
        darkPrimaryContainer = OrangeDarkPrimaryContainer,
        darkOnPrimaryContainer = OrangeDarkOnPrimaryContainer
    ),
    AppColorScheme(
        name = "Yellow",
        lightPrimary = YellowLightPrimary,
        lightOnPrimary = YellowLightOnPrimary,
        lightPrimaryContainer = YellowLightPrimaryContainer,
        lightOnPrimaryContainer = YellowLightOnPrimaryContainer,
        darkPrimary = YellowDarkPrimary,
        darkOnPrimary = YellowDarkOnPrimary,
        darkPrimaryContainer = YellowDarkPrimaryContainer,
        darkOnPrimaryContainer = YellowDarkOnPrimaryContainer
    ),
    AppColorScheme(
        name = "Pink",
        lightPrimary = PinkLightPrimary,
        lightOnPrimary = PinkLightOnPrimary,
        lightPrimaryContainer = PinkLightPrimaryContainer,
        lightOnPrimaryContainer = PinkLightOnPrimaryContainer,
        darkPrimary = PinkDarkPrimary,
        darkOnPrimary = PinkDarkOnPrimary,
        darkPrimaryContainer = PinkDarkPrimaryContainer,
        darkOnPrimaryContainer = PinkDarkOnPrimaryContainer
    ),
    AppColorScheme(
        name = "Teal",
        lightPrimary = TealLightPrimary,
        lightOnPrimary = TealLightOnPrimary,
        lightPrimaryContainer = TealLightPrimaryContainer,
        lightOnPrimaryContainer = TealLightOnPrimaryContainer,
        darkPrimary = TealDarkPrimary,
        darkOnPrimary = TealDarkOnPrimary,
        darkPrimaryContainer = TealDarkPrimaryContainer,
        darkOnPrimaryContainer = TealDarkOnPrimaryContainer
    )
)