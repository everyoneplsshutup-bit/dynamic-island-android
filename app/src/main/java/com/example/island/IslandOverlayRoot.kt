package com.example.island

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.BuildConfig
import com.example.DynamicIslandApp
import com.example.floating.FloatingSidebarOverlay
import com.example.media.MediaUiState
import com.example.media.MusicCompactWaveform
import com.example.media.MusicExpandedView
import com.example.ocr.OcrSelectionOverlay
import com.example.system.AccentTheme
import com.example.system.AnimationStyle
import com.example.system.CompanionPosition
import com.example.system.ContentEffect
import com.example.system.ExpandedSwipeAction
import com.example.system.IdleWidgetType
import com.example.system.IslandSettings
import com.example.system.IslandTab
import com.example.system.LockScreenPreset
import com.example.system.OpeningEffect
import com.example.system.PillStyle
import com.example.system.TimerEngine
import com.example.system.TimerStopwatchPanel
import com.example.system.TimerUiState
import com.example.system.WhenIdleMode
import com.example.ui.theme.IslandColors
import com.example.ui.theme.LocalAppBackdrop
import com.example.ui.theme.MotionTokens
import com.example.ui.theme.islandGlassChrome
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    isHighlighted: Boolean
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
        if (isLiquid && !reduceGlass) {
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.11f * g),
                        accentColor.copy(alpha = 0.15f * g),
                        Color.Transparent
                    ),
                    center = Offset(left + w * 0.28f, h * 0.18f),
                    radius = (maxOf(w, h) * 0.88f).coerceAtLeast(1f)
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
                Color.White.copy(alpha = 0.24f + (if (isLiquid) 0.34f else 0.16f) * g),
                accentColor.copy(alpha = (if (isLiquid) 0.42f else 0.22f) * g + 0.10f),
                Color.White.copy(alpha = 0.14f + 0.16f * g)
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
        style = Stroke(width = (if (isHighlighted) 1.5f else if (isLiquid) 1.15f else 0.9f).dp.toPx())
    )
}

/**
 * Runtime debug guard ensuring no rendered text/glyph enters the camera cutout exclusion zone.
 */
