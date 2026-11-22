package com.example.system

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
 * Compact Build #5 Timer, Stopwatch & Sleep Dial capped strictly at <= 124 dp.
 * Timer tick updates ONLY the tabular numeral text with a 150 ms crossfade; container never re-animates.
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
    val orangeAccent = IslandColors.AccentOrange

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 124.dp)
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
        ) {
            listOf("Timer", "Stopwatch", "Sleep").forEachIndexed { idx, label ->
                SegmentedButton(
                    selected = modeIndex == idx,
                    onClick = { onHaptic(); onTimerSetSound(); modeIndex = idx },
                    shape = SegmentedButtonDefaults.itemShape(index = idx, count = 3),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = orangeAccent.copy(alpha = 0.25f),
                        activeContentColor = orangeAccent,
                        inactiveContainerColor = Color.White.copy(alpha = 0.06f),
                        inactiveContentColor = IslandColors.TextSecondary
                    ),
                    modifier = Modifier.testTag("timer_mode_${label.lowercase()}")
                ) {
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        when (modeIndex) {
            0 -> {
                if (state.isActive) {
                    ActiveTimerRingView(
                        state = state,
                        accentColor = orangeAccent,
                        onPause = { onHaptic(); onTimerSetSound(); onPauseTimer() },
                        onResume = { onHaptic(); onTimerSetSound(); onResumeTimer() },
                        onCancel = { onHaptic(); onCancelTimer() }
                    )
                } else {
                    WheelTimerPickerView(
                        state = state,
                        accentColor = orangeAccent,
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
                    accentColor = orangeAccent,
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
 * Interactive 24h circular Bedtime-to-Wake Sleep Dial compactly sized to <= 88 dp row inside the 124 dp panel.
 */
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
            .height(86.dp)
            .testTag("sleep_dial_panel"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { start ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val r = (minOf(size.width, size.height) / 2f) - 8.dp.toPx()
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
            Canvas(modifier = Modifier.size(82.dp)) {
                val strokeW = 8.dp.toPx()
                val r = (size.minDimension - strokeW - 6.dp.toPx()) / 2f
                val cx = size.width / 2f
                val cy = size.height / 2f

                drawCircle(
                    color = Color.White.copy(alpha = 0.09f),
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

                drawCircle(color = IslandColors.AccentPurple, radius = 5.5.dp.toPx(), center = bedPos)
                drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = bedPos)
                drawCircle(color = accentColor, radius = 5.5.dp.toPx(), center = wakePos)
                drawCircle(color = Color.Black, radius = 2.5.dp.toPx(), center = wakePos)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Bedtime, contentDescription = null, tint = IslandColors.AccentPurple, modifier = Modifier.size(12.dp))
                Text(
                    text = "${sleepHours}h${sleepRemMin}m",
                    style = TextStyle(
                        color = IslandColors.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    ),
                    modifier = Modifier.testTag("sleep_dial_duration_text")
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("BED", color = IslandColors.AccentPurple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        formatClockMinutes(bedtimeMin),
                        style = TextStyle(color = IslandColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("WAKE", color = accentColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        formatClockMinutes(wakeMin),
                        style = TextStyle(color = IslandColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")
                    )
                }
            }

            Button(
                onClick = { onStartSleepTimer(durationMs) },
                enabled = sleepMinutes > 0,
                colors = ButtonDefaults.buttonColors(containerColor = IslandColors.AccentPurple, contentColor = Color.White),
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .testTag("sleep_start_button")
            ) {
                Text("Start Sleep", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
    val formattedText = TimerEngine.formatDuration(state.remainingMs)
    val ringColor = if (isUrgent) IslandColors.AccentRed else accentColor

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .graphicsLayer {
                        scaleX = urgentPulse
                        scaleY = urgentPulse
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(54.dp)) {
                    val strokeW = 5.dp.toPx()
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
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = stroke
                    )
                }
                Text(
                    text = "${(progress * 100).roundToInt()}%",
                    color = ringColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.label.ifBlank { "Timer" },
                    color = IslandColors.TextSecondary,
                    fontSize = 10.sp
                )
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
                            color = if (isUrgent) IslandColors.AccentRed else IslandColors.TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum"
                        ),
                        modifier = Modifier.testTag("timer_remaining_text")
                    )
                }
                WavyProgressBar(
                    progress = progress,
                    isActiveWave = state.isRunning,
                    accentColor = ringColor,
                    height = 12.dp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable(onClick = onCancel)
                        .testTag("timer_cancel_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel timer", tint = IslandColors.TextPrimary, modifier = Modifier.size(18.dp))
                }

                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(44.dp)
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
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        if (state.mirroredClockLabel != null && state.mirroredRemainingMs != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Clock: ${state.mirroredClockLabel}", color = IslandColors.TextSecondary, fontSize = 10.sp)
                Text(
                    TimerEngine.formatDuration(state.mirroredRemainingMs),
                    color = accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(horizontal = 4.dp, vertical = 2.dp),
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
                contentColor = Color.Black
            ),
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            modifier = Modifier
                .height(48.dp)
                .testTag("timer_start_button")
        ) {
            Text("Start", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
    val itemHeight = 25.dp
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selected.coerceIn(0, values.lastIndex))
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val centerIndex by remember {
        derivedStateOf {
            val offset = listState.firstVisibleItemScrollOffset
            val idx = listState.firstVisibleItemIndex + if (offset > 20) 1 else 0
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
                .width(38.dp)
                .height(itemHeight * 3)
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.08f))
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
                            fontSize = if (isCenter) 15.sp else 11.sp,
                            fontWeight = if (isCenter) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isCenter) accentColor else IslandColors.TextSecondary.copy(alpha = 0.55f)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(2.dp))
        Text(label, fontSize = 10.sp, color = IslandColors.TextSecondary)
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
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = TimerEngine.formatStopwatch(state.stopwatchElapsedMs),
                    style = TextStyle(
                        color = IslandColors.TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Light,
                        fontFeatureSettings = "tnum"
                    ),
                    modifier = Modifier.testTag("stopwatch_display")
                )
                if (state.laps.isNotEmpty()) {
                    Text(
                        text = "Lap ${state.laps.size} · ${TimerEngine.formatStopwatch(state.laps.first())}",
                        style = TextStyle(color = IslandColors.TextSecondary, fontSize = 11.sp, fontFeatureSettings = "tnum")
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable(onClick = if (state.isStopwatchRunning) onLap else onReset)
                        .testTag("stopwatch_lap_reset_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isStopwatchRunning) Icons.Filled.Flag else Icons.Filled.Refresh,
                        contentDescription = if (state.isStopwatchRunning) "Lap" else "Reset",
                        tint = IslandColors.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(44.dp)
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
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
