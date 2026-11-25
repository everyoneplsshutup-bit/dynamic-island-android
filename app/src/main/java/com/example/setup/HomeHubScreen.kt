package com.example.setup

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DynamicIslandApp
import com.example.island.AlertsIslandPanel
import com.example.island.BottomChooserBar
import com.example.island.ClipsIslandPanel
import com.example.island.DeadZoneGeometry
import com.example.island.IslandService
import com.example.island.MemoIslandPanel
import com.example.island.NotesIslandPanel
import com.example.island.SearchIslandPanel
import com.example.island.ToolsIslandPanel
import com.example.island.VaultIslandPanel
import com.example.keepalive.IslandKeepAliveService
import com.example.media.MusicExpandedView
import com.example.system.AnimationStyle
import com.example.system.IslandSettings
import com.example.system.IslandTab
import com.example.system.PillStyle
import com.example.system.TimerStopwatchPanel
import com.example.ui.theme.CrimsonCircularHaloIcon
import com.example.ui.theme.CrimsonSquircleIcon
import com.example.ui.theme.IslandColors
import com.example.ui.theme.LocalAppBackdrop
import com.example.ui.theme.MainDockTab
import com.example.ui.theme.MotionTokens
import com.example.ui.theme.ObsidianCrimsonBackground
import com.example.ui.theme.ObsidianGlassCard
import com.example.ui.theme.islandGlassChrome

