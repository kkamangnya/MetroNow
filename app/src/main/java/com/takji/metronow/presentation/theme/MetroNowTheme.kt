package com.takji.metronow.presentation.theme

import android.app.Activity
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
import com.takji.metronow.domain.model.AppTheme

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF71E69B),
    onPrimary = Color(0xFF062112),
    secondary = Color(0xFF9FCDB0),
    background = Color(0xFF090B0E),
    onBackground = Color(0xFFF1F5F2),
    surface = Color(0xFF11151A),
    onSurface = Color(0xFFF1F5F2),
    surfaceVariant = Color(0xFF1A2026),
    onSurfaceVariant = Color(0xFFABB6AF),
    error = Color(0xFFFFB4AB),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF006D3B),
    onPrimary = Color.White,
    secondary = Color(0xFF426552),
    background = Color(0xFFF7F9F7),
    onBackground = Color(0xFF171D19),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171D19),
    surfaceVariant = Color(0xFFE1E9E3),
    onSurfaceVariant = Color(0xFF414943),
)

@Composable
fun MetroNowTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val dark = when (theme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = MetroNowTypography,
        content = content,
    )
}

@Composable
fun MetroNowSystemBars(theme: AppTheme) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (theme) {
        AppTheme.SYSTEM -> systemDark
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = (if (dark) DarkScheme.background else LightScheme.background).toArgb()
            window.navigationBarColor = (if (dark) DarkScheme.background else LightScheme.background).toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
}
