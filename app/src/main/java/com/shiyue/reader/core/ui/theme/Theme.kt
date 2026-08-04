package com.shiyue.reader.core.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = DeepInkGreen,
    onPrimary = WarmIvorySurface,
    primaryContainer = DeepInkGreenContainer,
    onPrimaryContainer = DeepInkGreen,
    secondary = CopperGold,
    secondaryContainer = CopperGoldContainer,
    onSecondaryContainer = DeepInkGreen,
    tertiary = CopperGold,
    tertiaryContainer = CopperGoldContainer,
    onTertiaryContainer = DeepInkGreen,
    background = WarmIvory,
    onBackground = DeepInkGreen,
    surface = WarmIvorySurface,
    onSurface = DeepInkGreen,
    surfaceVariant = DeepInkGreenContainer,
    onSurfaceVariant = ColorTokens.LightOnSurfaceVariant,
)

private val DarkColors = darkColorScheme(
    primary = PaleGreen,
    onPrimary = DeepInkGreen,
    primaryContainer = DeepInkGreen,
    onPrimaryContainer = PaleGreen,
    secondary = PaleCopper,
    secondaryContainer = ColorTokens.DarkCopperContainer,
    onSecondaryContainer = WarmOnDark,
    tertiary = PaleCopper,
    tertiaryContainer = ColorTokens.DarkCopperContainer,
    onTertiaryContainer = WarmOnDark,
    background = WarmBlack,
    onBackground = WarmOnDark,
    surface = WarmDarkSurface,
    onSurface = WarmOnDark,
    surfaceVariant = ColorTokens.DarkSurfaceVariant,
    onSurfaceVariant = ColorTokens.DarkOnSurfaceVariant,
)

private object ColorTokens {
    val LightOnSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF46534D)
    val DarkCopperContainer = androidx.compose.ui.graphics.Color(0xFF5A4026)
    val DarkSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF303832)
    val DarkOnSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFCAD4CC)
}

@Composable
fun ShiyueTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ShiyueTypography,
        content = content,
    )
}
