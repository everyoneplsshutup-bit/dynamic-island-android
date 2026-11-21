package com.example.island

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragIndicator
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DynamicIslandApp
import com.example.floating.FloatingSidebarOverlay
import com.example.media.MusicCompactWaveform
import com.example.media.MusicExpandedView
import com.example.ocr.OcrSelectionOverlay
import com.example.search.LaunchableApp
import com.example.system.AccentTheme
import com.example.system.AnimationStyle
import com.example.system.CalculatorIslandView
import com.example.system.CompanionPosition
import com.example.system.ContentEffect
import com.example.system.ExpandedSwipeAction
import com.example.system.IslandSettings
import com.example.system.IslandTab
import com.example.system.LockScreenPreset
import com.example.system.OpeningEffect
import com.example.system.PillStyle
import com.example.system.TimerEngine
import com.example.system.TimerStopwatchPanel
import com.example.ui.theme.IslandColors
import com.example.ui.theme.MotionTokens
import com.example.vault.VaultItemEntity
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IslandOverlayRoot(
    app: DynamicIslandApp,
    onRequestHaptic: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val state by app.islandController.state.collectAsState()
    val settings by app.settingsRepository.settingsFlow.collectAsState(initial = IslandSettings())
    val mediaState by app.mediaHub.state.collectAsState()
    val timerState by app.timerEngine.state.collectAsState()

    var previewVaultItem by remember { mutableStateOf<VaultItemEntity?>(null) }

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

    val materialYouAccent = remember(settings.accentTheme) {
        IslandColors.extractMaterialYouAccent(context, fallback = IslandColors.AccentOrange)
    }
    val accentColor = when (settings.accentTheme) {
        AccentTheme.ORANGE -> IslandColors.AccentOrange
        AccentTheme.CYAN -> IslandColors.AccentCyan
        AccentTheme.GREEN -> IslandColors.AccentGreen
        AccentTheme.DYNAMIC -> materialYouAccent
    }
    val musicAccent = remember(mediaState.artworkAccentArgb, accentColor) {
        mediaState.artworkAccentArgb?.let { Color(it) } ?: accentColor
    }

    // Lock screen visibility policy
    if (state.isDeviceLocked && settings.lockScreenPreset == LockScreenPreset.HIDDEN) {
        return
    }

    // Apple Two-Stage + Liquid Reveal GPU Motion Choreography
    var showMediumStage by remember { mutableStateOf(state.mode == IslandMode.MEDIUM) }
    var showExpandedCard by remember { mutableStateOf(state.mode == IslandMode.EXPANDED) }
    val pillPreStretchX = remember { Animatable(1f) }
    val cardScaleX = remember { Animatable(if (state.mode == IslandMode.EXPANDED || state.mode == IslandMode.MEDIUM) 1f else 0.42f) }
    val cardScaleY = remember { Animatable(if (state.mode == IslandMode.EXPANDED || state.mode == IslandMode.MEDIUM) 1f else 0.24f) }
    val cardAlpha = remember { Animatable(if (state.mode == IslandMode.EXPANDED || state.mode == IslandMode.MEDIUM) 1f else 0f) }
    val revealBlurDp = remember { Animatable(0f) }

    LaunchedEffect(state.mode, effectiveStyle, settings.openingEffect, elasticity, durationScale) {
        val useLiquidBlur = settings.openingEffect == OpeningEffect.LIQUID
        when (state.mode) {
            IslandMode.MEDIUM -> {
                showExpandedCard = false
                showMediumStage = true
                if (effectiveStyle == AnimationStyle.NONE) {
                    pillPreStretchX.snapTo(1f)
                    cardScaleX.snapTo(1f)
                    cardScaleY.snapTo(1f)
                    cardAlpha.snapTo(1f)
                    revealBlurDp.snapTo(0f)
                } else {
                    revealBlurDp.snapTo(if (useLiquidBlur) 10f else 0f)
                    cardScaleX.snapTo(if (settings.openingEffect == OpeningEffect.POP) 0.78f else 0.66f)
                    cardScaleY.snapTo(if (settings.openingEffect == OpeningEffect.POP) 0.70f else 0.55f)
                    cardAlpha.snapTo(0.35f)
                    val spec = MotionTokens.idleToMediumSpec<Float>(effectiveStyle, elasticity, durationScale)
                    launch { cardScaleX.animateTo(1f, spec) }
                    launch { cardScaleY.animateTo(1f, spec) }
                    if (useLiquidBlur) {
                        launch { revealBlurDp.animateTo(0f, MotionTokens.effectsDefault(durationScale)) }
                    }
                    cardAlpha.animateTo(1f, MotionTokens.contentCrossfadeTween(effectiveStyle, durationScale))
                }
            }

            IslandMode.EXPANDED -> {
                val cameFromMedium = showMediumStage
                showMediumStage = false
                if (effectiveStyle == AnimationStyle.NONE) {
                    showExpandedCard = true
                    pillPreStretchX.snapTo(1f)
                    cardScaleX.snapTo(1f)
                    cardScaleY.snapTo(1f)
                    cardAlpha.snapTo(1f)
                    revealBlurDp.snapTo(0f)
                } else {
                    if (!cameFromMedium) {
                        pillPreStretchX.animateTo(
                            targetValue = 1.24f,
                            animationSpec = MotionTokens.spatialFast(effectiveStyle, elasticity, durationScale)
                        )
                        val initScale = when (settings.openingEffect) {
                            OpeningEffect.POP -> 0.82f
                            OpeningEffect.SMOOTH -> 0.68f
                            else -> 0.48f
                        }
                        cardScaleX.snapTo(initScale)
                        cardScaleY.snapTo((initScale * 0.65f).coerceAtLeast(0.28f))
                        cardAlpha.snapTo(0.15f)
                    } else {
                        cardScaleX.snapTo(0.76f)
                        cardScaleY.snapTo(0.52f)
                        cardAlpha.snapTo(0.65f)
                    }
                    revealBlurDp.snapTo(if (useLiquidBlur) 14f else 0f)
                    showExpandedCard = true
                    val expandSpec = MotionTokens.openingEffectSpec<Float>(
                        openingEffect = settings.openingEffect,
                        style = effectiveStyle,
                        elasticity = elasticity,
                        durationScale = durationScale
                    )
                    launch { pillPreStretchX.snapTo(1f) }
                    launch { cardScaleX.animateTo(1f, expandSpec) }
                    launch { cardScaleY.animateTo(1f, expandSpec) }
                    if (useLiquidBlur) {
                        launch { revealBlurDp.animateTo(0f, MotionTokens.effectsDefault(durationScale)) }
                    }
                    cardAlpha.animateTo(1f, MotionTokens.contentCrossfadeTween(effectiveStyle, durationScale))
                }
            }

            else -> {
                if (effectiveStyle == AnimationStyle.NONE) {
                    showExpandedCard = false
                    showMediumStage = false
                    cardAlpha.snapTo(0f)
                    cardScaleX.snapTo(0.45f)
                    cardScaleY.snapTo(0.25f)
                    revealBlurDp.snapTo(0f)
                    pillPreStretchX.snapTo(1f)
                } else if (showExpandedCard || showMediumStage) {
                    val collapseSpec = MotionTokens.collapseSpec<Float>(effectiveStyle, durationScale)
                    launch { cardScaleX.animateTo(0.46f, collapseSpec) }
                    launch { cardScaleY.animateTo(0.26f, collapseSpec) }
                    cardAlpha.animateTo(0f, tween(durationMillis = 120))
                    showExpandedCard = false
                    showMediumStage = false
                    revealBlurDp.snapTo(0f)
                    pillPreStretchX.snapTo(1.16f)
                    pillPreStretchX.animateTo(1f, MotionTokens.spatialDefault(effectiveStyle, elasticity, durationScale))
                }
            }
        }
    }

    val compactScale by animateFloatAsState(
        targetValue = if (state.mode == IslandMode.COMPACT || state.mode == IslandMode.SPLIT) 1f else 0.94f,
        animationSpec = MotionTokens.spatialDefault(effectiveStyle, elasticity, durationScale),
        label = "compact_scale"
    )

    val useGlass = settings.pillStyle == PillStyle.GLASS && !settings.solidFallback
    val cardBackground = if (useGlass) {
        IslandColors.blendGlassBase(accentColor, settings.glassDensity)
    } else {
        Brush.verticalGradient(colors = listOf(IslandColors.PillBlack, IslandColors.PillBlack))
    }

    val pillBorderBrush = IslandColors.liquidBorderBrush(
        useGlass = useGlass,
        isHighlighted = state.isDragOverIsland || state.isCalibrationPreview,
        accentColor = if (state.isCalibrationPreview) IslandColors.AccentRed else accentColor
    )
    val pillBorderColor = when {
        state.isCalibrationPreview -> IslandColors.AccentRed
        state.isDragOverIsland -> accentColor
        useGlass -> IslandColors.GlassSpecularBorder
        else -> Color.White.copy(alpha = 0.16f)
    }

    val pullOffsetYPx = state.pullDownProgress * 22f
    val pullScaleBoost = 1f + state.pullDownProgress * 0.14f

    Box(modifier = Modifier.fillMaxSize()) {
        // Select mode active indicator banner
        if (state.isSelectModeActive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = (settings.cutoutYOffsetDp + settings.cutoutHeightDp + 16f).dp),
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

        // Scrim tap outside Medium/Expanded collapses cleanly
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

        // Main Island Container anchored at camera cutout
        val blurMod = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && revealBlurDp.value > 0.4f) {
            Modifier.blur(revealBlurDp.value.dp)
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(
                    x = settings.cutoutXOffsetDp.dp,
                    y = (settings.cutoutYOffsetDp - 2f).coerceAtLeast(0f).dp
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                showExpandedCard -> {
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(0.5f, 0f)
                                scaleX = cardScaleX.value
                                scaleY = cardScaleY.value
                                alpha = cardAlpha.value
                            }
                            .then(blurMod)
                    ) {
                        ExpandedIslandCard(
                            app = app,
                            state = state,
                            settings = settings,
                            accentColor = accentColor,
                            musicAccent = musicAccent,
                            cardBackground = cardBackground,
                            borderBrush = pillBorderBrush,
                            useGlass = useGlass,
                            effectiveStyle = effectiveStyle,
                            elasticity = elasticity,
                            durationScale = durationScale,
                            onRequestHaptic = onRequestHaptic,
                            onOpenAppSettings = onOpenAppSettings,
                            onPreviewVaultItem = { previewVaultItem = it }
                        )
                    }
                }

                showMediumStage -> {
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(0.5f, 0f)
                                scaleX = cardScaleX.value
                                scaleY = cardScaleY.value
                                alpha = cardAlpha.value
                            }
                            .then(blurMod)
                    ) {
                        MediumPillView(
                            app = app,
                            state = state,
                            settings = settings,
                            accentColor = accentColor,
                            musicAccent = musicAccent,
                            cardBackground = cardBackground,
                            borderBrush = pillBorderBrush,
                            mediaState = mediaState,
                            timerState = timerState,
                            onRequestHaptic = onRequestHaptic
                        )
                    }
                }

                state.mode == IslandMode.IDLE -> {
                    Box(
                        modifier = Modifier.graphicsLayer {
                            scaleX = pillPreStretchX.value * pullScaleBoost
                            scaleY = pullScaleBoost
                            translationY = pullOffsetYPx
                        }
                    ) {
                        IdlePillView(
                            settings = settings,
                            accentColor = accentColor,
                            isCalibrationPreview = state.isCalibrationPreview,
                            isDragOverIsland = state.isDragOverIsland,
                            isLowBatteryWarning = state.isLowBatteryWarning
                        )
                    }
                }

                else -> {
                    // COMPACT or SPLIT with Companion Bubble (+N) on user-chosen side
                    val companionView: @Composable () -> Unit = {
                        if (state.mode == IslandMode.SPLIT && state.secondaryActivity != null) {
                            SplitSecondaryBubble(
                                activity = state.secondaryActivity!!,
                                companionCount = state.companionCount,
                                accentColor = activityAccent(state.secondaryActivity!!, accentColor, musicAccent),
                                borderColor = pillBorderColor,
                                onClick = {
                                    onRequestHaptic()
                                    app.islandController.swapPrimaryAndSecondary()
                                }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.graphicsLayer {
                            scaleX = compactScale * pillPreStretchX.value * pullScaleBoost
                            scaleY = compactScale * pullScaleBoost
                            translationY = pullOffsetYPx
                        }
                    ) {
                        if (settings.companionPosition == CompanionPosition.LEFT) {
                            companionView()
                        }
                        CompactPillView(
                            state = state,
                            settings = settings,
                            accentColor = accentColor,
                            musicAccent = musicAccent,
                            isPlaying = mediaState.isPlaying,
                            albumArt = mediaState.albumArt,
                            pillBorderColor = pillBorderColor
                        )
                        if (settings.companionPosition == CompanionPosition.RIGHT) {
                            companionView()
                        }
                    }
                }
            }
        }

        // Draggable Floating Mini-Window inside the overlay (Calc / Notes / Timer / Search)
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

        // MIUI-style Floating Apps Sidebar overlay when opened
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

        // Flashlight Volumetric Beam Overlay Card
        if (state.isFlashlightCardOpen) {
            FlashlightBeamOverlayCard(
                app = app,
                accentColor = accentColor,
                onDismiss = { app.islandController.setFlashlightCardOpen(false) }
            )
        }

        // Vault Full-Screen Preview Overlay
        val activePreview = previewVaultItem
        if (activePreview != null) {
            VaultFullScreenPreviewOverlay(
                item = activePreview,
                app = app,
                accentColor = accentColor,
                onClose = { previewVaultItem = null },
                onCollapseIsland = {
                    previewVaultItem = null
                    app.islandController.collapse()
                }
            )
        }

        // Custom iOS-style Interactive OCR Text Selection Overlay (blue handles + loupe + Callout Copy)
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

        // Transient status banner
        AnimatedVisibility(
            visible = state.statusBanner != null,
            enter = fadeIn(tween(140)),
            exit = fadeOut(tween(120)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = (settings.cutoutYOffsetDp + settings.cutoutHeightDp + 24f).dp)
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
    is IslandActivity.Timer -> IslandColors.AccentOrange
    is IslandActivity.Charging -> IslandColors.AccentGreen
    is IslandActivity.Music -> musicAccent
    is IslandActivity.VoiceRecording -> IslandColors.AccentRed
    else -> defaultAccent
}

