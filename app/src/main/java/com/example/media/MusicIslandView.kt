package com.example.media

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.system.TimerEngine
import com.example.ui.theme.IslandColors

/**
 * 120Hz RenderThread-only waveform using graphicsLayer scaleY (zero Measure/Layout passes per frame).
 */
@Composable
fun MusicCompactWaveform(isPlaying: Boolean, accentColor: Color) {
    if (!isPlaying) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(16.dp)
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.7f))
                )
            }
        }
        return
    }

    val infinite = rememberInfiniteTransition(label = "wave")
    val p1 by infinite.animateFloat(
        initialValue = 0.25f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "w1"
    )
    val p2 by infinite.animateFloat(
        initialValue = 0.9f, targetValue = 0.22f,
        animationSpec = infiniteRepeatable(tween(340, easing = LinearEasing), RepeatMode.Reverse),
        label = "w2"
    )
    val p3 by infinite.animateFloat(
        initialValue = 0.35f, targetValue = 0.96f,
        animationSpec = infiniteRepeatable(tween(490, easing = LinearEasing), RepeatMode.Reverse),
        label = "w3"
    )
    val p4 by infinite.animateFloat(
        initialValue = 0.8f, targetValue = 0.28f,
        animationSpec = infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Reverse),
        label = "w4"
    )
    val bars = listOf(p1, p2, p3, p4)

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(16.dp)
    ) {
        bars.forEach { frac ->
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(14.dp)
                    .graphicsLayer { scaleY = frac }
                    .clip(CircleShape)
                    .background(accentColor)
            )
        }
    }
}

