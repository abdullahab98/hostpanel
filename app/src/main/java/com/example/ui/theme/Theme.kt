package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    secondary = CyberBlue,
    tertiary = CyberGreen,
    background = CyberBlack,
    surface = CyberDarkGray,
    onPrimary = Color(0xFF020813),
    onSecondary = Color.White,
    onTertiary = Color(0xFF020813),
    onBackground = CyberTextPrimary,
    onSurface = CyberTextPrimary,
    surfaceVariant = CyberSlateCard,
    onSurfaceVariant = CyberTextSecondary,
    error = CyberError,
    outline = CyberSlateBorder
)

private val LightColorScheme = lightColorScheme(
    primary = CyberBlue,
    secondary = CyberCyan,
    tertiary = CyberGreen,
    background = Color(0xFFF4F6F9),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color(0xFF020813),
    onTertiary = Color.White,
    onBackground = Color(0xFF121824),
    onSurface = Color(0xFF121824),
    surfaceVariant = Color(0xFFECEFF1),
    onSurfaceVariant = Color(0xFF546E7A),
    error = Color(0xFFD32F2F),
    outline = Color(0xFFCFD8DC)
)

@Composable
fun MyApplicationTheme(
    themeMode: String = "dark",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            // Transparent status bar — edge-to-edge content flows under it
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