/**
 * Idle Pill: minimal ring around the camera cutout. Zero text under camera.
 * Shows vivid red Calibration Halo (#FF3B30) when Calibrate Preview is active.
 */
@Composable
private fun IdlePillView(
    settings: IslandSettings,
    accentColor: Color,
    isCalibrationPreview: Boolean,
    isDragOverIsland: Boolean,
    isLowBatteryWarning: Boolean
) {
    val holeW = settings.cutoutWidthDp.dp
    val holeH = settings.cutoutHeightDp.dp
    val ringBorderColor = when {
        isCalibrationPreview -> IslandColors.AccentRed
        isDragOverIsland -> accentColor
        isLowBatteryWarning -> IslandColors.AccentRed.copy(alpha = 0.72f)
        else -> Color.White.copy(alpha = 0.24f)
    }
    val strokeWidth = if (isDragOverIsland || isCalibrationPreview || isLowBatteryWarning) 2.dp else 1.dp

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(IslandColors.PillBlack)
            .border(strokeWidth, ringBorderColor, CircleShape)
            .padding(horizontal = 4.dp, vertical = 3.dp)
            .testTag("idle_camera_ring")
    ) {
        if (isLowBatteryWarning) {
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Filled.BatteryAlert,
                contentDescription = "Low battery warning",
                tint = IslandColors.AccentRed,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(8.dp))
            Spacer(modifier = Modifier.size(width = holeW, height = holeH))
            Spacer(Modifier.width(8.dp))
        } else {
            Spacer(modifier = Modifier.size(width = holeW, height = holeH))
        }
    }
}

