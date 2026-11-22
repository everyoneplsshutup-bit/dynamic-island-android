package com.example.island

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DynamicIslandApp
import com.example.system.IslandSettings
import com.example.ui.theme.IslandColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(
    app: DynamicIslandApp,
    onBack: () -> Unit
) {
    val settings by app.settingsRepository.settingsFlow.collectAsState(initial = IslandSettings())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current

    var previewExpanded by remember { mutableStateOf(true) }
    var worstCaseFixture by remember { mutableIntStateOf(0) } // 0=Notification, 1=Music, 2=Charging

    val liveZone = remember(
        settings.cutoutXOffsetDp,
        settings.cutoutYOffsetDp,
        settings.cutoutWidthDp,
        settings.cutoutHeightDp,
        view.width,
        view.height
    ) {
        DeadZoneGeometry.measureLive(context, settings)
    }

    val previewZone = remember(settings, liveZone) {
        DeadZoneGeometry.fromSyntheticHole(
            holeWidthDp = settings.cutoutWidthDp.coerceAtLeast(liveZone.holeWidthDp),
            holeHeightDp = settings.cutoutHeightDp.coerceAtLeast(liveZone.holeHeightDp),
            holeTopDp = settings.cutoutYOffsetDp
        )
    }

    val previewState = remember(worstCaseFixture) {
        val sampleActivity = when (worstCaseFixture) {
            1 -> IslandActivity.Music(
                packageName = "com.unshoo.pixelmusic",
                title = "Extremely Long Track Name To Verify 8dp Dead Zone Safety",
                artist = "Artist Name",
                isPlaying = true,
                positionMs = 42_000L,
                durationMs = 210_000L
            )
            2 -> IslandActivity.Charging(
                levelPercent = 100,
                isCharging = true,
                isFull = true,
                wattageWatts = 99f
            )
            else -> IslandActivity.NotificationPreview(
                id = "calib_worst_case",
                packageName = "com.android.systemui",
                appName = "System Photos Screenshot",
                title = "Saved to Photos — super long notification title that must stay clear",
                text = "Saved to Photos"
            )
        }
        IslandState(
            mode = IslandMode.COMPACT,
            activityQueue = listOf(sampleActivity),
            isCalibrationPreview = true
        )
    }

    DisposableEffect(Unit) {
        app.islandController.setCalibrationPreview(true)
        onDispose { app.islandController.setCalibrationPreview(false) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Camera Calibration") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("calibration_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .widthIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Red Calibrate Mode Preview Box with dashed exclusion zone & live CompactPillView
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0A0A0E),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .border(1.dp, IslandColors.AccentRed.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    // Crosshair + Dashed Red >=8dp Exclusion Zone Rect around punch-hole
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f + settings.cutoutXOffsetDp.dp.toPx()
                        val topY = settings.cutoutYOffsetDp.dp.toPx()
                        val holeW = previewZone.holeWidthDp.dp.toPx()
                        val holeH = previewZone.holeHeightDp.dp.toPx()
                        val marginPx = DeadZoneGeometry.REQUIRED_MARGIN_DP.dp.toPx()

                        drawLine(
                            color = IslandColors.AccentRed.copy(alpha = 0.24f),
                            start = Offset(cx, 0f),
                            end = Offset(cx, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                        // Cutout outline
                        drawRoundRect(
                            color = IslandColors.AccentRed.copy(alpha = 0.9f),
                            topLeft = Offset(cx - holeW / 2f, topY),
                            size = Size(holeW, holeH),
                            cornerRadius = CornerRadius(settings.cutoutCornerRadiusDp.dp.toPx()),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        // Exclusion zone outline (+8 dp on all sides)
                        drawRoundRect(
                            color = IslandColors.AccentRed.copy(alpha = 0.72f),
                            topLeft = Offset(cx - holeW / 2f - marginPx, (topY - marginPx).coerceAtLeast(0f)),
                            size = Size(holeW + marginPx * 2f, holeH + marginPx * 2f),
                            cornerRadius = CornerRadius((settings.cutoutCornerRadiusDp + 6f).dp.toPx()),
                            style = Stroke(
                                width = 1.4.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                            )
                        )
                    }

                    Box(
                        modifier = Modifier.offset(
                            x = settings.cutoutXOffsetDp.dp,
                            y = settings.cutoutYOffsetDp.dp
                        )
                    ) {
                        if (!previewExpanded) {
                            IdlePillView(
                                settings = settings,
                                state = previewState,
                                exclusionZone = previewZone,
                                accentColor = IslandColors.AccentRed,
                                isCalibrationPreview = true,
                                isDragOverIsland = false,
                                isLowBatteryWarning = false
                            )
                        } else {
                            CompactPillView(
                                state = previewState,
                                settings = settings,
                                exclusionZone = previewZone,
                                accentColor = IslandColors.AccentOrange
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Red Dashed = 8dp Exclusion · ${previewZone.holeWidthDp.toInt()}×${previewZone.holeHeightDp.toInt()}dp",
                            color = IslandColors.AccentRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Button(
                            onClick = { previewExpanded = !previewExpanded },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (previewExpanded) IslandColors.AccentRed else Color.White.copy(alpha = 0.14f),
                                contentColor = Color.White
                            ),
                            shape = CircleShape,
                            modifier = Modifier.height(32.dp).testTag("calibration_preview_expand")
                        ) {
                            Icon(Icons.Filled.OpenInFull, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (previewExpanded) "Idle Ring" else "Live Pill", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Worst-case fixture selector + Test Alert button
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Saved to Photos", "60-char Music", "100% · 99W").forEachIndexed { idx, label ->
                    FilterChip(
                        selected = worstCaseFixture == idx,
                        onClick = { worstCaseFixture = idx },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }

            // Cutout & Exclusion Rect telemetry readout (px + dp)
            Surface(
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (liveZone.detectedFromSystem) {
                            "Detected Cutout: ${liveZone.cutoutRectPx.width()}×${liveZone.cutoutRectPx.height()} px (${liveZone.cutoutRectDp.width.toInt()}×${liveZone.cutoutRectDp.height.toInt()} dp)"
                        } else {
                            "Configured Cutout: ${liveZone.cutoutRectPx.width()}×${liveZone.cutoutRectPx.height()} px (${liveZone.cutoutRectDp.width.toInt()}×${liveZone.cutoutRectDp.height.toInt()} dp)"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Safe Inset Top: ${liveZone.safeInsetTopPx} px (${liveZone.safeInsetTopDp.toInt()} dp) · Exclusion (+8dp): ${liveZone.exclusionRectPx.width()}×${liveZone.exclusionRectPx.height()} px (${liveZone.exclusionRectDp.width.toInt()}×${liveZone.exclusionRectDp.height.toInt()} dp)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val measured = DeadZoneGeometry.measureLive(context, IslandSettings())
                        val wDp = measured.cutoutRectDp.width.coerceIn(14f, 80f)
                        val hDp = measured.cutoutRectDp.height.coerceIn(14f, 60f)
                        val topDp = measured.cutoutRectDp.top.coerceIn(0f, 36f)
                        previewExpanded = true
                        scope.launch {
                            app.settingsRepository.update {
                                it.copy(
                                    cutoutXOffsetDp = 0f,
                                    cutoutYOffsetDp = topDp,
                                    cutoutWidthDp = wDp,
                                    cutoutHeightDp = hDp,
                                    calibrationConfirmed = false
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("calibration_autodetect")
                ) {
                    Icon(Icons.Filled.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Auto-Detect", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            // Redmi Note 10 Pro centered punch-hole baseline (~26x33 dp, 74x93 px)
                            app.settingsRepository.update {
                                it.copy(
                                    cutoutXOffsetDp = 0f,
                                    cutoutYOffsetDp = 6f,
                                    cutoutWidthDp = 26f,
                                    cutoutHeightDp = 33f,
                                    cutoutCornerRadiusDp = 16f
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("calibration_reset")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reset 26×33", fontSize = 12.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        app.soundManager.playTap()
                        app.islandController.upsertActivity(
                            IslandActivity.NotificationPreview(
                                id = "calib_test_alert_${System.currentTimeMillis()}",
                                packageName = context.packageName,
                                appName = "System Photos Screenshot Alert",
                                title = "Saved to Photos — dead-zone clearance test alert",
                                text = "This longer message stays strictly in Expanded/Alerts view"
                            ),
                            autoShowCompact = true,
                            autoDismissMs = settings.alertsDwellMs
                        )
                    },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Test Alert", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        app.soundManager.playToggle(true)
                        scope.launch {
                            app.settingsRepository.update { it.copy(calibrationConfirmed = true) }
                        }
                    },
                    modifier = Modifier.weight(1.25f).height(48.dp).testTag("calibration_confirm_btn")
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (settings.calibrationConfirmed) "Confirmed" else "Text Clears Camera", fontSize = 12.sp)
                }
            }

            NudgeSliderRow(
                label = "Horizontal X Offset",
                value = settings.cutoutXOffsetDp,
                range = -40f..40f,
                unit = "dp"
            ) { v -> scope.launch { app.settingsRepository.update { it.copy(cutoutXOffsetDp = v) } } }

            NudgeSliderRow(
                label = "Vertical Y Offset",
                value = settings.cutoutYOffsetDp,
                range = 0f..36f,
                unit = "dp"
            ) { v -> scope.launch { app.settingsRepository.update { it.copy(cutoutYOffsetDp = v) } } }

            NudgeSliderRow(
                label = "Cutout Clearance Width",
                value = settings.cutoutWidthDp,
                range = 14f..80f,
                unit = "dp"
            ) { v -> scope.launch { app.settingsRepository.update { it.copy(cutoutWidthDp = v) } } }

            NudgeSliderRow(
                label = "Cutout Clearance Height",
                value = settings.cutoutHeightDp,
                range = 14f..60f,
                unit = "dp"
            ) { v -> scope.launch { app.settingsRepository.update { it.copy(cutoutHeightDp = v) } } }

            NudgeSliderRow(
                label = "Corner Radius",
                value = settings.cutoutCornerRadiusDp,
                range = 6f..28f,
                unit = "dp"
            ) { v -> scope.launch { app.settingsRepository.update { it.copy(cutoutCornerRadiusDp = v) } } }
        }
    }
}

@Composable
private fun NudgeSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontWeight = FontWeight.Medium)
            Text("${value.toInt()} $unit", color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range
        )
    }
}
