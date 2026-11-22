package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val IslandDarkColorScheme = darkColorScheme(
    primary = IslandColors.AccentOrange,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF332100),
    onPrimaryContainer = Color(0xFFFFD8A8),
    secondary = IslandColors.AccentCyan,
    onSecondary = Color.Black,
    tertiary = IslandColors.AccentGreen,
    background = IslandColors.SurfaceContainerLowest,
    onBackground = IslandColors.TextPrimary,
    surface = IslandColors.SurfaceContainerLow,
    onSurface = IslandColors.TextPrimary,
    surfaceVariant = IslandColors.SurfaceContainer,
    onSurfaceVariant = IslandColors.TextSecondary,
    surfaceContainerLowest = IslandColors.SurfaceContainerLowest,
    surfaceContainerLow = IslandColors.SurfaceContainerLow,
    surfaceContainer = IslandColors.SurfaceContainer,
    surfaceContainerHigh = IslandColors.SurfaceContainerHigh,
    surfaceContainerHighest = IslandColors.SurfaceContainerHighest,
    error = IslandColors.AccentRed,
    onError = Color.White,
    outline = Color(0xFF2D2E38)
)

private val IslandLightColorScheme = lightColorScheme(
    primary = Color(0xFFD97706),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEDD5),
    onPrimaryContainer = Color(0xFF451A03),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    tertiary = Color(0xFF16A34A),
    background = IslandColors.AppBgLight,
    onBackground = Color(0xFF111827),
    surface = IslandColors.AppSurfaceLight,
    onSurface = Color(0xFF111827),
    surfaceVariant = IslandColors.AppSurfaceVariantLight,
    onSurfaceVariant = Color(0xFF4B5563),
    error = Color(0xFFDC2626),
    onError = Color.White,
    outline = Color(0xFFD1D5DB)
)

val IslandShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun DynamicIslandTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    accentOverride: Color? = null,
    content: @Composable () -> Unit
) {
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> IslandDarkColorScheme
        else -> IslandLightColorScheme
    }

    val colorScheme = if (accentOverride != null) {
        baseScheme.copy(primary = accentOverride)
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = IslandShapes,
        typography = DynamicIslandTypography,
        content = content
    )
}
