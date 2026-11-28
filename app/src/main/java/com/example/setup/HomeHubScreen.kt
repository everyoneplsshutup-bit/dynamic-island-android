package com.example.setup

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MonitorHeart
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DynamicIslandApp
import com.example.island.AlertsIslandPanel
import com.example.island.BottomChooserBar
import com.example.island.ClipsIslandPanel
import com.example.island.DeadZoneGeometry
import com.example.island.IslandActivity
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

    val liveBatteryPercent = remember(islandState.batteryLevelPercent) {
        if (islandState.batteryLevelPercent > 0) {
            islandState.batteryLevelPercent
        } else {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0, 100) ?: 100
        }
    }

    ObsidianCrimsonBackground(showHorizonPlanet = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            // Mockup Header Bar (App squircle emblem + "Dynamic Island" + circular gear button)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF23252D),
                                    Color(0xFF14151A)
                                )
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            .height(12.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .border(2.2.dp, accentColor, CircleShape)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dynamic Island",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "Smart · Elegant · Powerful",
                        color = IslandColors.TextSecondary,
                        fontSize = 12.5.sp,
                        maxLines = 1
                    )
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xD41B1D23))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                        .testTag("open_settings_button")
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Open Settings", tint = Color.White)
                }
            }

            // Open atmospheric planetary horizon gap matching youtubers742-home-hub.png
            Spacer(Modifier.height(78.dp))

            // 1. Active / Inactive Status Banner Card
            ObsidianGlassCard(
                cornerRadius = 22.dp,
                highlighted = !overlayActive,
                accentColor = if (overlayActive) IslandColors.AccentGreen else IslandColors.AccentOrange,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                onClick = onOpenWizard
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    val statusTint = if (overlayActive) IslandColors.AccentGreen else IslandColors.AccentOrange
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        statusTint.copy(alpha = 0.34f),
                                        Color(0xFF191C22)
                                    )
                                )
                            )
                            .border(1.3.dp, statusTint, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (overlayActive) Icons.Filled.Check else Icons.Filled.Warning,
                            contentDescription = if (overlayActive) "Active" else "Inactive",
                            tint = statusTint
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = buildAnnotatedString {
                                append("Dynamic Island is ")
                                withStyle(SpanStyle(color = statusTint, fontWeight = FontWeight.Bold)) {
                                    append(if (overlayActive) "Active" else "Inactive")
                                }
                            },
                            color = Color.White,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (overlayActive) {
                                "Ready to enhance your experience!"
                            } else {
                                "Tap to grant Accessibility & Overlay access"
                            },
                            color = IslandColors.TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = IslandColors.TextSecondary
                    )
                }
            }

            if (settings.accessibilityWasEnabled && !isServiceRunning) {
                Spacer(Modifier.height(10.dp))
                ObsidianGlassCard(
                    cornerRadius = 20.dp,
                    highlighted = true,
                    accentColor = IslandColors.AccentOrange,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = IslandColors.AccentOrange)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Accessibility was switched off", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            Text("MIUI stopped Accessibility. Re-enable for lock-screen & OCR.", color = IslandColors.TextSecondary, fontSize = 11.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) { Text("Enable", fontSize = 12.sp) }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 2. Quick Access Bento Card matching youtubers742-home-hub.png (Customise Island replaces Go Premium)
            ObsidianGlassCard(cornerRadius = 26.dp, contentPadding = PaddingValues(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CrimsonSquircleIcon(icon = Icons.Filled.Home, size = 40.dp, iconSize = 21.dp, tint = accentColor)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Quick Access", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.5.sp)
                        Text("Open features instantly", color = IslandColors.TextSecondary, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    HomeQuickTile(
                        title = "Notes",
                        icon = Icons.Filled.Description,
                        onClick = { onSelectDockTab(MainDockTab.NOTES) },
                        modifier = Modifier.weight(1f)
                    )
                    HomeQuickTile(
                        title = "Alerts",
                        icon = Icons.Filled.Notifications,
                        badgeCount = alertsCount,
                        onClick = { onSelectDockTab(MainDockTab.ALERTS) },
                        modifier = Modifier.weight(1f)
                    )
                    HomeQuickTile(
                        title = "Shelf",
                        icon = Icons.Filled.Layers,
                        onClick = { onSelectDockTab(MainDockTab.SHELF) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    HomeQuickTile(
                        title = "Tools",
                        icon = Icons.Filled.Build,
                        onClick = { onSelectDockTab(MainDockTab.TOOLS) },
                        modifier = Modifier.weight(1f)
                    )
                    HomeQuickTile(
                        title = "Music",
                        icon = Icons.Filled.MusicNote,
                        onClick = { onSelectDockTab(MainDockTab.MUSIC) },
                        modifier = Modifier.weight(1f)
                    )
                    HomeQuickTile(
                        title = "Timer",
                        icon = Icons.Filled.Timer,
                        onClick = { onSelectDockTab(MainDockTab.TIMER) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    HomeQuickTile(
                        title = "Search /\nApp Opener",
                        icon = Icons.Filled.Search,
                        onClick = { onSelectDockTab(MainDockTab.SEARCH) },
                        modifier = Modifier.weight(1f)
                    )
                    ObsidianGlassCard(
                        highlighted = true,
                        accentColor = accentColor,
                        cornerRadius = 18.dp,
                        contentPadding = PaddingValues(14.dp),
                        onClick = onOpenFeaturePicker,
                        modifier = Modifier
                            .weight(2.08f)
                            .height(106.dp)
                            .testTag("open_feature_picker_button")
                    ) {
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CrimsonSquircleIcon(
                                    icon = Icons.Filled.DashboardCustomize,
                                    size = 32.dp,
                                    iconSize = 17.dp,
                                    tint = accentColor
                                )
                                Spacer(Modifier.width(9.dp))
                                Text(
                                    text = "Customise Island",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${settings.enabledTabs.size} active\nin island dock",
                                    color = IslandColors.TextSecondary,
                                    fontSize = 11.5.sp,
                                    lineHeight = 14.sp
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.22f))
                                        .border(1.dp, accentColor.copy(alpha = 0.72f), CircleShape)
                                        .padding(horizontal = 11.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Customise",
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 3. System Status Card matching youtubers742-home-hub.png
            ObsidianGlassCard(cornerRadius = 24.dp, contentPadding = PaddingValues(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.MonitorHeart,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("System Status", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
                            Text("Everything is running smoothly", color = IslandColors.TextSecondary, fontSize = 11.5.sp)
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(IslandColors.AccentGreen.copy(alpha = 0.16f))
                            .border(1.dp, IslandColors.AccentGreen.copy(alpha = 0.48f), CircleShape)
                            .padding(horizontal = 11.dp, vertical = 5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(IslandColors.AccentGreen)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (overlayActive) "Online" else "Standby",
                            color = IslandColors.AccentGreen,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TelemetryColumn(
                        icon = Icons.Filled.Memory,
                        primary = "Android ${Build.VERSION.RELEASE}",
                        secondary = Build.MODEL.take(16),
                        modifier = Modifier.weight(1f)
                    )
                    Box(Modifier.width(1.dp).height(42.dp).background(Color.White.copy(alpha = 0.08f)))
                    TelemetryColumn(
                        icon = Icons.Filled.PhoneAndroid,
                        primary = "${metrics.widthPixels} × ${metrics.heightPixels}",
                        secondary = "Resolution",
                        modifier = Modifier.weight(1f)
                    )
                    Box(Modifier.width(1.dp).height(42.dp).background(Color.White.copy(alpha = 0.08f)))
                    TelemetryColumn(
                        icon = if (islandState.isBatteryCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryStd,
                        primary = "${liveBatteryPercent}%",
                        secondary = if (islandState.isBatteryCharging) "Charging" else "Battery",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // 4. Below-the-fold Setup, Calibration, Notification Inbox & Interactive Island Sandbox
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ObsidianGlassCard(
                    cornerRadius = 18.dp,
                    contentPadding = PaddingValues(14.dp),
                    onClick = onOpenWizard,
                    modifier = Modifier.weight(1f).testTag("open_wizard_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CrimsonSquircleIcon(icon = Icons.Filled.RocketLaunch, size = 34.dp, iconSize = 17.dp, tint = accentColor)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Setup Wizard", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                            Text(
                                text = if (settings.wizardCompleted) "Checklist ready" else "Required",
                                color = IslandColors.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                ObsidianGlassCard(
                    cornerRadius = 18.dp,
                    contentPadding = PaddingValues(14.dp),
                    onClick = onOpenCalibration,
                    modifier = Modifier.weight(1f).testTag("open_calibration_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CrimsonSquircleIcon(icon = Icons.Filled.CenterFocusStrong, size = 34.dp, iconSize = 17.dp, tint = accentColor)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Calibration", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                            Text("Align cutout", color = IslandColors.TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            ObsidianGlassCard(
                cornerRadius = 18.dp,
                contentPadding = PaddingValues(14.dp),
                onClick = onOpenNotifications,
                modifier = Modifier.fillMaxWidth().testTag("open_notifications_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    CrimsonSquircleIcon(icon = Icons.Filled.Notifications, size = 34.dp, iconSize = 17.dp, tint = accentColor)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Notification Inbox & App Rules", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                        Text("Manage per-app island previews", color = IslandColors.TextSecondary, fontSize = 11.5.sp)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = IslandColors.TextSecondary)
                }
            }

            Spacer(Modifier.height(14.dp))

            // Interactive Live Island Sandbox (below fold so first screen matches mockup cleanly)
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
                        .padding(top = 8.dp, bottom = 6.dp),
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
                                text = "Live Island Sandbox · ${islandState.activeTab.title}",
                                color = IslandColors.TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Button(
                            onClick = {
                                app.soundManager.playNotification()
                                app.islandController.upsertActivity(
                                    IslandActivity.NotificationPreview(
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
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp).testTag("home_test_pill_btn")
                        ) {
                            Text("Test Pill", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
                                    rotateAlbumArt = settings.rotateAlbumArt,
                                    expandedMarqueeEnabled = settings.expandedLyricsMarquee,
                                    lyricsOffsetMs = settings.lyricsOffsetMs,
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
                                    },
                                    onOpenLyricsSettings = onOpenSettings
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
                                IslandTab.TOOLS -> ToolsIslandPanel(
                                    app = app,
                                    settings = settings,
                                    accentColor = accentColor,
                                    isScreenRecording = islandState.activityQueue.any { it is IslandActivity.ScreenRecording },
                                    onHaptic = {},
                                    onCollapse = {}
                                )
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
        }
    }
}

@Composable
private fun HomeQuickTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0
) {
    val badgeColor = MaterialTheme.colorScheme.primary
    Box(modifier = modifier.height(106.dp)) {
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
                    textAlign = TextAlign.Center,
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
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            ) {
                Text(
                    text = if (badgeCount > 99) "99+" else "$badgeCount",
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TelemetryColumn(
    icon: ImageVector,
    primary: String,
    secondary: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = IslandColors.TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(5.dp))
        Text(
            text = primary,
            color = Color.White,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = secondary,
            color = IslandColors.TextTertiary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