internal fun Modifier.debugCameraDeadZoneGuard(
    exclusionZone: ExclusionZone,
    label: String
): Modifier {
    if (!BuildConfig.DEBUG) return this
    val guardState = DeadZoneGuardState()
    return this
        .onGloballyPositioned { coords ->
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
            guardState.violated = hit
            if (hit) {
                Log.e("DeadZoneGuard", "Overlap detected for $label: bounds=$bounds vs cutout=${exclusionZone.exclusionRectPx}")
            }
        }
        .drawBehind {
            if (guardState.violated) {
                drawRect(color = Color.Red, style = Stroke(width = 1.5.dp.toPx()))
            }
        }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IslandOverlayRoot(
    app: DynamicIslandApp,
    exclusionZone: ExclusionZone = DeadZoneGeometry.measureLive(LocalContext.current, IslandSettings()),
    crossWindowBlurSupported: Boolean = false,
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
    val compactMetrics = remember(holeW, holeH, exclusionZone.isLandscape, settings.pillSizePreset) {
        DeadZoneGeometry.compactSlotMetrics(
            holeWidthDp = holeW,
            holeHeightDp = holeH,
            isLandscape = exclusionZone.isLandscape,
            slotWidthDp = settings.pillSizePreset.slotWidthDp
        )
    }
    val idleWidthDp = remember(settings.whenIdleMode, compactMetrics.totalWidthDp, holeW) {
        when (settings.whenIdleMode) {
            WhenIdleMode.INFO_PILL -> compactMetrics.totalWidthDp
            WhenIdleMode.CAMERA_ONLY -> (holeW + 94f).coerceIn(114f, 152f)
            WhenIdleMode.HIDDEN -> (holeW + 24f).coerceAtLeast(48f)
        }
    }
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
            IslandMode.MEDIUM -> DeadZoneGeometry.MEDIUM_MAX_WIDTH_DP
            IslandMode.EXPANDED -> DeadZoneGeometry.FULL_MAX_WIDTH_DP
        }
    }
    val morphHeightDpState = morphTransition.animateFloat(
        transitionSpec = morphSpringSpec,
        label = "morph_height"
    ) { mode ->
        when (mode) {
            IslandMode.IDLE, IslandMode.COMPACT, IslandMode.SPLIT -> compactMetrics.pillHeightDp
            IslandMode.MEDIUM -> DeadZoneGeometry.MEDIUM_MAX_HEIGHT_DP
            IslandMode.EXPANDED -> DeadZoneGeometry.FULL_MAX_HEIGHT_DP
        }
    }
    val morphRadiusDpState = morphTransition.animateFloat(
        transitionSpec = morphSpringSpec,
        label = "morph_radius"
    ) { mode ->
        when (mode) {
            IslandMode.IDLE, IslandMode.COMPACT, IslandMode.SPLIT -> compactMetrics.pillHeightDp * 0.5f
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
            IslandMode.MEDIUM -> 2f
            IslandMode.EXPANDED -> 4f
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

    // Staggered Apple choreography: Header -> Body -> Bottom Chooser (fast clear on collapse)
    val headerRevealState = morphTransition.animateFloat(
        transitionSpec = {
            when {
                effectiveStyle == AnimationStyle.NONE -> snap()
                targetState == IslandMode.EXPANDED || targetState == IslandMode.MEDIUM ->
                    tween(durationMillis = (200 / durationScale).toInt().coerceIn(80, 320), delayMillis = 70, easing = FastOutSlowInEasing)
                else -> tween(durationMillis = (95 / durationScale).toInt().coerceIn(40, 140), easing = FastOutLinearInEasing)
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
                    tween(durationMillis = (260 / durationScale).toInt().coerceIn(100, 380), delayMillis = 140, easing = MotionTokens.ContentCrossfadeEasing)
                else -> tween(durationMillis = (85 / durationScale).toInt().coerceIn(40, 130), easing = FastOutLinearInEasing)
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
                    tween(durationMillis = (240 / durationScale).toInt().coerceIn(90, 360), delayMillis = 200, easing = MotionTokens.ContentCrossfadeEasing)
                else -> tween(durationMillis = (75 / durationScale).toInt().coerceIn(35, 110), easing = FastOutLinearInEasing)
            }
        },
        label = "chooser_reveal"
    ) { mode ->
        if (mode == IslandMode.EXPANDED) 1f else 0f
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

    Box(modifier = Modifier.fillMaxSize()) {
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

        // Anchor island relative to live DisplayCutout in Portrait or Landscape
        val anchorAlign = when {
            exclusionZone.isLandscape && exclusionZone.isRightEdgeInLandscape -> Alignment.CenterEnd
            exclusionZone.isLandscape -> Alignment.CenterStart
            else -> Alignment.TopCenter
        }
        val anchorOffsetX = if (!exclusionZone.isLandscape) {
            val screenCenterDp = (exclusionZone.screenWidthPx / exclusionZone.density) / 2f
            (exclusionZone.cutoutRectDp.centerX - screenCenterDp).dp
        } else {
            4.dp
        }
        val anchorOffsetY = if (!exclusionZone.isLandscape) {
            (exclusionZone.cutoutRectDp.top - 2f).coerceAtLeast(0f).dp
        } else {
            0.dp
        }

        Box(
            modifier = Modifier
                .align(anchorAlign)
                .offset(x = anchorOffsetX, y = anchorOffsetY),
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
                                    accentColor = accentColor,
                                    isHighlighted = state.isDragOverIsland
                                )
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
                            headerAlphaProvider = { headerRevealState.value.coerceIn(0f, 1f) },
                            contentAlphaProvider = { bodyRevealState.value.coerceIn(0f, 1f) },
                            contentOffsetYDProvider = { (1f - bodyRevealState.value.coerceIn(0f, 1f)) * 10f },
                            chooserAlphaProvider = { chooserRevealState.value.coerceIn(0f, 1f) },
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
                                    accentColor = accentColor,
                                    isHighlighted = state.isDragOverIsland
                                )
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
                            headerAlphaProvider = { headerRevealState.value.coerceIn(0f, 1f) },
                            bodyAlphaProvider = { bodyRevealState.value.coerceIn(0f, 1f) },
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
                            accentColor = accentColor,
                            musicAccent = musicAccent,
                            isPlaying = mediaState.isPlaying,
                            albumArt = mediaState.albumArt,
                            effectivePillStyle = effectivePillStyle,
                            crossWindowBlurSupported = crossWindowBlurSupported
                        )
                        if (settings.companionPosition == CompanionPosition.RIGHT) {
                            companionView()
                        }
                    }
                }
            }
        }

        val miniTab = state.activeFloatingMiniTab
        if (miniTab != null) {
            DraggableFloatingMiniWindow(
                app = app,
                tab = miniTab,
                accentColor = accentColor,
                onRequestHaptic = onRequestHaptic,
                onExpandToIsland = {
                    app.islandController.setFloatingMiniTab(null)
                    app.islandController.expand(miniTab)
                },
                onClose = {
                    app.soundManager.playCollapse()
                    app.islandController.setFloatingMiniTab(null)
                }
            )
        }

        AnimatedVisibility(
            visible = state.isFloatingSidebarOpen,
            enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.92f),
            exit = fadeOut(tween(120)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 72.dp, end = 10.dp)
        ) {
            FloatingSidebarOverlay(
                app = app,
                settings = settings,
                accentColor = accentColor,
                onClose = { app.islandController.setFloatingSidebarOpen(false) }
            )
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
                    app.soundManager.playTap()
                    app.clipboardMonitor.copyToClipboard(selected)
                    app.islandController.dismissOcrSelection()
                    app.islandController.upsertActivity(
                        IslandActivity.Alert(
                            id = "ocr_copied_${System.currentTimeMillis()}",
                            kind = AlertKind.OCR_COPIED,
                            title = "Copied text",
                            subtitle = selected.take(56)
                        ),
                        autoShowCompact = true,
                        autoDismissMs = 3500L
                    )
                },
                onShare = { selected ->
                    app.soundManager.playTap()
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, selected)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching {
                        context.startActivity(
                            Intent.createChooser(shareIntent, "Share text")
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    app.islandController.dismissOcrSelection()
                },
                onDismiss = { app.islandController.dismissOcrSelection() },
                onHapticTick = onRequestHaptic
            )
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
                    .background(Color(0xF016171E))
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
    is IslandActivity.VoiceRecording -> IslandColors.AccentRed
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
                    backdrop = LocalAppBackdrop.current
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

    val wingWidthDp = when (settings.pillSizePreset) {
        com.example.system.PillSizePreset.SMALL -> 32.dp
        com.example.system.PillSizePreset.MEDIUM -> 38.dp
        com.example.system.PillSizePreset.LARGE -> 46.dp
    }
    val effectivePillStyle = if (settings.solidFallback) PillStyle.BLACK else settings.pillStyle
    val pillAccent = if (isLowBatteryWarning) IslandColors.AccentRed else accentColor
    val idleBorder = when {
        isCalibrationPreview || isLowBatteryWarning -> IslandColors.AccentRed.copy(alpha = 0.85f)
        isDragOverIsland -> accentColor
        else -> pillAccent.copy(alpha = 0.78f)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height((holeH + 4f).coerceAtLeast(36f).dp)
            .islandGlassChrome(
                shape = CircleShape,
                pillStyle = effectivePillStyle,
                glassDensity = settings.glassDensity,
                reduceGlass = settings.reduceGlass,
                solidFallback = settings.solidFallback,
                accentColor = pillAccent,
                isHighlighted = isDragOverIsland || isCalibrationPreview || isLowBatteryWarning,
                backdrop = LocalAppBackdrop.current
            )
            .border(strokeWidth, idleBorder, CircleShape)
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .testTag("idle_camera_ring")
    ) {
        Box(
            modifier = Modifier.width(wingWidthDp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (isLowBatteryWarning) {
                Icon(
                    imageVector = Icons.Filled.BatteryAlert,
                    contentDescription = "Low battery warning",
                    tint = IslandColors.AccentRed,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(pillAccent)
                    )
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(2.5.dp)
                            .clip(CircleShape)
                            .background(pillAccent.copy(alpha = 0.58f))
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width((holeW + 16f).dp))
        Box(
            modifier = Modifier.width(wingWidthDp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(pillAccent.copy(alpha = 0.55f))
            )
        }
    }
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
            Text("${state.batteryLevelPercent}%", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        IdleWidgetType.NEXT_ALARM -> {
            val nextAlarm = remember(nowMs) {
                val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                val trigger = runCatching { am?.nextAlarmClock?.triggerTime }.getOrNull()
                if (trigger != null && trigger > 0L) SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(trigger)) else "--:--"
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Icon(Icons.Filled.Alarm, contentDescription = "Next alarm", tint = accentColor, modifier = Modifier.size(12.dp))
                Text(nextAlarm, color = Color.White, fontSize = 10.sp, maxLines = 1)
            }
        }
        IdleWidgetType.UNREAD_COUNT -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(Icons.Filled.Notifications, contentDescription = "Unread", tint = accentColor, modifier = Modifier.size(12.dp))
            Text("${state.notificationsHistory.size}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
        IdleWidgetType.NETWORK -> {
            val netLabel = remember(nowMs) {
                runCatching {
                    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
                    when {
                        caps == null -> "Offline"
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "5G/4G"
                        else -> "Online"
                    }
                }.getOrDefault("Online")
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Icon(Icons.Filled.Wifi, contentDescription = "Network", tint = accentColor, modifier = Modifier.size(12.dp))
                Text(netLabel, color = Color.White, fontSize = 10.sp, maxLines = 1)
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
            Text(speedLabel, color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        IdleWidgetType.SEARCH_SHORTCUT -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(Icons.Filled.Search, contentDescription = "Search", tint = accentColor, modifier = Modifier.size(12.dp))
            Text("Search", color = IslandColors.TextSecondary, fontSize = 10.sp, maxLines = 1)
        }
        IdleWidgetType.QUICK_NOTE -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(Icons.Filled.Edit, contentDescription = "Quick note", tint = IslandColors.AccentLime, modifier = Modifier.size(12.dp))
            Text("Note", color = IslandColors.TextSecondary, fontSize = 10.sp, maxLines = 1)
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
    crossWindowBlurSupported: Boolean = false
) {
    val activity = state.primaryActivity
    val hideTextOnLock = state.isDeviceLocked && settings.lockScreenPreset == LockScreenPreset.PRIVACY_HIDE_TEXT
    val holeW = exclusionZone.holeWidthDp.coerceAtLeast(settings.cutoutWidthDp)
    val holeH = exclusionZone.holeHeightDp.coerceAtLeast(settings.cutoutHeightDp)
    val metrics = DeadZoneGeometry.compactSlotMetrics(
        holeWidthDp = holeW,
        holeHeightDp = holeH,
        isLandscape = exclusionZone.isLandscape,
        slotWidthDp = settings.pillSizePreset.slotWidthDp
    )
    val context = LocalContext.current

    val notifAppIcon: Bitmap? = remember(activity) {
        val pkg = (activity as? IslandActivity.NotificationPreview)?.packageName ?: return@remember null
        runCatching {
            context.packageManager.getApplicationIcon(pkg).toBitmap(width = 48, height = 48)
        }.getOrNull()
    }

    val isLowBattery = (activity is IslandActivity.Alert && activity.kind == AlertKind.LOW_BATTERY) ||
        (state.isLowBatteryWarning && activity is IslandActivity.Charging)

    val pillModifier = Modifier.islandGlassChrome(
        shape = CircleShape,
        pillStyle = effectivePillStyle,
        glassDensity = settings.glassDensity,
        reduceGlass = settings.reduceGlass,
        solidFallback = settings.solidFallback,
        accentColor = if (isLowBattery) IslandColors.AccentRed else accentColor,
        isHighlighted = state.isDragOverIsland || state.isCalibrationPreview || isLowBattery,
        backdrop = LocalAppBackdrop.current,
        crossWindowBlurActive = crossWindowBlurSupported
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
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    is IslandActivity.Charging -> Text(
                        text = "${activity.levelPercent}%",
                        color = if (isLowBattery) IslandColors.AccentRed else IslandColors.AccentGreen,
                        fontSize = 10.sp,
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

    // Portrait: symmetrical leftSlotDp (88dp) + centerSpacerDp (holeW+16dp) + rightSlotDp (88dp)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (activity.title.isNotBlank() && !hideTextOnLock) {
                                Text(
                                    text = activity.title,
                                    color = IslandColors.TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .widthIn(max = metrics.musicTitleMaxDp.dp)
                                        .debugCameraDeadZoneGuard(exclusionZone, "music_title")
                                )
                            }
                            MusicCompactWaveform(isPlaying = isPlaying, accentColor = musicAccent, positionMs = activity.positionMs)
                        }
                    }
                    is IslandActivity.Timer -> {
                        Text(
                            text = TimerEngine.formatDuration(activity.remainingMs),
                            style = TextStyle(
                                color = accentColor,
                                fontSize = 12.sp,
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
                    is IslandActivity.Charging -> {
                        val wattsText = activity.wattageWatts?.takeIf { it > 0.5f && activity.levelPercent < 100 }
                            ?.let { " ${it.roundToInt()}W" }.orEmpty()
                        val frac = (activity.levelPercent / 100f).coerceIn(0.05f, 1f)
                        val barColor = if (isLowBattery) IslandColors.AccentRed else IslandColors.AccentGreen
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(9.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.16f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(frac)
                                        .height(9.dp)
                                        .clip(CircleShape)
                                        .background(barColor)
                                )
                            }
                            Text(
                                text = "${activity.levelPercent}%$wattsText",
                                style = TextStyle(
                                    color = barColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .widthIn(max = (metrics.rightSlotDp - 26f).dp)
                                    .debugCameraDeadZoneGuard(exclusionZone, "charging_text")
                            )
                        }
                    }
                    is IslandActivity.NotificationPreview -> {
                        // Notification compact pill: app name ONLY (never title/body!), ellipsis at 80dp
                        Text(
                            text = activity.appName.ifBlank { "Notification" },
                            color = IslandColors.TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .widthIn(max = metrics.notificationAppNameMaxDp.dp)
                                .debugCameraDeadZoneGuard(exclusionZone, "notif_app_name")
                        )
                    }
                    is IslandActivity.VoiceRecording -> {
                        Text(
                            text = TimerEngine.formatDuration(activity.elapsedMs),
                            color = IslandColors.AccentRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                    is IslandActivity.Alert -> {
                        val alertColor = if (activity.kind == AlertKind.LOW_BATTERY) IslandColors.AccentRed else accentColor
                        Text(
                            text = if (hideTextOnLock) "Alert" else activity.title,
                            color = alertColor,
                            fontSize = 11.sp,
                            fontWeight = if (activity.kind == AlertKind.LOW_BATTERY) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
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
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(0.6.dp, musicAccent.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                )
            } else {
                Icon(Icons.Filled.MusicNote, contentDescription = "Music", tint = musicAccent, modifier = Modifier.size(18.dp))
            }
        }
        is IslandActivity.Timer -> {
            Icon(Icons.Filled.Timer, contentDescription = "Timer", tint = accentColor, modifier = Modifier.size(18.dp))
        }
        is IslandActivity.Charging -> {
            Icon(
                imageVector = if (isLowBattery) Icons.Filled.BatteryAlert else Icons.Filled.BatteryChargingFull,
                contentDescription = "Battery",
                tint = if (isLowBattery) IslandColors.AccentRed else IslandColors.AccentGreen,
                modifier = Modifier.size(18.dp)
            )
        }
        is IslandActivity.NotificationPreview -> {
            if (notifAppIcon != null) {
                Image(
                    bitmap = notifAppIcon.asImageBitmap(),
                    contentDescription = activity.appName,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(0.6.dp, IslandColors.CrimsonBorder.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                )
            } else {
                Icon(Icons.Filled.Notifications, contentDescription = activity.appName, tint = accentColor, modifier = Modifier.size(18.dp))
            }
        }
        is IslandActivity.VoiceRecording -> {
            Icon(Icons.Filled.Mic, contentDescription = "Voice Recording", tint = IslandColors.AccentRed, modifier = Modifier.size(17.dp))
        }
        is IslandActivity.Alert -> {
            val isLow = activity.kind == AlertKind.LOW_BATTERY
            Icon(
                imageVector = if (isLow) Icons.Filled.BatteryAlert else Icons.Filled.Notifications,
                contentDescription = activity.title,
                tint = if (isLow) IslandColors.AccentRed else accentColor,
                modifier = Modifier.size(18.dp)
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
                .size(36.dp)
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
                is IslandActivity.Charging -> Text("${activity.levelPercent}%", color = IslandColors.AccentGreen, fontSize = 10.sp)
                else -> Icon(Icons.Filled.Notifications, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
        }
        if (companionCount >= 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(15.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$companionCount",
                    color = Color.Black,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

/**
 * Stage 1 Medium Pill Preview: capped widthIn(max = 320.dp), heightIn(96..120.dp), 28dp radius,
 * body strictly below camera clearance (>= 12dp spacer).
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
    val primary = state.primaryActivity
    val holeW = exclusionZone.holeWidthDp.coerceAtLeast(settings.cutoutWidthDp)
    val holeH = exclusionZone.holeHeightDp.coerceAtLeast(settings.cutoutHeightDp)
    val shape = RoundedCornerShape(IslandColors.MediumOuterRadius)

    val chromeMod = if (drawOwnChrome) {
        Modifier.islandGlassChrome(
            shape = shape,
            pillStyle = effectivePillStyle,
            glassDensity = settings.glassDensity,
            reduceGlass = settings.reduceGlass,
            solidFallback = settings.solidFallback,
            accentColor = accentColor,
            isHighlighted = state.isDragOverIsland,
            backdrop = LocalAppBackdrop.current,
            crossWindowBlurActive = crossWindowBlurSupported
        )
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (drawOwnChrome) 24.dp else 0.dp)
            .widthIn(max = DeadZoneGeometry.MEDIUM_MAX_WIDTH_DP.dp)
            .heightIn(min = 96.dp, max = DeadZoneGeometry.MEDIUM_MAX_HEIGHT_DP.dp)
            .then(chromeMod)
            .clickable {
                onRequestHaptic()
                app.islandController.expand()
            }
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .testTag("medium_stage1_pill"),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Top header strictly clears punch-hole camera
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height((holeH + 6f).coerceAtLeast(35f).dp)
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
                    is IslandActivity.Charging -> "Power"
                    is IslandActivity.NotificationPreview -> primary.appName.ifBlank { "Notification" }
                    else -> state.activeTab.title
                },
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = 96.dp)
                    .debugCameraDeadZoneGuard(exclusionZone, "medium_header_left")
            )
            Spacer(Modifier.width((holeW + 16f).dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(0.6.dp, accentColor.copy(alpha = 0.40f), CircleShape)
                    .clickable {
                        onRequestHaptic()
                        app.islandController.expand()
                    }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .debugCameraDeadZoneGuard(exclusionZone, "medium_header_right")
                    .testTag("medium_expand_btn")
            ) {
                Text("Full", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Open full card", tint = accentColor, modifier = Modifier.size(14.dp))
            }
        }

        // Body sits strictly >= 4 dp + header clearance below camera
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
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (mediaState.albumArt != null) {
                            Image(
                                bitmap = mediaState.albumArt.asImageBitmap(),
                                contentDescription = "Album art",
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(0.75.dp, accentColor.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.10f))
                                    .border(0.75.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = musicAccent, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mediaState.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(mediaState.artist, color = IslandColors.TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { onRequestHaptic(); app.mediaHub.skipPrevious() }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(19.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(musicAccent)
                                .border(0.75.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                                .clickable {
                                    onRequestHaptic()
                                    if (mediaState.isPlaying) app.soundManager.playMusicPause() else app.soundManager.playMusicPlay()
                                    app.mediaHub.togglePlayPause()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (mediaState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = { onRequestHaptic(); app.mediaHub.skipNext() }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(19.dp))
                        }
                    }

                    // Slim glowing accent track progress line matching Liquid Glass mockups
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(trackFraction.coerceAtLeast(0.02f))
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(musicAccent)
                        )
                    }
                }
            }

            is IslandActivity.Timer -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = TimerEngine.formatDuration(timerState.remainingMs),
                        style = TextStyle(color = accentColor, fontSize = 22.sp, fontWeight = FontWeight.Light, fontFeatureSettings = "tnum")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                onRequestHaptic()
                                if (timerState.isRunning) app.timerEngine.pauseTimer() else app.timerEngine.resumeTimer()
                            },
                            modifier = Modifier.size(34.dp).clip(CircleShape).background(accentColor)
                        ) {
                            Icon(
                                imageVector = if (timerState.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Toggle timer",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = { onRequestHaptic(); app.timerEngine.cancelTimer() },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1A1417))
                                .border(0.75.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel timer", tint = Color.White)
                        }
                    }
                }
            }

            is IslandActivity.Charging -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.BatteryChargingFull, contentDescription = null, tint = IslandColors.AccentGreen)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${primary.levelPercent}% · ${primary.pluggedLabel}",
                            color = IslandColors.AccentGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (primary.wattageWatts != null && primary.wattageWatts > 0.5f) {
                        Text("${primary.wattageWatts.roundToInt()} W", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    settings.enabledTabs.filter { settings.isTabMasterEnabled(it) }.take(5).forEach { tab ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (tab == state.activeTab) accentColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                                .clickable {
                                    onRequestHaptic()
                                    app.islandController.expand(tab)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(tabIcon(tab), contentDescription = tab.title, tint = if (tab == state.activeTab) accentColor else Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
        }
    }
}

private fun maxPanelHeightForTab(@Suppress("UNUSED_PARAMETER") tab: IslandTab) = 78.dp

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

    val chromeMod = if (drawOwnChrome) {
        Modifier.islandGlassChrome(
            shape = outerShape,
            pillStyle = effectivePillStyle,
            glassDensity = settings.glassDensity,
            reduceGlass = settings.reduceGlass,
            solidFallback = settings.solidFallback,
            accentColor = accentColor,
            isHighlighted = state.isDragOverIsland,
            backdrop = LocalAppBackdrop.current,
            crossWindowBlurActive = crossWindowBlurSupported
        )
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (drawOwnChrome) IslandColors.ScreenHorizontalMargin else 0.dp)
            .widthIn(max = DeadZoneGeometry.FULL_MAX_WIDTH_DP.dp)
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
                    .widthIn(max = 112.dp)
                    .debugCameraDeadZoneGuard(exclusionZone, "expanded_header_right")
            ) {
                val primaryLive = state.primaryActivity
                if (primaryLive != null) {
                    val liveTab = when (primaryLive) {
                        is IslandActivity.Music -> IslandTab.MUSIC
                        is IslandActivity.Timer -> IslandTab.TIMER
                        is IslandActivity.VoiceRecording -> IslandTab.MEMO
                        is IslandActivity.Alert, is IslandActivity.NotificationPreview -> IslandTab.ALERTS
                        else -> state.activeTab
                    }
                    val liveText = when (primaryLive) {
                        is IslandActivity.Music -> primaryLive.title.ifBlank { "Music" }
                        is IslandActivity.Timer -> TimerEngine.formatDuration(primaryLive.remainingMs)
                        is IslandActivity.Charging -> "${primaryLive.levelPercent}%"
                        is IslandActivity.NotificationPreview -> primaryLive.appName.ifBlank { primaryLive.title }
                        is IslandActivity.Alert -> primaryLive.title
                        is IslandActivity.VoiceRecording -> TimerEngine.formatDuration(primaryLive.elapsedMs)
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
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Box(Modifier.size(5.dp).clip(CircleShape).background(liveColor))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = liveText,
                            color = IslandColors.TextPrimary,
                            fontSize = 10.sp,
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
private fun DraggableFloatingMiniWindow(
    app: DynamicIslandApp,
    tab: IslandTab,
    accentColor: Color,
    onRequestHaptic: () -> Unit,
    onExpandToIsland: () -> Unit,
    onClose: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(20f) }
    var offsetY by remember { mutableFloatStateOf(140f) }
    val timerState by app.timerEngine.state.collectAsState()

    Column(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .widthIn(max = DeadZoneGeometry.MEDIUM_MAX_WIDTH_DP.dp)
            .heightIn(max = DeadZoneGeometry.FULL_MAX_HEIGHT_DP.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(IslandColors.CardRadius))
            .background(Color(0xF50C0D12))
            .border(1.dp, accentColor.copy(alpha = 0.65f), RoundedCornerShape(IslandColors.CardRadius))
            .padding(bottom = 6.dp)
            .testTag("floating_mini_window")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(0f, 380f)
                        offsetY = (offsetY + dragAmount.y).coerceIn(60f, 1100f)
                    }
                }
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DragIndicator, contentDescription = "Drag floating window", tint = IslandColors.TextSecondary, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Floating · ${tab.title}", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExpandToIsland, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.OpenInFull, contentDescription = "Dock to Island", tint = IslandColors.TextSecondary, modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close floating window", tint = IslandColors.TextSecondary, modifier = Modifier.size(15.dp))
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().heightIn(max = 78.dp)) {
            when (tab) {
                IslandTab.NOTES -> NotesIslandPanel(app = app, accentColor = accentColor)
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
                else -> SearchIslandPanel(app = app, accentColor = accentColor, onCollapse = onClose)
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
            .heightIn(max = 36.dp)
            .padding(horizontal = 10.dp)
            .clip(RoundedCornerShape(IslandColors.ExpandedInnerSheetRadius))
            .background(chooserBg)
            .border(
                width = 0.8.dp,
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.24f),
                        accentColor.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.18f)
                    )
                ),
                shape = RoundedCornerShape(IslandColors.ExpandedInnerSheetRadius)
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
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
                            .heightIn(min = 30.dp)
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
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (morePage) "Back" else "More",
                            fontSize = 9.sp,
                            lineHeight = 10.sp,
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
            .heightIn(min = 30.dp)
            .graphicsLayer {
                val s = scaleState.value
                scaleX = s
                scaleY = s
            }
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accentColor.copy(alpha = 0.24f) else Color.Transparent)
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
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = tab.title,
            fontSize = 9.sp,
            lineHeight = 10.sp,
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