@Composable
private fun MusicLiveEqualizer(isPlaying: Boolean, accentColor: Color) {
    val heights = if (isPlaying) {
        val infinite = rememberInfiniteTransition(label = "eq")
        val e1 by infinite.animateFloat(0.24f, 0.92f, infiniteRepeatable(tween(340, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "e1")
        val e2 by infinite.animateFloat(0.95f, 0.28f, infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "e2")
        val e3 by infinite.animateFloat(0.38f, 1.00f, infiniteRepeatable(tween(290, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "e3")
        val e4 by infinite.animateFloat(0.88f, 0.24f, infiniteRepeatable(tween(390, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "e4")
        val e5 by infinite.animateFloat(0.32f, 0.94f, infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "e5")
        val e6 by infinite.animateFloat(0.84f, 0.26f, infiniteRepeatable(tween(360, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "e6")
        listOf(e1, e2, e3, e4, e5, e6)
    } else {
        listOf(0.22f, 0.28f, 0.24f, 0.28f, 0.22f, 0.20f)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(20.dp)
    ) {
        heights.forEachIndexed { idx, frac ->
            val alpha = if (isPlaying) (0.72f + (idx % 2) * 0.28f) else 0.36f
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(18.dp)
                    .graphicsLayer { scaleY = frac }
                    .clip(CircleShape)
                    .background((if (isPlaying) accentColor else IslandColors.TextSecondary).copy(alpha = alpha))
            )
        }
    }
}

@Composable
fun MusicExpandedView(
    state: MediaUiState,
    accentColor: Color,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    // Artwork subtle RenderThread breathing pulse + soft ambient halo while playing
    val infinite = rememberInfiniteTransition(label = "art_pulse")
    val playingPulse by infinite.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(880, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse_val"
    )
    val artScale by animateFloatAsState(
        targetValue = if (state.isPlaying) playingPulse else 0.94f,
        animationSpec = spring(dampingRatio = 0.70f, stiffness = 420f),
        label = "art_scale"
    )
    val playBtnScale by animateFloatAsState(
        targetValue = if (state.isPlaying) 1.06f else 1.0f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 500f),
        label = "play_btn_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(216.dp)
            .drawBehind {
                if (state.hasActiveSession) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = if (state.isPlaying) 0.16f else 0.06f),
                                Color.Transparent
                            ),
                            center = Offset(38.dp.toPx(), 34.dp.toPx()),
                            radius = 120.dp.toPx()
                        ),
                        center = Offset(38.dp.toPx(), 34.dp.toPx()),
                        radius = 120.dp.toPx()
                    )
                }
            }
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header: Artwork + Track Info + Live Equalizer + Like
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenPlayer),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        scaleX = artScale
                        scaleY = artScale
                    }
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color(0xFF1F2028)),
                contentAlignment = Alignment.Center
            ) {
                if (state.albumArt != null) {
                    Image(
                        bitmap = state.albumArt.asImageBitmap(),
                        contentDescription = "Album art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = "Music",
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (state.hasActiveSession) state.title else "No Active Music Session",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 15.sp,
                    color = IslandColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        !state.hasNotificationAccess -> "Enable Notification Access for PixelMusic control"
                        state.hasActiveSession -> listOf(state.artist, state.appLabel)
                            .filter { it.isNotBlank() }
                            .joinToString(" · ")
                        else -> "Start playing in PixelMusic, Spotify, or YT Music"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = IslandColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))
            MusicLiveEqualizer(isPlaying = state.isPlaying, accentColor = accentColor)

            if (state.supportsLike) {
                IconButton(
                    onClick = onToggleLike,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("music_like_button")
                ) {
                    Icon(
                        imageVector = if (state.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like track",
                        tint = if (state.isLiked) IslandColors.AccentRed else IslandColors.TextSecondary
                    )
                }
            }
        }

        // Live synced lyrics block (compactly capped to fit inside 216.dp)
        if (state.lyrics.isNotEmpty()) {
            SyncedLyricsPanel(
                lyrics = state.lyrics,
                positionMs = state.positionMs,
                accentColor = accentColor
            )
        }

        // Knob Seek Bar (draggable knob on track + current / total time at ends)
        KnobSeekBar(
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            accentColor = accentColor,
            onSeekTo = onSeekTo
        )

        // Transport Controls Row (Shuffle/Repeat gracefully hidden if not offered)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.supportsShuffle) {
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("music_shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Toggle shuffle",
                        tint = if (state.isShuffled) accentColor else IslandColors.TextSecondary
                    )
                }
            }

            IconButton(
                onClick = onPrevious,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("music_prev_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous track",
                    tint = IslandColors.TextPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        scaleX = playBtnScale
                        scaleY = playBtnScale
                    }
                    .clip(CircleShape)
                    .background(if (state.isPlaying) accentColor.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.16f))
                    .clickable(onClick = onTogglePlayPause)
                    .testTag("music_play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = if (state.isPlaying) accentColor else IslandColors.TextPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("music_next_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next track",
                    tint = IslandColors.TextPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            if (state.supportsRepeat) {
                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("music_repeat_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Repeat,
                        contentDescription = "Cycle repeat (${state.repeatModeLabel})",
                        tint = if (state.repeatModeLabel != "Off") accentColor else IslandColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncedLyricsPanel(
    lyrics: List<LyricLine>,
    positionMs: Long,
    accentColor: Color
) {
    val activeIndex = remember(lyrics, positionMs) {
        val timed = lyrics.indexOfLast { it.timestampMs in 0..positionMs }
        if (timed >= 0) timed else 0
    }
    val listState = rememberLazyListState()
    LaunchedEffect(activeIndex) {
        if (activeIndex in lyrics.indices) {
            listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        LazyColumn(state = listState) {
            itemsIndexed(lyrics) { index, line ->
                val isCurrent = index == activeIndex
                Text(
                    text = line.text,
                    color = if (isCurrent) accentColor else IslandColors.TextSecondary.copy(alpha = 0.65f),
                    fontSize = if (isCurrent) 12.sp else 11.sp,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
fun KnobSeekBar(
    positionMs: Long,
    durationMs: Long,
    accentColor: Color,
    onSeekTo: (Long) -> Unit
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    val liveFraction = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(liveFraction) }
    var widthPx by remember { mutableFloatStateOf(1f) }

    val displayFraction = if (isDragging) dragFraction else liveFraction
    val displayedPositionMs = (displayFraction * safeDuration).toLong()

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(safeDuration) {
                    detectTapGestures { offset ->
                        val frac = (offset.x / widthPx).coerceIn(0f, 1f)
                        onSeekTo((frac * safeDuration).toLong())
                    }
                }
                .pointerInput(safeDuration) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragFraction = (offset.x / widthPx).coerceIn(0f, 1f)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragFraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            onSeekTo((dragFraction * safeDuration).toLong())
                            isDragging = false
                        },
                        onDragCancel = { isDragging = false }
                    )
                }
                .testTag("music_knob_seekbar")
        ) {
            val cy = size.height / 2f
            val pad = 8.dp.toPx()
            val trackStart = pad
            val trackEnd = (size.width - pad).coerceAtLeast(pad + 1f)
            val knobX = trackStart + (trackEnd - trackStart) * displayFraction

            drawLine(
                color = Color.White.copy(alpha = 0.22f),
                start = Offset(trackStart, cy),
                end = Offset(trackEnd, cy),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = accentColor,
                start = Offset(trackStart, cy),
                end = Offset(knobX, cy),
                strokeWidth = if (isDragging) 5.dp.toPx() else 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            if (isDragging) {
                drawCircle(
                    color = accentColor.copy(alpha = 0.30f),
                    radius = 13.dp.toPx(),
                    center = Offset(knobX, cy)
                )
            }
            drawCircle(
                color = Color.White,
                radius = if (isDragging) 8.dp.toPx() else 6.5.dp.toPx(),
                center = Offset(knobX, cy)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = TimerEngine.formatDuration(displayedPositionMs),
                style = TextStyle(fontSize = 11.sp, color = IslandColors.TextSecondary, fontFeatureSettings = "tnum")
            )
            Text(
                text = TimerEngine.formatDuration(durationMs),
                style = TextStyle(fontSize = 11.sp, color = IslandColors.TextSecondary, fontFeatureSettings = "tnum")
            )
        }
    }
}
