package com.example.island

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.DynamicIslandApp
import com.example.floating.FloatingSidebarOverlay
import com.example.media.MusicCompactWaveform
import com.example.media.MusicExpandedView
import com.example.search.LaunchableApp
import com.example.system.AccentTheme
import com.example.system.CalculatorIslandView
import com.example.system.IdleWidgetType
import com.example.system.IslandSettings
import com.example.system.IslandTab
import com.example.system.LockScreenPreset
import com.example.system.PillStyle
import com.example.system.SpringFeel
import com.example.system.TimerEngine
import com.example.system.TimerStopwatchPanel
import com.example.ui.theme.IslandColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val calcState by app.calculatorEngine.state.collectAsState()

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

    // Spring parameters tuned for 120 Hz on Snapdragon 732G (no full-screen blur)
    val damping = when {
        reduceMotion -> Spring.DampingRatioNoBouncy
        state.mode == IslandMode.EXPANDED -> when (settings.springFeel) {
            SpringFeel.SNAPPY -> 0.72f
            SpringFeel.BALANCED -> 0.78f
            SpringFeel.GENTLE -> 0.86f
        }
        else -> Spring.DampingRatioNoBouncy // critically-damped settling for collapse
    }
    val stiffness = if (reduceMotion) 1200f else (440f * settings.animationSpeed)

    val expandScale by animateFloatAsState(
        targetValue = if (state.mode == IslandMode.EXPANDED) 1f else 0.96f,
        animationSpec = spring(dampingRatio = damping, stiffness = stiffness),
        label = "island_scale"
    )
    val expandAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = stiffness),
        label = "island_alpha"
    )

    val useGlass = settings.pillStyle == PillStyle.GLASS && !settings.solidFallback
    val glassAlpha = (0.58f + settings.glassDensity * 0.32f).coerceIn(0.55f, 0.92f)
    val cardBackground = if (useGlass) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1A1B22).copy(alpha = glassAlpha),
                Color(0xFF0A0A0E).copy(alpha = (glassAlpha + 0.08f).coerceAtMost(0.95f))
            )
        )
    } else {
        Brush.verticalGradient(colors = listOf(IslandColors.PillBlack, IslandColors.PillBlack))
    }

    val pillBorderColor = if (useGlass) IslandColors.GlassSpecularBorder else Color.White.copy(alpha = 0.10f)

    Box(modifier = Modifier.fillMaxSize()) {
        // Select mode active indicator banner at top
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
                        text = "Select Mode ON · Long-press any text to copy",
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

        // Main Island Container positioned around the camera cutout
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(
                    x = settings.cutoutXOffsetDp.dp,
                    y = (settings.cutoutYOffsetDp - 2f).coerceAtLeast(0f).dp
                )
                .graphicsLayer {
                    scaleX = expandScale
                    scaleY = expandScale
                    alpha = expandAlpha
                },
            contentAlignment = Alignment.TopCenter
        ) {
            when (state.mode) {
                IslandMode.IDLE -> {
                    IdlePillView(
                        settings = settings,
                        batteryPercent = batteryState.levelPercent,
                        accentColor = accentColor,
                        isCalibrationPreview = state.isCalibrationPreview
                    )
                }

                IslandMode.COMPACT, IslandMode.SPLIT -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                IslandMode.EXPANDED -> {
                    ExpandedIslandCard(
                        app = app,
                        state = state,
                        settings = settings,
                        accentColor = accentColor,
                        cardBackground = cardBackground,
                        borderColor = pillBorderColor,
                        useGlass = useGlass,
                        onRequestHaptic = onRequestHaptic,
                        onOpenAppSettings = onOpenAppSettings
                    )
                }
            }
        }

        // MIUI-style Floating Apps Sidebar overlay when opened from Tools
        AnimatedVisibility(
            visible = state.isFloatingSidebarOpen,
            enter = fadeIn(),
            exit = fadeOut(),
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
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = (settings.cutoutYOffsetDp + settings.cutoutHeightDp + 24f).dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xEE18181F))
                    .border(1.dp, accentColor.copy(alpha = 0.6f), CircleShape)
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

