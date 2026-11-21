package com.example.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils

object IslandColors {
    // Core Dynamic Island & iOS 26 Liquid Glass palette
    val PillBlack = Color(0xFF000000)
    val CardSurfaceBlack = Color(0xFF08090C)
    val CardSurfaceSolid = Color(0xFF13141A)
    val CardSurfaceElevated = Color(0xFF1D1E27)
    val GlassTintLight = Color(0x20FFFFFF)
    val GlassTintDark = Color(0xC40A0B10)
    val GlassSpecularBorder = Color(0x5CE0EBFF)
    val GlassSpecularTop = Color(0x88FFFFFF)
    val GlassSpecularMid = Color(0x30FFFFFF)
    val GlassSpecularBottom = Color(0x16FFFFFF)
    val GlassSubtleBorder = Color(0x28FFFFFF)

    // Accent & Semantic Activity colors
    val AccentOrange = Color(0xFFFF9F0A) // Timer activity color
    val AccentCyan = Color(0xFF64D2FF)   // Alerts / OCR / Info
    val AccentGreen = Color(0xFF30D158)  // Charging / Vault Ready
    val AccentRed = Color(0xFFFF3B30)    // Calibration camera ring & recording
    val AccentPurple = Color(0xFFBF5AF2) // Sleep / Media
    val SelectionBlue = Color(0xFF0A84FF) // Custom iOS OCR Selection handles & loupe
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB4B8C0)
    val TextTertiary = Color(0xFF7B808A)

    // Concentric radii scale (Apple HIG / WWDC23)
    val ExpandedOuterRadius = 44.dp
    val ExpandedInnerSheetRadius = 32.dp
    val CardRadius = 28.dp
    val ChipRadius = 8.dp
    val ScreenHorizontalMargin = 11.dp

    // Exact iOS Calculator tokens
    val CalcBackground = Color(0xFF000000)
    val CalcDigitBg = Color(0xFF333333)
    val CalcDigitText = Color(0xFFFFFFFF)
    val CalcFunctionBg = Color(0xFFA5A5A5)
    val CalcFunctionText = Color(0xFF000000)
    val CalcOperatorBg = Color(0xFFFF9F0A)
    val CalcOperatorText = Color(0xFFFFFFFF)
    val CalcOperatorActiveBg = Color(0xFFD4D4D2)
    val CalcOperatorActiveText = Color(0xFFFF9F0A)

    // App Settings surface tokens
    val AppBgDark = Color(0xFF08090D)
    val AppSurfaceDark = Color(0xFF13141C)
    val AppSurfaceVariantDark = Color(0xFF1D1F2A)
    val AppBgLight = Color(0xFFF5F6FA)
    val AppSurfaceLight = Color(0xFFFFFFFF)
    val AppSurfaceVariantLight = Color(0xFFEBEEF5)

    fun extractMaterialYouAccent(context: Context, fallback: Color = AccentOrange): Color {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return fallback
        return runCatching {
            val resId = context.resources.getIdentifier("system_accent1_400", "color", "android")
            if (resId != 0) {
                val raw = context.getColor(resId)
                val hsl = FloatArray(3)
                ColorUtils.colorToHSL(raw, hsl)
                hsl[1] = hsl[1].coerceAtLeast(0.55f)
                hsl[2] = hsl[2].coerceIn(0.50f, 0.68f)
                Color(ColorUtils.HSLToColor(hsl))
            } else {
                fallback
            }
        }.getOrDefault(fallback)
    }

    fun blendGlassBase(materialYouColor: Color, density: Float): Brush {
        val alphaTop = (0.56f + density * 0.28f).coerceIn(0.48f, 0.88f)
        val alphaBot = (alphaTop + 0.10f).coerceAtMost(0.94f)
        val tintedTop = Color(
            ColorUtils.blendARGB(Color(0xFF161822).toArgb(), materialYouColor.toArgb(), 0.16f)
        ).copy(alpha = alphaTop)
        val tintedBottom = Color(
            ColorUtils.blendARGB(Color(0xFF090A0F).toArgb(), materialYouColor.toArgb(), 0.08f)
        ).copy(alpha = alphaBot)
        return Brush.verticalGradient(colors = listOf(tintedTop, tintedBottom))
    }

    fun liquidBorderBrush(useGlass: Boolean, isHighlighted: Boolean, accentColor: Color): Brush {
        return when {
            isHighlighted -> Brush.linearGradient(
                colors = listOf(accentColor, accentColor.copy(alpha = 0.55f), accentColor)
            )
            useGlass -> Brush.linearGradient(
                colors = listOf(
                    GlassSpecularTop,
                    accentColor.copy(alpha = 0.28f),
                    GlassSpecularBottom
                ),
                start = Offset(0f, 0f),
                end = Offset(480f, 620f)
            )
            else -> Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.24f), Color.White.copy(alpha = 0.08f))
            )
        }
    }
}

