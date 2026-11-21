package com.example.island

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
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
    val view = LocalView.current
    val density = LocalDensity.current
    var cutoutText by remember { mutableStateOf("Checking DisplayCutout...") }
    var previewExpanded by remember { mutableStateOf(false) }

    val previewScale by animateFloatAsState(
        targetValue = if (previewExpanded) 1.0f else 0.92f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
        label = "calib_preview_scale"
    )

    DisposableEffect(Unit) {
        app.islandController.setCalibrationPreview(true)
        onDispose { app.islandController.setCalibrationPreview(false) }
    }

    LaunchedEffect(view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val rect = view.rootWindowInsets?.displayCutout?.boundingRects?.firstOrNull()
            cutoutText = if (rect != null) {
                val wDp = with(density) { rect.width().toDp().value.toInt() }
                val hDp = with(density) { rect.height().toDp().value.toInt() }
                val topDp = with(density) { rect.top.toDp().value.toInt() }
                "Detected DisplayCutout: ${rect.width()}×${rect.height()} px (~${wDp}×${hDp} dp, top ${topDp} dp)"
            } else {
                "No hardware DisplayCutout reported in this window (emulator/standard bezel)"
            }
        } else {
            cutoutText = "DisplayCutout API requires Android 9+"
        }
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Red Calibrate Mode Preview Box (island grows directly out of the red camera ring)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0A0A0E),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
                    .border(1.dp, IslandColors.AccentRed.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    // Precision alignment crosshairs
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f + settings.cutoutXOffsetDp.dp.toPx()
                        val cy = settings.cutoutYOffsetDp.dp.toPx() + (settings.cutoutHeightDp.dp.toPx() / 2f) + 3.dp.toPx()
                        drawLine(
                            color = IslandColors.AccentRed.copy(alpha = 0.28f),
                            start = Offset(cx, 0f),
                            end = Offset(cx, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = IslandColors.AccentRed.copy(alpha = 0.28f),
                            start = Offset(0f, cy),
                            end = Offset(size.width, cy),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Red calibration ring or expanded capsule growing cleanly around camera cutout
                    AnimatedContent(
                        targetState = previewExpanded,
                        label = "calib_expand_anim",
                        modifier = Modifier
                            .offset(
                                x = settings.cutoutXOffsetDp.dp,
                                y = settings.cutoutYOffsetDp.dp
                            )
                            .graphicsLayer {
                                scaleX = previewScale
                                scaleY = previewScale
                            }
                    ) { expanded ->
                        if (!expanded) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black)
                                    .border(2.5.dp, IslandColors.AccentRed, CircleShape)
                                    .padding(horizontal = 5.dp, vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(settings.cutoutWidthDp.dp, settings.cutoutHeightDp.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF20222A))
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .widthIn(min = 240.dp, max = 300.dp)
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(Color(0xFF14151C))
                                    .border(2.dp, IslandColors.AccentRed, RoundedCornerShape(32.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Left Safe", color = Color.White, fontSize = 10.sp)
                                    Box(
                                        modifier = Modifier
                                            .size(settings.cutoutWidthDp.dp, settings.cutoutHeightDp.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF22242E))
                                            .border(1.5.dp, IslandColors.AccentRed, CircleShape)
                                    )
                                    Text("Right Safe", color = Color.White, fontSize = 10.sp)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Expanded content sits below camera void",
                                    color = IslandColors.TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Red Calibrate Mode · X:${settings.cutoutXOffsetDp.toInt()} Y:${settings.cutoutYOffsetDp.toInt()}",
                            color = IslandColors.AccentRed,
                            style = MaterialTheme.typography.labelMedium,
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
                            Text(if (previewExpanded) "Shrink" else "Preview Expand", fontSize = 11.sp)
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = cutoutText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val rect = view.rootWindowInsets?.displayCutout?.boundingRects?.firstOrNull()
                            if (rect != null) {
                                val wDp = with(density) { rect.width().toDp().value }.coerceIn(18f, 64f)
                                val hDp = with(density) { rect.height().toDp().value }.coerceIn(18f, 48f)
                                val topDp = with(density) { rect.top.toDp().value }.coerceIn(0f, 32f)
                                scope.launch {
                                    app.settingsRepository.update {
                                        it.copy(
                                            cutoutXOffsetDp = 0f,
                                            cutoutYOffsetDp = topDp,
                                            cutoutWidthDp = wDp,
                                            cutoutHeightDp = hDp
                                        )
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("calibration_autodetect")
                ) {
                    Icon(Icons.Filled.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Auto-Detect")
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            // Redmi Note 10 Pro centered punch-hole baseline
                            app.settingsRepository.update {
                                it.copy(
                                    cutoutXOffsetDp = 0f,
                                    cutoutYOffsetDp = 6f,
                                    cutoutWidthDp = 26f,
                                    cutoutHeightDp = 26f
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("calibration_reset")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Reset Default")
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
                range = 16f..68f,
                unit = "dp"
            ) { v -> scope.launch { app.settingsRepository.update { it.copy(cutoutWidthDp = v) } } }

            NudgeSliderRow(
                label = "Cutout Clearance Height",
                value = settings.cutoutHeightDp,
                range = 16f..52f,
                unit = "dp"
            ) { v -> scope.launch { app.settingsRepository.update { it.copy(cutoutHeightDp = v) } } }
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
