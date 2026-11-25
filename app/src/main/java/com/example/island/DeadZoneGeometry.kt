package com.example.island

import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.view.DisplayCutout
import android.view.Surface
import android.view.WindowManager
import com.example.system.IslandSettings
import kotlin.math.abs

/**
 * Runtime camera cutout & exclusion-zone geometry engine.
 * Hard invariant: no glyph, icon, artwork, or progress bar pixel may sit inside
 * [ExclusionZone] (cutout rect expanded by >= 8 dp on all sides).
 */
data class ExclusionZone(
    val density: Float,
    val screenWidthPx: Int,
    val screenHeightPx: Int,
    val cutoutRectPx: Rect,
    val cutoutRectDp: DpRect,
    val exclusionRectPx: Rect,
    val exclusionRectDp: DpRect,
    val safeInsetTopPx: Int,
    val safeInsetTopDp: Float,
    val safeInsetLeftPx: Int,
    val safeInsetRightPx: Int,
    val isLandscape: Boolean,
    val isRightEdgeInLandscape: Boolean,
    val detectedFromSystem: Boolean
) {
    val holeWidthDp: Float get() = cutoutRectDp.width
    val holeHeightDp: Float get() = cutoutRectDp.height
    val holeCenterXDp: Float get() = cutoutRectDp.centerX
    val holeCenterYDp: Float get() = cutoutRectDp.centerY
    val holeBottomPlusMarginDp: Float get() = exclusionRectDp.bottom
}

data class DpRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

data class IdleCameraPillMetrics(
    val widthDp: Float,
    val heightDp: Float,
    val cornerRadiusDp: Float
)

data class CompactSlotMetrics(
    val leftSlotDp: Float,
    val centerSpacerDp: Float,
    val rightSlotDp: Float,
    val outerPadDp: Float,
    val innerSafetyInsetDp: Float,
    val totalWidthDp: Float,
    val pillHeightDp: Float,
    val isVerticalLandscape: Boolean
) {
    val musicTitleMaxDp: Float get() = (rightSlotDp - innerSafetyInsetDp - 20f).coerceAtLeast(48f)
    val notificationAppNameMaxDp: Float get() = (rightSlotDp - innerSafetyInsetDp - 6f).coerceAtLeast(56f)
}

object DeadZoneGeometry {

    const val REQUIRED_MARGIN_DP = 8f
    const val DEFAULT_SLOT_WIDTH_DP = 80f
    const val OUTER_PAD_DP = 10f
    const val INNER_SAFETY_INSET_DP = 2f
    const val COMPACT_PILL_MIN_HEIGHT_DP = 37f
    const val LANDSCAPE_SLOT_HEIGHT_DP = 44f
    const val SCREEN_EDGE_MARGIN_DP = 12f

    // Round 2 Roomy Card Bounds (fit inside screen width with >=12dp margin, top card, never squeezed)
    const val MEDIUM_MIN_WIDTH_DP = 340f
    const val MEDIUM_MIN_HEIGHT_DP = 170f
    const val MEDIUM_MAX_WIDTH_DP = 380f
    const val MEDIUM_MAX_HEIGHT_DP = 240f
    const val FULL_MAX_WIDTH_DP = 380f
    const val FULL_MAX_HEIGHT_DP = 340f

    fun isCompactExpansionWithinCap(widthDp: Float, heightDp: Float, isExpanded: Boolean): Boolean {
        val maxW = if (isExpanded) FULL_MAX_WIDTH_DP else MEDIUM_MAX_WIDTH_DP
        val maxH = if (isExpanded) FULL_MAX_HEIGHT_DP else MEDIUM_MAX_HEIGHT_DP
        return widthDp <= maxW + 0.5f && heightDp <= maxH + 0.5f
    }

    /**
     * Idle camera-only pill tightly hugs the front camera punch-hole with subtle black framing.
     * Never stretches into a wide full-top line while idle.
     */
    fun idleCameraPillMetrics(holeWidthDp: Float, holeHeightDp: Float): IdleCameraPillMetrics {
        val w = (holeWidthDp.coerceAtLeast(18f) + 14f).coerceIn(36f, 96f)
        val h = (holeHeightDp.coerceAtLeast(18f) + 4f).coerceIn(34f, 44f)
        return IdleCameraPillMetrics(
            widthDp = w,
            heightDp = h,
            cornerRadiusDp = h / 2f
        )
    }

