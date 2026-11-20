package com.example.island

import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import com.example.search.LaunchableApp
import com.example.system.AccentTheme
import com.example.system.CalculatorIslandView
import com.example.system.IslandSettings
import com.example.system.IslandTab
import com.example.system.LockScreenPreset
import com.example.system.PillStyle
import com.example.system.SpringFeel
import com.example.system.TimerEngine
import com.example.system.TimerStopwatchPanel
import com.example.ui.theme.IslandColors
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
    val batteryState by app.batteryMonitor.state.collectAsState()

    val context = LocalContext.current
    val systemAnimScale = remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
    }
    val reduceMotion = settings.reducedMotion || systemAnimScale == 0f

    val accentColor = when (settings.accentTheme) {
        AccentTheme.ORANGE -> IslandColors.AccentOrange
        AccentTheme.CYAN -> IslandColors.AccentCyan
        AccentTheme.GREEN -> IslandColors.AccentGreen
        AccentTheme.DYNAMIC -> IslandColors.AccentOrange
    }

    // Lock screen visibility policy
    if (state.isDeviceLocked && settings.lockScreenPreset == LockScreenPreset.HIDDEN) {
        return
    }

    val damping = when {
        reduceMotion -> Spring.DampingRatioNoBouncy
        state.mode == IslandMode.EXPANDED -> when (settings.springFeel) {
            SpringFeel.SNAPPY -> 0.72f
            SpringFeel.BALANCED -> 0.78f
            SpringFeel.GENTLE -> 0.86f
        }
        else -> Spring.DampingRatioNoBouncy
    }
    val stiffness = if (reduceMotion) 1400f else (460f * settings.animationSpeed)

    // Stage 1 + Stage 2 Apple-style choreography strictly via GPU graphicsLayer
    var showExpandedCard by remember { mutableStateOf(state.mode == IslandMode.EXPANDED) }
    val pillPreStretchX = remember { Animatable(1f) }
    val cardScaleX = remember { Animatable(if (state.mode == IslandMode.EXPANDED) 1f else 0.45f) }
    val cardScaleY = remember { Animatable(if (state.mode == IslandMode.EXPANDED) 1f else 0.25f) }
    val cardAlpha = remember { Animatable(if (state.mode == IslandMode.EXPANDED) 1f else 0f) }

    LaunchedEffect(state.mode, reduceMotion, settings.animationSpeed, settings.springFeel) {
        if (state.mode == IslandMode.EXPANDED) {
            if (reduceMotion) {
                showExpandedCard = true
                pillPreStretchX.snapTo(1f)
                cardScaleX.snapTo(1f)
                cardScaleY.snapTo(1f)
                cardAlpha.snapTo(1f)
            } else {
                // Stage 1: Pill widens sideways briefly around the punch-hole camera
                pillPreStretchX.animateTo(
                    targetValue = 1.24f,
                    animationSpec = spring(dampingRatio = 0.65f, stiffness = stiffness * 1.4f)
                )
                // Stage 2: Smooth spring expansion from camera anchor
                showExpandedCard = true
                launch { pillPreStretchX.snapTo(1f) }
                launch { cardScaleX.animateTo(1f, spring(dampingRatio = damping, stiffness = stiffness)) }
                launch { cardScaleY.animateTo(1f, spring(dampingRatio = damping, stiffness = stiffness)) }
                cardAlpha.animateTo(1f, tween(durationMillis = 120))
            }
        } else {
            if (reduceMotion) {
                showExpandedCard = false
                cardAlpha.snapTo(0f)
                cardScaleX.snapTo(0.45f)
                cardScaleY.snapTo(0.25f)
                pillPreStretchX.snapTo(1f)
            } else if (showExpandedCard) {
                // Stage 1 Collapse: Card springs inward smoothly toward camera hole
                launch { cardScaleX.animateTo(0.46f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = stiffness * 1.15f)) }
                launch { cardScaleY.animateTo(0.26f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = stiffness * 1.15f)) }
                cardAlpha.animateTo(0f, tween(durationMillis = 110))
                showExpandedCard = false
                // Stage 2 Collapse: Sideways pill settles back cleanly around camera ring
                pillPreStretchX.snapTo(1.18f)
                pillPreStretchX.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.76f, stiffness = stiffness)
                )
            }
        }
    }

    val compactScale by animateFloatAsState(
        targetValue = if (state.mode == IslandMode.COMPACT || state.mode == IslandMode.SPLIT) 1f else 0.94f,
        animationSpec = spring(dampingRatio = damping, stiffness = stiffness),
        label = "compact_scale"
    )

    val useGlass = settings.pillStyle == PillStyle.GLASS && !settings.solidFallback
    val glassAlpha = (0.60f + settings.glassDensity * 0.30f).coerceIn(0.56f, 0.92f)
    val cardBackground = if (useGlass) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1C1E26).copy(alpha = glassAlpha),
                Color(0xFF0A0A0E).copy(alpha = (glassAlpha + 0.08f).coerceAtMost(0.95f))
            )
        )
    } else {
        Brush.verticalGradient(colors = listOf(IslandColors.PillBlack, IslandColors.PillBlack))
    }

    val pillBorderBrush = IslandColors.liquidBorderBrush(
        useGlass = useGlass,
        isHighlighted = state.isDragOverIsland,
        accentColor = accentColor
    )
    val pillBorderColor = when {
        state.isDragOverIsland -> accentColor
        useGlass -> IslandColors.GlassSpecularBorder
        else -> Color.White.copy(alpha = 0.14f)
    }

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
                        .background(accentColor)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Mode ON · Tap or long-press any text",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Scrim tap outside expanded card collapses immediately
        if (state.mode == IslandMode.EXPANDED) {
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

        // Main Island Container anchored at camera cutout (RenderThread transforms only)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(
                    x = settings.cutoutXOffsetDp.dp,
                    y = (settings.cutoutYOffsetDp - 2f).coerceAtLeast(0f).dp
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            if (showExpandedCard) {
                Box(
                    modifier = Modifier.graphicsLayer {
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        scaleX = cardScaleX.value
                        scaleY = cardScaleY.value
                        alpha = cardAlpha.value
                    }
                ) {
                    ExpandedIslandCard(
                        app = app,
                        state = state,
                        settings = settings,
                        accentColor = accentColor,
                        cardBackground = cardBackground,
                        borderBrush = pillBorderBrush,
                        useGlass = useGlass,
                        reduceMotion = reduceMotion,
                        onRequestHaptic = onRequestHaptic,
                        onOpenAppSettings = onOpenAppSettings
                    )
                }
            } else {
                when (state.mode) {
                    IslandMode.IDLE -> {
                        Box(
                            modifier = Modifier.graphicsLayer {
                                scaleX = pillPreStretchX.value
                            }
                        ) {
                            IdlePillView(
                                settings = settings,
                                accentColor = accentColor,
                                isCalibrationPreview = state.isCalibrationPreview,
                                isDragOverIsland = state.isDragOverIsland
                            )
                        }
                    }

                    IslandMode.COMPACT, IslandMode.SPLIT, IslandMode.EXPANDED -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.graphicsLayer {
                                scaleX = compactScale * pillPreStretchX.value
                                scaleY = compactScale
                            }
                        ) {
                            CompactPillView(
                                state = state,
                                settings = settings,
                                accentColor = accentColor,
                                isPlaying = mediaState.isPlaying,
                                albumArt = mediaState.albumArt,
                                pillBorderColor = pillBorderColor
                            )
                            if (state.mode == IslandMode.SPLIT && state.secondaryActivity != null) {
                                SplitSecondaryBubble(
                                    activity = state.secondaryActivity!!,
                                    accentColor = accentColor,
                                    borderColor = pillBorderColor
                                )
                            }
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
                settings = settings,
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

        // MIUI-style Floating Apps Sidebar overlay when opened from Tools
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

/**
 * Idle Pill is strictly a thin minimal ring around the camera cutout.
 * Zero text, zero widgets under or beside the camera in IDLE.
 */
@Composable
private fun IdlePillView(
    settings: IslandSettings,
    accentColor: Color,
    isCalibrationPreview: Boolean,
    isDragOverIsland: Boolean
) {
    val holeW = settings.cutoutWidthDp.dp
    val holeH = settings.cutoutHeightDp.dp
    val ringBorderColor = when {
        isDragOverIsland -> accentColor
        isCalibrationPreview -> accentColor
        else -> Color.White.copy(alpha = 0.22f)
    }
    val strokeWidth = if (isDragOverIsland || isCalibrationPreview) 2.dp else 1.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(CircleShape)
            .background(IslandColors.PillBlack)
            .border(strokeWidth, ringBorderColor, CircleShape)
            .padding(horizontal = 4.dp, vertical = 3.dp)
            .testTag("idle_camera_ring")
    ) {
        Spacer(modifier = Modifier.size(width = holeW, height = holeH))
    }
}

@Composable
private fun CompactPillView(
    state: IslandState,
    settings: IslandSettings,
    accentColor: Color,
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
        // LEFT SLOT (strictly left of camera cutout)
        when (activity) {
            is IslandActivity.Music -> {
                if (albumArt != null) {
                    Image(
                        bitmap = albumArt.asImageBitmap(),
                        contentDescription = "Album art",
                        modifier = Modifier.size(22.dp).clip(CircleShape)
                    )
                } else {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                }
            }
            is IslandActivity.Timer -> {
                Icon(Icons.Filled.Timer, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
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

        // CAMERA CUTOUT CLEARANCE
        Spacer(Modifier.width((settings.cutoutWidthDp + 10f).dp))

        // RIGHT SLOT (strictly right of camera cutout)
        when (activity) {
            is IslandActivity.Music -> {
                MusicCompactWaveform(isPlaying = isPlaying, accentColor = accentColor)
            }
            is IslandActivity.Timer -> {
                Text(
                    text = TimerEngine.formatDuration(activity.remainingMs),
                    style = TextStyle(
                        color = accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum"
                    )
                )
            }
            is IslandActivity.Charging -> {
                Text(
                    text = "${activity.levelPercent}%",
                    style = TextStyle(
                        color = IslandColors.AccentGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    )
                )
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
                Text(
                    text = if (hideTextOnLock) "Alert" else activity.title,
                    color = accentColor,
                    fontSize = 11.sp,
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
    accentColor: Color,
    borderColor: Color
) {
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
                text = (activity.remainingMs / 1000L).toString(),
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            is IslandActivity.Music -> Icon(Icons.Filled.MusicNote, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            is IslandActivity.Charging -> Text("${activity.levelPercent}%", color = IslandColors.AccentGreen, fontSize = 10.sp)
            else -> Icon(Icons.Filled.Notifications, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
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
    cardBackground: Brush,
    borderBrush: Brush,
    useGlass: Boolean,
    reduceMotion: Boolean,
    onRequestHaptic: () -> Unit,
    onOpenAppSettings: () -> Unit
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .widthIn(max = 388.dp)
            .clip(RoundedCornerShape(34.dp))
            .background(cardBackground)
            .border(if (state.isDragOverIsland) 1.5.dp else 1.dp, borderBrush, RoundedCornerShape(34.dp))
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
        // Camera-aware Top Header Row: Left control + Camera Clearance + Right controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height((settings.cutoutHeightDp + 14f).coerceAtLeast(40f).dp)
                .padding(horizontal = 12.dp),
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

        // User-pinned App Shortcuts Quick Launch Row (pre-decoded ImageBitmap icons)
        if (pinnedApps.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            ) {
                items(pinnedApps, key = { it.packageName }) { shortcut ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable {
                                onRequestHaptic()
                                app.soundManager.playTap()
                                app.searchEngine.launchApp(shortcut.packageName)
                                app.islandController.collapse()
                            }
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        val bmp = shortcut.iconBitmap
                        if (bmp != null) {
                            Image(bitmap = bmp, contentDescription = shortcut.label, modifier = Modifier.size(15.dp).clip(CircleShape))
                            Spacer(Modifier.width(5.dp))
                        }
                        Text(shortcut.label, color = IslandColors.TextPrimary, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }

        // Capped 216.dp content box with smooth directional spring AnimatedContent per tab
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(216.dp)
        ) {
            AnimatedContent(
                targetState = state.activeTab,
                transitionSpec = {
                    if (reduceMotion) {
                        fadeIn(tween(80)) togetherWith fadeOut(tween(80))
                    } else {
                        val forward = targetState.ordinal >= initialState.ordinal
                        val enterOffset = if (forward) { fullW: Int -> (fullW * 0.14f).roundToInt() } else { fullW: Int -> -(fullW * 0.14f).roundToInt() }
                        val exitOffset = if (forward) { fullW: Int -> -(fullW * 0.10f).roundToInt() } else { fullW: Int -> (fullW * 0.10f).roundToInt() }
                        (slideInHorizontally(spring(dampingRatio = 0.82f, stiffness = 520f), initialOffsetX = enterOffset) +
                            fadeIn(tween(140)) +
                            scaleIn(initialScale = 0.96f, animationSpec = spring(dampingRatio = 0.82f, stiffness = 520f)))
                            .togetherWith(
                                slideOutHorizontally(tween(110), targetOffsetX = exitOffset) +
                                    fadeOut(tween(95))
                            )
                    }
                },
                label = "island_tab_content"
            ) { tab ->
                when (tab) {
                    IslandTab.MUSIC -> MusicExpandedView(
                        state = mediaState,
                        accentColor = accentColor,
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
                        onTimerSetSound = { app.soundManager.playTimerSet() }
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

        Spacer(Modifier.height(6.dp))

        // Locked Bottom Chooser: 5 main tabs + More button opening second row
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
    settings: IslandSettings,
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
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xF50C0D12))
            .border(1.dp, accentColor.copy(alpha = 0.65f), RoundedCornerShape(26.dp))
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
                onTimerSetSound = { app.soundManager.playTimerSet() }
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
    val activeEnabled = settings.enabledTabs.filter { settings.isTabMasterEnabled(it) }
    val mainTabs = listOf(
        IslandTab.SEARCH,
        IslandTab.MUSIC,
        IslandTab.CALC,
        IslandTab.TIMER,
        IslandTab.TOOLS
    ).filter { it in activeEnabled }

    val moreTabs = listOf(
        IslandTab.VAULT,
        IslandTab.CLIPS,
        IslandTab.NOTES,
        IslandTab.MEMO,
        IslandTab.ALERTS
    ).filter { it in activeEnabled }

    // Never glass on glass! Use translucent floating dock only when main card is solid black
    val chooserBg = if (!useLightGlass) {
        Color(0x22FFFFFF)
    } else {
        Color(0xFF1A1B23)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(chooserBg)
            .border(0.75.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(22.dp))
            .padding(horizontal = 4.dp, vertical = 4.dp)
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
            pressed -> 0.90f
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
