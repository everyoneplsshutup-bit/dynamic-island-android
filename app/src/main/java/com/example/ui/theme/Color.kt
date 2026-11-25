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
import com.example.system.AccentTheme
import com.example.system.PillStyle

object IslandColors {
    // Obsidian & Crimson Glass Palette (matching the 12 mockup screens)
    val ObsidianBg = Color(0xFF050507)
    val ObsidianSurface = Color(0xFF0E0B0D)
    val ObsidianCard = Color(0xFF140E11)
    val ObsidianCardElevated = Color(0xFF1C1317)
    val ObsidianDock = Color(0xEA100A0C)

    val CrimsonPrimary = Color(0xFFE50914)
    val CrimsonBright = Color(0xFFFF1E2D)
    val CrimsonGlow = Color(0xFFFF3B47)
    val CrimsonDeep = Color(0xFF4A070D)
    val CrimsonMuted = Color(0xFF28090C)
    val CrimsonBorder = Color(0x55FF2A38)
    val CrimsonBorderStrong = Color(0xCCFF2A38)

    // Core Dynamic Island & Glass palette
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
    val GlassSubtleBorder = Color(0x22FFFFFF)

    // M3E Surface Container Ladder (Obsidian-tinted)
    val SurfaceContainerLowest = Color(0xFF050507)
    val SurfaceContainerLow = Color(0xFF0E0B0D)
    val SurfaceContainer = Color(0xFF151013)
    val SurfaceContainerHigh = Color(0xFF1E161A)
    val SurfaceContainerHighest = Color(0xFF281D22)

