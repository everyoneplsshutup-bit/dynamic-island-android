package com.example.system

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IslandColors
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun TimerStopwatchPanel(
    state: TimerUiState,
    accentColor: Color,
    onPickerChanged: (Int, Int, Int) -> Unit,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onCancelTimer: () -> Unit,
    onStartStopwatch: () -> Unit,
    onPauseStopwatch: () -> Unit,
    onResetStopwatch: () -> Unit,
    onLapStopwatch: () -> Unit,
    onHaptic: () -> Unit = {},
    onTimerSetSound: () -> Unit = {},
    onStartSleepTimer: (Long) -> Unit = {}
) {
    var modeIndex by remember { mutableIntStateOf(0) } // 0 = Timer, 1 = Stopwatch, 2 = Sleep

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
        ) {
            listOf("Timer", "Stopwatch", "Sleep").forEachIndexed { idx, label ->
                SegmentedButton(
                    selected = modeIndex == idx,
                    onClick = { onHaptic(); onTimerSetSound(); modeIndex = idx },
                    shape = SegmentedButtonDefaults.itemShape(index = idx, count = 3),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = accentColor.copy(alpha = 0.25f),
                        activeContentColor = accentColor,
                        inactiveContainerColor = Color.White.copy(alpha = 0.06f),
                        inactiveContentColor = IslandColors.TextSecondary
                    ),
                    modifier = Modifier.testTag("timer_mode_${label.lowercase()}")
                ) {
                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        when (modeIndex) {
            0 -> {
                if (state.isActive) {
                    ActiveTimerRingView(
                        state = state,
                        accentColor = accentColor,
                        onPause = { onHaptic(); onTimerSetSound(); onPauseTimer() },
                        onResume = { onHaptic(); onTimerSetSound(); onResumeTimer() },
                        onCancel = { onHaptic(); onCancelTimer() }
                    )
                } else {
                    WheelTimerPickerView(
                        state = state,
                        accentColor = accentColor,
                        onPickerChanged = { h, m, s ->
                            onHaptic()
                            onPickerChanged(h, m, s)
                        },
                        onStart = {
                            onHaptic()
                            onTimerSetSound()
                            onStartTimer()
                        }
                    )
                }

                if (state.mirroredClockLabel != null && state.mirroredRemainingMs != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Clock: ${state.mirroredClockLabel}",
                            color = IslandColors.TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = TimerEngine.formatDuration(state.mirroredRemainingMs),
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            1 -> {
                StopwatchView(
                    state = state,
                    accentColor = accentColor,
                    onStart = { onHaptic(); onTimerSetSound(); onStartStopwatch() },
                    onPause = { onHaptic(); onTimerSetSound(); onPauseStopwatch() },
                    onReset = { onHaptic(); onResetStopwatch() },
                    onLap = { onHaptic(); onTimerSetSound(); onLapStopwatch() }
                )
            }

            else -> {
                SleepDialView(
                    accentColor = accentColor,
                    onHaptic = onHaptic,
                    onStartSleepTimer = { durationMs ->
                        onHaptic()
                        onTimerSetSound()
                        onStartSleepTimer(durationMs)
                        modeIndex = 0
                    }
                )
            }
        }
    }
}

/**
 * Interactive 24h circular Bedtime-to-Wake Sleep Dial (drag Bedtime violet or Wake orange knobs).
 */
@Composable
private fun SleepDialView(
    accentColor: Color,
    onHaptic: () -> Unit,
    onStartSleepTimer: (Long) -> Unit
) {
    // Minutes from midnight in 15-min steps: default 23:00 (1380) -> 07:00 (420) = 8h 0m
    var bedtimeMin by remember { mutableIntStateOf(23 * 60) }
    var wakeMin by remember { mutableIntStateOf(7 * 60) }
    var draggingKnob by remember { mutableIntStateOf(0) } // 1 = bedtime, 2 = wake

    val sleepMinutes = remember(bedtimeMin, wakeMin) {
        val diff = wakeMin - bedtimeMin
        if (diff > 0) diff else diff + 24 * 60
    }
    val sleepHours = sleepMinutes / 60
    val sleepRemMin = sleepMinutes % 60
    val durationMs = sleepMinutes * 60_000L

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(174.dp)
            .testTag("sleep_dial_panel"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(152.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { start ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val r = (minOf(size.width, size.height) / 2f) - 14.dp.toPx()
                            val bedRad = minutesToRad(bedtimeMin)
                            val wakeRad = minutesToRad(wakeMin)
                            val bedPos = Offset(cx + cos(bedRad).toFloat() * r, cy + sin(bedRad).toFloat() * r)
                            val wakePos = Offset(cx + cos(wakeRad).toFloat() * r, cy + sin(wakeRad).toFloat() * r)
                            val dBed = hypot(start.x - bedPos.x, start.y - bedPos.y)
                            val dWake = hypot(start.x - wakePos.x, start.y - wakePos.y)
                            draggingKnob = if (dBed <= dWake) 1 else 2
                            onHaptic()
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val angleDeg = (atan2(change.position.y - cy, change.position.x - cx) * 180.0 / PI) + 90.0
                            val normDeg = ((angleDeg % 360.0) + 360.0) % 360.0
                            val snappedMin = (((normDeg / 360.0) * 1440.0) / 15.0).roundToInt() * 15 % 1440
                            if (draggingKnob == 1) {
                                if (snappedMin != bedtimeMin) {
                                    bedtimeMin = snappedMin
                                    onHaptic()
                                }
                            } else if (draggingKnob == 2) {
                                if (snappedMin != wakeMin) {
                                    wakeMin = snappedMin
                                    onHaptic()
                                }
                            }
                        },
                        onDragEnd = { draggingKnob = 0 },
                        onDragCancel = { draggingKnob = 0 }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(150.dp)) {
                val strokeW = 14.dp.toPx()
                val r = (size.minDimension - strokeW - 12.dp.toPx()) / 2f
                val cx = size.width / 2f
                val cy = size.height / 2f

                drawCircle(
                    color = Color.White.copy(alpha = 0.09f),
                    radius = r,
                    style = Stroke(width = strokeW)
                )

                // 24 tick marks around dial
                for (h in 0 until 24) {
                    val rad = (h * 15f - 90f) * PI / 180.0
                    val innerR = r - strokeW * 0.35f
                    val outerR = r + strokeW * 0.35f
                    drawLine(
                        color = Color.White.copy(alpha = if (h % 6 == 0) 0.40f else 0.14f),
                        start = Offset(cx + cos(rad).toFloat() * innerR, cy + sin(rad).toFloat() * innerR),
                        end = Offset(cx + cos(rad).toFloat() * outerR, cy + sin(rad).toFloat() * outerR),
                        strokeWidth = if (h % 6 == 0) 2.dp.toPx() else 1.dp.toPx()
                    )
                }

                val startDeg = (bedtimeMin / 1440f) * 360f - 90f
                val sweepDeg = (sleepMinutes / 1440f) * 360f
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to IslandColors.AccentPurple,
                        0.5f to accentColor,
                        1.0f to IslandColors.AccentPurple
                    ),
                    startAngle = startDeg,
                    sweepAngle = sweepDeg,
                    useCenter = false,
                    topLeft = Offset(cx - r, cy - r),
                    size = androidx.compose.ui.geometry.Size(r * 2f, r * 2f),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )

                val bedRad = minutesToRad(bedtimeMin)
                val wakeRad = minutesToRad(wakeMin)
                val bedPos = Offset(cx + cos(bedRad).toFloat() * r, cy + sin(bedRad).toFloat() * r)
                val wakePos = Offset(cx + cos(wakeRad).toFloat() * r, cy + sin(wakeRad).toFloat() * r)

                drawCircle(color = IslandColors.AccentPurple, radius = 9.dp.toPx(), center = bedPos)
                drawCircle(color = Color.White, radius = 4.dp.toPx(), center = bedPos)

                drawCircle(color = accentColor, radius = 9.dp.toPx(), center = wakePos)
                drawCircle(color = Color.Black, radius = 4.dp.toPx(), center = wakePos)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Bedtime, contentDescription = null, tint = IslandColors.AccentPurple, modifier = Modifier.size(16.dp))
                Text(
                    text = "${sleepHours}h ${sleepRemMin}m",
                    style = TextStyle(
                        color = IslandColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    ),
                    modifier = Modifier.testTag("sleep_dial_duration_text")
                )
                Text("sleep", color = IslandColors.TextSecondary, fontSize = 10.sp)
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("BEDTIME", color = IslandColors.AccentPurple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        formatClockMinutes(bedtimeMin),
                        style = TextStyle(color = IslandColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("WAKE", color = accentColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        formatClockMinutes(wakeMin),
                        style = TextStyle(color = IslandColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")
                    )
                }
            }

            Text(
                text = "Drag purple or amber dial handles to adjust window.",
                color = IslandColors.TextSecondary,
                fontSize = 10.sp,
                lineHeight = 13.sp
            )

            Button(
                onClick = { onStartSleepTimer(durationMs) },
                enabled = sleepMinutes > 0,
                colors = ButtonDefaults.buttonColors(containerColor = IslandColors.AccentPurple, contentColor = Color.White),
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("sleep_start_button")
            ) {
                Text("Start Sleep", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun minutesToRad(minutesFromMidnight: Int): Double {
    val deg = (minutesFromMidnight / 1440.0) * 360.0 - 90.0
    return deg * PI / 180.0
}

private fun formatClockMinutes(minutesFromMidnight: Int): String {
    val norm = ((minutesFromMidnight % 1440) + 1440) % 1440
    return String.format("%02d:%02d", norm / 60, norm % 60)
}

@Composable
private fun ActiveTimerRingView(
    state: TimerUiState,
    accentColor: Color,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    val progress = if (state.totalMs > 0L) {
        (state.remainingMs.toFloat() / state.totalMs.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val isUrgent = state.isRunning && state.remainingMs in 1L..5_000L
    val urgentPulse = if (isUrgent) {
        val infinite = rememberInfiniteTransition(label = "urgent_timer")
        val p by infinite.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.055f,
            animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "urgent_scale"
        )
        p
    } else {
        1.0f
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(106.dp)
                .graphicsLayer {
                    scaleX = urgentPulse
                    scaleY = urgentPulse
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(100.dp)) {
                val strokeW = 7.dp.toPx()
                val stroke = Stroke(width = strokeW, cap = StrokeCap.Round)
                drawArc(
                    color = Color.White.copy(alpha = 0.14f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = stroke
                )
                val sweep = 360f * progress
                drawArc(
                    color = if (isUrgent) IslandColors.AccentRed else accentColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = stroke
                )
                if (progress > 0.01f) {
                    val tipRad = ((-90f + sweep) * PI / 180.0)
                    val radius = (size.minDimension - strokeW) / 2f
                    val tipOffset = Offset(
                        x = center.x + (cos(tipRad) * radius).toFloat(),
                        y = center.y + (sin(tipRad) * radius).toFloat()
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = 3.2.dp.toPx(),
                        center = tipOffset
                    )
                }
            }
            Text(
                text = TimerEngine.formatDuration(state.remainingMs),
                style = TextStyle(
                    color = if (isUrgent) IslandColors.AccentRed else IslandColors.TextPrimary,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Light,
                    fontFeatureSettings = "tnum"
                ),
                modifier = Modifier.testTag("timer_remaining_text")
            )
        }

        Spacer(Modifier.height(6.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .clickable(onClick = onCancel)
                    .testTag("timer_cancel_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel timer", tint = IslandColors.TextPrimary)
            }

            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor)
                    .clickable(onClick = if (state.isRunning) onPause else onResume)
                    .testTag("timer_pause_resume_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isRunning) "Pause timer" else "Resume timer",
                    tint = Color.Black,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun WheelTimerPickerView(
    state: TimerUiState,
    accentColor: Color,
    onPickerChanged: (Int, Int, Int) -> Unit,
    onStart: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NumberWheelColumn(
                label = "hr",
                range = 0..23,
                selected = state.selectedHours,
                accentColor = accentColor,
                testTag = "wheel_hours",
                onSelected = { h -> onPickerChanged(h, state.selectedMinutes, state.selectedSeconds) }
            )
            NumberWheelColumn(
                label = "min",
                range = 0..59,
                selected = state.selectedMinutes,
                accentColor = accentColor,
                testTag = "wheel_minutes",
                onSelected = { m -> onPickerChanged(state.selectedHours, m, state.selectedSeconds) }
            )
            NumberWheelColumn(
                label = "sec",
                range = 0..59,
                selected = state.selectedSeconds,
                accentColor = accentColor,
                testTag = "wheel_seconds",
                onSelected = { s -> onPickerChanged(state.selectedHours, state.selectedMinutes, s) }
            )
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onStart,
            enabled = (state.selectedHours + state.selectedMinutes + state.selectedSeconds) > 0,
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = Color.Black
            ),
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .testTag("timer_start_button")
        ) {
            Text("Start Timer", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NumberWheelColumn(
    label: String,
    range: IntRange,
    selected: Int,
    accentColor: Color,
    testTag: String,
    onSelected: (Int) -> Unit
) {
    val values = remember(range) { range.toList() }
    val itemHeight = 30.dp
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selected.coerceIn(0, values.lastIndex))
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val centerIndex by remember {
        derivedStateOf {
            val offset = listState.firstVisibleItemScrollOffset
            val idx = listState.firstVisibleItemIndex + if (offset > 24) 1 else 0
            idx.coerceIn(0, values.lastIndex)
        }
    }

    LaunchedEffect(centerIndex) {
        val v = values[centerIndex]
        if (v != selected) onSelected(v)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(50.dp)
                .height(itemHeight * 3)
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            )

            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(vertical = itemHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.height(itemHeight * 3)
            ) {
                items(values) { num ->
                    val isCenter = num == values[centerIndex]
                    Box(
                        modifier = Modifier
                            .height(itemHeight)
                            .fillMaxWidth()
                            .clickable { onSelected(num) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format("%02d", num),
                            fontSize = if (isCenter) 19.sp else 14.sp,
                            fontWeight = if (isCenter) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isCenter) accentColor else IslandColors.TextSecondary.copy(alpha = 0.55f)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(3.dp))
        Text(label, fontSize = 11.sp, color = IslandColors.TextSecondary)
    }
}

@Composable
private fun StopwatchView(
    state: TimerUiState,
    accentColor: Color,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onLap: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = TimerEngine.formatStopwatch(state.stopwatchElapsedMs),
            style = TextStyle(
                color = IslandColors.TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                fontFeatureSettings = "tnum"
            ),
            modifier = Modifier.testTag("stopwatch_display")
        )

        Spacer(Modifier.height(6.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .clickable(onClick = if (state.isStopwatchRunning) onLap else onReset)
                    .testTag("stopwatch_lap_reset_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.isStopwatchRunning) Icons.Filled.Flag else Icons.Filled.Refresh,
                    contentDescription = if (state.isStopwatchRunning) "Lap" else "Reset",
                    tint = IslandColors.TextPrimary
                )
            }

            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor)
                    .clickable(onClick = if (state.isStopwatchRunning) onPause else onStart)
                    .testTag("stopwatch_start_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.isStopwatchRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isStopwatchRunning) "Pause stopwatch" else "Start stopwatch",
                    tint = Color.Black,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        if (state.laps.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                items(state.laps.withIndex().toList()) { (idx, lapMs) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Lap ${state.laps.size - idx}", color = IslandColors.TextSecondary, fontSize = 11.sp)
                        Text(
                            text = TimerEngine.formatStopwatch(lapMs),
                            style = TextStyle(color = IslandColors.TextPrimary, fontSize = 11.sp, fontFeatureSettings = "tnum")
                        )
                    }
                }
            }
        }
    }
}