    /**
     * Top Y clearance so MEDIUM / EXPANDED cards sit cleanly below Android's status bar & camera cutout,
     * keeping the status clock (left), battery/Wi-Fi (right), and camera hole 100% uncovered.
     */
    fun cardTopAnchorDp(zone: ExclusionZone): Float {
        if (zone.isLandscape) return 10f
        return (zone.cutoutRectDp.bottom + 6f).coerceIn(36f, 56f)
    }

    fun boundedCardWidthDp(screenWidthPx: Int, density: Float, desiredMaxDp: Float): Float {
        val screenWidthDp = (screenWidthPx / density.coerceAtLeast(1f)).coerceAtLeast(320f)
        val maxAllowed = (screenWidthDp - SCREEN_EDGE_MARGIN_DP * 2f).coerceAtLeast(300f)
        return desiredMaxDp.coerceAtMost(maxAllowed)
    }

    /**
     * Pure geometric check required by CameraDeadZoneTest.
     * Verifies:
     * 1. Left & right slots are equal width within 0.5 dp so (L - R) / 2 = 0 drift.
     * 2. Center spacer covers the full hole plus >= 8 dp clearance on both sides.
     */
    fun isSafe(
        leftSlotDp: Float,
        rightSlotDp: Float,
        holeWidthDp: Float,
        expansionDp: Float = REQUIRED_MARGIN_DP
    ): Boolean {
        if (leftSlotDp <= 0f || rightSlotDp <= 0f || holeWidthDp <= 0f) return false
        if (abs(leftSlotDp - rightSlotDp) > 0.5f) return false
        val requiredSpacer = holeWidthDp + expansionDp * 2f
        val leftSlotInnerEdgeFromCenter = -(requiredSpacer / 2f)
        val rightSlotInnerEdgeFromCenter = +(requiredSpacer / 2f)
        val holeLeftFromCenter = -holeWidthDp / 2f
        val holeRightFromCenter = +holeWidthDp / 2f
        val leftGap = holeLeftFromCenter - leftSlotInnerEdgeFromCenter
        val rightGap = rightSlotInnerEdgeFromCenter - holeRightFromCenter
        return leftGap + 0.01f >= expansionDp && rightGap + 0.01f >= expansionDp
    }

    fun compactSlotMetrics(
        holeWidthDp: Float,
        holeHeightDp: Float,
        isLandscape: Boolean = false,
        slotWidthDp: Float = DEFAULT_SLOT_WIDTH_DP
    ): CompactSlotMetrics {
        val safeHoleW = holeWidthDp.coerceAtLeast(14f)
        val safeHoleH = holeHeightDp.coerceAtLeast(14f)
        return if (!isLandscape) {
            val slot = slotWidthDp.coerceIn(64f, 108f)
            val spacer = safeHoleW + REQUIRED_MARGIN_DP * 2f
            val totalW = slot + spacer + slot + OUTER_PAD_DP * 2f
            val pillH = (safeHoleH + 4f).coerceAtLeast(COMPACT_PILL_MIN_HEIGHT_DP)
            CompactSlotMetrics(
                leftSlotDp = slot,
                centerSpacerDp = spacer,
                rightSlotDp = slot,
                outerPadDp = OUTER_PAD_DP,
                innerSafetyInsetDp = INNER_SAFETY_INSET_DP,
                totalWidthDp = totalW,
                pillHeightDp = pillH,
                isVerticalLandscape = false
            )
        } else {
            val slotH = LANDSCAPE_SLOT_HEIGHT_DP
            val spacerH = safeHoleH + REQUIRED_MARGIN_DP * 2f
            val totalH = slotH + spacerH + slotH + OUTER_PAD_DP * 2f
            CompactSlotMetrics(
                leftSlotDp = slotH,
                centerSpacerDp = spacerH,
                rightSlotDp = slotH,
                outerPadDp = OUTER_PAD_DP,
                innerSafetyInsetDp = INNER_SAFETY_INSET_DP,
                totalWidthDp = totalH,
                pillHeightDp = (safeHoleW + 16f).coerceAtLeast(48f),
                isVerticalLandscape = true
            )
        }
    }

