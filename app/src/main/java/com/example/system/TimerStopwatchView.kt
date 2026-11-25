package com.example.system

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.ui.theme.MotionTokens
import com.example.ui.theme.WavyProgressBar
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Roomy Timer, Stopwatch & Sleep Dial card for the Dynamic Island pill.
 * Features clear Timer / Stopwatch / Sleep tabs, 32sp+ tabular digits,
 * full Start pill, and 44dp+ touch targets.
 */
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
    val crimsonAccent = accentColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(0.75.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Timer", "Stopwatch", "Sleep").forEachIndexed { idx, label ->
                val selected = modeIndex == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .clip(CircleShape)
                        .background(if (selected) crimsonAccent.copy(alpha = 0.34f) else Color.Transparent)
                        .clickable { onHaptic(); onTimerSetSound(); modeIndex = idx }
                        .testTag("timer_mode_${label.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) Color.White else Color.White.copy(alpha = 0.72f),
                        maxLines = 1
                    )
                }
            }
        }

        when (modeIndex) {
            0 -> {
                if (state.isActive) {
                    ActiveTimerRingView(
                        state = state,
                        accentColor = crimsonAccent,
                        onPause = { onHaptic(); onTimerSetSound(); onPauseTimer() },
                        onResume = { onHaptic(); onTimerSetSound(); onResumeTimer() },
                        onCancel = { onHaptic(); onCancelTimer() }
                    )
                } else {
                    WheelTimerPickerView(
                        state = state,
                        accentColor = crimsonAccent,
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
            }

            1 -> {
                StopwatchView(
                    state = state,
                    accentColor = crimsonAccent,
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

@Composable
private fun SleepDialView(
    accentColor: Color,
    onHaptic: () -> Unit,
    onStartSleepTimer: (Long) -> Unit
) {
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
            .height(92.dp)
            .testTag("sleep_dial_panel"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { start ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val r = (minOf(size.width, size.height) / 2f) - 7.dp.toPx()
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
                            if (draggingKnob == 1 && snappedMin != bedtimeMin) {
                                bedtimeMin = snappedMin
                                onHaptic()
                            } else if (draggingKnob == 2 && snappedMin != wakeMin) {
                                wakeMin = snappedMin
                                onHaptic()
                            }
                        },
                        onDragEnd = { draggingKnob = 0 },
                        onDragCancel = { draggingKnob = 0 }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(80.dp)) {
                val strokeW = 7.dp.toPx()
                val r = (size.minDimension - strokeW - 6.dp.toPx()) / 2f
                val cx = size.width / 2f
                val cy = size.height / 2f

                drawCircle(
                    color = Color.White.copy(alpha = 0.12f),
                    radius = r,
                    style = Stroke(width = strokeW)
                )

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

                drawCircle(color = IslandColors.AccentPurple, radius = 5.dp.toPx(), center = bedPos)
                drawCircle(color = accentColor, radius = 5.dp.toPx(), center = wakePos)
            }

            Text(
                text = "${sleepHours}h${sleepRemMin}m",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                modifier = Modifier.testTag("sleep_dial_duration_text")
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(Icons.Filled.Bedtime, contentDescription = null, tint = IslandColors.AccentPurple, modifier = Modifier.size(16.dp))
                Text(
                    text = "${formatClockMinutes(bedtimeMin)} → ${formatClockMinutes(wakeMin)}",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum"
                    ),
                    maxLines = 1
                )
            }
            Text(
                text = "Drag dial · ${sleepHours}h ${sleepRemMin}m sleep",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 12.sp,
                maxLines = 1
            )
        }

        Button(
            onClick = { onStartSleepTimer(durationMs) },
            enabled = sleepMinutes > 0,
            colors = ButtonDefaults.buttonColors(containerColor = IslandColors.AccentPurple, contentColor = Color.White),
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            modifier = Modifier
                .height(44.dp)
                .testTag("sleep_start_button")
        ) {
            Text("Start", fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
    val urgentPulseState = animateFloatAsState(
        targetValue = if (isUrgent) 1.05f else 1.0f,
        animationSpec = MotionTokens.spatialFast(),
        label = "urgent_scale"
    )
    val formattedText = TimerEngine.formatDuration(state.remainingMs)
    val ringColor = if (isUrgent) IslandColors.AccentRed else accentColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer {
                        scaleX = urgentPulseState.value
                        scaleY = urgentPulseState.value
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(52.dp)) {
                    val strokeW = 4.5.dp.toPx()
                    val stroke = Stroke(width = strokeW, cap = StrokeCap.Round)
                    drawArc(
                        color = Color.White.copy(alpha = 0.14f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = stroke
                    )
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = stroke
                    )
                }
                Text(
                    text = "${(progress * 100).roundToInt()}%",
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AnimatedContent(
                    targetState = formattedText,
                    transitionSpec = {
                        fadeIn(tween(MotionTokens.TIMER_TICK_CROSSFADE_MS)) togetherWith
                            fadeOut(tween(MotionTokens.TIMER_TICK_CROSSFADE_MS))
                    },
                    label = "timer_tick_crossfade"
                ) { textVal ->
                    Text(
                        text = textVal,
                        style = TextStyle(
                            color = if (isUrgent) IslandColors.AccentRed else Color.White,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        maxLines = 1,
                        modifier = Modifier.testTag("timer_remaining_text")
                    )
                }
                if (state.mirroredClockLabel != null && state.mirroredRemainingMs != null) {
                    Text(
                        text = "${state.mirroredClockLabel} ${TimerEngine.formatDuration(state.mirroredRemainingMs)}",
                        color = ringColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = if (state.isRunning) state.label.ifBlank { "Counting down" } else "Paused",
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(ringColor, ringColor.copy(alpha = 0.74f))
                        )
                    )
                    .border(1.2.dp, Color.White.copy(alpha = 0.50f), CircleShape)
                    .clickable(onClick = if (state.isRunning) onPause else onResume)
                    .testTag("timer_pause_resume_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isRunning) "Pause timer" else "Resume timer",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, Color.White.copy(alpha = 0.26f), CircleShape)
                    .clickable(onClick = onCancel)
                    .testTag("timer_cancel_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Cancel timer", tint = Color.White, modifier = Modifier.size(20.dp))
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.07f))
                .border(0.75.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NumberWheelColumn(
                label = "h",
                range = 0..23,
                selected = state.selectedHours,
                accentColor = accentColor,
                testTag = "wheel_hours",
                onSelected = { h -> onPickerChanged(h, state.selectedMinutes, state.selectedSeconds) }
            )
            NumberWheelColumn(
                label = "m",
                range = 0..59,
                selected = state.selectedMinutes,
                accentColor = accentColor,
                testTag = "wheel_minutes",
                onSelected = { m -> onPickerChanged(state.selectedHours, m, state.selectedSeconds) }
            )
            NumberWheelColumn(
                label = "s",
                range = 0..59,
                selected = state.selectedSeconds,
                accentColor = accentColor,
                testTag = "wheel_seconds",
                onSelected = { s -> onPickerChanged(state.selectedHours, state.selectedMinutes, s) }
            )
        }

        Button(
            onClick = onStart,
            enabled = (state.selectedHours + state.selectedMinutes + state.selectedSeconds) > 0,
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = Color.White
            ),
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            modifier = Modifier
                .height(46.dp)
                .testTag("timer_start_button")
        ) {
            Text("Start", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
    val itemHeight = 26.dp
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selected.coerceIn(0, values.lastIndex))
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val centerIndex by remember {
        derivedStateOf {
            val offset = listState.firstVisibleItemScrollOffset
            val idx = listState.firstVisibleItemIndex + if (offset > 22) 1 else 0
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
                .width(42.dp)
                .height(itemHeight * 3)
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.10f))
            )

            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(vertical = itemHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.height(itemHeight * 3)
            ) {
                items(values.size, key = { values[it] }) { idx ->
                    val num = values[idx]
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
                            fontSize = if (isCenter) 20.sp else 12.sp,
                            fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCenter) accentColor else Color.White.copy(alpha = 0.50f)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(3.dp))
        Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.72f), fontWeight = FontWeight.Medium)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = TimerEngine.formatStopwatch(state.stopwatchElapsedMs),
                style = TextStyle(
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Light,
                    fontFeatureSettings = "tnum"
                ),
                maxLines = 1,
                modifier = Modifier.testTag("stopwatch_display")
            )
            Text(
                text = if (state.laps.isNotEmpty()) {
                    "Lap ${state.laps.size} · ${TimerEngine.formatStopwatch(state.laps.first())}"
                } else {
                    if (state.isStopwatchRunning) "Running" else "Tap Start to time"
                },
                style = TextStyle(color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp, fontFeatureSettings = "tnum")
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, Color.White.copy(alpha = 0.26f), CircleShape)
                    .clickable(onClick = if (state.isStopwatchRunning) onLap else onReset)
                    .testTag("stopwatch_lap_reset_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.isStopwatchRunning) Icons.Filled.Flag else Icons.Filled.Refresh,
                    contentDescription = if (state.isStopwatchRunning) "Lap" else "Reset",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(accentColor, accentColor.copy(alpha = 0.76f))
                        )
                    )
                    .border(1.2.dp, Color.White.copy(alpha = 0.50f), CircleShape)
                    .clickable(onClick = if (state.isStopwatchRunning) onPause else onStart)
                    .testTag("stopwatch_start_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.isStopwatchRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isStopwatchRunning) "Pause stopwatch" else "Start stopwatch",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