@Composable
fun HomeHubScreen(
    app: DynamicIslandApp,
    onSelectDockTab: (MainDockTab) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWizard: () -> Unit,
    onOpenCalibration: () -> Unit,
    onOpenFeaturePicker: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    val context = LocalContext.current
    val settings by app.settingsRepository.settingsFlow.collectAsState(initial = IslandSettings())
    val islandState by app.islandController.state.collectAsState()
    val isServiceRunning by IslandService.isRunning.collectAsState()
    val isAccHosting by IslandService.isHostingOverlay.collectAsState()
    val isFallbackHosting by IslandKeepAliveService.isFallbackHosting.collectAsState()
    val mediaState by app.mediaHub.state.collectAsState()
    val timerState by app.timerEngine.state.collectAsState()
    val alertsCount = islandState.notificationsHistory.size + islandState.alertsHistory.size

    val overlayActive = isAccHosting || isFallbackHosting || isServiceRunning
    val accentColor = IslandColors.resolveAccentColor(context, settings.accentTheme)
    val musicAccent = mediaState.artworkAccentArgb?.let { Color(it) } ?: accentColor
    val effectivePillStyle = if (settings.solidFallback) PillStyle.BLACK else settings.pillStyle
    val useGlass = effectivePillStyle != PillStyle.BLACK
    val effectiveStyle = if (settings.reducedMotion) AnimationStyle.SMOOTH else settings.animationStyle
    val outerShape = RoundedCornerShape(IslandColors.ExpandedOuterRadius)
    val metrics = context.resources.displayMetrics

    ObsidianCrimsonBackground(showHorizonPlanet = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
        ) {
            // Mockup Header Bar (App squircle emblem + "Dynamic Island" + circular gear button)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(IslandColors.CrimsonPrimary.copy(alpha = 0.35f), Color(0xFF1A070B))
                            )
                        )
                        .border(1.dp, IslandColors.CrimsonBorder, RoundedCornerShape(13.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(11.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .border(1.8.dp, IslandColors.CrimsonBright, CircleShape)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dynamic Island",
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Smart · Elegant · Powerful",
                        color = IslandColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xBB191114))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                        .testTag("open_settings_button")
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Open Settings", tint = Color.White)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Live Interactive Island Sandbox (Top of scroll viewport so chooser_tab_* & panel controls are immediately visible)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = DeadZoneGeometry.FULL_MAX_WIDTH_DP.dp)
                        .heightIn(max = DeadZoneGeometry.FULL_MAX_HEIGHT_DP.dp)
                        .align(Alignment.CenterHorizontally)
                        .islandGlassChrome(
                            shape = outerShape,
                            pillStyle = effectivePillStyle,
                            glassDensity = settings.glassDensity,
                            reduceGlass = settings.reduceGlass,
                            solidFallback = settings.solidFallback,
                            accentColor = accentColor,
                            backdrop = LocalAppBackdrop.current
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF24262E))
                                        .border(1.dp, accentColor, CircleShape)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Live Island · ${islandState.activeTab.title}",
                                    color = IslandColors.TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            val dotPulse = if (overlayActive) 1.0f else 0.72f
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background((if (overlayActive) IslandColors.AccentGreen else accentColor).copy(alpha = 0.16f))
                                    .padding(horizontal = 9.dp, vertical = 3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .graphicsLayer { alpha = dotPulse }
                                        .clip(CircleShape)
                                        .background(if (overlayActive) IslandColors.AccentGreen else accentColor)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = if (overlayActive) "Overlay ON" else "Preview",
                                    color = if (overlayActive) IslandColors.AccentGreen else accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .heightIn(min = 150.dp, max = 188.dp)
                        ) {
                            AnimatedContent(
                                targetState = islandState.activeTab,
                                transitionSpec = {
                                    if (effectiveStyle == AnimationStyle.NONE) {
                                        fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                                    } else {
                                        MotionTokens.contentEnterTransition(effectiveStyle) togetherWith
                                            MotionTokens.contentExitTransition(effectiveStyle)
                                    }
                                },
                                label = "sandbox_tab"
                            ) { tab ->
                                when (tab) {
                                    IslandTab.MUSIC -> MusicExpandedView(
                                        state = mediaState,
                                        accentColor = musicAccent,
                                        onTogglePlayPause = {
                                            if (mediaState.isPlaying) app.soundManager.playMusicPause() else app.soundManager.playMusicPlay()
                                            app.mediaHub.togglePlayPause()
                                        },
                                        onPrevious = {
                                            app.soundManager.playMusicNext()
                                            app.mediaHub.skipPrevious()
                                        },
                                        onNext = {
                                            app.soundManager.playMusicNext()
                                            app.mediaHub.skipNext()
                                        },
                                        onSeekTo = { app.mediaHub.seekTo(it) },
                                        onToggleShuffle = {
                                            app.soundManager.playToggle(!mediaState.isShuffled)
                                            app.mediaHub.sendShuffleCommand()
                                        },
                                        onCycleRepeat = {
                                            app.soundManager.playTap()
                                            app.mediaHub.sendRepeatCommand()
                                        },
                                        onToggleLike = {
                                            app.soundManager.playToggle(!mediaState.isLiked)
                                            app.mediaHub.sendLikeCommand()
                                        },
                                        onOpenPlayer = {
                                            app.soundManager.playTap()
                                            app.mediaHub.openPlayerApp()
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
                                        onTimerSetSound = { app.soundManager.playTimerSet() },
                                        onStartSleepTimer = { durationMs -> app.timerEngine.startSleepTimer(durationMs) }
                                    )
                                    IslandTab.SEARCH -> SearchIslandPanel(app = app, accentColor = accentColor, onCollapse = {})
                                    IslandTab.TOOLS -> ToolsIslandPanel(app = app, settings = settings, accentColor = accentColor, onHaptic = {}, onCollapse = {})
                                    IslandTab.VAULT -> VaultIslandPanel(app = app, accentColor = accentColor, onCollapse = {})
                                    IslandTab.CLIPS -> ClipsIslandPanel(app = app, accentColor = accentColor)
                                    IslandTab.NOTES -> NotesIslandPanel(app = app, accentColor = accentColor)
                                    IslandTab.MEMO -> MemoIslandPanel(app = app, accentColor = accentColor)
                                    IslandTab.ALERTS -> AlertsIslandPanel(app = app, state = islandState, accentColor = accentColor)
                                }
                            }
                        }

                        Spacer(Modifier.height(2.dp))

                        BottomChooserBar(
                            settings = settings,
                            activeTab = islandState.activeTab,
                            isMoreOpen = islandState.isMoreRowOpen,
                            accentColor = accentColor,
                            useLightGlass = useGlass,
                            onSelectTab = { app.islandController.selectTab(it) },
                            onToggleMore = { app.islandController.toggleMoreRow() }
                        )
                    }
                }

                // Active / Inactive Overlay Status Card matching youtubers742-home-hub.png
                ObsidianGlassCard(
                    highlighted = !overlayActive,
                    onClick = onOpenWizard
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            (if (overlayActive) IslandColors.AccentGreen else IslandColors.CrimsonBright).copy(alpha = 0.36f),
                                            Color(0xFF0E1310)
                                        )
                                    )
                                )
                                .border(
                                    1.2.dp,
                                    if (overlayActive) IslandColors.AccentGreen else IslandColors.CrimsonBright,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (overlayActive) Icons.Filled.Check else Icons.Filled.Warning,
                                contentDescription = null,
                                tint = if (overlayActive) IslandColors.AccentGreen else IslandColors.CrimsonBright
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = buildAnnotatedString {
                                    append("Dynamic Island is ")
                                    withStyle(
                                        SpanStyle(
                                            color = if (overlayActive) IslandColors.AccentGreen else IslandColors.CrimsonBright,
                                            fontWeight = FontWeight.Bold
                                        )
                                    ) {
                                        append(if (overlayActive) "Active" else "Inactive")
                                    }
                                },
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (overlayActive) {
                                    when {
                                        isAccHosting -> "Accessibility overlay live on top"
                                        isFallbackHosting -> "Window overlay fallback live on top"
                                        else -> "Ready to enhance your experience"
                                    }
                                } else {
                                    "Tap here to grant Accessibility & Overlay access"
                                },
                                color = IslandColors.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = IslandColors.TextSecondary)
                    }
                }

                if (settings.accessibilityWasEnabled && !isServiceRunning) {
                    ObsidianGlassCard(highlighted = true) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = IslandColors.CrimsonBright)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Accessibility was switched off", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("MIUI stopped Accessibility. Re-enable for lock-screen & OCR.", color = IslandColors.TextSecondary, fontSize = 11.sp)
                            }
                            Button(
                                onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } },
                                colors = ButtonDefaults.buttonColors(containerColor = IslandColors.CrimsonPrimary),
                                shape = CircleShape
                            ) { Text("Enable", fontSize = 12.sp) }
                        }
                    }
                }

                // Quick Access 3-column Bento Card matching youtubers742-home-hub.png
                ObsidianGlassCard(cornerRadius = 24.dp, contentPadding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CrimsonSquircleIcon(icon = Icons.Filled.Home, size = 38.dp, iconSize = 20.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Quick Access", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Open features instantly", color = IslandColors.TextSecondary, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        BentoQuickTile("Notes", Icons.Filled.Description, onClick = { onSelectDockTab(MainDockTab.NOTES) }, modifier = Modifier.weight(1f))
                        BentoQuickTile(
                            title = "Alerts",
                            icon = Icons.Filled.Notifications,
                            badgeCount = alertsCount,
                            onClick = { onSelectDockTab(MainDockTab.ALERTS) },
                            modifier = Modifier.weight(1f)
                        )
                        BentoQuickTile("Shelf", Icons.Filled.Layers, onClick = { onSelectDockTab(MainDockTab.SHELF) }, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        BentoQuickTile("Tools", Icons.Filled.Build, onClick = { onSelectDockTab(MainDockTab.TOOLS) }, modifier = Modifier.weight(1f))
                        BentoQuickTile("Music", Icons.Filled.MusicNote, onClick = { onSelectDockTab(MainDockTab.MUSIC) }, modifier = Modifier.weight(1f))
                        BentoQuickTile("Timer", Icons.Filled.Timer, onClick = { onSelectDockTab(MainDockTab.TIMER) }, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        BentoQuickTile("Search /\nApp Opener", Icons.Filled.Search, onClick = { onSelectDockTab(MainDockTab.SEARCH) }, modifier = Modifier.weight(1f))
                        ObsidianGlassCard(
                            highlighted = true,
                            cornerRadius = 18.dp,
                            contentPadding = PaddingValues(14.dp),
                            onClick = onOpenFeaturePicker,
                            modifier = Modifier
                                .weight(2.08f)
                                .height(108.dp)
                                .testTag("open_feature_picker_button")
                        ) {
                            Column(
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CrimsonSquircleIcon(icon = Icons.Filled.DashboardCustomize, size = 30.dp, iconSize = 16.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Customise Island", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "${settings.enabledTabs.size} / 8 dock slots active",
                                        color = IslandColors.TextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color(0xFF470B13))
                                            .border(1.dp, IslandColors.CrimsonBorderStrong, CircleShape)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Reorder >", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Setup Wizard + Calibration + Alerts History utility row (preserves all existing testTags)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ObsidianGlassCard(
                        cornerRadius = 18.dp,
                        onClick = onOpenWizard,
                        modifier = Modifier.weight(1f).testTag("open_wizard_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CrimsonSquircleIcon(icon = Icons.Filled.RocketLaunch, size = 34.dp, iconSize = 17.dp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Setup Wizard", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                                Text(
                                    if (settings.wizardCompleted) "Checklist ready" else "Required",
                                    color = IslandColors.TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    ObsidianGlassCard(
                        cornerRadius = 18.dp,
                        onClick = onOpenCalibration,
                        modifier = Modifier.weight(1f).testTag("open_calibration_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CrimsonSquircleIcon(icon = Icons.Filled.CenterFocusStrong, size = 34.dp, iconSize = 17.dp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Calibration", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                                Text("Align cutout", color = IslandColors.TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }

                ObsidianGlassCard(
                    cornerRadius = 18.dp,
                    onClick = onOpenNotifications,
                    modifier = Modifier.fillMaxWidth().testTag("open_notifications_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        CrimsonSquircleIcon(icon = Icons.Filled.Notifications, size = 34.dp, iconSize = 17.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Notification Inbox & App Rules", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Manage per-app island previews", color = IslandColors.TextSecondary, fontSize = 11.sp)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = IslandColors.TextSecondary)
                    }
                }

                // Real System Status Telemetry Card matching youtubers742-home-hub.png
                ObsidianGlassCard(cornerRadius = 22.dp, contentPadding = PaddingValues(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CrimsonSquircleIcon(icon = Icons.Filled.Memory, size = 34.dp, iconSize = 17.dp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("System Status", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Live device telemetry", color = IslandColors.TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(IslandColors.AccentGreen.copy(alpha = 0.16f))
                                .border(1.dp, IslandColors.AccentGreen.copy(alpha = 0.45f), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(IslandColors.AccentGreen))
                            Spacer(Modifier.width(5.dp))
                            Text(if (overlayActive) "Active" else "Standby", color = IslandColors.AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        StatusCell(
                            icon = Icons.Filled.Memory,
                            primary = "Android ${Build.VERSION.RELEASE}",
                            secondary = Build.MODEL.take(16),
                            modifier = Modifier.weight(1f)
                        )
                        StatusCell(
                            icon = Icons.Filled.PhoneAndroid,
                            primary = "${metrics.widthPixels} × ${metrics.heightPixels}",
                            secondary = "Resolution",
                            modifier = Modifier.weight(1f)
                        )
                        StatusCell(
                            icon = Icons.Filled.BatteryChargingFull,
                            primary = "${islandState.batteryLevelPercent}%",
                            secondary = if (islandState.isBatteryCharging) "Charging" else "Battery",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Live Diagnostics & Quick Pill Test Card
                val overlayPermissionOk = Settings.canDrawOverlays(context)
                val diag = islandState.diagnostics
                val crossBlurSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    runCatching {
                        val wm = context.getSystemService(android.view.WindowManager::class.java)
                        wm?.isCrossWindowBlurEnabled == true
                    }.getOrDefault(false)
                } else {
                    false
                }
                ObsidianGlassCard(cornerRadius = 22.dp, contentPadding = PaddingValues(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Live Diagnostics", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "Overlay: ${if (overlayPermissionOk) "OK" else "Off"} · Accessibility: ${if (isAccHosting) "Live" else if (isServiceRunning) "On" else "Off"}",
                                color = IslandColors.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = {
                                app.soundManager.playNotification()
                                app.islandController.upsertActivity(
                                    com.example.island.IslandActivity.NotificationPreview(
                                        id = "test_diag_${System.currentTimeMillis()}",
                                        packageName = context.packageName,
                                        appName = "Dynamic Island",
                                        title = "Preview Alert",
                                        text = "Liquid glass capsule & cutout alignment check"
                                    ),
                                    autoShowCompact = true,
                                    autoDismissMs = settings.alertsDwellMs
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp).testTag("home_test_pill_btn")
                        ) {
                            Text("Test Pill", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Cross-Window Blur: ${if (crossBlurSupported) "Supported" else "Layer Fallback"} · Collisions: ${diag.deadZoneCollisionCount}",
                        color = IslandColors.TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        "Last Notif: ${diag.lastNotificationApp} · Suppressed: ${diag.lastSuppressionReason}",
                        color = IslandColors.TextTertiary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun BentoQuickTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0
) {
    Box(modifier = modifier.height(108.dp)) {
        ObsidianGlassCard(
            cornerRadius = 18.dp,
            contentPadding = PaddingValues(10.dp),
            onClick = onClick,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                CrimsonCircularHaloIcon(icon = icon, size = 46.dp, iconSize = 22.dp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
            }
        }
        if (badgeCount > 0) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(IslandColors.CrimsonBright)
            ) {
                Text(
                    text = if (badgeCount > 99) "99+" else "$badgeCount",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StatusCell(
    icon: ImageVector,
    primary: String,
    secondary: String,
    modifier: Modifier = Modifier
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Icon(icon, contentDescription = null, tint = IslandColors.TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(primary, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(secondary, color = IslandColors.TextTertiary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
