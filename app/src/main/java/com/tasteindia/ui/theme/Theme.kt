package com.tasteindia.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// A warm, spice-derived palette. Fixed rather than dynamic so the app looks the
// same on every device in the demo recording; contrast ratios were checked in
// both schemes.
private val Turmeric = Color(0xFFB4691A)
private val TurmericLight = Color(0xFFFFDDB5)
private val Tamarind = Color(0xFF5B4130)
private val Cardamom = Color(0xFF4A6547)
private val CardamomLight = Color(0xFFCCEBC5)
private val Chilli = Color(0xFFA33A2C)

private val LightScheme = lightColorScheme(
    primary = Turmeric,
    onPrimary = Color.White,
    primaryContainer = TurmericLight,
    onPrimaryContainer = Color(0xFF2C1600),
    secondary = Cardamom,
    onSecondary = Color.White,
    secondaryContainer = CardamomLight,
    onSecondaryContainer = Color(0xFF0A2008),
    tertiary = Tamarind,
    onTertiary = Color.White,
    error = Chilli,
    onError = Color.White,
    background = Color(0xFFFFF8F3),
    onBackground = Color(0xFF211A14),
    surface = Color(0xFFFFF8F3),
    onSurface = Color(0xFF211A14),
    surfaceVariant = Color(0xFFF2DFD1),
    onSurfaceVariant = Color(0xFF51443A),
    outline = Color(0xFF837469),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFFFB870),
    onPrimary = Color(0xFF4A2800),
    primaryContainer = Color(0xFF6A3B00),
    onPrimaryContainer = TurmericLight,
    secondary = Color(0xFFB1CFAA),
    onSecondary = Color(0xFF1D361C),
    secondaryContainer = Color(0xFF334D31),
    onSecondaryContainer = CardamomLight,
    tertiary = Color(0xFFDCC2AC),
    onTertiary = Color(0xFF3E2D20),
    error = Color(0xFFFFB4A9),
    onError = Color(0xFF680003),
    background = Color(0xFF191210),
    onBackground = Color(0xFFEDE0DA),
    surface = Color(0xFF191210),
    onSurface = Color(0xFFEDE0DA),
    surfaceVariant = Color(0xFF51443A),
    onSurfaceVariant = Color(0xFFD5C3B5),
    outline = Color(0xFF9E8E82),
)

@Composable
fun TasteIndiaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkScheme else LightScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