@Composable
private fun CompactPillView(
    state: IslandState,
    settings: IslandSettings,
    accentColor: Color,
    musicAccent: Color,
    isPlaying: Boolean,
    albumArt: android.graphics.Bitmap?,
    pillBorderColor: Color
) {
    val activity = state.primaryActivity
    val hideTextOnLock = state.isDeviceLocked && settings.lockScreenPreset == LockScreenPreset.PRIVACY_HIDE_TEXT

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height((settings.cutoutHeightDp + 10f).coerceAtLeast(36f).dp)
            .clip(CircleShape)
            .background(IslandColors.PillBlack)
            .border(if (state.isDragOverIsland) 1.5.dp else 1.dp, pillBorderColor, CircleShape)
            .padding(horizontal = 10.dp)
    ) {
        // LEFT SLOT (strictly left of camera cutout >= 8dp)
        when (activity) {
            is IslandActivity.Music -> {
                if (albumArt != null) {
                    Image(
                        bitmap = albumArt.asImageBitmap(),
                        contentDescription = "Album art",
                        modifier = Modifier.size(22.dp).clip(CircleShape)
                    )
                } else {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = musicAccent, modifier = Modifier.size(18.dp))
                }
            }
            is IslandActivity.Timer -> {
                Icon(Icons.Filled.Timer, contentDescription = null, tint = IslandColors.AccentOrange, modifier = Modifier.size(18.dp))
            }
            is IslandActivity.Charging -> {
                Icon(Icons.Filled.BatteryChargingFull, contentDescription = null, tint = IslandColors.AccentGreen, modifier = Modifier.size(18.dp))
            }
            is IslandActivity.NotificationPreview -> {
                Icon(Icons.Filled.Notifications, contentDescription = null, tint = accentColor, modifier = Modifier.size(17.dp))
            }
            is IslandActivity.VoiceRecording -> {
                Icon(Icons.Filled.Mic, contentDescription = null, tint = IslandColors.AccentRed, modifier = Modifier.size(17.dp))
            }
            is IslandActivity.Alert -> {
                Icon(Icons.Filled.Notifications, contentDescription = null, tint = accentColor, modifier = Modifier.size(17.dp))
            }
            null -> {
                Spacer(Modifier.width(12.dp))
            }
        }

        // CAMERA CUTOUT CLEARANCE (>= 8dp on both sides -> +16dp total)
        Spacer(Modifier.width((settings.cutoutWidthDp + 16f).dp))

        // RIGHT SLOT (strictly right of camera cutout)
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
                            modifier = Modifier.widthIn(max = 72.dp)
                        )
                    }
                    MusicCompactWaveform(isPlaying = isPlaying, accentColor = musicAccent)
                }
            }
            is IslandActivity.Timer -> {
                Text(
                    text = TimerEngine.formatDuration(activity.remainingMs),
                    style = TextStyle(
                        color = IslandColors.AccentOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum"
                    )
                )
            }
            is IslandActivity.Charging -> {
                val wattsText = activity.wattageWatts?.takeIf { it > 0.5f }
                    ?.let { " · ${it.roundToInt()}W" }.orEmpty()
                val frac = (activity.levelPercent / 100f).coerceIn(0.05f, 1f)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(frac)
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(IslandColors.AccentGreen)
                        )
                    }
                    Text(
                        text = "${activity.levelPercent}%$wattsText",
                        style = TextStyle(
                            color = IslandColors.AccentGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        )
                    )
                }
            }
            is IslandActivity.NotificationPreview -> {
                Text(
                    text = if (hideTextOnLock) activity.appName else activity.title,
                    color = IslandColors.TextPrimary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 84.dp)
                )
            }
            is IslandActivity.VoiceRecording -> {
                Text(
                    text = TimerEngine.formatDuration(activity.elapsedMs),
                    color = IslandColors.AccentRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            is IslandActivity.Alert -> {
                val alertColor = if (activity.kind == AlertKind.LOW_BATTERY) IslandColors.AccentRed else accentColor
                Text(
                    text = if (hideTextOnLock) "Alert" else activity.title,
                    color = alertColor,
                    fontSize = 11.sp,
                    fontWeight = if (activity.kind == AlertKind.LOW_BATTERY) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 86.dp)
                )
            }
            null -> {
                Spacer(Modifier.width(12.dp))
            }
        }
    }
}