    // Accent & Semantic Activity colors
    val ElectricBlue = Color(0xFF1E90FF) // Liquid Glass default hero accent
    val NeonPurple = Color(0xFFA855F7)   // Position chooser Liquid Glass accent
    val CyanBright = Color(0xFF00E5FF)   // Tools Liquid Glass accent
    val AccentOrange = Color(0xFFFF9F0A) // Amber / Gold Liquid Glass & Timer accent
    val AccentCyan = Color(0xFF64D2FF)   // Alerts / OCR / Info
    val AccentGreen = Color(0xFF30D158)  // Charging / Shelf Ready / Active
    val AccentLime = Color(0xFFA6E33A)   // Notes / Shelf countdown healthy
    val AccentAmber = Color(0xFFFFB340)  // Shelf < 2:00 warning
    val AccentRed = Color(0xFFFF2433)    // Crimson primary / calibration ring / recording / <= 15% battery
    val AccentPurple = Color(0xFFBF5AF2) // Sleep / Sidebar option
    val SelectionBlue = Color(0xFF0A84FF) // Native Android/Apple OCR Selection blue
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFA6A8B2)
    val TextTertiary = Color(0xFF6F727C)

    // Concentric radii scale (Apple HIG / Build #5 spec: 36dp full, 28dp medium)
    val ExpandedOuterRadius = 36.dp
    val MediumOuterRadius = 28.dp
    val ExpandedInnerSheetRadius = 28.dp
    val CardRadius = 22.dp
    val ChipRadius = 8.dp
    val ScreenHorizontalMargin = 16.dp

    // App Settings surface tokens
    val AppBgDark = Color(0xFF050507)
    val AppSurfaceDark = Color(0xFF120D10)
    val AppSurfaceVariantDark = Color(0xFF1C1418)
    val AppBgLight = Color(0xFFF5F6FA)
    val AppSurfaceLight = Color(0xFFFFFFFF)
    val AppSurfaceVariantLight = Color(0xFFEBEEF5)

    fun resolveAccentColor(context: Context, theme: AccentTheme): Color = when (theme) {
        AccentTheme.BLUE -> ElectricBlue
        AccentTheme.PURPLE -> NeonPurple
        AccentTheme.CYAN -> CyanBright
        AccentTheme.ORANGE -> AccentOrange
        AccentTheme.CRIMSON -> CrimsonBright
        AccentTheme.GREEN -> AccentGreen
        AccentTheme.DYNAMIC -> extractMaterialYouAccent(context, fallback = ElectricBlue)
    }

    fun extractMaterialYouAccent(context: Context, fallback: Color = ElectricBlue): Color {
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

    fun glassAlphaRange(pillStyle: PillStyle, density: Float, reduceGlass: Boolean): Pair<Float, Float> {
        val d = density.coerceIn(0f, 1f)
        if (pillStyle == PillStyle.BLACK) return 1f to 1f
        if (reduceGlass) {
            val top = (0.72f + d * 0.18f).coerceIn(0.72f, 0.92f)
            return top to (top + 0.05f).coerceAtMost(0.96f)
        }
        return when (pillStyle) {
            PillStyle.APPLE_GLASS -> {
                val top = (0.48f + d * 0.28f).coerceIn(0.42f, 0.78f)
                val bot = (top + 0.10f).coerceAtMost(0.86f)
                top to bot
            }
            PillStyle.LIQUID_GLASS -> {
                // Optical crystal Liquid Glass: crisp translucent body + luminous edge refraction
                val top = (0.12f + d * 0.16f).coerceIn(0.10f, 0.30f)
                val bot = (top + 0.06f).coerceAtMost(0.36f)
                top to bot
            }
            PillStyle.BLACK -> 1f to 1f
        }
    }

    fun blendGlassBase(
        materialYouColor: Color,
        density: Float,
        pillStyle: PillStyle = PillStyle.LIQUID_GLASS,
        reduceGlass: Boolean = false
    ): Brush {
        if (pillStyle == PillStyle.BLACK) {
            return Brush.verticalGradient(colors = listOf(Color(0xFA060608), PillBlack))
        }
        val (alphaTop, alphaBot) = glassAlphaRange(pillStyle, density, reduceGlass)
        val tintStrengthTop = if (pillStyle == PillStyle.LIQUID_GLASS) 0.18f else 0.10f
        val tintStrengthBot = if (pillStyle == PillStyle.LIQUID_GLASS) 0.08f else 0.04f
        val baseTopArgb = if (pillStyle == PillStyle.LIQUID_GLASS) 0xFF0E1624.toInt() else 0xFF14151C.toInt()
        val baseBotArgb = if (pillStyle == PillStyle.LIQUID_GLASS) 0xFF060912.toInt() else 0xFF090A0F.toInt()
        val tintedTop = Color(
            ColorUtils.blendARGB(baseTopArgb, materialYouColor.toArgb(), tintStrengthTop)
        ).copy(alpha = alphaTop)
        val tintedBottom = Color(
            ColorUtils.blendARGB(baseBotArgb, materialYouColor.toArgb(), tintStrengthBot)
        ).copy(alpha = alphaBot)
        return Brush.verticalGradient(colors = listOf(tintedTop, tintedBottom))
    }

    fun blendGlassBase(materialYouColor: Color, density: Float): Brush =
        blendGlassBase(materialYouColor, density, PillStyle.LIQUID_GLASS, reduceGlass = false)

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
                colors = listOf(accentColor, Color.White.copy(alpha = 0.90f), accentColor)
            )
            useGlass && isLiquidStyle -> Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.78f),
                    accentColor.copy(alpha = 0.88f),
                    Color(0x889CEBFF),
                    Color.White.copy(alpha = 0.34f),
                    accentColor.copy(alpha = 0.62f)
                ),
                start = Offset(0f, 0f),
                end = Offset(440f, 480f)
            )
            useGlass -> Brush.linearGradient(
                colors = listOf(
                    GlassSpecularTop,
                    accentColor.copy(alpha = 0.32f),
                    Color.White.copy(alpha = 0.16f)
                ),
                start = Offset(0f, 0f),
                end = Offset(420f, 480f)
            )
            else -> Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.22f),
                    accentColor.copy(alpha = 0.28f),
                    Color.White.copy(alpha = 0.08f)
                )
            )
        }
    }
}
