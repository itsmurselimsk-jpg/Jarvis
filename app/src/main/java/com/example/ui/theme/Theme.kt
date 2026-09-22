package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisColorScheme =
  darkColorScheme(
    primary = ArcCyan,
    onPrimary = VoidBlack,
    primaryContainer = SurfaceElevated,
    onPrimaryContainer = ArcCyan,
    secondary = ArcBlue,
    onSecondary = Color.White,
    secondaryContainer = SurfaceDark,
    onSecondaryContainer = TextCyan,
    tertiary = StarkGold,
    onTertiary = VoidBlack,
    tertiaryContainer = SurfaceElevated,
    onTertiaryContainer = StarkGold,
    background = VoidBlack,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    error = WarningRed,
    onError = Color.White
  )

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = JarvisColorScheme,
    typography = Typography,
    content = content
  )
}