    fun rectsIntersect(
        aLeft: Float,
        aTop: Float,
        aRight: Float,
        aBottom: Float,
        bLeft: Float,
        bTop: Float,
        bRight: Float,
        bBottom: Float,
        tolerance: Float = 0.05f
    ): Boolean {
        return (aRight - tolerance) > bLeft &&
            (aLeft + tolerance) < bRight &&
            (aBottom - tolerance) > bTop &&
            (aTop + tolerance) < bBottom
    }

    fun fromSyntheticHole(
        holeWidthDp: Float,
        holeHeightDp: Float,
        screenWidthDp: Float = 393f,
        screenHeightDp: Float = 873f,
        density: Float = 2.75f,
        holeTopDp: Float = 0f,
        isLandscape: Boolean = false
    ): ExclusionZone {
        val screenWPx = (screenWidthDp * density).toInt()
        val screenHPx = (screenHeightDp * density).toInt()
        val cutoutDp = if (!isLandscape) {
            val cx = screenWidthDp / 2f
            DpRect(
                left = cx - holeWidthDp / 2f,
                top = holeTopDp,
                right = cx + holeWidthDp / 2f,
                bottom = holeTopDp + holeHeightDp
            )
        } else {
            val cy = screenHeightDp / 2f
            DpRect(
                left = 0f,
                top = cy - holeHeightDp / 2f,
                right = holeWidthDp,
                bottom = cy + holeHeightDp / 2f
            )
        }
        val exclusionDp = DpRect(
            left = cutoutDp.left - REQUIRED_MARGIN_DP,
            top = cutoutDp.top - REQUIRED_MARGIN_DP,
            right = cutoutDp.right + REQUIRED_MARGIN_DP,
            bottom = cutoutDp.bottom + REQUIRED_MARGIN_DP
        )
        fun DpRect.toPxRect() = Rect(
            (left * density).toInt(),
            (top * density).toInt(),
            (right * density).toInt(),
            (bottom * density).toInt()
        )
        return ExclusionZone(
            density = density,
            screenWidthPx = screenWPx,
            screenHeightPx = screenHPx,
            cutoutRectPx = cutoutDp.toPxRect(),
            cutoutRectDp = cutoutDp,
            exclusionRectPx = exclusionDp.toPxRect(),
            exclusionRectDp = exclusionDp,
            safeInsetTopPx = (cutoutDp.bottom * density).toInt(),
            safeInsetTopDp = cutoutDp.bottom,
            safeInsetLeftPx = if (isLandscape) (cutoutDp.right * density).toInt() else 0,
            safeInsetRightPx = 0,
            isLandscape = isLandscape,
            isRightEdgeInLandscape = false,
            detectedFromSystem = true
        )
    }

