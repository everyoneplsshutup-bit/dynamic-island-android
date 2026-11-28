package com.example.island

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DynamicIslandApp
import com.example.system.IslandSettings
import com.example.ui.theme.CrimsonPillButton
import com.example.ui.theme.CrimsonSquircleIcon
import com.example.ui.theme.IslandColors
import com.example.ui.theme.ObsidianCrimsonBackground
import com.example.ui.theme.ObsidianGlassCard
import com.example.ui.theme.ObsidianPillButton
import kotlinx.coroutines.launch

@Composable
fun CalibrationScreen(
    app: DynamicIslandApp,
    onBack: () -> Unit
) {
    val settings by app.settingsRepository.settingsFlow.collectAsState(initial = IslandSettings())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current
    val accent = MaterialTheme.colorScheme.primary

    var previewExpanded by remember { mutableStateOf(true) }
    var worstCaseFixture by remember { mutableIntStateOf(0) }

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

    val alignmentScore = remember(settings, liveZone) {
        val xPenalty = (kotlin.math.abs(settings.cutoutXOffsetDp) * 0.8f)
        val yPenalty = kotlin.math.abs(settings.cutoutYOffsetDp - liveZone.cutoutRectDp.top) * 0.6f
        (100 - (xPenalty + yPenalty).toInt()).coerceIn(68, 100)
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

    ObsidianCrimsonBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
        ) {
            // Centered Header matching everyoneplsshutup-calibration-v2.png (<- | Calibration | spacer)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xD41B1D23))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                        .testTag("calibration_back")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Calibration", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Measure your camera cutout for perfect fit",
                        color = IslandColors.TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.size(46.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Full-bleed Radar Crosshair Preview matching everyoneplsshutup-calibration-v2.png (real measurements)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f + settings.cutoutXOffsetDp.dp.toPx()
                        val topY = (settings.cutoutYOffsetDp + 18f).dp.toPx()
                        val holeW = previewZone.holeWidthDp.dp.toPx()
                        val holeH = previewZone.holeHeightDp.dp.toPx()
                        val holeCy = topY + holeH / 2f
                        val marginPx = DeadZoneGeometry.REQUIRED_MARGIN_DP.dp.toPx()
                        val centerPt = Offset(cx, holeCy)

                        // Concentric glowing radar circles
                        listOf(1.1f, 1.85f, 2.75f).forEachIndexed { idx, scale ->
                            val rad = (maxOf(holeW, holeH) * scale).coerceAtLeast(26f)
                            drawCircle(
                                color = accent.copy(alpha = (0.36f - idx * 0.10f).coerceAtLeast(0.08f)),
                                radius = rad,
                                center = centerPt,
                                style = Stroke(width = 1.4.dp.toPx())
                            )
                        }

                        // Dashed horizontal & glowing vertical crosshairs
                        drawLine(
                            brush = Brush.verticalGradient(
                                listOf(Color.Transparent, accent.copy(alpha = 0.72f), Color.Transparent)
                            ),
                            start = Offset(cx, 0f),
                            end = Offset(cx, size.height),
                            strokeWidth = 1.6.dp.toPx()
                        )
                        drawLine(
                            color = accent.copy(alpha = 0.42f),
                            start = Offset(0f, holeCy),
                            end = Offset(size.width, holeCy),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        )

                        // Cutout circle & dashed +8dp Safe Zone ring
                        drawRoundRect(
                            color = accent,
                            topLeft = Offset(cx - holeW / 2f, topY),
                            size = Size(holeW, holeH),
                            cornerRadius = CornerRadius(settings.cutoutCornerRadiusDp.dp.toPx()),
                            style = Stroke(width = 1.6.dp.toPx())
                        )
                        drawRoundRect(
                            color = accent.copy(alpha = 0.78f),
                            topLeft = Offset(cx - holeW / 2f - marginPx, (topY - marginPx).coerceAtLeast(2f)),
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
                            y = (settings.cutoutYOffsetDp + 18f).dp
                        )
                    ) {
                        if (!previewExpanded) {
                            IdlePillView(
                                settings = settings,
                                state = previewState,
                                exclusionZone = previewZone,
                                accentColor = accent,
                                isCalibrationPreview = true,
                                isDragOverIsland = false,
                                isLowBatteryWarning = false
                            )
                        } else {
                            CompactPillView(
                                state = previewState,
                                settings = settings,
                                exclusionZone = previewZone,
                                accentColor = accent
                            )
                        }
                    }

                    // Safe Zone badge toggle at bottom of radar zone
                    Button(
                        onClick = { previewExpanded = !previewExpanded },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (previewExpanded) accent.copy(alpha = 0.85f) else Color(0xD41B1D23),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.75f)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                            .height(34.dp)
                            .testTag("calibration_preview_expand")
                    ) {
                        Text(
                            text = if (previewExpanded) "Safe Zone · Live Pill" else "Safe Zone · Idle Ring",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // 4-Column REAL Telemetry Card (never fake mockup numbers)
                ObsidianGlassCard(cornerRadius = 22.dp, contentPadding = PaddingValues(vertical = 14.dp, horizontal = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TelemetryCell(
                            Icons.Filled.AspectRatio,
                            "Cutout px",
                            "${liveZone.cutoutRectPx.width()}×${liveZone.cutoutRectPx.height()}",
                            Modifier.weight(1f)
                        )
                        TelemetryCell(
                            Icons.Filled.CenterFocusStrong,
                            "X Offset",
                            "${settings.cutoutXOffsetDp.toInt()} dp",
                            Modifier.weight(1f)
                        )
                        TelemetryCell(
                            Icons.Filled.OpenWith,
                            "Y Offset",
                            "${settings.cutoutYOffsetDp.toInt()} dp",
                            Modifier.weight(1f)
                        )
                        TelemetryCell(
                            Icons.Filled.Height,
                            "Hole dp",
                            "${settings.cutoutWidthDp.toInt()}×${settings.cutoutHeightDp.toInt()}",
                            Modifier.weight(1f)
                        )
                    }
                }

                // Alignment Score + Auto-Measure / 74x93 px Preset Card
                ObsidianGlassCard(cornerRadius = 22.dp, contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(82.dp)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val sw = 7.dp.toPx()
                                val pad = sw / 2f + 2.dp.toPx()
                                val sz = Size(size.width - pad * 2f, size.height - pad * 2f)
                                drawArc(
                                    color = Color(0xFF23252D),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = Offset(pad, pad),
                                    size = sz,
                                    style = Stroke(width = sw, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    brush = Brush.sweepGradient(listOf(accent.copy(alpha = 0.72f), accent, accent.copy(alpha = 0.92f))),
                                    startAngle = -90f,
                                    sweepAngle = (alignmentScore / 100f) * 360f,
                                    useCenter = false,
                                    topLeft = Offset(pad, pad),
                                    size = sz,
                                    style = Stroke(width = sw, cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$alignmentScore%", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("aligned", color = IslandColors.TextSecondary, fontSize = 10.5.sp)
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Camera Cutout Check", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = "Safe Top: ${liveZone.safeInsetTopPx} px · Clear Zone: ${liveZone.exclusionRectDp.width.toInt()}×${liveZone.exclusionRectDp.height.toInt()} dp",
                                color = IslandColors.TextSecondary,
                                fontSize = 12.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                                    calibrationConfirmed = true
                                                )
                                            }
                                        }
                                    },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(36.dp).testTag("calibration_autodetect")
                                ) {
                                    Icon(Icons.Filled.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Re-measure", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            app.settingsRepository.update {
                                                it.copy(
                                                    cutoutXOffsetDp = 0f,
                                                    cutoutYOffsetDp = 0f,
                                                    cutoutWidthDp = 27f,
                                                    cutoutHeightDp = 34f,
                                                    cutoutCornerRadiusDp = 17f
                                                )
                                            }
                                        }
                                    },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF23252D)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(36.dp).testTag("calibration_reset")
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("74×93 px", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Worst-case fixture selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("Saved to Photos", "60-char Music", "100% · 99W").forEachIndexed { idx, label ->
                        val active = worstCaseFixture == idx
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(CircleShape)
                                .background(if (active) accent.copy(alpha = 0.26f) else Color(0xD41B1D23))
                                .border(1.dp, if (active) accent else Color.White.copy(alpha = 0.14f), CircleShape)
                                .clickable { worstCaseFixture = idx }
                        ) {
                            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                }

                // Slider Cards matching everyoneplsshutup-calibration-v2.png
                CalibrationSliderCard(
                    icon = Icons.Filled.Tune,
                    title = "Horizontal X Offset",
                    subtitle = "Center or shift left/right of punch-hole.",
                    value = settings.cutoutXOffsetDp,
                    range = -40f..40f,
                    unit = "dp"
                ) { v -> scope.launch { app.settingsRepository.update { it.copy(cutoutXOffsetDp = v) } } }

                CalibrationSliderCard(
                    icon = Icons.Filled.SwapVert,
                    title = "Vertical Y Offset",
                    subtitle = "Move the pill up or down around the camera.",
                    value = settings.cutoutYOffsetDp,
                    range = 0f..36f,
                    unit = "dp"
                ) { v -> scope.launch { app.settingsRepository.update { it.copy(cutoutYOffsetDp = v) } } }

                CalibrationSliderCard(
                    icon = Icons.Filled.AspectRatio,
                    title = "Cutout Clearance Width",
                    subtitle = "Clearance gap spanned cleanly across the lens.",
                    value = settings.cutoutWidthDp,
                    range = 14f..80f,
                    unit = "dp"
                ) { v -> scope.launch { app.settingsRepository.update { it.copy(cutoutWidthDp = v) } } }

                CalibrationSliderCard(
                    icon = Icons.Filled.Height,
                    title = "Cutout Clearance Height",
                    subtitle = "Minimum vertical clearance around cutout.",
                    value = settings.cutoutHeightDp,
                    range = 14f..60f,
                    unit = "dp"
                ) { v -> scope.launch { app.settingsRepository.update { it.copy(cutoutHeightDp = v) } } }

                CalibrationSliderCard(
                    icon = Icons.Filled.CenterFocusStrong,
                    title = "Corner Radius",
                    subtitle = "Outer capsule curvature radius.",
                    value = settings.cutoutCornerRadiusDp,
                    range = 6f..28f,
                    unit = "dp"
                ) { v -> scope.launch { app.settingsRepository.update { it.copy(cutoutCornerRadiusDp = v) } } }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ObsidianPillButton(
                        text = "Test Alert",
                        icon = Icons.Filled.NotificationsActive,
                        onClick = {
                            app.soundManager.playTap()
                            app.islandController.upsertActivity(
                                IslandActivity.NotificationPreview(
                                    id = "calib_test_alert_${System.currentTimeMillis()}",
                                    packageName = context.packageName,
                                    appName = "System Photos Screenshot Alert",
                                    title = "Saved to Photos — dead-zone clearance test alert",
                                    text = "Longer preview message stays strictly in Expanded view"
                                ),
                                autoShowCompact = true,
                                autoDismissMs = settings.alertsDwellMs
                            )
                        },
                        modifier = Modifier.weight(0.85f).height(52.dp)
                    )
                    CrimsonPillButton(
                        text = if (settings.calibrationConfirmed) "Save Calibration" else "Confirm Clear",
                        icon = if (settings.calibrationConfirmed) Icons.Filled.Save else Icons.Filled.CheckCircle,
                        onClick = {
                            app.soundManager.playToggle(true)
                            scope.launch { app.settingsRepository.update { it.copy(calibrationConfirmed = true) } }
                            onBack()
                        },
                        modifier = Modifier.weight(1.15f).height(52.dp).testTag("calibration_confirm_btn")
                    )
                }

                Text(
                    text = "You can re-calibrate anytime from Settings.",
                    color = IslandColors.TextTertiary,
                    fontSize = 11.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)
                )
            }
        }
    }
}

@Composable
private fun TelemetryCell(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.height(5.dp))
        Text(label, color = IslandColors.TextSecondary, fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CalibrationSliderCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Float) -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    ObsidianGlassCard(cornerRadius = 22.dp, contentPadding = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            CrimsonSquircleIcon(icon = icon, size = 40.dp, iconSize = 20.dp, tint = accent)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                Text(subtitle, color = IslandColors.TextSecondary, fontSize = 11.5.sp)
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF20222A))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                    .padding(horizontal = 11.dp, vertical = 5.dp)
            ) {
                Text("${value.toInt()} $unit", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(6.dp))
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = Color(0xFF252831)
            )
        )
    }
}