@Composable
private fun IdlePillView(
    settings: IslandSettings,
    batteryPercent: Int,
    accentColor: Color,
    isCalibrationPreview: Boolean
) {
    val holeW = settings.cutoutWidthDp.dp
    val holeH = settings.cutoutHeightDp.dp
    val ringBorderColor = if (isCalibrationPreview) accentColor else Color.White.copy(alpha = 0.22f)

    // Idle state is just a thin border around the camera (+ optional idle widget left/right)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(IslandColors.PillBlack)
            .border(if (isCalibrationPreview) 1.5.dp else 1.dp, ringBorderColor, CircleShape)
            .padding(horizontal = if (settings.idleWidget == IdleWidgetType.NONE) 4.dp else 10.dp, vertical = 2.dp)
    ) {
        if (settings.idleWidget == IdleWidgetType.CLOCK) {
            val timeStr = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }
            Text(timeStr, color = IslandColors.TextSecondary, fontSize = 10.sp)
            Spacer(Modifier.width(6.dp))
        } else if (settings.idleWidget == IdleWidgetType.SEARCH_SHORTCUT) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = accentColor, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(6.dp))
        }

        // Exact camera cutout clearance box (never draw text/icons inside this)
        Spacer(modifier = Modifier.size(width = holeW, height = holeH))

        if (settings.idleWidget == IdleWidgetType.BATTERY_RING) {
            Spacer(Modifier.width(6.dp))
            Text("$batteryPercent%", color = IslandColors.AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        } else if (settings.idleWidget == IdleWidgetType.QUICK_NOTE) {
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Filled.Description, contentDescription = null, tint = accentColor, modifier = Modifier.size(12.dp))
        }
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
            .border(1.dp, pillBorderColor, CircleShape)
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

        // CAMERA CUTOUT CLEARANCE (nothing sits under the punch-hole)
        Spacer(Modifier.width((settings.cutoutWidthDp + 10f).dp))

        // RIGHT SLOT (strictly right of camera cutout)
        when (activity) {
            is IslandActivity.Music -> {
                MusicCompactWaveform(isPlaying = isPlaying, accentColor = accentColor)
            }
            is IslandActivity.Timer -> {
                Text(
                    text = TimerEngine.formatDuration(activity.remainingMs),
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            is IslandActivity.Charging -> {
                Text(
                    text = "${activity.levelPercent}%",
                    color = IslandColors.AccentGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
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
    borderColor: Color,
    useGlass: Boolean,
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
            .widthIn(max = 420.dp)
            .shadow(16.dp, RoundedCornerShape(36.dp))
            .clip(RoundedCornerShape(36.dp))
            .background(cardBackground)
            .border(1.dp, borderColor, RoundedCornerShape(36.dp))
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
                        app.islandController.collapse()
                        onOpenAppSettings()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Open Settings", tint = IslandColors.TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        // User-pinned App Shortcuts Quick Launch Row (Feature 17)
        if (pinnedApps.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                items(pinnedApps, key = { it.packageName }) { shortcut ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable {
                                onRequestHaptic()
                                app.searchEngine.launchApp(shortcut.packageName)
                                app.islandController.collapse()
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        val bmp = remember(shortcut.packageName) { shortcut.icon?.toBitmap(48, 48)?.asImageBitmap() }
                        if (bmp != null) {
                            Image(bitmap = bmp, contentDescription = shortcut.label, modifier = Modifier.size(16.dp).clip(CircleShape))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(shortcut.label, color = IslandColors.TextPrimary, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }

        // Active Module Content
        when (state.activeTab) {
            IslandTab.MUSIC -> MusicExpandedView(
                state = mediaState,
                accentColor = accentColor,
                onTogglePlayPause = { onRequestHaptic(); app.mediaHub.togglePlayPause() },
                onPrevious = { onRequestHaptic(); app.mediaHub.skipPrevious() },
                onNext = { onRequestHaptic(); app.mediaHub.skipNext() },
                onSeekTo = { pos -> app.mediaHub.seekTo(pos) },
                onToggleShuffle = { onRequestHaptic(); app.mediaHub.sendShuffleCommand() },
                onCycleRepeat = { onRequestHaptic(); app.mediaHub.sendRepeatCommand() },
                onToggleLike = { onRequestHaptic(); app.mediaHub.sendLikeCommand() },
                onOpenPlayer = { onRequestHaptic(); app.mediaHub.openPlayerApp(); app.islandController.collapse() }
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
                onHaptic = onRequestHaptic,
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
                onHaptic = onRequestHaptic
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

    // Never glass on glass! Use light glass only when main card is solid black, otherwise subtle surface
    val chooserBg = if (!useLightGlass) {
        Color(0x22FFFFFF)
    } else {
        Color(0xFF1A1B23)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .shadow(6.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(chooserBg)
            .border(0.75.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(22.dp))
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accentColor.copy(alpha = 0.24f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
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