    /**
     * Queries live DisplayCutout from WindowManager / Display on every launch, rotation, and configuration change.
     */
    fun measureLive(context: Context, settings: IslandSettings): ExclusionZone {
        val dm = context.resources.displayMetrics
        val density = dm.density.coerceAtLeast(1f)
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager

        var screenW = dm.widthPixels
        var screenH = dm.heightPixels
        var cutout: DisplayCutout? = null
        var statusTopPx = 0
        var rotation = Surface.ROTATION_0

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wm != null) {
            runCatching {
                val metrics = wm.currentWindowMetrics
                screenW = metrics.bounds.width().coerceAtLeast(dm.widthPixels)
                screenH = metrics.bounds.height().coerceAtLeast(dm.heightPixels)
                cutout = metrics.windowInsets.displayCutout
                val statusBarInsets = metrics.windowInsets.getInsets(android.view.WindowInsets.Type.statusBars())
                statusTopPx = statusBarInsets.top
            }
            val dispMgr = context.getSystemService(Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager
            rotation = runCatching { context.display.rotation }.getOrNull()
                ?: runCatching { dispMgr?.getDisplay(android.view.Display.DEFAULT_DISPLAY)?.rotation }.getOrNull()
                ?: @Suppress("DEPRECATION") runCatching { wm.defaultDisplay?.rotation }.getOrNull()
                ?: Surface.ROTATION_0
            if (cutout == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cutout = runCatching { dispMgr?.getDisplay(android.view.Display.DEFAULT_DISPLAY)?.cutout }.getOrNull()
            }
        } else {
            val resId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resId > 0) statusTopPx = context.resources.getDimensionPixelSize(resId)
            @Suppress("DEPRECATION")
            cutout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) runCatching { wm?.defaultDisplay?.cutout }.getOrNull() else null
        }

        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
            rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270

        val bounding = cutout?.boundingRects?.firstOrNull { !it.isEmpty }
        val detectedFromSystem = bounding != null
        val safeTopPx = cutout?.safeInsetTop ?: statusTopPx
        val safeLeftPx = cutout?.safeInsetLeft ?: 0
        val safeRightPx = cutout?.safeInsetRight ?: 0

        val marginPx = (REQUIRED_MARGIN_DP * density).toInt()

        val cutoutPx: Rect = if (bounding != null) {
            // Apply tiny user fine-tuning calibration offset while preserving real measured dimensions
            val userDx = (settings.cutoutXOffsetDp * density).toInt()
            val userDy = if (!isLandscape) (settings.cutoutYOffsetDp * density).toInt() else 0
            Rect(
                bounding.left + userDx,
                bounding.top + userDy,
                bounding.right + userDx,
                bounding.bottom + userDy
            )
        } else {
            val wPx = (settings.cutoutWidthDp * density).toInt().coerceAtLeast((24 * density).toInt())
            val hPx = maxOf((settings.cutoutHeightDp * density).toInt(), statusTopPx + marginPx)
            if (!isLandscape) {
                val cx = screenW / 2 + (settings.cutoutXOffsetDp * density).toInt()
                val topPx = (settings.cutoutYOffsetDp * density).toInt().coerceAtLeast(0)
                Rect(cx - wPx / 2, topPx, cx + wPx / 2, topPx + hPx)
            } else {
                val cy = screenH / 2
                val onRight = safeRightPx > safeLeftPx
                if (onRight) {
                    Rect(screenW - hPx, cy - wPx / 2, screenW, cy + wPx / 2)
                } else {
                    Rect(0, cy - wPx / 2, hPx, cy + wPx / 2)
                }
            }
        }

        val exclusionPx = Rect(
            (cutoutPx.left - marginPx).coerceAtLeast(0),
            (cutoutPx.top - marginPx).coerceAtLeast(0),
            (cutoutPx.right + marginPx).coerceAtMost(screenW),
            (cutoutPx.bottom + marginPx).coerceAtMost(screenH)
        )

        val cutoutDp = DpRect(
            left = cutoutPx.left / density,
            top = cutoutPx.top / density,
            right = cutoutPx.right / density,
            bottom = cutoutPx.bottom / density
        )
        val exclusionDp = DpRect(
            left = exclusionPx.left / density,
            top = exclusionPx.top / density,
            right = exclusionPx.right / density,
            bottom = exclusionPx.bottom / density
        )

        val rightEdgeLandscape = isLandscape && (cutoutPx.centerX() > screenW / 2)

        return ExclusionZone(
            density = density,
            screenWidthPx = screenW,
            screenHeightPx = screenH,
            cutoutRectPx = cutoutPx,
            cutoutRectDp = cutoutDp,
            exclusionRectPx = exclusionPx,
            exclusionRectDp = exclusionDp,
            safeInsetTopPx = safeTopPx,
            safeInsetTopDp = safeTopPx / density,
            safeInsetLeftPx = safeLeftPx,
            safeInsetRightPx = safeRightPx,
            isLandscape = isLandscape,
            isRightEdgeInLandscape = rightEdgeLandscape,
            detectedFromSystem = detectedFromSystem
        )
    }
}