@Composable
private fun SplitSecondaryBubble(
    activity: IslandActivity,
    companionCount: Int,
    accentColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.clickable(onClick = onClick).testTag("split_companion_bubble")) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(IslandColors.PillBlack)
                .border(1.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when (activity) {
                is IslandActivity.Timer -> Text(
                    text = "${(activity.remainingMs / 1000L).coerceAtMost(99)}",
                    color = IslandColors.AccentOrange,
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
 * Stage 1 Medium Pill Preview (first tap on two-stage open):
 * Camera cutout clearance at top + compact quick controls + tap chevron to open Stage 2 Expanded.
 */
@Composable
private fun MediumPillView(
    app: DynamicIslandApp,
    state: IslandState,
    settings: IslandSettings,
    accentColor: Color,
    musicAccent: Color,
    cardBackground: Brush,
    borderBrush: Brush,
    mediaState: com.example.media.MediaUiState,
    timerState: com.example.system.TimerUiState,
    onRequestHaptic: () -> Unit
) {
    val primary = state.primaryActivity
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .widthIn(max = 328.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(cardBackground)
            .border(1.dp, borderBrush, RoundedCornerShape(32.dp))
            .clickable {
                onRequestHaptic()
                app.islandController.expand()
            }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("medium_stage1_pill"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top header strictly clears punch-hole camera
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height((settings.cutoutHeightDp + 10f).coerceAtLeast(34f).dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (primary) {
                    is IslandActivity.Music -> mediaState.appLabel.ifBlank { "Now Playing" }
                    is IslandActivity.Timer -> "Timer"
                    is IslandActivity.Charging -> "Power"
                    else -> state.activeTab.title
                },
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width((settings.cutoutWidthDp + 16f).dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f))
                    .clickable {
                        onRequestHaptic()
                        app.islandController.expand()
                    }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .testTag("medium_expand_btn")
            ) {
                Text("Full", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Open full card", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }

        // Body strip below camera
        when (primary) {
            is IslandActivity.Music -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (mediaState.albumArt != null) {
                        Image(
                            bitmap = mediaState.albumArt.asImageBitmap(),
                            contentDescription = "Album art",
                            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = musicAccent)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mediaState.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(mediaState.artist, color = IslandColors.TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { onRequestHaptic(); app.mediaHub.skipPrevious() }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White)
                    }
                    IconButton(
                        onClick = {
                            onRequestHaptic()
                            if (mediaState.isPlaying) app.soundManager.playMusicPause() else app.soundManager.playMusicPlay()
                            app.mediaHub.togglePlayPause()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (mediaState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = musicAccent
                        )
                    }
                    IconButton(onClick = { onRequestHaptic(); app.mediaHub.skipNext() }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White)
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
                        style = TextStyle(color = IslandColors.AccentOrange, fontSize = 24.sp, fontWeight = FontWeight.Light, fontFeatureSettings = "tnum")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                onRequestHaptic()
                                if (timerState.isRunning) app.timerEngine.pauseTimer() else app.timerEngine.resumeTimer()
                            },
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(IslandColors.AccentOrange)
                        ) {
                            Icon(
                                imageVector = if (timerState.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Toggle timer",
                                tint = Color.Black
                            )
                        }
                        IconButton(
                            onClick = { onRequestHaptic(); app.timerEngine.cancelTimer() },
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f))
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
                                .size(38.dp)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExpandedIslandCard(
    app: DynamicIslandApp,
    state: IslandState,
    settings: IslandSettings,
    accentColor: Color,
    musicAccent: Color,
    cardBackground: Brush,
    borderBrush: Brush,
    useGlass: Boolean,
    effectiveStyle: AnimationStyle,
    elasticity: Float,
    durationScale: Float,
    onRequestHaptic: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onPreviewVaultItem: (VaultItemEntity) -> Unit
) {
    val mediaState by app.mediaHub.state.collectAsState()
    val timerState by app.timerEngine.state.collectAsState()
    val calcState by app.calculatorEngine.state.collectAsState()

    var pinnedApps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    LaunchedEffect(settings.pinnedAppPackages) {
        if (settings.pinnedAppPackages.isNotEmpty()) {
            val all = app.searchEngine.getInstalledApps()
            val map = all.associateBy { it.packageName }
            pinnedApps = settings.pinnedAppPackages.mapNotNull { map[it] }
        } else {
            pinnedApps = emptyList()
        }
    }

    val outerShape = RoundedCornerShape(IslandColors.ExpandedOuterRadius)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = IslandColors.ScreenHorizontalMargin)
            .widthIn(max = 408.dp)
            .clip(outerShape)
            .background(cardBackground)
            .border(if (state.isDragOverIsland) 1.5.dp else 1.dp, borderBrush, outerShape)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = {
                    onRequestHaptic()
                    onOpenAppSettings()
                }
            )
            .padding(bottom = 10.dp)
    ) {
        // Camera-aware Top Header Row: Left control + Camera Clearance (>= 8dp both sides) + Right controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height((settings.cutoutHeightDp + 16f).coerceAtLeast(42f).dp)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        onRequestHaptic()
                        app.islandController.collapse()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Collapse island", tint = IslandColors.TextSecondary, modifier = Modifier.size(18.dp))
                }
                Text(
                    text = state.activeTab.title,
                    color = IslandColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Camera cutout safety spacer in the middle
            Spacer(Modifier.width((settings.cutoutWidthDp + 16f).dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        onRequestHaptic()
                        app.soundManager.playTap()
                        app.islandController.collapse()
                        onOpenAppSettings()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Open Settings", tint = IslandColors.TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        // EXPANDED HOME LAYOUT (Dynamic Ledge): Live Activity FIRST + up to 3 User Shortcuts beside it
        val primaryLive = state.primaryActivity
        if (primaryLive != null || pinnedApps.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 3.dp)
            ) {
                if (primaryLive != null) {
                    val liveTab = when (primaryLive) {
                        is IslandActivity.Music -> IslandTab.MUSIC
                        is IslandActivity.Timer -> IslandTab.TIMER
                        is IslandActivity.VoiceRecording -> IslandTab.MEMO
                        is IslandActivity.Alert -> IslandTab.ALERTS
                        else -> state.activeTab
                    }
                    val liveText = when (primaryLive) {
                        is IslandActivity.Music -> primaryLive.title.ifBlank { "Music" }
                        is IslandActivity.Timer -> TimerEngine.formatDuration(primaryLive.remainingMs)
                        is IslandActivity.Charging -> "${primaryLive.levelPercent}% Charging"
                        is IslandActivity.NotificationPreview -> primaryLive.title.ifBlank { primaryLive.appName }
                        is IslandActivity.Alert -> primaryLive.title
                        is IslandActivity.VoiceRecording -> "Rec ${TimerEngine.formatDuration(primaryLive.elapsedMs)}"
                    }
                    val liveColor = activityAccent(primaryLive, accentColor, musicAccent)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(CircleShape)
                            .background(liveColor.copy(alpha = 0.22f))
                            .clickable {
                                onRequestHaptic()
                                app.islandController.selectTab(liveTab)
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(liveColor))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = liveText,
                            color = IslandColors.TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 128.dp)
                        )
                    }
                }
                if (pinnedApps.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(pinnedApps.take(3), key = { it.packageName }) { shortcut ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E2028))
                                    .clickable {
                                        onRequestHaptic()
                                        app.soundManager.playTap()
                                        app.searchEngine.launchApp(shortcut.packageName)
                                        app.islandController.collapse()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                val bmp = shortcut.iconBitmap
                                if (bmp != null) {
                                    Image(bitmap = bmp, contentDescription = shortcut.label, modifier = Modifier.size(14.dp).clip(CircleShape))
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(shortcut.label, color = IslandColors.TextPrimary, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // Expanded Notification Swipe Handler (Right = Next Notif, Left = Dismiss — dynamicSpot v3.01)
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

        val innerBlur = remember { Animatable(0f) }
        LaunchedEffect(state.activeTab, settings.contentEffect, effectiveStyle) {
            if (effectiveStyle != AnimationStyle.NONE && settings.contentEffect == ContentEffect.BLUR_IN) {
                innerBlur.snapTo(8f)
                innerBlur.animateTo(0f, MotionTokens.effectsFast(durationScale))
            } else {
                innerBlur.snapTo(0f)
            }
        }
        val contentBlurMod = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && innerBlur.value > 0.4f) {
            Modifier.blur(innerBlur.value.dp)
        } else {
            Modifier
        }

        // Unified 220.dp capped content box with ContentEffect + MotionTokens M3 Expressive transitions
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .then(expandedSwipeModifier)
                .then(contentBlurMod)
        ) {
            AnimatedContent(
                targetState = state.activeTab,
                transitionSpec = {
                    if (effectiveStyle == AnimationStyle.NONE) {
                        fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    } else {
                        val fadeSpec = MotionTokens.contentCrossfadeTween(effectiveStyle, durationScale)
                        val moveSpec = MotionTokens.spatialFast<IntOffset>(effectiveStyle, elasticity, durationScale)
                        val scaleSpec = MotionTokens.spatialFast<Float>(effectiveStyle, elasticity, durationScale)
                        when (settings.contentEffect) {
                            ContentEffect.FADE -> fadeIn(fadeSpec) togetherWith fadeOut(tween(90))
                            ContentEffect.SLIDE_UP -> (slideInVertically(moveSpec) { h -> (h * 0.16f).roundToInt() } + fadeIn(fadeSpec))
                                .togetherWith(slideOutVertically(tween(95)) { h -> -(h * 0.08f).roundToInt() } + fadeOut(tween(80)))
                            ContentEffect.SCALE_IN -> (scaleIn(initialScale = 0.88f, animationSpec = scaleSpec) + fadeIn(fadeSpec))
                                .togetherWith(fadeOut(tween(85)))
                            ContentEffect.BLUR_IN -> {
                                val forward = targetState.ordinal >= initialState.ordinal
                                val enterOffset = if (forward) { fullW: Int -> (fullW * 0.14f).roundToInt() } else { fullW: Int -> -(fullW * 0.14f).roundToInt() }
                                val exitOffset = if (forward) { fullW: Int -> -(fullW * 0.10f).roundToInt() } else { fullW: Int -> (fullW * 0.10f).roundToInt() }
                                (slideInHorizontally(moveSpec, initialOffsetX = enterOffset) +
                                    fadeIn(fadeSpec) +
                                    scaleIn(initialScale = 0.95f, animationSpec = scaleSpec))
                                    .togetherWith(
                                        slideOutHorizontally(tween(110), targetOffsetX = exitOffset) +
                                            fadeOut(tween(95))
                                    )
                            }
                        }
                    }
                },
                label = "island_tab_content"
            ) { tab ->
                when (tab) {
                    IslandTab.MUSIC -> MusicExpandedView(
                        state = mediaState,
                        accentColor = musicAccent,
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

                    IslandTab.CALC -> CalculatorIslandView(
                        state = calcState,
                        onDigit = { app.calculatorEngine.pressDigit(it) },
                        onDecimal = { app.calculatorEngine.pressDecimal() },
                        onClear = { app.calculatorEngine.pressClear() },
                        onToggleSign = { app.calculatorEngine.pressToggleSign() },
                        onPercent = { app.calculatorEngine.pressPercent() },
                        onOperator = { app.calculatorEngine.pressOperator(it) },
                        onEquals = { app.calculatorEngine.pressEquals() },
                        onBackspace = { app.calculatorEngine.pressBackspace() },
                        onHaptic = onRequestHaptic,
                        onKeySound = { app.soundManager.playCalcKey() },
                        compactMode = true
                    )

                    IslandTab.TIMER -> TimerStopwatchPanel(
                        state = timerState,
                        accentColor = IslandColors.AccentOrange,
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
                        onCollapse = { app.islandController.collapse() },
                        onPreviewItem = onPreviewVaultItem
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

        Spacer(Modifier.height(6.dp))

        // Locked Bottom Chooser strictly honoring user-ordered settings.enabledTabs
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
    var offsetY by remember { mutableFloatStateOf(160f) }
    val calcState by app.calculatorEngine.state.collectAsState()
    val timerState by app.timerEngine.state.collectAsState()

    Column(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .widthIn(max = 356.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(IslandColors.CardRadius))
            .background(Color(0xF50C0D12))
            .border(1.dp, accentColor.copy(alpha = 0.65f), RoundedCornerShape(IslandColors.CardRadius))
            .padding(bottom = 8.dp)
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
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DragIndicator, contentDescription = "Drag floating window", tint = IslandColors.TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Floating · ${tab.title}", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExpandToIsland, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Filled.OpenInFull, contentDescription = "Dock to Island", tint = IslandColors.TextSecondary, modifier = Modifier.size(15.dp))
                }
                IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close floating window", tint = IslandColors.TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }

        when (tab) {
            IslandTab.CALC -> CalculatorIslandView(
                state = calcState,
                onDigit = { app.calculatorEngine.pressDigit(it) },
                onDecimal = { app.calculatorEngine.pressDecimal() },
                onClear = { app.calculatorEngine.pressClear() },
                onToggleSign = { app.calculatorEngine.pressToggleSign() },
                onPercent = { app.calculatorEngine.pressPercent() },
                onOperator = { app.calculatorEngine.pressOperator(it) },
                onEquals = { app.calculatorEngine.pressEquals() },
                onBackspace = { app.calculatorEngine.pressBackspace() },
                onHaptic = onRequestHaptic,
                onKeySound = { app.soundManager.playCalcKey() },
                compactMode = true
            )
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
    // Strictly preserve user-defined order from settings.enabledTabs (5 main tabs + More row)
    val orderedActive = settings.enabledTabs.filter { settings.isTabMasterEnabled(it) }
    val mainTabs = orderedActive.take(5)
    val moreTabs = orderedActive.drop(5)

    // Never glass on glass: opaque elevated bar when outer island is Glass; translucent bar when outer is Solid Black
    val chooserBg = if (!useLightGlass) {
        Color(0x22FFFFFF)
    } else {
        Color(0xFF1A1B23)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .clip(RoundedCornerShape(IslandColors.ExpandedInnerSheetRadius))
            .background(chooserBg)
            .border(0.75.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(IslandColors.ExpandedInnerSheetRadius))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        AnimatedVisibility(visible = isMoreOpen && moreTabs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                moreTabs.forEach { tab ->
                    ChooserChip(
                        tab = tab,
                        selected = activeTab == tab,
                        accentColor = accentColor,
                        onClick = { onSelectTab(tab) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            mainTabs.forEach { tab ->
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
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isMoreOpen) accentColor.copy(alpha = 0.22f) else Color.Transparent)
                        .clickable(onClick = onToggleMore)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("chooser_more_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreHoriz,
                        contentDescription = "More tabs",
                        tint = if (isMoreOpen) accentColor else IslandColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "More",
                        fontSize = 10.sp,
                        color = if (isMoreOpen) accentColor else IslandColors.TextSecondary
                    )
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
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> MotionTokens.BUTTON_PRESS_SCALE
            selected -> 1.05f
            else -> 1.0f
        },
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 620f),
        label = "chip_scale"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accentColor.copy(alpha = 0.24f) else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag("chooser_tab_${tab.name.lowercase()}")
    ) {
        Icon(
            imageVector = tabIcon(tab),
            contentDescription = tab.title,
            tint = if (selected) accentColor else IslandColors.TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = tab.title,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) accentColor else IslandColors.TextSecondary
        )
        if (selected) {
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
        }
    }
}

fun tabIcon(tab: IslandTab): ImageVector = when (tab) {
    IslandTab.SEARCH -> Icons.Filled.Search
    IslandTab.MUSIC -> Icons.Filled.MusicNote
    IslandTab.CALC -> Icons.Filled.Calculate
    IslandTab.TIMER -> Icons.Filled.Timer
    IslandTab.TOOLS -> Icons.Filled.Tune
    IslandTab.VAULT -> Icons.Filled.PhotoLibrary
    IslandTab.CLIPS -> Icons.Filled.ContentPaste
    IslandTab.NOTES -> Icons.Filled.Description
    IslandTab.MEMO -> Icons.Filled.Mic
    IslandTab.ALERTS -> Icons.Filled.Notifications
}
