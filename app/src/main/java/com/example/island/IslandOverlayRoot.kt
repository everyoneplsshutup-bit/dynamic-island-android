package com.example.island

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.TrafficStats
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.BuildConfig
import com.example.DynamicIslandApp
import com.example.media.MediaUiState
import com.example.media.MusicCompactWaveform
import com.example.media.MusicExpandedView
import com.example.notifications.OtpExtractor
import com.example.notifications.ReplySafetyGuard
import com.example.ocr.OcrBitmapHolder
import com.example.ocr.OcrSelectionOverlay
import com.example.system.AnimationStyle
import com.example.system.CalendarReminderReader
import com.example.system.CompanionPosition
import com.example.system.ExpandedSwipeAction
import com.example.system.IdleWidgetType
import com.example.system.IslandSettings
import com.example.system.IslandTab
import com.example.system.LockScreenPreset
import com.example.system.NextAlarmReader
import com.example.system.PillStyle
import com.example.system.ScreenRecorderManager
import com.example.system.TimerEngine
import com.example.system.TimerStopwatchPanel
import com.example.system.TimerUiState
import com.example.system.WhenIdleMode
import com.example.ui.theme.IslandColors
import com.example.ui.theme.LocalAppBackdrop
import com.example.ui.theme.MotionTokens
import com.example.ui.theme.WavyProgressBar
import com.example.ui.theme.islandGlassChrome
import com.example.ui.theme.metaballParentLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private class DeadZoneGuardState(var violated: Boolean = false)

private class MorphCapsuleShape(
    private val widthDpState: State<Float>,
    private val heightDpState: State<Float>,
    private val radiusDpState: State<Float>
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = (widthDpState.value * density.density).coerceIn(1f, size.width.coerceAtLeast(1f))
        val h = (heightDpState.value * density.density).coerceIn(1f, size.height.coerceAtLeast(1f))
        val r = (radiusDpState.value * density.density).coerceIn(0f, minOf(w, h) / 2f)
        val left = ((size.width - w) / 2f).coerceAtLeast(0f)
        return Outline.Rounded(
            RoundRect(
                left = left,
                top = 0f,
                right = left + w,
                bottom = h,
                cornerRadius = CornerRadius(r, r)
            )
        )
    }
}

private fun DrawScope.drawMorphCapsuleChrome(
    widthDp: Float,
    heightDp: Float,
    radiusDp: Float,
    glassAlpha: Float,
    pillStyle: PillStyle,
    glassDensity: Float,
    reduceGlass: Boolean,
    solidFallback: Boolean,
    accentColor: Color,
    isHighlighted: Boolean,
    performanceMode: Boolean = false
) {
    val w = (widthDp * density).coerceIn(1f, size.width.coerceAtLeast(1f))
    val h = (heightDp * density).coerceIn(1f, size.height.coerceAtLeast(1f))
    val r = (radiusDp * density).coerceIn(0f, minOf(w, h) / 2f)
    val left = ((size.width - w) / 2f).coerceAtLeast(0f)
    val corner = CornerRadius(r, r)

    val effectiveStyle = if (solidFallback) PillStyle.BLACK else pillStyle
    val g = glassAlpha.coerceIn(0f, 1f)

    if (effectiveStyle == PillStyle.BLACK) {
        drawRoundRect(
            color = IslandColors.PillBlack,
            topLeft = Offset(left, 0f),
            size = Size(w, h),
            cornerRadius = corner
        )
        drawRoundRect(
            color = if (isHighlighted) accentColor.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.16f),
            topLeft = Offset(left, 0f),
            size = Size(w, h),
            cornerRadius = corner,
            style = Stroke(width = (if (isHighlighted) 1.4f else 1.0f).dp.toPx())
        )
        return
    }

    val isLiquid = effectiveStyle == PillStyle.LIQUID_GLASS
    val d = glassDensity.coerceIn(0f, 1f)
    val targetBaseAlpha = if (isLiquid) (0.76f - d * 0.44f).coerceIn(0.28f, 0.82f) else (0.88f - d * 0.36f).coerceIn(0.42f, 0.90f)
    val obsidianUnderlayAlpha = 0.96f * (1f - g) + targetBaseAlpha * g

    // Continuous obsidian core smoothly liquefies into translucent glass so card never vanishes mid-morph
    drawRoundRect(
        color = IslandColors.PillBlack.copy(alpha = obsidianUnderlayAlpha),
        topLeft = Offset(left, 0f),
        size = Size(w, h),
        cornerRadius = corner
    )

    if (g > 0.01f) {
        val topTintAlpha = if (isLiquid) (0.16f + d * 0.08f) else 0.12f
        val botTintAlpha = if (isLiquid) (0.12f + d * 0.06f) else 0.07f
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.14f * g),
                    accentColor.copy(alpha = topTintAlpha * g),
                    accentColor.copy(alpha = botTintAlpha * g)
                ),
                startY = 0f,
                endY = h
            ),
            topLeft = Offset(left, 0f),
            size = Size(w, h),
            cornerRadius = corner
        )
        if (isLiquid && !reduceGlass && !performanceMode) {
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f * g),
                        accentColor.copy(alpha = 0.14f * g),
                        Color.Transparent
                    ),
                    center = Offset(left + w * 0.24f, h * 0.14f),
                    radius = (maxOf(w, h) * 0.88f).coerceAtLeast(1f)
                ),
                topLeft = Offset(left, 0f),
                size = Size(w, h),
                cornerRadius = corner
            )
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x2890E0FF).copy(alpha = 0.16f * g),
                        accentColor.copy(alpha = 0.10f * g),
                        Color.Transparent
                    ),
                    center = Offset(left + w * 0.80f, h * 0.84f),
                    radius = (maxOf(w, h) * 0.70f).coerceAtLeast(1f)
                ),
                topLeft = Offset(left, 0f),
                size = Size(w, h),
                cornerRadius = corner
            )
        }
    }

    val borderBrush = if (isHighlighted) {
        Brush.horizontalGradient(listOf(accentColor, accentColor.copy(alpha = 0.8f)))
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.24f + (if (isLiquid) 0.44f else 0.16f) * g),
                accentColor.copy(alpha = (if (isLiquid) 0.48f else 0.22f) * g + 0.10f),
                Color.White.copy(alpha = 0.16f + (if (isLiquid) 0.24f else 0.14f) * g)
            ),
            startY = 0f,
            endY = h
        )
    }
    drawRoundRect(
        brush = borderBrush,
        topLeft = Offset(left, 0f),
        size = Size(w, h),
        cornerRadius = corner,
        style = Stroke(width = (if (isHighlighted) 1.5f else if (isLiquid) 1.25f else 0.9f).dp.toPx())
    )
}

/**
 * Runtime debug guard ensuring no rendered text/glyph enters the camera cutout exclusion zone.
 * Never paints visible debug boxes on-screen; records collision diagnostics cleanly.
 */
