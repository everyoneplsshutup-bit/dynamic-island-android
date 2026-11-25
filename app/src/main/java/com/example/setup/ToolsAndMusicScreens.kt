package com.example.setup

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ScreenLockRotation
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DynamicIslandApp
import com.example.ocr.MediaProjectionPromptActivity
import com.example.ocr.OcrPrivacyGate
import com.example.system.IslandSettings
import com.example.system.TimerEngine
import com.example.ui.theme.CrimsonCircularHaloIcon
import com.example.ui.theme.CrimsonPillButton
import com.example.ui.theme.IslandColors
import com.example.ui.theme.MockupScreenHeader
import com.example.ui.theme.ObsidianCrimsonBackground
import com.example.ui.theme.ObsidianGlassCard
import com.example.ui.theme.WavyProgressBar
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun ToolsTabScreen(
    app: DynamicIslandApp,
    onOpenStopwatchTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val toggleState by app.quickTogglesManager.state.collectAsState()
    var showCompassDialog by remember { mutableStateOf(false) }
    var showLevelDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    ObsidianCrimsonBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
        ) {
            MockupScreenHeader(
                title = "Tools",
                subtitle = "Useful utilities at your fingertips",
                icon = Icons.Filled.Build,
                trailingAction = {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xAA191114))
                                .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                        ) {
                            Icon(Icons.Filled.BrightnessHigh, contentDescription = "Tool permissions", tint = Color.White)
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Refresh System States") },
                                onClick = {
                                    showMenu = false
                                    app.quickTogglesManager.refreshSystemStates()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Modify System Settings (Brightness)") },
                                onClick = {
                                    showMenu = false
                                    app.quickTogglesManager.openWriteSettings()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Do Not Disturb Access") },
                                onClick = {
                                    showMenu = false
                                    app.quickTogglesManager.openDndAccessSettings()
                                }
                            )
                        }
                    }
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ToolBentoTile(
                        title = "Flashlight",
                        subtitle = if (toggleState.isTorchOn) "Beam ON" else "Tap to toggle",
                        icon = Icons.Filled.FlashlightOn,
                        isActive = toggleState.isTorchOn,
                        onClick = {
                            app.soundManager.playToggle(!toggleState.isTorchOn)
                            app.quickTogglesManager.toggleFlashlight()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ToolBentoTile(
                        title = "Do Not Disturb",
                        subtitle = when {
                            !toggleState.hasDndPermission -> "Grant DND access"
                            toggleState.isDndOn -> "Silenced"
                            else -> "Normal alerts"
                        },
                        icon = Icons.Filled.DoNotDisturbOn,
                        isActive = toggleState.isDndOn,
                        onClick = {
                            app.soundManager.playToggle(!toggleState.isDndOn)
                            app.quickTogglesManager.toggleDoNotDisturb()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (toggleState.isTorchOn && toggleState.maxTorchStrength > 1) {
                    ObsidianGlassCard(highlighted = true) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Torch Intensity", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("${toggleState.torchStrength} / ${toggleState.maxTorchStrength}", color = IslandColors.CrimsonBright, fontSize = 12.sp)
                        }
                        Slider(
                            value = toggleState.torchStrength.toFloat(),
                            onValueChange = { app.quickTogglesManager.setTorchStrength(it.roundToInt()) },
                            valueRange = 1f..toggleState.maxTorchStrength.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = IslandColors.CrimsonBright,
                                activeTrackColor = IslandColors.CrimsonPrimary
                            )
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ToolBentoTile(
                        title = "Compass",
                        subtitle = "Live magnetometer",
                        icon = Icons.Filled.Explore,
                        isActive = showCompassDialog,
                        onClick = {
                            app.soundManager.playTap()
                            showCompassDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ToolBentoTile(
                        title = "Level",
                        subtitle = "2-axis tilt bubble",
                        icon = Icons.Filled.Straighten,
                        isActive = showLevelDialog,
                        onClick = {
                            app.soundManager.playTap()
                            showLevelDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ToolBentoTile(
                        title = "Keep Awake",
                        subtitle = if (toggleState.isKeepAwakeOn) "Screen awake ON" else "Normal timeout",
                        icon = Icons.Filled.ScreenLockRotation,
                        isActive = toggleState.isKeepAwakeOn,
                        onClick = {
                            app.soundManager.playToggle(!toggleState.isKeepAwakeOn)
                            app.quickTogglesManager.toggleKeepAwake()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ToolBentoTile(
                        title = "Stopwatch",
                        subtitle = "Millisecond laps",
                        icon = Icons.Filled.Timer,
                        isActive = false,
                        onClick = {
                            app.soundManager.playTap()
                            onOpenStopwatchTab()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ToolBentoTile(
                        title = "Select OCR",
                        subtitle = "Copy screen text",
                        icon = Icons.Filled.TextFields,
                        isActive = false,
                        onClick = {
                            scope.launch {
                                val allowed = OcrPrivacyGate.isScreenReadingAllowed(app.settingsRepository)
                                if (allowed) {
                                    app.soundManager.playTap()
                                    app.islandController.setSelectMode(true)
                                    Toast.makeText(context, "Select Mode active on Dynamic Island", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Enable Screen Reading / OCR in Settings > Privacy first", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ToolBentoTile(
                        title = "Shot → Shelf",
                        subtitle = "Capture 10m shelf",
                        icon = Icons.Filled.Layers,
                        isActive = false,
                        onClick = {
                            app.soundManager.playTap()
                            MediaProjectionPromptActivity.launchForVaultScreenshot(context)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Real Display Brightness & System Connectivity Panels
                ObsidianGlassCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Screen Brightness", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            if (toggleState.hasWriteSettingsPermission) "${(toggleState.brightness * 100 / 255f).roundToInt()}%" else "Tap to grant",
                            color = IslandColors.CrimsonBright,
                            fontSize = 12.sp
                        )
                    }
                    Slider(
                        value = toggleState.brightness.toFloat(),
                        onValueChange = { app.quickTogglesManager.setScreenBrightness(it.roundToInt()) },
                        valueRange = 10f..255f,
                        colors = SliderDefaults.colors(
                            thumbColor = IslandColors.CrimsonBright,
                            activeTrackColor = IslandColors.CrimsonPrimary
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ObsidianQuickLinkChip(
                            text = "Wi-Fi Panel",
                            icon = Icons.Filled.Wifi,
                            onClick = { app.quickTogglesManager.openWifiPanel() },
                            modifier = Modifier.weight(1f)
                        )
                        ObsidianQuickLinkChip(
                            text = "Bluetooth",
                            icon = Icons.Filled.Bluetooth,
                            onClick = { app.quickTogglesManager.openBluetoothPanel() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    if (showCompassDialog) {
        LiveCompassDialog(onDismiss = { showCompassDialog = false })
    }
    if (showLevelDialog) {
        LiveBubbleLevelDialog(onDismiss = { showLevelDialog = false })
    }
}

@Composable
private fun ToolBentoTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ObsidianGlassCard(
        highlighted = isActive,
        contentPadding = PaddingValues(vertical = 18.dp, horizontal = 12.dp),
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            CrimsonCircularHaloIcon(icon = icon, isActive = isActive)
            Spacer(Modifier.height(12.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = IslandColors.TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ObsidianQuickLinkChip(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(44.dp)
            .clip(CircleShape)
            .background(Color(0xCC1E1418))
            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = IslandColors.CrimsonBright, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun LiveCompassDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var azimuthDeg by remember { mutableFloatStateOf(0f) }
    var hasSensor by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotSensor = sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sm == null || rotSensor == null) {
            hasSensor = false
            onDispose { }
        } else {
            val rotMatrix = FloatArray(9)
            val orientation = FloatArray(3)
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                    SensorManager.getOrientation(rotMatrix, orientation)
                    val deg = ((Math.toDegrees(orientation[0].toDouble()).toFloat()) + 360f) % 360f
                    azimuthDeg = deg
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sm.registerListener(listener, rotSensor, SensorManager.SENSOR_DELAY_UI)
            onDispose { sm.unregisterListener(listener) }
        }
    }

    val cardinal = when (((azimuthDeg + 22.5f) % 360f / 45f).toInt()) {
        0 -> "N"
        1 -> "NE"
        2 -> "E"
        3 -> "SE"
        4 -> "S"
        5 -> "SW"
        6 -> "W"
        else -> "NW"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IslandColors.ObsidianCardElevated,
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = { Text("Compass") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (!hasSensor) {
                    Text("Rotation vector sensor unavailable on this device.", color = IslandColors.TextSecondary)
                } else {
                    Canvas(modifier = Modifier.size(180.dp)) {
                        val r = size.minDimension / 2f - 10.dp.toPx()
                        drawCircle(color = Color(0xFF2B1014), radius = r, style = Stroke(width = 3.dp.toPx()))
                        drawCircle(color = IslandColors.CrimsonBorderStrong, radius = r, style = Stroke(width = 1.5.dp.toPx()))
                        val rad = ((azimuthDeg - 90f) * PI / 180.0)
                        val tip = Offset(center.x + cos(rad).toFloat() * (r - 12.dp.toPx()), center.y + sin(rad).toFloat() * (r - 12.dp.toPx()))
                        drawLine(color = IslandColors.CrimsonBright, start = center, end = tip, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                        drawCircle(color = Color.White, radius = 6.dp.toPx(), center = center)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("${azimuthDeg.roundToInt()}° $cardinal", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = IslandColors.CrimsonPrimary)) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun LiveBubbleLevelDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var pitchDeg by remember { mutableFloatStateOf(0f) }
    var rollDeg by remember { mutableFloatStateOf(0f) }
    var hasSensor by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accel = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sm == null || accel == null) {
            hasSensor = false
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val ax = event.values[0]
                    val ay = event.values[1]
                    val az = event.values[2]
                    val norm = sqrt((ax * ax + ay * ay + az * az).toDouble()).coerceAtLeast(0.01)
                    rollDeg = Math.toDegrees(atan2(-ax.toDouble(), sqrt(ay * ay + az * az.toDouble()))).toFloat()
                    pitchDeg = Math.toDegrees(atan2(ay.toDouble(), norm)).toFloat()
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
            onDispose { sm.unregisterListener(listener) }
        }
    }

    val isFlat = kotlin.math.abs(rollDeg) < 1.5f && kotlin.math.abs(pitchDeg) < 1.5f

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IslandColors.ObsidianCardElevated,
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = { Text("Surface Level") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (!hasSensor) {
                    Text("Accelerometer unavailable on this device.", color = IslandColors.TextSecondary)
                } else {
                    Canvas(modifier = Modifier.size(180.dp)) {
                        val r = size.minDimension / 2f - 8.dp.toPx()
                        drawCircle(color = Color(0xFF150B0E), radius = r)
                        drawCircle(color = if (isFlat) IslandColors.AccentGreen else IslandColors.CrimsonBorderStrong, radius = r, style = Stroke(width = 2.dp.toPx()))
                        drawLine(color = Color.White.copy(alpha = 0.2f), start = Offset(center.x - r, center.y), end = Offset(center.x + r, center.y), strokeWidth = 1.dp.toPx())
                        drawLine(color = Color.White.copy(alpha = 0.2f), start = Offset(center.x, center.y - r), end = Offset(center.x, center.y + r), strokeWidth = 1.dp.toPx())
                        val dx = (rollDeg / 45f).coerceIn(-1f, 1f) * (r - 18.dp.toPx())
                        val dy = (pitchDeg / 45f).coerceIn(-1f, 1f) * (r - 18.dp.toPx())
                        drawCircle(
                            color = if (isFlat) IslandColors.AccentGreen else IslandColors.CrimsonBright,
                            radius = 14.dp.toPx(),
                            center = Offset(center.x + dx, center.y + dy)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (isFlat) "Level (0°)" else "Roll ${rollDeg.roundToInt()}° · Pitch ${pitchDeg.roundToInt()}°",
                        color = if (isFlat) IslandColors.AccentGreen else Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = IslandColors.CrimsonPrimary)) {
                Text("Close")
            }
        }
    )
}

@Composable
fun MusicTabScreen(
    app: DynamicIslandApp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mediaState by app.mediaHub.state.collectAsState()
    val settings by app.settingsRepository.settingsFlow.collectAsState(initial = IslandSettings())

    val spinDeg = if (mediaState.isPlaying && settings.rotateAlbumArt) {
        (mediaState.positionMs / 38f) % 360f
    } else 0f

    val progress = if (mediaState.durationMs > 0L) {
        (mediaState.positionMs.toFloat() / mediaState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    ObsidianCrimsonBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
        ) {
            MockupScreenHeader(
                title = "Music",
                subtitle = mediaState.appLabel.ifBlank { "On-Device Media Session" },
                icon = Icons.Filled.MusicNote,
                trailingAction = {
                    IconButton(
                        onClick = { app.mediaHub.openPlayerApp() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xAA191114))
                            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open player app", tint = Color.White)
                    }
                }
            )

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                if (!mediaState.hasNotificationAccess) {
                    item {
                        ObsidianGlassCard(highlighted = true) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Notification Access Required", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("Grant Notification Access so Music controls live media sessions.", color = IslandColors.TextSecondary, fontSize = 12.sp)
                                }
                                Spacer(Modifier.width(8.dp))
                                CrimsonPillButton(
                                    text = "Grant",
                                    onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } }
                                )
                            }
                        }
                    }
                }

                item {
                    // Centered Crimson Halo Album Art Card matching youtubers742-music.png
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(210.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(IslandColors.CrimsonPrimary.copy(alpha = 0.35f), Color(0xFF14090C))
                                )
                            )
                            .border(1.2.dp, IslandColors.CrimsonBorder, RoundedCornerShape(28.dp))
                            .clickable { app.mediaHub.openPlayerApp() }
                    ) {
                        val bmp = mediaState.albumArt
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = mediaState.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { rotationZ = if (settings.rotateAlbumArt) spinDeg * 0.05f else 0f }
                            )
                        } else {
                            CrimsonCircularHaloIcon(
                                icon = Icons.Filled.MusicNote,
                                size = 96.dp,
                                iconSize = 46.dp,
                                isActive = mediaState.isPlaying
                            )
                        }
                    }
                }

                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = mediaState.title.ifBlank { "Nothing Playing" },
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (mediaState.artist.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = mediaState.artist,
                                color = IslandColors.TextSecondary,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        WavyProgressBar(
                            progress = progress,
                            isActiveWave = mediaState.isPlaying,
                            accentColor = IslandColors.CrimsonBright,
                            interactive = mediaState.durationMs > 0L,
                            onSeekFraction = { frac ->
                                val target = (frac * mediaState.durationMs).toLong()
                                app.mediaHub.seekTo(target)
                            }
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(TimerEngine.formatDuration(mediaState.positionMs), color = IslandColors.TextSecondary, fontSize = 12.sp)
                            Text(TimerEngine.formatDuration(mediaState.durationMs), color = IslandColors.TextSecondary, fontSize = 12.sp)
                        }
                    }
                }

                item {
                    // 5 Transport Controls matching youtubers742-music.png
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TransportCircleButton(
                            icon = Icons.Filled.Shuffle,
                            contentDescription = "Shuffle",
                            active = mediaState.isShuffled,
                            onClick = {
                                app.soundManager.playToggle(!mediaState.isShuffled)
                                app.mediaHub.sendShuffleCommand()
                            }
                        )
                        TransportCircleButton(
                            icon = Icons.Filled.SkipPrevious,
                            contentDescription = "Previous",
                            onClick = {
                                app.soundManager.playMusicNext()
                                app.mediaHub.skipPrevious()
                            }
                        )
                        IconButton(
                            onClick = {
                                if (mediaState.isPlaying) app.soundManager.playMusicPause() else app.soundManager.playMusicPlay()
                                app.mediaHub.togglePlayPause()
                            },
                            modifier = Modifier
                                .size(66.dp)
                                .clip(CircleShape)
                                .background(IslandColors.CrimsonPrimary)
                                .border(1.5.dp, IslandColors.CrimsonGlow, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (mediaState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (mediaState.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        TransportCircleButton(
                            icon = Icons.Filled.SkipNext,
                            contentDescription = "Next",
                            onClick = {
                                app.soundManager.playMusicNext()
                                app.mediaHub.skipNext()
                            }
                        )
                        TransportCircleButton(
                            icon = if (mediaState.supportsLike) (if (mediaState.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder) else Icons.Filled.Repeat,
                            contentDescription = if (mediaState.supportsLike) "Like" else "Repeat",
                            active = if (mediaState.supportsLike) mediaState.isLiked else mediaState.repeatModeLabel != "Off",
                            onClick = {
                                app.soundManager.playTap()
                                if (mediaState.supportsLike) app.mediaHub.sendLikeCommand() else app.mediaHub.sendRepeatCommand()
                            }
                        )
                    }
                }

                // On-Device Lyrics / Session Info
                if (mediaState.lyrics.isNotEmpty()) {
                    item {
                        ObsidianGlassCard {
                            Text("Synced Lyrics", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    items(mediaState.lyrics) { line ->
                        val active = line.timestampMs >= 0L && kotlin.math.abs(line.timestampMs - mediaState.positionMs) < 3500L
                        Text(
                            text = line.text,
                            color = if (active) IslandColors.CrimsonBright else IslandColors.TextSecondary,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            fontSize = if (active) 15.sp else 13.sp,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        )
                    }
                } else {
                    item {
                        ObsidianGlassCard {
                            Text(
                                text = if (mediaState.hasActiveSession) "Playing from ${mediaState.appLabel}. Tap album art to open player."
                                else "Start playback in PixelMusic or your configured player app.",
                                color = IslandColors.TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransportCircleButton(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (active) Color(0xFF470B13) else Color(0xCC181014))
            .border(1.dp, if (active) IslandColors.CrimsonBorderStrong else Color.White.copy(alpha = 0.16f), CircleShape)
    ) {
        Icon(icon, contentDescription = contentDescription, tint = if (active) IslandColors.CrimsonBright else Color.White, modifier = Modifier.size(22.dp))
    }
}
