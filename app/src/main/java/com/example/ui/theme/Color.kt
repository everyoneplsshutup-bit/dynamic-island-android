package com.example.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.example.system.PillStyle

object IslandColors {
    // Core Dynamic Island & iOS 26 Liquid Glass palette
    val PillBlack = Color(0xFF000000)
    val CardSurfaceBlack = Color(0xFF08090C)
    val CardSurfaceSolid = Color(0xFF13141A)
    val CardSurfaceElevated = Color(0xFF1D1E27)
    val GlassTintLight = Color(0x20FFFFFF)
    val GlassTintDark = Color(0xC40A0B10)
    val GlassSpecularBorder = Color(0x66E4EEFF)
    val GlassSpecularTop = Color(0x96FFFFFF)
    val GlassSpecularMid = Color(0x36FFFFFF)
    val GlassSpecularBottom = Color(0x18FFFFFF)
    val GlassSubtleBorder = Color(0x2CFFFFFF)

    // M3E Surface Container Ladder
    val SurfaceContainerLowest = Color(0xFF07080B)
    val SurfaceContainerLow = Color(0xFF101218)
    val SurfaceContainer = Color(0xFF171922)
    val SurfaceContainerHigh = Color(0xFF1F222E)
    val SurfaceContainerHighest = Color(0xFF282C3B)

    // Accent & Semantic Activity colors
    val AccentOrange = Color(0xFFFF9F0A) // Timer activity color
    val AccentCyan = Color(0xFF64D2FF)   // Alerts / OCR / Info
    val AccentGreen = Color(0xFF30D158)  // Charging / Shelf Ready
    val AccentLime = Color(0xFFA6E33A)   // Notes / Shelf countdown healthy
    val AccentAmber = Color(0xFFFFB340)  // Shelf < 2:00 warning
    val AccentRed = Color(0xFFFF3B30)    // Calibration camera ring & recording / < 1:00 / <= 15% battery
    val AccentPurple = Color(0xFFBF5AF2) // Sleep / Sidebar option
    val SelectionBlue = Color(0xFF0A84FF) // Native Android/Apple OCR Selection blue
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB4B8C0)
    val TextTertiary = Color(0xFF7B808A)

    // Concentric radii scale (Apple HIG / Build #5 spec: 36dp full, 28dp medium)
    val ExpandedOuterRadius = 36.dp
    val MediumOuterRadius = 28.dp
    val ExpandedInnerSheetRadius = 28.dp
    val CardRadius = 28.dp
    val ChipRadius = 8.dp
    val ScreenHorizontalMargin = 16.dp

    // Exact iOS Calculator tokens (non-clipping press blend)
    val CalcBackground = Color(0xFF06070A)
    val CalcDigitBg = Color(0xFF2E3038)
    val CalcDigitText = Color(0xFFFFFFFF)
    val CalcFunctionBg = Color(0xFFA5A7AE)
    val CalcFunctionText = Color(0xFF090A0D)
    val CalcOperatorBg = Color(0xFFFF9F0A)
    val CalcOperatorText = Color(0xFFFFFFFF)
    val CalcOperatorActiveBg = Color(0xFFF3F4F6)
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

    /**
     * Density slider maps 0f (crystal clear translucent glass) -> 1f (rich opaque dark glass).
     */
    fun glassAlphaRange(pillStyle: PillStyle, density: Float, reduceGlass: Boolean): Pair<Float, Float> {
        val d = density.coerceIn(0f, 1f)
        if (pillStyle == PillStyle.BLACK) return 1f to 1f
        if (reduceGlass) {
            val top = (0.76f + d * 0.20f).coerceIn(0.76f, 0.96f)
            return top to (top + 0.04f).coerceAtMost(0.98f)
        }
        return when (pillStyle) {
            PillStyle.APPLE_GLASS -> {
                val top = (0.34f + d * 0.56f).coerceIn(0.32f, 0.92f)
                val bot = (top + 0.10f).coerceAtMost(0.96f)
                top to bot
            }
            PillStyle.LIQUID_GLASS -> {
                val top = (0.20f + d * 0.58f).coerceIn(0.18f, 0.84f)
                val bot = (top + 0.12f).coerceAtMost(0.90f)
                top to bot
            }
            PillStyle.BLACK -> 1f to 1f
        }
    }

    fun blendGlassBase(
        materialYouColor: Color,
        density: Float,
        pillStyle: PillStyle = PillStyle.APPLE_GLASS,
        reduceGlass: Boolean = false
    ): Brush {
        if (pillStyle == PillStyle.BLACK) {
            return Brush.verticalGradient(colors = listOf(PillBlack, PillBlack))
        }
        val (alphaTop, alphaBot) = glassAlphaRange(pillStyle, density, reduceGlass)
        val tintStrengthTop = if (pillStyle == PillStyle.LIQUID_GLASS) 0.22f else 0.15f
        val tintStrengthBot = if (pillStyle == PillStyle.LIQUID_GLASS) 0.12f else 0.08f
        val tintedTop = Color(
            ColorUtils.blendARGB(Color(0xFF1A1E2B).toArgb(), materialYouColor.toArgb(), tintStrengthTop)
        ).copy(alpha = alphaTop)
        val tintedBottom = Color(
            ColorUtils.blendARGB(Color(0xFF0A0C12).toArgb(), materialYouColor.toArgb(), tintStrengthBot)
        ).copy(alpha = alphaBot)
        return Brush.verticalGradient(colors = listOf(tintedTop, tintedBottom))
    }

    fun blendGlassBase(materialYouColor: Color, density: Float): Brush =
        blendGlassBase(materialYouColor, density, PillStyle.APPLE_GLASS, reduceGlass = false)

    fun pressTint(base: Color, pressed: Boolean): Color =
        if (!pressed) base else lerp(base, Color.White, 0.22f)

    fun liquidBorderBrush(
        useGlass: Boolean,
        isHighlighted: Boolean,
        accentColor: Color,
        isLiquidStyle: Boolean = false
    ): Brush {
        return when {
            isHighlighted -> Brush.linearGradient(
                colors = listOf(accentColor, accentColor.copy(alpha = 0.55f), accentColor)
            )
            useGlass -> Brush.linearGradient(
                colors = listOf(
                    if (isLiquidStyle) Color.White.copy(alpha = 0.78f) else GlassSpecularTop,
                    accentColor.copy(alpha = if (isLiquidStyle) 0.42f else 0.26f),
                    GlassSpecularBottom
                ),
                start = Offset(0f, 0f),
                end = Offset(420f, 480f)
            )
            else -> Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.08f))
            )
        }
    }
}