internal fun Modifier.debugCameraDeadZoneGuard(
    exclusionZone: ExclusionZone,
    label: String,
    onViolation: (() -> Unit)? = null
): Modifier {
    if (!BuildConfig.DEBUG) return this
    val guardState = DeadZoneGuardState()
    return this.onGloballyPositioned { coords ->
        val bounds = coords.boundsInWindow()
        val hit = DeadZoneGeometry.rectsIntersect(
            aLeft = bounds.left,
            aTop = bounds.top,
            aRight = bounds.right,
            aBottom = bounds.bottom,
            bLeft = exclusionZone.exclusionRectPx.left.toFloat(),
            bTop = exclusionZone.exclusionRectPx.top.toFloat(),
            bRight = exclusionZone.exclusionRectPx.right.toFloat(),
            bBottom = exclusionZone.exclusionRectPx.bottom.toFloat()
        )
        if (hit) {
            if (!guardState.violated) {
                guardState.violated = true
                onViolation?.invoke()
            }
            Log.e("DeadZoneGuard", "Overlap detected for $label: bounds=$bounds vs cutout=${exclusionZone.exclusionRectPx}")
        } else {
            guardState.violated = false
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IslandOverlayRoot(
    app: DynamicIslandApp,
    exclusionZone: ExclusionZone = DeadZoneGeometry.measureLive(LocalContext.current, IslandSettings()),
    crossWindowBlurSupported: Boolean = false,
    windowOriginDp: Offset = Offset.Zero,
    pullDownProgressProvider: () -> Float = { 0f },
    isPillPressedProvider: () -> Boolean = { false },
    onRequestHaptic: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val state by app.islandController.state.collectAsState()
    val settings by app.settingsRepository.settingsFlow.collectAsState(initial = IslandSettings())
    val mediaState by app.mediaHub.state.collectAsState()
    val timerState by app.timerEngine.state.collectAsState()

    val context = LocalContext.current
    val systemAnimScale = remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
    }
    val reduceMotion = settings.reducedMotion || systemAnimScale == 0f || settings.animationStyle == AnimationStyle.NONE
    val effectiveStyle = if (reduceMotion) AnimationStyle.NONE else settings.animationStyle
    val elasticity = settings.animationElasticity
    val durationScale = settings.animationSpeed

    val accentColor = remember(settings.accentTheme) {
        IslandColors.resolveAccentColor(context, settings.accentTheme)
    }
    val musicAccent = remember(mediaState.artworkAccentArgb, accentColor) {
        mediaState.artworkAccentArgb?.let { Color(it) } ?: accentColor
    }
    val notifCardAccent = remember(state.primaryActivity, settings.matchAppColor, accentColor) {
        val np = state.primaryActivity as? IslandActivity.NotificationPreview
        if (settings.matchAppColor && np?.accentArgb != null) Color(np.accentArgb) else accentColor
    }

    if (!settings.islandEnabled && !state.isCalibrationPreview) {
        return
    }

    // Lock screen visibility policy
    if (state.isDeviceLocked && settings.lockScreenPreset == LockScreenPreset.HIDDEN) {
        return
    }

    // Single M3 Expressive integrator: one updateTransition drives width, height, corner radius, and position
    val holeW = exclusionZone.holeWidthDp.coerceAtLeast(settings.cutoutWidthDp)
    val holeH = exclusionZone.holeHeightDp.coerceAtLeast(settings.cutoutHeightDp)
    val primaryActivity = state.primaryActivity
    val compactMetrics = remember(holeW, holeH, exclusionZone.isLandscape, settings.pillSizePreset, primaryActivity) {
        when (primaryActivity) {
            is IslandActivity.Music -> DeadZoneGeometry.musicCompactPillMetrics(
                holeWidthDp = holeW,
                holeHeightDp = holeH,
                isLandscape = exclusionZone.isLandscape
            )
            is IslandActivity.Alert -> DeadZoneGeometry.compactSlotMetrics(
                holeWidthDp = holeW,
                holeHeightDp = holeH,
                isLandscape = exclusionZone.isLandscape,
                slotWidthDp = maxOf(settings.pillSizePreset.slotWidthDp, 112f)
            )
            else -> DeadZoneGeometry.compactSlotMetrics(
                holeWidthDp = holeW,
                holeHeightDp = holeH,
                isLandscape = exclusionZone.isLandscape,
                slotWidthDp = settings.pillSizePreset.slotWidthDp
            )
        }
    }
    val idleMetrics = remember(holeW, holeH, settings.appleWidthIdlePill) {
        DeadZoneGeometry.idleCameraPillMetrics(holeW, holeH, appleWidth = settings.appleWidthIdlePill)
    }
    val idleWidthDp = remember(settings.whenIdleMode, compactMetrics.totalWidthDp, idleMetrics.widthDp) {
        when (settings.whenIdleMode) {
            WhenIdleMode.INFO_PILL -> compactMetrics.totalWidthDp
            WhenIdleMode.CAMERA_ONLY, WhenIdleMode.HIDDEN -> idleMetrics.widthDp
        }
    }
    val idleHeightDp = remember(settings.whenIdleMode, compactMetrics.pillHeightDp, idleMetrics.heightDp) {
        when (settings.whenIdleMode) {
            WhenIdleMode.INFO_PILL -> compactMetrics.pillHeightDp
            WhenIdleMode.CAMERA_ONLY, WhenIdleMode.HIDDEN -> idleMetrics.heightDp
        }
    }
    val mediumTargetWidthDp = remember(exclusionZone.screenWidthPx, exclusionZone.density) {
        DeadZoneGeometry.boundedCardWidthDp(
            screenWidthPx = exclusionZone.screenWidthPx,
            density = exclusionZone.density,
            desiredMaxDp = DeadZoneGeometry.MEDIUM_MAX_WIDTH_DP
        )
    }
    val fullTargetWidthDp = remember(exclusionZone.screenWidthPx, exclusionZone.density) {
        DeadZoneGeometry.boundedCardWidthDp(
            screenWidthPx = exclusionZone.screenWidthPx,
            density = exclusionZone.density,
            desiredMaxDp = DeadZoneGeometry.FULL_MAX_WIDTH_DP
        )
    }
    val cardTopClearanceDp = remember(exclusionZone) {
        DeadZoneGeometry.cardTopAnchorDp(exclusionZone)
    }
    var measuredMediumHeightDp by remember { mutableFloatStateOf(202f) }
    var measuredExpandedHeightDp by remember { mutableFloatStateOf(316f) }
    val densityPx = androidx.compose.ui.platform.LocalDensity.current.density

    val morphTransition = updateTransition(targetState = state.mode, label = "island_morph")
    val morphSpringSpec: @Composable Transition.Segment<IslandMode>.() -> FiniteAnimationSpec<Float> = {
        when {
            effectiveStyle == AnimationStyle.NONE -> snap()
            targetState == IslandMode.IDLE || targetState == IslandMode.COMPACT || targetState == IslandMode.SPLIT -> {
                if (initialState == IslandMode.EXPANDED || initialState == IslandMode.MEDIUM) {
                    MotionTokens.collapseSpec(effectiveStyle, durationScale)
                } else {
                    MotionTokens.pillMorphSpec(effectiveStyle, durationScale)
                }
            }
            targetState == IslandMode.MEDIUM -> {
                if (initialState == IslandMode.EXPANDED) {
                    MotionTokens.collapseSpec(effectiveStyle, durationScale)
                } else {
                    MotionTokens.idleToMediumSpec(effectiveStyle, elasticity, durationScale)
                }
            }
            targetState == IslandMode.EXPANDED -> {
                MotionTokens.openingEffectSpec(settings.openingEffect, effectiveStyle, elasticity, durationScale)
            }
            else -> MotionTokens.spatialDefault(effectiveStyle, elasticity, durationScale)
        }
    }

    val morphWidthDpState = morphTransition.animateFloat(
        transitionSpec = morphSpringSpec,
        label = "morph_width"
    ) { mode ->
        when (mode) {
            IslandMode.IDLE -> idleWidthDp
            IslandMode.COMPACT -> compactMetrics.totalWidthDp
            IslandMode.SPLIT -> compactMetrics.totalWidthDp + 44f
            IslandMode.MEDIUM -> mediumTargetWidthDp
            IslandMode.EXPANDED -> fullTargetWidthDp
        }
    }
    val morphHeightDpState = morphTransition.animateFloat(
        transitionSpec = morphSpringSpec,
        label = "morph_height"
    ) { mode ->
        when (mode) {
            IslandMode.IDLE -> idleHeightDp
            IslandMode.COMPACT, IslandMode.SPLIT -> compactMetrics.pillHeightDp
            IslandMode.MEDIUM -> measuredMediumHeightDp.coerceIn(DeadZoneGeometry.MEDIUM_MIN_HEIGHT_DP, DeadZoneGeometry.MEDIUM_MAX_HEIGHT_DP)
            IslandMode.EXPANDED -> measuredExpandedHeightDp.coerceIn(220f, DeadZoneGeometry.FULL_MAX_HEIGHT_DP)
        }
    }
    val morphRadiusDpState = morphTransition.animateFloat(
        transitionSpec = morphSpringSpec,
        label = "morph_radius"
    ) { mode ->
        when (mode) {
            IslandMode.IDLE -> idleHeightDp * 0.5f
            IslandMode.COMPACT, IslandMode.SPLIT -> compactMetrics.pillHeightDp * 0.5f
            IslandMode.MEDIUM -> IslandColors.MediumOuterRadius.value
            IslandMode.EXPANDED -> IslandColors.ExpandedOuterRadius.value
        }
    }
    val morphPosYDpState = morphTransition.animateFloat(
        transitionSpec = morphSpringSpec,
        label = "morph_pos_y"
    ) { mode ->
        when (mode) {
            IslandMode.IDLE, IslandMode.COMPACT, IslandMode.SPLIT -> 0f
            IslandMode.MEDIUM, IslandMode.EXPANDED -> cardTopClearanceDp
        }
    }
    val glassAlphaState = morphTransition.animateFloat(
        transitionSpec = {
            if (effectiveStyle == AnimationStyle.NONE) snap()
            else MotionTokens.effectsDefault(durationScale)
        },
        label = "glass_alpha"
    ) { mode ->
        if (mode == IslandMode.MEDIUM || mode == IslandMode.EXPANDED) 1f else 0f
    }

    // Staggered Apple choreography: Header -> Body -> Bottom Chooser (<=80 ms clear on collapse, >=85% morph handover on open)
    val headerRevealState = morphTransition.animateFloat(
        transitionSpec = {
            when {
                effectiveStyle == AnimationStyle.NONE -> snap()
                targetState == IslandMode.EXPANDED || targetState == IslandMode.MEDIUM ->
                    tween(durationMillis = (180 / durationScale).toInt().coerceIn(70, 280), delayMillis = 60, easing = FastOutSlowInEasing)
                else -> tween(durationMillis = (70 / durationScale).toInt().coerceIn(30, 80), easing = FastOutLinearInEasing)
            }
        },
        label = "header_reveal"
    ) { mode ->
        if (mode == IslandMode.MEDIUM || mode == IslandMode.EXPANDED) 1f else 0f
    }
    val bodyRevealState = morphTransition.animateFloat(
        transitionSpec = {
            when {
                effectiveStyle == AnimationStyle.NONE -> snap()
                targetState == IslandMode.EXPANDED || targetState == IslandMode.MEDIUM ->
                    tween(durationMillis = (220 / durationScale).toInt().coerceIn(90, 340), delayMillis = 120, easing = MotionTokens.ContentCrossfadeEasing)
                else -> tween(durationMillis = (70 / durationScale).toInt().coerceIn(30, 80), easing = FastOutLinearInEasing)
            }
        },
        label = "body_reveal"
    ) { mode ->
        if (mode == IslandMode.MEDIUM || mode == IslandMode.EXPANDED) 1f else 0f
    }
    val chooserRevealState = morphTransition.animateFloat(
        transitionSpec = {
            when {
                effectiveStyle == AnimationStyle.NONE -> snap()
                targetState == IslandMode.EXPANDED ->
                    tween(durationMillis = (210 / durationScale).toInt().coerceIn(80, 320), delayMillis = 170, easing = MotionTokens.ContentCrossfadeEasing)
                else -> tween(durationMillis = (65 / durationScale).toInt().coerceIn(30, 80), easing = FastOutLinearInEasing)
            }
        },
        label = "chooser_reveal"
    ) { mode ->
        if (mode == IslandMode.EXPANDED) 1f else 0f
    }

    val collapsedBaseW = if (state.mode == IslandMode.IDLE) idleWidthDp else compactMetrics.totalWidthDp
    val morphStartW = if (morphTransition.currentState == IslandMode.MEDIUM && state.mode == IslandMode.EXPANDED) {
        mediumTargetWidthDp
    } else {
        collapsedBaseW
    }
    val morphEndW = if (state.mode == IslandMode.EXPANDED) fullTargetWidthDp else mediumTargetWidthDp
    val morphFrac = MotionTokens.computeMorphFraction(morphWidthDpState.value, morphStartW, morphEndW)
    val handoverGate = when {
        effectiveStyle == AnimationStyle.NONE -> 1f
        state.mode == IslandMode.MEDIUM || state.mode == IslandMode.EXPANDED ->
            ((morphFrac - 0.85f) / 0.15f).coerceIn(0f, 1f)
        else -> 0f
    }

    val isPressedNow = isPillPressedProvider()
    val pressScaleState = animateFloatAsState(
        targetValue = if (isPressedNow) MotionTokens.TAP_SQUISH_SCALE else 1f,
        animationSpec = if (effectiveStyle == AnimationStyle.NONE) snap() else MotionTokens.spatialFast(effectiveStyle, elasticity, durationScale),
        label = "press_squish"
    )

    val morphCapsuleShape = remember(morphWidthDpState, morphHeightDpState, morphRadiusDpState) {
        MorphCapsuleShape(
            widthDpState = morphWidthDpState,
            heightDpState = morphHeightDpState,
            radiusDpState = morphRadiusDpState
        )
    }

    val effectivePillStyle = if (settings.solidFallback) PillStyle.BLACK else settings.pillStyle
    val useGlass = effectivePillStyle != PillStyle.BLACK
    val pillBorderColor = when {
        state.isCalibrationPreview -> IslandColors.AccentRed
        state.isDragOverIsland -> accentColor
        useGlass -> IslandColors.GlassSpecularBorder
        else -> Color.White.copy(alpha = 0.16f)
    }

    val isCalibratedOrPreview = settings.calibrationConfirmed || state.isCalibrationPreview
    val showExpandedCard = state.mode == IslandMode.EXPANDED ||
        (effectiveStyle != AnimationStyle.NONE && morphTransition.currentState == IslandMode.EXPANDED && state.mode != IslandMode.MEDIUM)
    val showMediumStage = !showExpandedCard && (
        state.mode == IslandMode.MEDIUM ||
            (effectiveStyle != AnimationStyle.NONE && morphTransition.currentState == IslandMode.MEDIUM)
        )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val containerWidthDp = maxWidth.value
        if (state.isSelectModeActive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = (exclusionZone.holeBottomPlusMarginDp + 8f).dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(IslandColors.SelectionBlue)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Mode ON · Tap any text to adjust & copy",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (state.mode == IslandMode.EXPANDED || state.mode == IslandMode.MEDIUM) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onRequestHaptic()
                        app.islandController.collapse()
                    }
            )
        }

        // Anchor island relative to live DisplayCutout in Portrait or Landscape (accounting for shrunk windowOriginDp)
        val anchorAlign = when {
            exclusionZone.isLandscape && exclusionZone.isRightEdgeInLandscape -> Alignment.CenterEnd
            exclusionZone.isLandscape -> Alignment.CenterStart
            else -> Alignment.TopCenter
        }
        val windowCenterDp = windowOriginDp.x + (containerWidthDp / 2f)
        val anchorOffsetX = if (!exclusionZone.isLandscape) {
            (exclusionZone.cutoutRectDp.centerX - windowCenterDp).dp
        } else {
            4.dp
        }
        val anchorOffsetY = if (!exclusionZone.isLandscape) {
            ((exclusionZone.cutoutRectDp.top - 2f).coerceAtLeast(0f) - windowOriginDp.y).dp
        } else {
            0.dp
        }
        val swipeUpDismissModifier = if (settings.swipeUpDismissEnabled && state.mode != IslandMode.IDLE) {
            Modifier.pointerInput(state.mode, settings.swipeUpDismissEnabled) {
                var dragY = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragY = 0f },
                    onVerticalDrag = { _, delta -> dragY += delta },
                    onDragEnd = {
                        if (dragY < -44f) {
                            onRequestHaptic()
                            when (state.mode) {
                                IslandMode.EXPANDED, IslandMode.MEDIUM -> app.islandController.collapse()
                                IslandMode.COMPACT, IslandMode.SPLIT -> {
                                    val p = state.primaryActivity
                                    if (p is IslandActivity.NotificationPreview || p is IslandActivity.Alert) {
                                        app.islandController.dismissActiveNotification()
                                    } else {
                                        app.islandController.collapseToIdle()
                                    }
                                }
                                IslandMode.IDLE -> Unit
                            }
                        }
                    }
                )
            }
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .align(anchorAlign)
                .offset(x = anchorOffsetX, y = anchorOffsetY)
                .then(swipeUpDismissModifier),
            contentAlignment = anchorAlign
        ) {
            when {
                !isCalibratedOrPreview && state.mode != IslandMode.EXPANDED && state.mode != IslandMode.MEDIUM -> {
                    IdlePillView(
                        settings = settings,
                        state = state,
                        exclusionZone = exclusionZone,
                        accentColor = accentColor,
                        isCalibrationPreview = false,
                        isDragOverIsland = state.isDragOverIsland,
                        isLowBatteryWarning = state.isLowBatteryWarning
                    )
                }

                showExpandedCard -> {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(0, (morphPosYDpState.value * densityPx).roundToInt()) }
                            .graphicsLayer {
                                val pull = pullDownProgressProvider()
                                val press = pressScaleState.value
                                val uniformPull = 1f + pull * 0.04f
                                transformOrigin = TransformOrigin(0.5f, 0f)
                                scaleX = press * uniformPull
                                scaleY = press * uniformPull
                                shape = morphCapsuleShape
                                clip = true
                                translationY = pull * 16f
                            }
                            .drawBehind {
                                drawMorphCapsuleChrome(
                                    widthDp = morphWidthDpState.value,
                                    heightDp = morphHeightDpState.value,
                                    radiusDp = morphRadiusDpState.value,
                                    glassAlpha = glassAlphaState.value,
                                    pillStyle = effectivePillStyle,
                                    glassDensity = settings.glassDensity,
                                    reduceGlass = settings.reduceGlass,
                                    solidFallback = settings.solidFallback,
                                    accentColor = notifCardAccent,
                                    isHighlighted = state.isDragOverIsland,
                                    performanceMode = settings.performanceMode
                                )
                            }
                            .onSizeChanged { sz ->
                                val hDp = sz.height / densityPx
                                if (hDp >= 120f && abs(hDp - measuredExpandedHeightDp) > 2f) {
                                    measuredExpandedHeightDp = hDp.coerceIn(220f, DeadZoneGeometry.FULL_MAX_HEIGHT_DP)
                                }
                            }
                    ) {
                        ExpandedIslandCard(
                            app = app,
                            state = state,
                            settings = settings,
                            exclusionZone = exclusionZone,
                            accentColor = accentColor,
                            musicAccent = musicAccent,
                            effectivePillStyle = effectivePillStyle,
                            crossWindowBlurSupported = crossWindowBlurSupported,
                            effectiveStyle = effectiveStyle,
                            elasticity = elasticity,
                            durationScale = durationScale,
                            drawOwnChrome = false,
                            headerAlphaProvider = { (headerRevealState.value * handoverGate).coerceIn(0f, 1f) },
                            contentAlphaProvider = { (bodyRevealState.value * handoverGate).coerceIn(0f, 1f) },
                            contentOffsetYDProvider = { (1f - (bodyRevealState.value * handoverGate).coerceIn(0f, 1f)) * 10f },
                            chooserAlphaProvider = { (chooserRevealState.value * handoverGate).coerceIn(0f, 1f) },
                            onRequestHaptic = onRequestHaptic,
                            onOpenAppSettings = onOpenAppSettings
                        )
                    }
                }

                showMediumStage -> {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(0, (morphPosYDpState.value * densityPx).roundToInt()) }
                            .graphicsLayer {
                                val pull = pullDownProgressProvider()
                                val press = pressScaleState.value
                                val uniformPull = 1f + pull * 0.04f
                                transformOrigin = TransformOrigin(0.5f, 0f)
                                scaleX = press * uniformPull
                                scaleY = press * uniformPull
                                shape = morphCapsuleShape
                                clip = true
                                translationY = pull * 16f
                            }
                            .drawBehind {
                                drawMorphCapsuleChrome(
                                    widthDp = morphWidthDpState.value,
                                    heightDp = morphHeightDpState.value,
                                    radiusDp = morphRadiusDpState.value,
                                    glassAlpha = glassAlphaState.value,
                                    pillStyle = effectivePillStyle,
                                    glassDensity = settings.glassDensity,
                                    reduceGlass = settings.reduceGlass,
                                    solidFallback = settings.solidFallback,
                                    accentColor = notifCardAccent,
                                    isHighlighted = state.isDragOverIsland,
                                    performanceMode = settings.performanceMode
                                )
                            }
                            .onSizeChanged { sz ->
                                val hDp = sz.height / densityPx
                                if (hDp >= 120f && abs(hDp - measuredMediumHeightDp) > 2f) {
                                    measuredMediumHeightDp = hDp.coerceIn(DeadZoneGeometry.MEDIUM_MIN_HEIGHT_DP, DeadZoneGeometry.MEDIUM_MAX_HEIGHT_DP)
                                }
                            }
                    ) {
                        MediumPillView(
                            app = app,
                            state = state,
                            settings = settings,
                            exclusionZone = exclusionZone,
                            accentColor = accentColor,
                            musicAccent = musicAccent,
                            effectivePillStyle = effectivePillStyle,
                            crossWindowBlurSupported = crossWindowBlurSupported,
                            mediaState = mediaState,
                            timerState = timerState,
                            drawOwnChrome = false,
                            headerAlphaProvider = { (headerRevealState.value * handoverGate).coerceIn(0f, 1f) },
                            bodyAlphaProvider = { (bodyRevealState.value * handoverGate).coerceIn(0f, 1f) },
                            onRequestHaptic = onRequestHaptic
                        )
                    }
                }

                state.mode == IslandMode.IDLE -> {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(0, (morphPosYDpState.value * densityPx).roundToInt()) }
                            .graphicsLayer {
                                val pull = pullDownProgressProvider()
                                val press = pressScaleState.value
                                val boost = press * (1f + pull * 0.14f)
                                val ratioX = (morphWidthDpState.value / idleWidthDp.coerceAtLeast(1f)).coerceIn(0.85f, 1.25f)
                                transformOrigin = TransformOrigin(0.5f, 0f)
                                scaleX = ratioX * boost
                                scaleY = boost
                                translationY = pull * 22f
                            }
                    ) {
                        IdlePillView(
                            settings = settings,
                            state = state,
                            exclusionZone = exclusionZone,
                            accentColor = accentColor,
                            isCalibrationPreview = state.isCalibrationPreview,
                            isDragOverIsland = state.isDragOverIsland,
                            isLowBatteryWarning = state.isLowBatteryWarning
                        )
                    }
                }

                else -> {
                    val showCompanion = state.mode == IslandMode.SPLIT && state.secondaryActivity != null
                    val companionView: @Composable () -> Unit = {
                        AnimatedVisibility(
                            visible = showCompanion,
                            enter = if (effectiveStyle == AnimationStyle.NONE) {
                                fadeIn(tween(0))
                            } else {
                                fadeIn(MotionTokens.effectsDefault(durationScale)) + scaleIn(
                                    animationSpec = MotionTokens.spatialFast(effectiveStyle, elasticity, durationScale),
                                    initialScale = 0.5f
                                )
                            },
                            exit = if (effectiveStyle == AnimationStyle.NONE) {
                                fadeOut(tween(0))
                            } else {
                                fadeOut(MotionTokens.effectsFast(durationScale)) + scaleOut(
                                    animationSpec = MotionTokens.collapseSpec(effectiveStyle, durationScale),
                                    targetScale = 0.5f
                                )
                            }
                        ) {
                            state.secondaryActivity?.let { secondary ->
                                SplitSecondaryBubble(
                                    activity = secondary,
                                    companionCount = state.companionCount,
                                    accentColor = activityAccent(secondary, accentColor, musicAccent),
                                    borderColor = pillBorderColor,
                                    effectivePillStyle = effectivePillStyle,
                                    glassDensity = settings.glassDensity,
                                    reduceGlass = settings.reduceGlass,
                                    solidFallback = settings.solidFallback,
                                    crossWindowBlurActive = crossWindowBlurSupported,
                                    onClick = {
                                        onRequestHaptic()
                                        app.islandController.swapPrimaryAndSecondary()
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .offset { IntOffset(0, (morphPosYDpState.value * densityPx).roundToInt()) }
                            .metaballParentLayer(settings.metaballRenderEffect && showCompanion && !reduceMotion)
                            .graphicsLayer {
                                val pull = pullDownProgressProvider()
                                val boost = pressScaleState.value * (1f + pull * 0.14f)
                                val pillRatioX = (morphWidthDpState.value / compactMetrics.totalWidthDp.coerceAtLeast(1f)).coerceIn(0.84f, 1.24f)
                                transformOrigin = TransformOrigin(0.5f, 0f)
                                scaleX = (if (showCompanion) 1f else pillRatioX) * boost
                                scaleY = boost
                                translationY = pull * 22f
                            }
                    ) {
                        if (settings.companionPosition == CompanionPosition.LEFT) {
                            companionView()
                        }
                        CompactPillView(
                            state = state,
                            settings = settings,
                            exclusionZone = exclusionZone,
                            accentColor = notifCardAccent,
                            musicAccent = musicAccent,
                            isPlaying = mediaState.isPlaying,
                            albumArt = mediaState.albumArt,
                            effectivePillStyle = effectivePillStyle,
                            crossWindowBlurSupported = crossWindowBlurSupported,
                            onCopyOtp = { otp ->
                                onRequestHaptic()
                                app.soundManager.playCopy()
                                app.clipboardMonitor.copyToClipboard(otp)
                                app.islandController.showTransientBanner("Copied $otp")
                            }
                        )
                        if (settings.companionPosition == CompanionPosition.RIGHT) {
                            companionView()
                        }
                    }
                }
            }
        }

        // §0 Concentric Energy Ring hugging the front camera punch-hole ONLY while charging (never on pill/card perimeters)
        val isChargingNow = state.isBatteryCharging || primaryActivity is IslandActivity.Charging
        if (settings.energyRingEnabled &&
            isChargingNow &&
            !state.isCalibrationPreview &&
            !state.isDragOverIsland &&
            !exclusionZone.isLandscape &&
            (state.mode == IslandMode.IDLE || state.mode == IslandMode.COMPACT || state.mode == IslandMode.SPLIT)
        ) {
            val ringColor = when {
                state.batteryLevelPercent <= 15 -> IslandColors.AccentRed
                state.batteryLevelPercent <= 30 -> IslandColors.AccentAmber
                else -> IslandColors.AccentGreen
            }
            val pctFrac = (state.batteryLevelPercent / 100f).coerceIn(0.05f, 1f)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("camera_charging_energy_ring")
            ) {
                val gapPx = 2.5.dp.toPx()
                val strokePx = 2.0.dp.toPx()
                val cutL = (exclusionZone.cutoutRectDp.left - windowOriginDp.x).dp.toPx() - gapPx
                val cutT = (exclusionZone.cutoutRectDp.top - windowOriginDp.y).dp.toPx() - gapPx
                val cutW = exclusionZone.cutoutRectDp.width.dp.toPx() + gapPx * 2f
                val cutH = exclusionZone.cutoutRectDp.height.dp.toPx() + gapPx * 2f
                drawArc(
                    color = ringColor.copy(alpha = 0.22f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(cutL, cutT),
                    size = Size(cutW.coerceAtLeast(1f), cutH.coerceAtLeast(1f)),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * pctFrac,
                    useCenter = false,
                    topLeft = Offset(cutL, cutT),
                    size = Size(cutW.coerceAtLeast(1f), cutH.coerceAtLeast(1f)),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        // §4.8 Clickable Privacy Dots indicator beside collapsed pill with popup tooltip showing active app names
        var showPrivacyTooltip by remember { mutableStateOf(false) }
        if (settings.privacyDotsEnabled &&
            state.privacyIndicator.anyActive &&
            (state.mode == IslandMode.IDLE || state.mode == IslandMode.COMPACT || state.mode == IslandMode.SPLIT) &&
            !exclusionZone.isLandscape
        ) {
            val activeHalfW = when (state.mode) {
                IslandMode.IDLE -> idleWidthDp / 2f
                IslandMode.SPLIT -> (compactMetrics.totalWidthDp + 44f) / 2f
                else -> compactMetrics.totalWidthDp / 2f
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .align(anchorAlign)
                    .offset(x = anchorOffsetX - (activeHalfW + 18f).dp, y = (anchorOffsetY + 11.dp))
                    .clip(CircleShape)
                    .clickable {
                        onRequestHaptic()
                        showPrivacyTooltip = !showPrivacyTooltip
                    }
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .testTag("privacy_dots_row")
            ) {
                if (state.privacyIndicator.cameraActive) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(IslandColors.PrivacyCameraGreen))
                }
                if (state.privacyIndicator.micActive) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(IslandColors.PrivacyMicOrange))
                }
                if (state.privacyIndicator.locationActive) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(IslandColors.PrivacyLocBlue))
                }
            }
            if (showPrivacyTooltip) {
                val activeList = state.privacyIndicator.activeApps.ifEmpty {
                    buildList {
                        if (state.privacyIndicator.cameraActive) add("Camera")
                        if (state.privacyIndicator.micActive) add("Mic")
                        if (state.privacyIndicator.locationActive) add("Location")
                    }
                }
                Box(
                    modifier = Modifier
                        .align(anchorAlign)
                        .offset(x = anchorOffsetX, y = (anchorOffsetY + 42.dp))
                        .clip(CircleShape)
                        .background(IslandColors.ObsidianCard.copy(alpha = 0.94f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.24f), CircleShape)
                        .clickable { showPrivacyTooltip = false }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("privacy_dots_tooltip")
                ) {
                    Text(
                        text = activeList.joinToString(" · "),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }

        // §4.16 Auto-Rotate Suggestion floating pill when auto-rotate is OFF and phone is held in landscape
        val pendingRotateTarget = state.pendingAutoRotateSuggestion
        if (pendingRotateTarget != null &&
            settings.autoRotateSuggestionEnabled &&
            (state.mode == IslandMode.IDLE || state.mode == IslandMode.COMPACT || state.mode == IslandMode.SPLIT)
        ) {
            val activeHalfW = when (state.mode) {
                IslandMode.IDLE -> idleWidthDp / 2f
                IslandMode.SPLIT -> (compactMetrics.totalWidthDp + 44f) / 2f
                else -> compactMetrics.totalWidthDp / 2f
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .align(anchorAlign)
                    .offset(x = anchorOffsetX + (activeHalfW + 34f).dp, y = (anchorOffsetY + 4.dp))
                    .height(30.dp)
                    .clip(CircleShape)
                    .background(IslandColors.ObsidianCard.copy(alpha = 0.92f))
                    .border(0.9.dp, accentColor.copy(alpha = 0.72f), CircleShape)
                    .clickable {
                        onRequestHaptic()
                        if (Settings.System.canWrite(context)) {
                            runCatching {
                                Settings.System.putInt(context.contentResolver, Settings.System.USER_ROTATION, pendingRotateTarget)
                            }
                        } else {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                Uri.parse("package:${context.packageName}")
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            runCatching { context.startActivity(intent) }
                        }
                        app.islandController.setAutoRotateSuggestion(null)
                    }
                    .padding(horizontal = 10.dp)
                    .testTag("auto_rotate_suggestion_chip")
            ) {
                Icon(Icons.Filled.ScreenRotation, contentDescription = "Rotate screen", tint = accentColor, modifier = Modifier.size(14.dp))
                Text("Rotate", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (state.isFlashlightCardOpen) {
            FlashlightBeamOverlayCard(
                app = app,
                accentColor = accentColor,
                onDismiss = { app.islandController.setFlashlightCardOpen(false) }
            )
        }

        val ocrSel = state.ocrSelection
        if (ocrSel != null) {
            OcrSelectionOverlay(
                selection = ocrSel,
                onUpdateRange = { s, e, h, ax, ay ->
                    app.islandController.updateOcrRange(s, e, h, ax, ay)
                },
                onFinishDrag = { app.islandController.finishOcrHandleDrag() },
                onSelectAll = { app.islandController.selectAllOcrTokens() },
                onCopy = { selected ->
                    app.soundManager.playCopy()
                    app.clipboardMonitor.copyToClipboard(selected)
                    OcrBitmapHolder.clear()
                    app.islandController.dismissOcrSelection()
                    app.islandController.upsertActivity(
                        IslandActivity.Alert(
                            id = "ocr_copied_${System.currentTimeMillis()}",
                            kind = AlertKind.OCR_COPIED,
                            title = "Copied text",
                            subtitle = selected.take(56)
                        ),
                        autoShowCompact = true,
                        autoDismissMs = 2000L
                    )
                },
                onShare = {},
                onDismiss = {
                    OcrBitmapHolder.clear()
                    app.islandController.dismissOcrSelection()
                },
                onHapticTick = onRequestHaptic
            )
        }

        // Floating Liquid Glass Permission Approval Prompt
        val approval = state.approvalPrompt
        AnimatedVisibility(
            visible = approval != null,
            enter = fadeIn(tween(160)) + scaleIn(initialScale = 0.94f),
            exit = fadeOut(tween(120)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = (cardTopClearanceDp + 6f).dp, start = 16.dp, end = 16.dp)
        ) {
            if (approval != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .widthIn(max = fullTargetWidthDp.dp)
                        .fillMaxWidth()
                        .islandGlassChrome(
                            shape = RoundedCornerShape(24.dp),
                            pillStyle = effectivePillStyle,
                            glassDensity = settings.glassDensity,
                            reduceGlass = settings.reduceGlass,
                            solidFallback = settings.solidFallback,
                            accentColor = accentColor,
                            isHighlighted = true,
                            backdrop = LocalAppBackdrop.current,
                            crossWindowBlurActive = crossWindowBlurSupported,
                            performanceMode = settings.performanceMode
                        )
                        .padding(16.dp)
                        .testTag("floating_approval_prompt")
                ) {
                    Text(approval.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(approval.reason, color = Color.White.copy(alpha = 0.78f), fontSize = 13.sp, lineHeight = 18.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .height(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable {
                                    onRequestHaptic()
                                    app.islandController.dismissApprovalPrompt()
                                }
                                .padding(horizontal = 16.dp)
                        ) {
                            Text("Later", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .height(40.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                                .clickable {
                                    onRequestHaptic()
                                    val intent = when (approval.permissionKey) {
                                        "overlay" -> Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        "notification_listener" -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                        "accessibility" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                        else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                                    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    runCatching { context.startActivity(intent) }
                                    app.islandController.dismissApprovalPrompt()
                                }
                                .padding(horizontal = 18.dp)
                        ) {
                            Text(approval.actionLabel, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = state.statusBanner != null,
            enter = fadeIn(tween(140)),
            exit = fadeOut(tween(120)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = (exclusionZone.holeBottomPlusMarginDp + 16f).dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(IslandColors.ObsidianCard)
                    .border(1.dp, accentColor.copy(alpha = 0.65f), CircleShape)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = state.statusBanner.orEmpty(),
                    color = IslandColors.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun activityAccent(
    activity: IslandActivity,
    defaultAccent: Color,
    musicAccent: Color
): Color = when (activity) {
    is IslandActivity.Timer -> defaultAccent
    is IslandActivity.Charging -> IslandColors.AccentGreen
    is IslandActivity.Music -> musicAccent
    is IslandActivity.VoiceRecording, is IslandActivity.ScreenRecording -> IslandColors.AccentRed
    is IslandActivity.DownloadProgress -> IslandColors.AccentCyan
    is IslandActivity.Alert -> if (activity.kind == AlertKind.LOW_BATTERY) IslandColors.AccentRed else defaultAccent
    else -> defaultAccent
}

/**
 * WhenIdleMode: CAMERA_ONLY / INFO_PILL (dual symmetric 88dp slots supporting all 12 widgets) / HIDDEN.
 */
@Composable
fun IdlePillView(
    settings: IslandSettings,
    state: IslandState = IslandState(),
    exclusionZone: ExclusionZone = DeadZoneGeometry.fromSyntheticHole(settings.cutoutWidthDp, settings.cutoutHeightDp),
    accentColor: Color,
    isCalibrationPreview: Boolean,
    isDragOverIsland: Boolean,
    isLowBatteryWarning: Boolean
) {
    if (settings.whenIdleMode == WhenIdleMode.HIDDEN && !isCalibrationPreview && !isDragOverIsland) {
        return
    }

    val holeW = exclusionZone.holeWidthDp.coerceAtLeast(settings.cutoutWidthDp)
    val holeH = exclusionZone.holeHeightDp.coerceAtLeast(settings.cutoutHeightDp)
    val ringBorderColor = when {
        isCalibrationPreview -> IslandColors.AccentRed
        isDragOverIsland -> accentColor
        isLowBatteryWarning -> IslandColors.AccentRed.copy(alpha = 0.78f)
        else -> Color.White.copy(alpha = 0.24f)
    }
    val strokeWidth = if (isDragOverIsland || isCalibrationPreview || isLowBatteryWarning) 2.dp else 1.dp

    if (settings.whenIdleMode == WhenIdleMode.INFO_PILL && !isCalibrationPreview) {
        val metrics = DeadZoneGeometry.compactSlotMetrics(
            holeWidthDp = holeW,
            holeHeightDp = holeH,
            isLandscape = exclusionZone.isLandscape,
            slotWidthDp = settings.pillSizePreset.slotWidthDp
        )
        var minuteTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
        LaunchedEffect(Unit) {
            while (isActive) {
                minuteTick = System.currentTimeMillis()
                delay(20_000L)
            }
        }
        val effectivePillStyle = if (settings.solidFallback) PillStyle.BLACK else settings.pillStyle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(metrics.pillHeightDp.dp)
                .islandGlassChrome(
                    shape = CircleShape,
                    pillStyle = effectivePillStyle,
                    glassDensity = settings.glassDensity,
                    reduceGlass = settings.reduceGlass,
                    solidFallback = settings.solidFallback,
                    accentColor = accentColor,
                    isHighlighted = isDragOverIsland || isLowBatteryWarning,
                    backdrop = LocalAppBackdrop.current,
                    performanceMode = settings.performanceMode
                )
                .padding(horizontal = metrics.outerPadDp.dp)
                .testTag("idle_info_pill")
        ) {
            Box(
                modifier = Modifier
                    .width(metrics.leftSlotDp.dp)
                    .debugCameraDeadZoneGuard(exclusionZone, "idle_left"),
                contentAlignment = Alignment.CenterStart
            ) {
                IdleWidgetSlot(
                    type = settings.idleLeftWidget,
                    state = state,
                    nowMs = minuteTick,
                    accentColor = accentColor,
                    isLowBattery = isLowBatteryWarning
                )
            }
            Spacer(Modifier.width(metrics.centerSpacerDp.dp))
            Box(
                modifier = Modifier
                    .width(metrics.rightSlotDp.dp)
                    .debugCameraDeadZoneGuard(exclusionZone, "idle_right"),
                contentAlignment = Alignment.CenterEnd
            ) {
                IdleWidgetSlot(
                    type = settings.idleRightWidget,
                    state = state,
                    nowMs = minuteTick,
                    accentColor = accentColor,
                    isLowBattery = isLowBatteryWarning
                )
            }
        }
        return
    }

    val idleMetrics = remember(holeW, holeH, settings.appleWidthIdlePill) {
        DeadZoneGeometry.idleCameraPillMetrics(holeW, holeH, appleWidth = settings.appleWidthIdlePill)
    }
    val hasExplicitBorder = isCalibrationPreview || isDragOverIsland || isLowBatteryWarning
    val idleBorder = when {
        isCalibrationPreview || isLowBatteryWarning -> IslandColors.AccentRed.copy(alpha = 0.90f)
        isDragOverIsland -> accentColor
        else -> Color.Transparent
    }

    // Idle CAMERA_ONLY state (§0): pure #000000 100% opacity, ZERO shadow, ZERO border, ZERO idle ring,
    // hugging only the camera hole (48-58 dp) or optional Apple-width (125 dp).
    Box(
        modifier = Modifier
            .width(idleMetrics.widthDp.dp)
            .height(idleMetrics.heightDp.dp)
            .clip(CircleShape)
            .background(IslandColors.PillBlack)
            .then(
                if (hasExplicitBorder) {
                    Modifier.border(width = 1.4.dp, color = idleBorder, shape = CircleShape)
                } else {
                    Modifier
                }
            )
            .testTag("idle_camera_ring")
    )
}

@Composable
private fun IdleWidgetSlot(
    type: IdleWidgetType,
    state: IslandState,
    nowMs: Long,
    accentColor: Color,
    isLowBattery: Boolean
) {
    val context = LocalContext.current
    val timeText = remember(nowMs) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMs)) }
    val dateText = remember(nowMs) { SimpleDateFormat("EEE d", Locale.getDefault()).format(Date(nowMs)) }

    when (type) {
        IdleWidgetType.NONE -> Spacer(Modifier.width(8.dp))
        IdleWidgetType.TIME, IdleWidgetType.CLOCK -> Text(
            text = timeText,
            style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
            maxLines = 1
        )
        IdleWidgetType.DATE -> Text(
            text = dateText,
            color = IslandColors.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        IdleWidgetType.BATTERY -> Text(
            text = "${state.batteryLevelPercent}%",
            color = if (isLowBattery) IslandColors.AccentRed else IslandColors.AccentGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        IdleWidgetType.BATTERY_RING -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val frac = (state.batteryLevelPercent / 100f).coerceIn(0.05f, 1f)
            val color = if (isLowBattery) IslandColors.AccentRed else IslandColors.AccentGreen
            Canvas(modifier = Modifier.size(12.dp)) {
                drawArc(color = Color.White.copy(alpha = 0.2f), startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(2.dp.toPx()))
                drawArc(color = color, startAngle = -90f, sweepAngle = 360f * frac, useCenter = false, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            }
            Text("${state.batteryLevelPercent}%", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        IdleWidgetType.NEXT_ALARM -> {
            val nextAlarm = remember(nowMs) {
                val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                val trigger = runCatching { am?.nextAlarmClock?.triggerTime }.getOrNull()
                if (trigger != null && trigger > 0L) SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(trigger)) else "--:--"
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Icon(Icons.Filled.Alarm, contentDescription = "Next alarm", tint = accentColor, modifier = Modifier.size(12.dp))
                Text(nextAlarm, color = Color.White, fontSize = 11.sp, maxLines = 1)
            }
        }
        IdleWidgetType.UNREAD_COUNT -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(Icons.Filled.Notifications, contentDescription = "Unread", tint = accentColor, modifier = Modifier.size(12.dp))
            Text("${state.notificationsHistory.size}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
        IdleWidgetType.DND_STATUS -> {
            val isDndOn = remember(nowMs) {
                runCatching {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    val filter = nm?.currentInterruptionFilter
                    filter != null && filter != NotificationManager.INTERRUPTION_FILTER_ALL
                }.getOrDefault(false)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Icon(
                    Icons.Filled.DoNotDisturbOn,
                    contentDescription = "Do Not Disturb",
                    tint = if (isDndOn) IslandColors.AccentAmber else IslandColors.TextSecondary,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = if (isDndOn) "DND" else "Ring",
                    color = if (isDndOn) IslandColors.AccentAmber else Color.White,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
        IdleWidgetType.NET_SPEED -> {
            var prevBytes by remember { mutableLongStateOf(TrafficStats.getTotalRxBytes().coerceAtLeast(0L)) }
            val speedLabel = remember(nowMs) {
                val cur = TrafficStats.getTotalRxBytes().coerceAtLeast(0L)
                val kbps = ((cur - prevBytes).coerceAtLeast(0L) / 20_480L).toInt()
                prevBytes = cur
                "${kbps}K/s"
            }
            Text(speedLabel, color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        IdleWidgetType.SEARCH_SHORTCUT -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(Icons.Filled.Search, contentDescription = "Search", tint = accentColor, modifier = Modifier.size(12.dp))
            Text("Search", color = IslandColors.TextSecondary, fontSize = 11.sp, maxLines = 1)
        }
        IdleWidgetType.QUICK_NOTE -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(Icons.Filled.Edit, contentDescription = "Quick note", tint = IslandColors.AccentLime, modifier = Modifier.size(12.dp))
            Text("Note", color = IslandColors.TextSecondary, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
fun CompactPillView(
    state: IslandState,
    settings: IslandSettings,
    exclusionZone: ExclusionZone = DeadZoneGeometry.fromSyntheticHole(settings.cutoutWidthDp, settings.cutoutHeightDp),
    accentColor: Color,
    musicAccent: Color = accentColor,
    isPlaying: Boolean = false,
    albumArt: Bitmap? = null,
    effectivePillStyle: PillStyle = if (settings.solidFallback) PillStyle.BLACK else settings.pillStyle,
    crossWindowBlurSupported: Boolean = false,
    onCopyOtp: (String) -> Unit = {}
) {
    val activity = state.primaryActivity
    val hideTextOnLock = state.isDeviceLocked && settings.lockScreenPreset == LockScreenPreset.PRIVACY_HIDE_TEXT
    val holeW = exclusionZone.holeWidthDp.coerceAtLeast(settings.cutoutWidthDp)
    val holeH = exclusionZone.holeHeightDp.coerceAtLeast(settings.cutoutHeightDp)
    val metrics = when (activity) {
        is IslandActivity.Music -> DeadZoneGeometry.musicCompactPillMetrics(
            holeWidthDp = holeW,
            holeHeightDp = holeH,
            isLandscape = exclusionZone.isLandscape
        )
        is IslandActivity.Alert -> DeadZoneGeometry.compactSlotMetrics(
            holeWidthDp = holeW,
            holeHeightDp = holeH,
            isLandscape = exclusionZone.isLandscape,
            slotWidthDp = maxOf(settings.pillSizePreset.slotWidthDp, 112f)
        )
        else -> DeadZoneGeometry.compactSlotMetrics(
            holeWidthDp = holeW,
            holeHeightDp = holeH,
            isLandscape = exclusionZone.isLandscape,
            slotWidthDp = settings.pillSizePreset.slotWidthDp
        )
    }
    val context = LocalContext.current

    val notifAppIcon: Bitmap? = remember(activity) {
        val pkg = (activity as? IslandActivity.NotificationPreview)?.packageName ?: return@remember null
        runCatching {
            context.packageManager.getApplicationIcon(pkg).toBitmap(width = 48, height = 48)
        }.getOrNull()
    }

    val isLowBattery = (activity is IslandActivity.Alert && activity.kind == AlertKind.LOW_BATTERY) ||
        (state.isLowBatteryWarning && activity is IslandActivity.Charging)

    // Per-feature animation signatures (§2): Alert top-bounce + glow pulse, Music stretch-X lead
    val alertBounceY = remember(activity?.id) { Animatable(if (activity is IslandActivity.Alert) -10f else 0f) }
    val alertGlowAlpha = remember(activity?.id) { Animatable(0f) }
    val musicScaleX = remember(activity?.id) { Animatable(if (activity is IslandActivity.Music) 0.90f else 1f) }
    LaunchedEffect(activity?.id) {
        if (activity is IslandActivity.Alert) {
            alertBounceY.snapTo(-10f)
            alertBounceY.animateTo(0f, animationSpec = MotionTokens.alertBounceSpring())
            alertGlowAlpha.snapTo(0.35f)
            alertGlowAlpha.animateTo(0f, animationSpec = tween(550))
        } else {
            alertBounceY.snapTo(0f)
            alertGlowAlpha.snapTo(0f)
        }
        if (activity is IslandActivity.Music) {
            musicScaleX.snapTo(0.88f)
            musicScaleX.animateTo(1f, animationSpec = MotionTokens.appleSpring())
        } else {
            musicScaleX.snapTo(1f)
        }
    }

    val pillModifier = Modifier
        .graphicsLayer {
            translationY = alertBounceY.value * density
            scaleX = musicScaleX.value
        }
        .drawBehind {
            if (alertGlowAlpha.value > 0.01f) {
                drawRoundRect(
                    color = accentColor.copy(alpha = alertGlowAlpha.value),
                    cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                )
            }
        }
        .islandGlassChrome(
            shape = CircleShape,
            pillStyle = effectivePillStyle,
            glassDensity = settings.glassDensity,
            reduceGlass = settings.reduceGlass,
            solidFallback = settings.solidFallback,
            accentColor = if (isLowBattery) IslandColors.AccentRed else accentColor,
            isHighlighted = state.isDragOverIsland || state.isCalibrationPreview || isLowBattery,
            backdrop = LocalAppBackdrop.current,
            crossWindowBlurActive = crossWindowBlurSupported,
            performanceMode = settings.performanceMode
        )

    if (metrics.isVerticalLandscape) {
        // Landscape camera-around vertical pill: top slot (44dp) -> cutout spacer -> bottom slot (44dp)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = pillModifier
                .width(metrics.pillHeightDp.dp)
                .padding(vertical = metrics.outerPadDp.dp)
                .testTag("compact_pill_landscape")
        ) {
            Box(
                modifier = Modifier
                    .height(metrics.leftSlotDp.dp)
                    .debugCameraDeadZoneGuard(exclusionZone, "compact_top"),
                contentAlignment = Alignment.Center
            ) {
                CompactLeftGlyph(
                    activity = activity,
                    albumArt = albumArt,
                    notifAppIcon = notifAppIcon,
                    accentColor = accentColor,
                    musicAccent = musicAccent,
                    isLowBattery = isLowBattery
                )
            }
            Spacer(Modifier.height(metrics.centerSpacerDp.dp))
            Box(
                modifier = Modifier
                    .height(metrics.rightSlotDp.dp)
                    .debugCameraDeadZoneGuard(exclusionZone, "compact_bottom"),
                contentAlignment = Alignment.Center
            ) {
                when (activity) {
                    is IslandActivity.Timer -> Text(
                        text = TimerEngine.formatDuration(activity.remainingMs),
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    is IslandActivity.Charging -> Text(
                        text = "${activity.levelPercent}%",
                        color = if (isLowBattery) IslandColors.AccentRed else IslandColors.AccentGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    is IslandActivity.Music -> MusicCompactWaveform(isPlaying = isPlaying, accentColor = musicAccent, positionMs = activity.positionMs)
                    else -> Spacer(Modifier.size(12.dp))
                }
            }
        }
        return
    }

    // Portrait: symmetrical leftSlotDp + centerSpacerDp (holeW+16dp) + rightSlotDp.
    // §0 HARD CAP: Music pill is strictly 180-200 dp (never >210 dp) and displays Album Art + Visualizer ONLY.
    Box(
        modifier = pillModifier
            .width(metrics.totalWidthDp.dp)
            .height(metrics.pillHeightDp.dp)
            .testTag("compact_pill_portrait")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = metrics.outerPadDp.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(metrics.leftSlotDp.dp)
                    .padding(end = metrics.innerSafetyInsetDp.dp)
                    .debugCameraDeadZoneGuard(exclusionZone, "compact_left"),
                contentAlignment = Alignment.CenterStart
            ) {
                CompactLeftGlyph(
                    activity = activity,
                    albumArt = albumArt,
                    notifAppIcon = notifAppIcon,
                    accentColor = accentColor,
                    musicAccent = musicAccent,
                    isLowBattery = isLowBattery
                )
            }

            Spacer(Modifier.width(metrics.centerSpacerDp.dp))

            Box(
                modifier = Modifier
                    .width(metrics.rightSlotDp.dp)
                    .padding(start = metrics.innerSafetyInsetDp.dp)
                    .debugCameraDeadZoneGuard(exclusionZone, "compact_right"),
                contentAlignment = Alignment.CenterEnd
            ) {
                when (activity) {
                    is IslandActivity.Music -> {
                        // §0 CRITICAL RULE: NO song title text in the compact pill EVER — visualizer bars ONLY.
                        MusicCompactWaveform(isPlaying = isPlaying, accentColor = musicAccent, positionMs = activity.positionMs)
                    }
                    is IslandActivity.Timer -> {
                        val secText = TimerEngine.formatDuration(activity.remainingMs)
                        AnimatedContent(
                            targetState = secText,
                            transitionSpec = {
                                (fadeIn(tween(140)) + slideInVertically(tween(150)) { it / 2 })
                                    .togetherWith(fadeOut(tween(100)) + slideOutVertically(tween(120)) { -it / 2 })
                            },
                            label = "timer_compact_digits"
                        ) { digit ->
                            Text(
                                text = digit,
                                style = TextStyle(
                                    color = accentColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFeatureSettings = "tnum"
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .widthIn(max = (metrics.rightSlotDp - metrics.innerSafetyInsetDp).dp)
                                    .debugCameraDeadZoneGuard(exclusionZone, "timer_text")
                            )
                        }
                    }
                    is IslandActivity.Charging -> {
                        // §3 P0-3 Glimpse: single live value (e.g. "100% · 99W" or "92%") cleanly right of camera
                        val wattsText = activity.wattageWatts?.takeIf { it > 0.5f && activity.levelPercent < 100 }
                            ?.let { " · ${it.roundToInt()}W" }.orEmpty()
                        val barColor = if (isLowBattery) IslandColors.AccentRed else IslandColors.AccentGreen
                        Text(
                            text = "${activity.levelPercent}%$wattsText",
                            style = TextStyle(
                                color = barColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .widthIn(max = (metrics.rightSlotDp - metrics.innerSafetyInsetDp).dp)
                                .debugCameraDeadZoneGuard(exclusionZone, "charging_text")
                        )
                    }
                    is IslandActivity.NotificationPreview -> {
                        val compactOtp = if (hideTextOnLock) null else activity.otpCode
                        if (compactOtp != null) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.88f))
                                    .clickable { onCopyOtp(compactOtp) }
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                                    .debugCameraDeadZoneGuard(exclusionZone, "compact_otp_chip")
                                    .testTag("compact_otp_chip")
                            ) {
                                Text(
                                    text = "Copy $compactOtp",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            // Notification compact pill: app name ONLY (never title/body)
                            Text(
                                text = activity.appName.ifBlank { "Alert" },
                                color = IslandColors.TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .widthIn(max = metrics.notificationAppNameMaxDp.dp)
                                    .debugCameraDeadZoneGuard(exclusionZone, "notif_app_name")
                            )
                        }
                    }
                    is IslandActivity.DownloadProgress -> {
                        val pct = if (activity.max > 0) ((activity.progress * 100) / activity.max).coerceIn(0, 100) else 0
                        Text(
                            text = if (activity.indeterminate) "Saving" else "$pct%",
                            style = TextStyle(
                                color = IslandColors.AccentCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum"
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .widthIn(max = (metrics.rightSlotDp - metrics.innerSafetyInsetDp).dp)
                                .debugCameraDeadZoneGuard(exclusionZone, "download_pct")
                        )
                    }
                    is IslandActivity.ScreenRecording -> {
                        val recText = if (activity.isCountdown) "REC ${activity.countdownSec}" else TimerEngine.formatDuration(activity.elapsedMs)
                        Text(
                            text = recText,
                            style = TextStyle(
                                color = IslandColors.AccentRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            ),
                            maxLines = 1,
                            modifier = Modifier.debugCameraDeadZoneGuard(exclusionZone, "screen_rec_text")
                        )
                    }
                    is IslandActivity.VoiceRecording -> {
                        Text(
                            text = TimerEngine.formatDuration(activity.elapsedMs),
                            style = TextStyle(
                                color = IslandColors.AccentRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum"
                            ),
                            maxLines = 1
                        )
                    }
                    is IslandActivity.Alert -> {
                        // Clean white text (§1) and full text width for "Saved to Shelf" (never "Saved to S...")
                        Text(
                            text = if (hideTextOnLock) "Alert" else activity.title,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .widthIn(max = metrics.notificationAppNameMaxDp.dp)
                                .debugCameraDeadZoneGuard(exclusionZone, "alert_title")
                        )
                    }
                    null -> Spacer(Modifier.width(12.dp))
                }
            }
        }

        // Bottom 2 dp draining dwell underline for active transient notification/alert
        if (activity != null && state.dwellTargetActivityId == activity.id && state.dwellTotalMs > 0L) {
            val dwellFrac = (state.dwellRemainingMs.toFloat() / state.dwellTotalMs.toFloat()).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.76f * dwellFrac)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(activityAccent(activity, accentColor, musicAccent).copy(alpha = if (state.isDwellPaused) 0.45f else 0.9f))
            )
        }
    }
}

@Composable
private fun CompactLeftGlyph(
    activity: IslandActivity?,
    albumArt: Bitmap?,
    notifAppIcon: Bitmap?,
    accentColor: Color,
    musicAccent: Color,
    isLowBattery: Boolean
) {
    when (activity) {
        is IslandActivity.Music -> {
            if (albumArt != null) {
                Image(
                    bitmap = albumArt.asImageBitmap(),
                    contentDescription = "Album art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Icon(Icons.Filled.MusicNote, contentDescription = "Music", tint = musicAccent, modifier = Modifier.size(20.dp))
            }
        }
        is IslandActivity.Timer -> {
            val frac = if (activity.totalMs > 0L) (activity.remainingMs.toFloat() / activity.totalMs.toFloat()).coerceIn(0f, 1f) else 0f
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawArc(color = Color.White.copy(alpha = 0.2f), startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(2.2.dp.toPx()))
                    drawArc(color = IslandColors.AccentRed, startAngle = -90f, sweepAngle = 360f * frac, useCenter = false, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round))
                }
                Icon(Icons.Filled.Timer, contentDescription = "Timer", tint = accentColor, modifier = Modifier.size(13.dp))
            }
        }
        is IslandActivity.Charging -> {
            Icon(
                imageVector = if (isLowBattery) Icons.Filled.BatteryAlert else Icons.Filled.BatteryChargingFull,
                contentDescription = "Battery",
                tint = if (isLowBattery) IslandColors.AccentRed else IslandColors.AccentGreen,
                modifier = Modifier.size(20.dp)
            )
        }
        is IslandActivity.NotificationPreview -> {
            if (notifAppIcon != null) {
                Image(
                    bitmap = notifAppIcon.asImageBitmap(),
                    contentDescription = activity.appName,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(7.dp))
                )
            } else {
                Icon(Icons.Filled.Notifications, contentDescription = activity.appName, tint = accentColor, modifier = Modifier.size(20.dp))
            }
        }
        is IslandActivity.DownloadProgress -> {
            Icon(Icons.Filled.Download, contentDescription = activity.title, tint = IslandColors.AccentCyan, modifier = Modifier.size(20.dp))
        }
        is IslandActivity.ScreenRecording -> {
            Icon(Icons.Filled.FiberManualRecord, contentDescription = "Screen recording", tint = IslandColors.AccentRed, modifier = Modifier.size(18.dp))
        }
        is IslandActivity.VoiceRecording -> {
            Icon(Icons.Filled.Mic, contentDescription = "Voice Recording", tint = IslandColors.AccentRed, modifier = Modifier.size(18.dp))
        }
        is IslandActivity.Alert -> {
            val isLow = activity.kind == AlertKind.LOW_BATTERY
            Icon(
                imageVector = if (isLow) Icons.Filled.BatteryAlert else Icons.Filled.Notifications,
                contentDescription = activity.title,
                tint = if (isLow) IslandColors.AccentRed else accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        null -> Spacer(Modifier.width(12.dp))
    }
}

@Composable
private fun SplitSecondaryBubble(
    activity: IslandActivity,
    companionCount: Int,
    accentColor: Color,
    borderColor: Color,
    effectivePillStyle: PillStyle,
    glassDensity: Float,
    reduceGlass: Boolean,
    solidFallback: Boolean,
    crossWindowBlurActive: Boolean,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.clickable(onClick = onClick).testTag("split_companion_bubble")) {
        Box(
            modifier = Modifier
                .size(37.dp)
                .islandGlassChrome(
                    shape = CircleShape,
                    pillStyle = effectivePillStyle,
                    glassDensity = glassDensity,
                    reduceGlass = reduceGlass,
                    solidFallback = solidFallback,
                    accentColor = accentColor,
                    backdrop = LocalAppBackdrop.current,
                    crossWindowBlurActive = crossWindowBlurActive
                ),
            contentAlignment = Alignment.Center
        ) {
            when (activity) {
                is IslandActivity.Timer -> Text(
                    text = "${(activity.remainingMs / 1000L).coerceAtMost(99)}",
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                is IslandActivity.Music -> Icon(Icons.Filled.MusicNote, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                is IslandActivity.Charging -> Text("${activity.levelPercent}%", color = IslandColors.AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                is IslandActivity.DownloadProgress -> Icon(Icons.Filled.Download, contentDescription = null, tint = IslandColors.AccentCyan, modifier = Modifier.size(16.dp))
                is IslandActivity.ScreenRecording -> Icon(Icons.Filled.FiberManualRecord, contentDescription = null, tint = IslandColors.AccentRed, modifier = Modifier.size(14.dp))
                else -> Icon(Icons.Filled.Notifications, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
        }
        if (companionCount >= 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$companionCount",
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

/**
 * Stage 1 Medium Pill Preview: roomy 340..380 x 170..240 dp card anchored below cutout & status bar,
 * rich Notification / Music / Timer views, and generous touch targets.
 */
@Composable
fun MediumPillView(
    app: DynamicIslandApp,
    state: IslandState,
    settings: IslandSettings,
    exclusionZone: ExclusionZone = DeadZoneGeometry.fromSyntheticHole(settings.cutoutWidthDp, settings.cutoutHeightDp),
    accentColor: Color,
    musicAccent: Color = accentColor,
    effectivePillStyle: PillStyle = if (settings.solidFallback) PillStyle.BLACK else settings.pillStyle,
    crossWindowBlurSupported: Boolean = false,
    mediaState: MediaUiState = MediaUiState(),
    timerState: TimerUiState = TimerUiState(),
    drawOwnChrome: Boolean = true,
    headerAlphaProvider: () -> Float = { 1f },
    bodyAlphaProvider: () -> Float = { 1f },
    onRequestHaptic: () -> Unit = {}
) {
    val context = LocalContext.current
    val primary = state.primaryActivity
    val holeW = exclusionZone.holeWidthDp.coerceAtLeast(settings.cutoutWidthDp)
    val holeH = exclusionZone.holeHeightDp.coerceAtLeast(settings.cutoutHeightDp)
    val shape = RoundedCornerShape(IslandColors.MediumOuterRadius)
    val boundedMaxW = remember(exclusionZone.screenWidthPx, exclusionZone.density) {
        DeadZoneGeometry.boundedCardWidthDp(
            screenWidthPx = exclusionZone.screenWidthPx,
            density = exclusionZone.density,
            desiredMaxDp = DeadZoneGeometry.MEDIUM_MAX_WIDTH_DP
        )
    }

    val resolvedNotifAccent = if (primary is IslandActivity.NotificationPreview && settings.matchAppColor && primary.accentArgb != null) {
        Color(primary.accentArgb)
    } else {
        accentColor
    }
    val chromeMod = if (drawOwnChrome) {
        Modifier.islandGlassChrome(
            shape = shape,
            pillStyle = effectivePillStyle,
            glassDensity = settings.glassDensity,
            reduceGlass = settings.reduceGlass,
            solidFallback = settings.solidFallback,
            accentColor = resolvedNotifAccent,
            isHighlighted = state.isDragOverIsland,
            backdrop = LocalAppBackdrop.current,
            crossWindowBlurActive = crossWindowBlurSupported,
            performanceMode = settings.performanceMode
        )
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .padding(horizontal = if (drawOwnChrome) 12.dp else 0.dp)
            .widthIn(min = DeadZoneGeometry.MEDIUM_MIN_WIDTH_DP.coerceAtMost(boundedMaxW).dp, max = boundedMaxW.dp)
            .fillMaxWidth()
            .heightIn(min = DeadZoneGeometry.MEDIUM_MIN_HEIGHT_DP.dp, max = DeadZoneGeometry.MEDIUM_MAX_HEIGHT_DP.dp)
            .then(chromeMod)
            .clickable {
                onRequestHaptic()
                app.islandController.expand()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("medium_stage1_pill"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top header strictly clears punch-hole camera exclusion zone even in standalone tests
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height((holeH + 6f).coerceAtLeast(36f).dp)
                .graphicsLayer {
                    val a = headerAlphaProvider()
                    alpha = a
                    translationY = (1f - a) * -4.dp.toPx()
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (primary) {
                    is IslandActivity.Music -> mediaState.appLabel.ifBlank { "Now Playing" }
                    is IslandActivity.Timer -> "Timer"
                    is IslandActivity.Charging -> "Charging"
                    is IslandActivity.NotificationPreview -> primary.appName.ifBlank { "Notification" }
                    is IslandActivity.Alert -> primary.title
                    else -> state.activeTab.title
                },
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = 108.dp)
                    .debugCameraDeadZoneGuard(exclusionZone, "medium_header_left")
            )
            Spacer(Modifier.width((holeW + 16f).dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(30.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(0.75.dp, accentColor.copy(alpha = 0.48f), CircleShape)
                    .clickable {
                        onRequestHaptic()
                        app.islandController.expand()
                    }
                    .padding(horizontal = 10.dp)
                    .debugCameraDeadZoneGuard(exclusionZone, "medium_header_right")
                    .testTag("medium_expand_btn")
            ) {
                Text("Full", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(2.dp))
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Open full card", tint = accentColor, modifier = Modifier.size(15.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val a = bodyAlphaProvider()
                    val s = 0.96f + 0.04f * a
                    alpha = a
                    scaleX = s
                    scaleY = s
                    translationY = (1f - a) * 8.dp.toPx()
                }
        ) {
        when (primary) {
            is IslandActivity.Music -> {
                val trackFraction = if (mediaState.durationMs > 0L) {
                    (mediaState.positionMs.toFloat() / mediaState.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (mediaState.albumArt != null) {
                            Image(
                                bitmap = mediaState.albumArt.asImageBitmap(),
                                contentDescription = "Album art",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(0.9.dp, musicAccent.copy(alpha = 0.70f), RoundedCornerShape(12.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .border(0.9.dp, musicAccent.copy(alpha = 0.55f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = musicAccent, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = mediaState.title.ifBlank { "No track playing" },
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                lineHeight = 20.sp,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = mediaState.artist.ifBlank { "Tap to open music" },
                                color = IslandColors.TextSecondary,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        IconButton(onClick = { onRequestHaptic(); app.mediaHub.skipPrevious() }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(musicAccent)
                                .border(0.9.dp, Color.White.copy(alpha = 0.48f), CircleShape)
                                .clickable {
                                    onRequestHaptic()
                                    if (mediaState.isPlaying) app.soundManager.playMusicPause() else app.soundManager.playMusicPlay()
                                    app.mediaHub.togglePlayPause()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (mediaState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play or pause",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        IconButton(onClick = { onRequestHaptic(); app.mediaHub.skipNext() }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = TimerEngine.formatDuration(mediaState.positionMs),
                            style = TextStyle(color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp, fontFeatureSettings = "tnum")
                        )
                        WavyProgressBar(
                            progress = trackFraction,
                            isActiveWave = mediaState.isPlaying,
                            accentColor = musicAccent,
                            height = 16.dp,
                            interactive = mediaState.durationMs > 0L,
                            onSeekFraction = { frac ->
                                if (mediaState.durationMs > 0L) app.mediaHub.seekTo((mediaState.durationMs * frac).toLong())
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = TimerEngine.formatDuration(mediaState.durationMs),
                            style = TextStyle(color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp, fontFeatureSettings = "tnum")
                        )
                    }
                }
            }

            is IslandActivity.NotificationPreview -> {
                val hideTextOnLock = state.isDeviceLocked && settings.lockScreenPreset == LockScreenPreset.PRIVACY_HIDE_TEXT
                val coroutineScope = rememberCoroutineScope()
                val appIconBmp = remember(primary.packageName) {
                    runCatching { context.packageManager.getApplicationIcon(primary.packageName).toBitmap(64, 64) }.getOrNull()
                }
                val richBitmap = remember(primary.id, primary.hasPicture) {
                    if (primary.hasPicture) NotificationMediaCache.getPicture(primary.id)?.takeIf { !it.isRecycled } else null
                }
                val otpMatch = remember(primary.title, primary.text, hideTextOnLock) {
                    if (hideTextOnLock) null else primary.otpCode ?: OtpExtractor.extract(primary.title, primary.text)
                }
                val replyPair = remember(primary.id) { NotificationMediaCache.getReplyAction(primary.id) }
                var showInlineReply by remember(primary.id) { mutableStateOf(false) }
                var replyDraft by remember(primary.id) { mutableStateOf("") }
                var awaitingSafetyConfirm by remember(primary.id) { mutableStateOf(false) }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (appIconBmp != null) {
                            Image(
                                bitmap = appIconBmp.asImageBitmap(),
                                contentDescription = primary.appName,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(0.75.dp, resolvedNotifAccent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(resolvedNotifAccent.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Notifications, contentDescription = null, tint = resolvedNotifAccent, modifier = Modifier.size(22.dp))
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = if (hideTextOnLock) primary.appName else primary.title.ifBlank { primary.appName },
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (hideTextOnLock) "Content hidden on lock screen" else primary.text.ifBlank { "New notification" },
                                color = Color.White.copy(alpha = 0.78f),
                                fontSize = 13.sp,
                                lineHeight = 17.sp,
                                maxLines = if (showInlineReply) 1 else 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (richBitmap != null && !hideTextOnLock) {
                            Image(
                                bitmap = richBitmap.asImageBitmap(),
                                contentDescription = "Notification preview image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(0.75.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(10.dp))
                            )
                        }
                    }

                    if (showInlineReply && replyPair != null && !hideTextOnLock) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .border(0.8.dp, resolvedNotifAccent.copy(alpha = 0.5f), CircleShape)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BasicTextField(
                                value = replyDraft,
                                onValueChange = {
                                    replyDraft = it
                                    awaitingSafetyConfirm = false
                                },
                                singleLine = true,
                                cursorBrush = SolidColor(resolvedNotifAccent),
                                textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                                modifier = Modifier.weight(1f),
                                decorationBox = { inner ->
                                    if (replyDraft.isEmpty()) {
                                        Text("Reply...", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                    }
                                    inner()
                                }
                            )
                            val needsGuard = ReplySafetyGuard.requiresDoubleConfirm(primary.title, primary.text, replyDraft)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .height(26.dp)
                                    .clip(CircleShape)
                                    .background(if (awaitingSafetyConfirm) IslandColors.AccentAmber else resolvedNotifAccent)
                                    .clickable {
                                        if (replyDraft.isBlank()) return@clickable
                                        onRequestHaptic()
                                        if (needsGuard && !awaitingSafetyConfirm) {
                                            awaitingSafetyConfirm = true
                                            return@clickable
                                        }
                                        val fillIn = Intent()
                                        val bundle = Bundle().apply {
                                            putCharSequence(replyPair.first.resultKey, replyDraft)
                                        }
                                        RemoteInput.addResultsToIntent(arrayOf(replyPair.first), fillIn, bundle)
                                        runCatching { replyPair.second.send(context, 0, fillIn) }
                                        app.islandController.setKeyboardFocusNeeded(false)
                                        showInlineReply = false
                                        replyDraft = ""
                                        awaitingSafetyConfirm = false
                                        app.islandController.showTransientBanner("Reply sent")
                                        app.islandController.collapse()
                                    }
                                    .padding(horizontal = 10.dp)
                                    .testTag("notif_inline_send_btn")
                            ) {
                                Text(
                                    text = if (awaitingSafetyConfirm) "Confirm?" else "Send",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (otpMatch != null) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(CircleShape)
                                    .background(resolvedNotifAccent)
                                    .clickable {
                                        onRequestHaptic()
                                        app.clipboardMonitor.copyToClipboard(otpMatch)
                                        app.islandController.showTransientBanner("Copied $otpMatch")
                                    }
                                    .padding(horizontal = 10.dp)
                            ) {
                                Text("Copy $otpMatch", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (!hideTextOnLock && primary.hasReplyAction && replyPair != null) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(CircleShape)
                                    .background(if (showInlineReply) resolvedNotifAccent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.14f))
                                    .clickable {
                                        onRequestHaptic()
                                        showInlineReply = !showInlineReply
                                        app.islandController.setKeyboardFocusNeeded(showInlineReply)
                                    }
                                    .padding(horizontal = 10.dp)
                                    .testTag("notif_reply_toggle_btn")
                            ) {
                                Text("Reply", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        if (!hideTextOnLock && otpMatch == null) {
                            primary.actions.take(if (showInlineReply) 1 else 2).forEach { act ->
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .height(34.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.14f))
                                        .clickable {
                                            onRequestHaptic()
                                            runCatching { act.pendingIntent?.send() }
                                            app.islandController.collapse()
                                        }
                                        .padding(horizontal = 10.dp)
                                ) {
                                    Text(act.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                }
                            }
                        }
                        if (!primary.channelId.isNullOrBlank()) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.10f))
                                    .clickable {
                                        onRequestHaptic()
                                        val key = "${primary.packageName}|${primary.channelId}"
                                        coroutineScope.launch {
                                            app.settingsRepository.update { s ->
                                                s.copy(mutedNotificationChannels = s.mutedNotificationChannels + key)
                                            }
                                        }
                                        app.islandController.dismissNotificationById(primary.id)
                                        app.islandController.showTransientBanner("Muted channel")
                                    }
                                    .padding(horizontal = 10.dp)
                                    .testTag("notif_mute_channel_btn")
                            ) {
                                Text("Mute", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .height(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable {
                                    onRequestHaptic()
                                    app.islandController.dismissNotificationById(primary.id)
                                }
                                .padding(horizontal = 10.dp)
                        ) {
                            Text("Clear", color = Color.White.copy(alpha = 0.90f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .height(34.dp)
                                .clip(CircleShape)
                                .background(resolvedNotifAccent.copy(alpha = 0.28f))
                                .border(0.75.dp, resolvedNotifAccent.copy(alpha = 0.6f), CircleShape)
                                .clickable {
                                    onRequestHaptic()
                                    val pi = NotificationMediaCache.getContentIntent(primary.id)
                                    if (pi != null) {
                                        runCatching { pi.send() }
                                    } else {
                                        runCatching {
                                            context.packageManager.getLaunchIntentForPackage(primary.packageName)?.let {
                                                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(it)
                                            }
                                        }
                                    }
                                    app.islandController.collapse()
                                }
                                .padding(horizontal = 12.dp)
                        ) {
                            Text("Open", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            is IslandActivity.Timer -> {
                val frac = if (timerState.totalMs > 0L) {
                    (timerState.remainingMs.toFloat() / timerState.totalMs.toFloat()).coerceIn(0f, 1f)
                } else 0f
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = primary.label.ifBlank { "Countdown" },
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = TimerEngine.formatDuration(timerState.remainingMs),
                                style = TextStyle(color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Light, fontFeatureSettings = "tnum")
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IconButton(
                                onClick = {
                                    onRequestHaptic()
                                    if (timerState.isRunning) app.timerEngine.pauseTimer() else app.timerEngine.resumeTimer()
                                },
                                modifier = Modifier.size(44.dp).clip(CircleShape).background(accentColor)
                            ) {
                                Icon(
                                    imageVector = if (timerState.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Toggle timer",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            IconButton(
                                onClick = { onRequestHaptic(); app.timerEngine.cancelTimer() },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .border(0.75.dp, Color.White.copy(alpha = 0.24f), CircleShape)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Cancel timer", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(frac.coerceAtLeast(0.03f))
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                    }
                }
            }

            is IslandActivity.Charging -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.BatteryChargingFull, contentDescription = null, tint = IslandColors.AccentGreen, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "${primary.levelPercent}% · ${primary.pluggedLabel}",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = primary.chargeEtaMinutes?.let { "~${it}m to full" }
                                        ?: if (primary.isFull) "Battery full" else "Power connected",
                                    color = Color.White.copy(alpha = 0.72f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                        if (primary.wattageWatts != null && primary.wattageWatts > 0.5f) {
                            Text("${primary.wattageWatts.roundToInt()} W", color = IslandColors.AccentGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((primary.levelPercent / 100f).coerceIn(0.04f, 1f))
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(IslandColors.AccentGreen)
                        )
                    }
                }
            }

            is IslandActivity.DownloadProgress -> {
                val pct = if (primary.max > 0) ((primary.progress * 100) / primary.max).coerceIn(0, 100) else 0
                val frac = if (primary.max > 0) (primary.progress.toFloat() / primary.max.toFloat()).coerceIn(0.04f, 1f) else 0.5f
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = primary.title.ifBlank { primary.appName },
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = listOf(primary.appName, primary.speedText, primary.etaText).filter { it.isNotBlank() }.joinToString(" · "),
                                color = IslandColors.TextSecondary,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = if (primary.indeterminate) "..." else "$pct%",
                            color = IslandColors.AccentCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(frac)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(IslandColors.AccentCyan)
                        )
                    }
                }
            }

            is IslandActivity.ScreenRecording -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.FiberManualRecord, contentDescription = null, tint = IslandColors.AccentRed, modifier = Modifier.size(22.dp))
                        Column {
                            Text("Screen Recording", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (primary.isCountdown) "Starting in ${primary.countdownSec}..." else TimerEngine.formatDuration(primary.elapsedMs),
                                color = IslandColors.AccentRed,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(44.dp)
                            .clip(CircleShape)
                            .background(IslandColors.AccentRed)
                            .clickable {
                                onRequestHaptic()
                                app.soundManager.playToggle(isOn = false)
                                ScreenRecorderManager.stopRecording(context)
                                app.islandController.collapse()
                            }
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Stop", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = (primary as? IslandActivity.Alert)?.subtitle?.ifBlank { "Tap a module or Full for controls" }
                            ?: "Quick Modules",
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        settings.enabledTabs.filter { settings.isTabMasterEnabled(it) }.take(5).forEach { tab ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .widthIn(min = 52.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        onRequestHaptic()
                                        app.islandController.expand(tab)
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (tab == state.activeTab) accentColor.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.10f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(tabIcon(tab), contentDescription = tab.title, tint = if (tab == state.activeTab) accentColor else Color.White, modifier = Modifier.size(20.dp))
                                }
                                Text(tab.title, color = Color.White.copy(alpha = 0.80f), fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

private fun maxPanelHeightForTab(tab: IslandTab) =
    if (tab == IslandTab.TOOLS) 240.dp else 234.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpandedIslandCard(
    app: DynamicIslandApp,
    state: IslandState,
    settings: IslandSettings,
    exclusionZone: ExclusionZone = DeadZoneGeometry.fromSyntheticHole(settings.cutoutWidthDp, settings.cutoutHeightDp),
    accentColor: Color,
    musicAccent: Color = accentColor,
    effectivePillStyle: PillStyle = if (settings.solidFallback) PillStyle.BLACK else settings.pillStyle,
    crossWindowBlurSupported: Boolean = false,
    effectiveStyle: AnimationStyle = AnimationStyle.FRUIT,
    elasticity: Float = settings.animationElasticity,
    durationScale: Float = settings.animationSpeed,
    drawOwnChrome: Boolean = true,
    headerAlphaProvider: () -> Float = { 1f },
    contentAlphaProvider: () -> Float = { 1f },
    contentOffsetYDProvider: () -> Float = { 0f },
    chooserAlphaProvider: () -> Float = { 1f },
    onRequestHaptic: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {}
) {
    val mediaState by app.mediaHub.state.collectAsState()
    val timerState by app.timerEngine.state.collectAsState()

    val holeW = exclusionZone.holeWidthDp.coerceAtLeast(settings.cutoutWidthDp)
    val holeH = exclusionZone.holeHeightDp.coerceAtLeast(settings.cutoutHeightDp)
    val outerShape = RoundedCornerShape(IslandColors.ExpandedOuterRadius)
    val useGlass = effectivePillStyle != PillStyle.BLACK
    val boundedMaxW = remember(exclusionZone.screenWidthPx, exclusionZone.density) {
        DeadZoneGeometry.boundedCardWidthDp(
            screenWidthPx = exclusionZone.screenWidthPx,
            density = exclusionZone.density,
            desiredMaxDp = DeadZoneGeometry.FULL_MAX_WIDTH_DP
        )
    }

    var showGlyfPad by remember { mutableStateOf(false) }

    val expandedNotifAccent = remember(state.primaryActivity, settings.matchAppColor, accentColor) {
        val np = state.primaryActivity as? IslandActivity.NotificationPreview
        if (settings.matchAppColor && np?.accentArgb != null) Color(np.accentArgb) else accentColor
    }
    val chromeMod = if (drawOwnChrome) {
        Modifier.islandGlassChrome(
            shape = outerShape,
            pillStyle = effectivePillStyle,
            glassDensity = settings.glassDensity,
            reduceGlass = settings.reduceGlass,
            solidFallback = settings.solidFallback,
            accentColor = expandedNotifAccent,
            isHighlighted = state.isDragOverIsland,
            backdrop = LocalAppBackdrop.current,
            crossWindowBlurActive = crossWindowBlurSupported,
            performanceMode = settings.performanceMode
        )
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .padding(horizontal = if (drawOwnChrome) 12.dp else 0.dp)
            .widthIn(max = boundedMaxW.dp)
            .fillMaxWidth()
            .heightIn(max = DeadZoneGeometry.FULL_MAX_HEIGHT_DP.dp)
            .then(chromeMod)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = {
                    onRequestHaptic()
                    when (settings.expandedLongPressGesture) {
                        ExpandedSwipeAction.COLLAPSE -> app.islandController.collapse()
                        ExpandedSwipeAction.DISMISS -> app.islandController.dismissActiveNotification()
                        ExpandedSwipeAction.NEXT_NOTIFICATION -> app.islandController.nextNotification()
                        ExpandedSwipeAction.NONE -> onOpenAppSettings()
                    }
                }
            )
            .padding(bottom = 3.dp)
            .testTag("expanded_island_card")
    ) {
        // Camera-aware Top Header Row (43dp at 33dp cutout height, strictly clears exclusionRectDp.bottom = 41dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height((holeH + 10f).coerceAtLeast(42f).dp)
                .padding(horizontal = 12.dp)
                .graphicsLayer {
                    val a = headerAlphaProvider()
                    alpha = a
                    translationY = (1f - a) * -5.dp.toPx()
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .widthIn(max = 112.dp)
                    .debugCameraDeadZoneGuard(exclusionZone, "expanded_header_left")
            ) {
                IconButton(
                    onClick = {
                        onRequestHaptic()
                        app.islandController.collapse()
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Collapse island", tint = IslandColors.TextSecondary, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = state.activeTab.title,
                    color = IslandColors.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width((holeW + 16f).dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .widthIn(max = 122.dp)
                    .debugCameraDeadZoneGuard(exclusionZone, "expanded_header_right")
            ) {
                val primaryLive = state.primaryActivity
                if (primaryLive != null) {
                    val liveTab = when (primaryLive) {
                        is IslandActivity.Music -> IslandTab.MUSIC
                        is IslandActivity.Timer -> IslandTab.TIMER
                        is IslandActivity.VoiceRecording -> IslandTab.MEMO
                        is IslandActivity.ScreenRecording -> IslandTab.TOOLS
                        is IslandActivity.Alert, is IslandActivity.NotificationPreview, is IslandActivity.DownloadProgress -> IslandTab.ALERTS
                        else -> state.activeTab
                    }
                    val liveText = when (primaryLive) {
                        is IslandActivity.Music -> if (mediaState.isPlaying) "Playing" else "Paused"
                        is IslandActivity.Timer -> TimerEngine.formatDuration(primaryLive.remainingMs)
                        is IslandActivity.Charging -> primaryLive.chargeEtaMinutes?.let { "${primaryLive.levelPercent}% · ~${it}m" } ?: "${primaryLive.levelPercent}%"
                        is IslandActivity.NotificationPreview -> primaryLive.appName.ifBlank { primaryLive.title }
                        is IslandActivity.Alert -> primaryLive.title
                        is IslandActivity.VoiceRecording -> TimerEngine.formatDuration(primaryLive.elapsedMs)
                        is IslandActivity.DownloadProgress -> if (primaryLive.max > 0) "${(primaryLive.progress * 100 / primaryLive.max).coerceIn(0, 100)}%" else "Saving"
                        is IslandActivity.ScreenRecording -> if (primaryLive.isCountdown) "REC ${primaryLive.countdownSec}" else TimerEngine.formatDuration(primaryLive.elapsedMs)
                    }
                    val liveColor = activityAccent(primaryLive, accentColor, musicAccent)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .widthIn(max = 76.dp)
                            .clip(CircleShape)
                            .background(liveColor.copy(alpha = 0.20f))
                            .clickable {
                                onRequestHaptic()
                                app.islandController.selectTab(liveTab)
                            }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(liveColor))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = liveText,
                            color = IslandColors.TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(2.dp))
                }
                IconButton(
                    onClick = {
                        onRequestHaptic()
                        showGlyfPad = !showGlyfPad
                    },
                    modifier = Modifier.size(28.dp).testTag("expanded_glyf_btn")
                ) {
                    Icon(
                        Icons.Filled.Gesture,
                        contentDescription = "Glyf gesture pad",
                        tint = if (showGlyfPad) accentColor else IslandColors.TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
                IconButton(
                    onClick = {
                        onRequestHaptic()
                        app.soundManager.playTap()
                        app.islandController.collapse()
                        onOpenAppSettings()
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Open Settings", tint = IslandColors.TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }

        val expandedCtx = LocalContext.current
        val nextAlarm = remember(state.activeTab) { NextAlarmReader.getNextAlarm(expandedCtx) }
        val nextCalendar = remember(state.activeTab, settings.calendarRemindersEnabled) {
            if (settings.calendarRemindersEnabled && CalendarReminderReader.hasCalendarPermission(expandedCtx)) {
                CalendarReminderReader.getNextUpcomingEvent(expandedCtx)
            } else null
        }
        if ((nextAlarm != null || nextCalendar != null) && state.activeTab != IslandTab.MUSIC) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (nextAlarm != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.09f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(Icons.Filled.Alarm, contentDescription = null, tint = accentColor, modifier = Modifier.size(12.dp))
                        Text(nextAlarm.formattedText, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (nextCalendar != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.09f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(Icons.Filled.Event, contentDescription = null, tint = IslandColors.AccentCyan, modifier = Modifier.size(12.dp))
                        Text(
                            text = "${nextCalendar.formattedStart} ${nextCalendar.title}",
                            color = Color.White,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (nextCalendar.joinUrl != null) {
                            Text(
                                text = "Join",
                                color = IslandColors.AccentCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    onRequestHaptic()
                                    val i = Intent(Intent.ACTION_VIEW, Uri.parse(nextCalendar.joinUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    runCatching { expandedCtx.startActivity(i) }
                                }
                            )
                        }
                    }
                }
            }
        }

        val expandedSwipeModifier = if (state.activeTab == IslandTab.ALERTS || state.primaryActivity is IslandActivity.NotificationPreview) {
            Modifier.pointerInput(state.activeTab, state.primaryActivity?.id, settings.expandedSwipeLeftGesture, settings.expandedSwipeRightGesture) {
                var dragDx = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragDx = 0f },
                    onHorizontalDrag = { _, delta -> dragDx += delta },
                    onDragEnd = {
                        val action = when {
                            dragDx > 90f -> settings.expandedSwipeRightGesture
                            dragDx < -90f -> settings.expandedSwipeLeftGesture
                            else -> ExpandedSwipeAction.NONE
                        }
                        when (action) {
                            ExpandedSwipeAction.NEXT_NOTIFICATION -> {
                                onRequestHaptic()
                                app.islandController.nextNotification()
                            }
                            ExpandedSwipeAction.DISMISS -> {
                                onRequestHaptic()
                                app.islandController.dismissActiveNotification()
                            }
                            ExpandedSwipeAction.COLLAPSE -> {
                                onRequestHaptic()
                                app.islandController.collapse()
                            }
                            ExpandedSwipeAction.NONE -> Unit
                        }
                    }
                )
            }
        } else {
            Modifier
        }

        // Compact content slot (hard-capped at 78dp so full card never exceeds 160dp, static blur kept)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .heightIn(max = maxPanelHeightForTab(state.activeTab))
                .graphicsLayer {
                    val a = contentAlphaProvider()
                    val s = 0.96f + 0.04f * a
                    alpha = a
                    scaleX = s
                    scaleY = s
                    translationY = contentOffsetYDProvider() * density
                }
                .then(expandedSwipeModifier)
        ) {
            if (showGlyfPad) {
                InlineGlyfPadOverlay(
                    settings = settings,
                    accentColor = accentColor,
                    onLaunched = {
                        showGlyfPad = false
                        app.islandController.collapse()
                    },
                    onClose = { showGlyfPad = false }
                )
            } else {
                AnimatedContent(
                    targetState = state.activeTab,
                    transitionSpec = {
                        MotionTokens.tabSwitchEnterTransition(effectiveStyle) togetherWith
                            MotionTokens.tabSwitchExitTransition(effectiveStyle)
                    },
                    label = "island_tab_content"
                ) { tab ->
                when (tab) {
                    IslandTab.MUSIC -> MusicExpandedView(
                        state = mediaState,
                        accentColor = musicAccent,
                        rotateAlbumArt = settings.rotateAlbumArt,
                        expandedMarqueeEnabled = settings.expandedLyricsMarquee,
                        lyricsOffsetMs = settings.lyricsOffsetMs,
                        onTogglePlayPause = {
                            onRequestHaptic()
                            if (mediaState.isPlaying) app.soundManager.playMusicPause() else app.soundManager.playMusicPlay()
                            app.mediaHub.togglePlayPause()
                        },
                        onPrevious = {
                            onRequestHaptic()
                            app.soundManager.playMusicNext()
                            app.mediaHub.skipPrevious()
                        },
                        onNext = {
                            onRequestHaptic()
                            app.soundManager.playMusicNext()
                            app.mediaHub.skipNext()
                        },
                        onSeekTo = { pos -> app.mediaHub.seekTo(pos) },
                        onToggleShuffle = {
                            onRequestHaptic()
                            app.soundManager.playToggle(!mediaState.isShuffled)
                            app.mediaHub.sendShuffleCommand()
                        },
                        onCycleRepeat = {
                            onRequestHaptic()
                            app.soundManager.playTap()
                            app.mediaHub.sendRepeatCommand()
                        },
                        onToggleLike = {
                            onRequestHaptic()
                            app.soundManager.playToggle(!mediaState.isLiked)
                            app.mediaHub.sendLikeCommand()
                        },
                        onOpenPlayer = {
                            onRequestHaptic()
                            app.soundManager.playTap()
                            app.mediaHub.openPlayerApp()
                            app.islandController.collapse()
                        },
                        onOpenLyricsSettings = {
                            onRequestHaptic()
                            app.islandController.collapse()
                            onOpenAppSettings()
                        }
                    )

                    IslandTab.TIMER -> TimerStopwatchPanel(
                        state = timerState,
                        accentColor = accentColor,
                        onPickerChanged = { h, m, s -> app.timerEngine.setPickerDuration(h, m, s) },
                        onStartTimer = { app.timerEngine.startTimer() },
                        onPauseTimer = { app.timerEngine.pauseTimer() },
                        onResumeTimer = { app.timerEngine.resumeTimer() },
                        onCancelTimer = { app.timerEngine.cancelTimer() },
                        onStartStopwatch = { app.timerEngine.startStopwatch() },
                        onPauseStopwatch = { app.timerEngine.pauseStopwatch() },
                        onResetStopwatch = { app.timerEngine.resetStopwatch() },
                        onLapStopwatch = { app.timerEngine.lapStopwatch() },
                        onHaptic = onRequestHaptic,
                        onTimerSetSound = { app.soundManager.playTimerSet() },
                        onStartSleepTimer = { durationMs -> app.timerEngine.startSleepTimer(durationMs) }
                    )

                    IslandTab.SEARCH -> SearchIslandPanel(
                        app = app,
                        accentColor = accentColor,
                        onCollapse = { app.islandController.collapse() }
                    )

                    IslandTab.TOOLS -> ToolsIslandPanel(
                        app = app,
                        settings = settings,
                        accentColor = accentColor,
                        isScreenRecording = state.activityQueue.any { it is IslandActivity.ScreenRecording },
                        onHaptic = onRequestHaptic,
                        onCollapse = { app.islandController.collapse() }
                    )

                    IslandTab.VAULT -> VaultIslandPanel(
                        app = app,
                        accentColor = accentColor,
                        onCollapse = { app.islandController.collapse() }
                    )

                    IslandTab.CLIPS -> ClipsIslandPanel(
                        app = app,
                        accentColor = accentColor
                    )

                    IslandTab.NOTES -> NotesIslandPanel(
                        app = app,
                        accentColor = accentColor
                    )

                    IslandTab.MEMO -> MemoIslandPanel(
                        app = app,
                        accentColor = accentColor
                    )

                    IslandTab.ALERTS -> AlertsIslandPanel(
                        app = app,
                        state = state,
                        accentColor = accentColor
                    )
                }
            }
            }
        }

        Spacer(Modifier.height(2.dp))

        // Locked Single-Row Bottom Chooser strictly honoring user-ordered settings.enabledTabs
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val a = chooserAlphaProvider()
                    alpha = a
                    translationY = (1f - a) * 8.dp.toPx()
                }
        ) {
            BottomChooserBar(
                settings = settings,
                activeTab = state.activeTab,
                isMoreOpen = state.isMoreRowOpen,
                accentColor = accentColor,
                useLightGlass = useGlass,
                onSelectTab = { tab ->
                    onRequestHaptic()
                    app.islandController.selectTab(tab)
                },
                onToggleMore = {
                    onRequestHaptic()
                    app.islandController.toggleMoreRow()
                }
            )
        }
    }
}

@Composable
private fun InlineGlyfPadOverlay(
    settings: IslandSettings,
    accentColor: Color,
    onLaunched: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val strokePoints = remember { mutableStateListOf<StrokePoint>() }
    var lastRecognized by remember { mutableStateOf("Draw C, W, or O to launch shortcut") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(0.8.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(lastRecognized, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(
                text = "Close",
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onClose)
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .pointerInput(settings.glyphCApp, settings.glyphWApp, settings.glyphOApp) {
                    detectDragGestures(
                        onDragStart = { start ->
                            strokePoints.clear()
                            strokePoints.add(StrokePoint(start.x, start.y))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            strokePoints.add(StrokePoint(change.position.x, change.position.y))
                        },
                        onDragEnd = {
                            val match = GlyfStrokeRecognizer.recognize(strokePoints.toList())
                            val targetPkg = when (match) {
                                GlyfGesture.C -> settings.glyphCApp
                                GlyfGesture.W -> settings.glyphWApp
                                GlyfGesture.O -> settings.glyphOApp
                                null -> ""
                            }
                            if (match != null && targetPkg.isNotBlank()) {
                                val launch = context.packageManager.getLaunchIntentForPackage(targetPkg)
                                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                if (launch != null) {
                                    runCatching { context.startActivity(launch) }
                                    onLaunched()
                                } else {
                                    lastRecognized = "No launchable app for '${match.symbol}'"
                                }
                            } else if (match != null) {
                                lastRecognized = "No app assigned for '${match.symbol}' (set in Features)"
                            } else {
                                lastRecognized = "Not recognized — draw C, W, or O"
                            }
                        }
                    )
                }
        ) {
            if (strokePoints.size > 1) {
                val path = Path().apply {
                    moveTo(strokePoints.first().x, strokePoints.first().y)
                    for (i in 1 until strokePoints.size) {
                        lineTo(strokePoints[i].x, strokePoints[i].y)
                    }
                }
                drawPath(path = path, color = accentColor, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
fun BottomChooserBar(
    settings: IslandSettings,
    activeTab: IslandTab,
    isMoreOpen: Boolean,
    accentColor: Color,
    useLightGlass: Boolean,
    onSelectTab: (IslandTab) -> Unit,
    onToggleMore: () -> Unit
) {
    val orderedActive = settings.enabledTabs.filter { settings.isTabMasterEnabled(it) }
    val mainTabs = orderedActive.take(5)
    val moreTabs = orderedActive.drop(5)

    // Translucent Liquid Glass chooser capsule so the background wallpaper clearly shines through the entire card
    val chooserBg = if (!useLightGlass) {
        Color(0x1EFFFFFF)
    } else {
        Color(0x320C1220)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp, max = 48.dp)
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(IslandColors.ExpandedInnerSheetRadius))
            .background(chooserBg)
            .border(
                width = 0.85.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.28f),
                        accentColor.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.20f)
                    )
                ),
                shape = RoundedCornerShape(IslandColors.ExpandedInnerSheetRadius)
            )
            .padding(horizontal = 4.dp, vertical = 3.dp)
    ) {
        val showMore = isMoreOpen && moreTabs.isNotEmpty()
        AnimatedContent(
            targetState = showMore,
            transitionSpec = {
                (fadeIn(tween(140)) + slideInVertically(tween(150)) { if (targetState) it / 2 else -it / 2 })
                    .togetherWith(fadeOut(tween(100)))
            },
            label = "bottom_chooser_row_mode"
        ) { morePage ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val visibleTabs = if (morePage) moreTabs else mainTabs
                visibleTabs.forEach { tab ->
                    ChooserChip(
                        tab = tab,
                        selected = activeTab == tab,
                        accentColor = accentColor,
                        onClick = { onSelectTab(tab) }
                    )
                }
                if (moreTabs.isNotEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .widthIn(min = 44.dp)
                            .heightIn(min = 36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (morePage) accentColor.copy(alpha = 0.22f) else Color.Transparent)
                            .clickable(onClick = onToggleMore)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("chooser_more_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreHoriz,
                            contentDescription = if (morePage) "Main tabs" else "More tabs",
                            tint = if (morePage) accentColor else IslandColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (morePage) "Back" else "More",
                            fontSize = 11.sp,
                            lineHeight = 12.sp,
                            color = if (morePage) accentColor else IslandColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChooserChip(
    tab: IslandTab,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scaleState = animateFloatAsState(
        targetValue = when {
            pressed -> MotionTokens.TAP_SQUISH_SCALE
            selected -> 1.03f
            else -> 1.0f
        },
        animationSpec = MotionTokens.spatialFast(),
        label = "chip_scale"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .widthIn(min = 44.dp)
            .heightIn(min = 36.dp)
            .graphicsLayer {
                val s = scaleState.value
                scaleX = s
                scaleY = s
            }
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accentColor.copy(alpha = 0.26f) else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .testTag("chooser_tab_${tab.name.lowercase()}")
    ) {
        Icon(
            imageVector = tabIcon(tab),
            contentDescription = tab.title,
            tint = if (selected) accentColor else IslandColors.TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = tab.title,
            fontSize = 11.sp,
            lineHeight = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) accentColor else IslandColors.TextSecondary,
            maxLines = 1
        )
    }
}

fun tabIcon(tab: IslandTab): ImageVector = when (tab) {
    IslandTab.SEARCH -> Icons.Filled.Search
    IslandTab.MUSIC -> Icons.Filled.MusicNote
    IslandTab.TIMER -> Icons.Filled.Timer
    IslandTab.TOOLS -> Icons.Filled.Tune
    IslandTab.VAULT -> Icons.Filled.PhotoLibrary
    IslandTab.CLIPS -> Icons.Filled.ContentPaste
    IslandTab.NOTES -> Icons.Filled.Description
    IslandTab.MEMO -> Icons.Filled.Mic
    IslandTab.ALERTS -> Icons.Filled.Notifications
}
