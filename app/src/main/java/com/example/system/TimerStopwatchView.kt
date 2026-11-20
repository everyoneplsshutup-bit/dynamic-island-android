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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IslandColors
import kotlin.math.PI
import kotlin.math.cos
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
    onTimerSetSound: () -> Unit = {}
) {
    var modeIndex by remember { mutableIntStateOf(0) } // 0 = Timer, 1 = Stopwatch

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(216.dp)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
        ) {
            listOf("Timer", "Stopwatch").forEachIndexed { idx, label ->
                SegmentedButton(
                    selected = modeIndex == idx,
                    onClick = { onHaptic(); onTimerSetSound(); modeIndex = idx },
                    shape = SegmentedButtonDefaults.itemShape(index = idx, count = 2),
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

        if (modeIndex == 0) {
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
        } else {
            StopwatchView(
                state = state,
                accentColor = accentColor,
                onStart = { onHaptic(); onTimerSetSound(); onStartStopwatch() },
                onPause = { onHaptic(); onTimerSetSound(); onPauseStopwatch() },
                onReset = { onHaptic(); onResetStopwatch() },
                onLap = { onHaptic(); onTimerSetSound(); onLapStopwatch() }
            )
        }
    }
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
