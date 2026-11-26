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
    // Neutral #121212 Dark Ladder & Liquid Glass Palette
    val ObsidianBg = Color(0xFF121212)
    val ObsidianSurface = Color(0xFF161618)
    val ObsidianCard = Color(0xFF1C1C20)
    val ObsidianCardElevated = Color(0xFF242429)
    val ObsidianGlassDark = Color(0xCC16161A)
    val ObsidianDock = Color(0xEC16161A)

    // Muted semantic crimson/red (only for REC indicator, destructive actions, <=15% battery)
    val CrimsonPrimary = Color(0xFFE53935)
    val CrimsonBright = Color(0xFFEF5350)
    val CrimsonGlow = Color(0xFFFF6B6B)
    val CrimsonDeep = Color(0xFF2A181A)
    val CrimsonMuted = Color(0xFF221618)
    val CrimsonBorder = Color(0x44FFFFFF)
    val CrimsonBorderStrong = Color(0xAA4A9EFF)

    // Core Dynamic Island & Glass palette
    val PillBlack = Color(0xFF000000)
    val CardSurfaceBlack = Color(0xFF121212)
    val CardSurfaceSolid = Color(0xFF18181B)
    val CardSurfaceElevated = Color(0xFF222227)
    val GlassTintLight = Color(0x20FFFFFF)
    val GlassTintDark = Color(0xCC121316)
    val GlassSpecularBorder = Color(0x66E4EEFF)
    val GlassSpecularTop = Color(0x96FFFFFF)
    val GlassSpecularMid = Color(0x36FFFFFF)
    val GlassSpecularBottom = Color(0x18FFFFFF)
    val GlassSubtleBorder = Color(0x24FFFFFF)

    // M3E Surface Container Ladder (Neutral grey overlays on #121212)
    val SurfaceContainerLowest = Color(0xFF0E0E10)
    val SurfaceContainerLow = Color(0xFF141416)
    val SurfaceContainer = Color(0xFF1A1A1E)
    val SurfaceContainerHigh = Color(0xFF212126)
    val SurfaceContainerHighest = Color(0xFF2A2A30)

    // Desaturated Accent & Semantic Activity colors
    val ElectricBlue = Color(0xFF4A9EFF) // Liquid Glass default hero accent
    val NeonPurple = Color(0xFFAB7DF8)   // Violet Liquid Glass accent
    val CyanBright = Color(0xFF3FD5E8)   // Teal Liquid Glass accent
    val AccentOrange = Color(0xFFF5A623) // Amber Liquid Glass & Timer accent
    val AccentCyan = Color(0xFF64D2FF)   // Alerts / OCR / Info
    val AccentGreen = Color(0xFF34C759)  // Charging / Granted / Active
    val AccentLime = Color(0xFF9CD94A)   // Notes / Shelf countdown healthy
    val AccentAmber = Color(0xFFFFB340)  // Shelf < 2:00 warning
    val AccentRed = Color(0xFFEF5350)    // REC / error / <= 15% battery
    val AccentPurple = Color(0xFFBF5AF2) // Sleep / Sidebar option
    val SelectionBlue = Color(0xFF33B5E5) // AOSP / Native OCR Selection blue
    val PrivacyCameraGreen = AccentGreen
    val PrivacyMicOrange = Color(0xFFFF9F0A)
    val PrivacyLocBlue = Color(0xFF0A84FF)
    val TextPrimary = Color(0xFFF5F5F7)
    val TextSecondary = Color(0xFFE0E0E0) // >= 4.5:1 contrast body text
    val TextMuted = Color(0xFF9EA0AA)
    val TextTertiary = Color(0xFF7D808C)

    // Concentric radii scale (Apple HIG / Build #5 spec: 36dp full, 28dp medium)
    val ExpandedOuterRadius = 36.dp
    val MediumOuterRadius = 28.dp
    val ExpandedInnerSheetRadius = 28.dp
    val CardRadius = 28.dp
    val ChipRadius = 8.dp
    val ScreenHorizontalMargin = 16.dp

    // App Settings surface tokens
    val AppBgDark = Color(0xFF121212)
    val AppSurfaceDark = Color(0xFF18181A)
    val AppSurfaceVariantDark = Color(0xFF222226)
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
                hsl[1] = hsl[1].coerceAtLeast(0.50f)
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
            val top = (0.78f + d * 0.14f).coerceIn(0.76f, 0.94f)
            return top to (top + 0.04f).coerceAtMost(0.96f)
        }
        return when (pillStyle) {
            PillStyle.APPLE_GLASS -> {
                val top = (0.56f + d * 0.24f).coerceIn(0.50f, 0.82f)
                val bot = (top + 0.08f).coerceAtMost(0.88f)
                top to bot
            }
            PillStyle.LIQUID_GLASS -> {
                val top = (0.16f + d * 0.16f).coerceIn(0.14f, 0.34f)
                val bot = (top + 0.06f).coerceAtMost(0.40f)
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
            return Brush.verticalGradient(colors = listOf(Color(0xFF141416), Color(0xFF101012)))
        }
        val (alphaTop, alphaBot) = glassAlphaRange(pillStyle, density, reduceGlass)
        val tintStrengthTop = if (pillStyle == PillStyle.LIQUID_GLASS) 0.14f else 0.08f
        val tintStrengthBot = if (pillStyle == PillStyle.LIQUID_GLASS) 0.06f else 0.03f
        val baseTopArgb = if (pillStyle == PillStyle.LIQUID_GLASS) 0xFF161B24.toInt() else 0xFF18181C.toInt()
        val baseBotArgb = if (pillStyle == PillStyle.LIQUID_GLASS) 0xFF101218.toInt() else 0xFF121215.toInt()
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
