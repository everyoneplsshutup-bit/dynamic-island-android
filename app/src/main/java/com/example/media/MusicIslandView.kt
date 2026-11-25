package com.example.media

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Lyrics
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.system.TimerEngine
import com.example.ui.theme.IslandColors
import com.example.ui.theme.MotionTokens
import com.example.ui.theme.WavyProgressBar

/**
 * Visualizer bars driven deterministically by playback position using graphicsLayer scaleY only.
 * Zero Measure/Layout passes per frame and zero infinite animation loops.
 */
@Composable
fun MusicCompactWaveform(isPlaying: Boolean, accentColor: Color, positionMs: Long = 0L) {
    if (!isPlaying) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(14.dp)
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.65f))
                )
            }
        }
        return
    }

    val step = ((positionMs / 160L).toInt() and 0x7FFFFFFF)
    val p0State = animateFloatAsState(
        targetValue = 0.34f + 0.66f * (((step * 3 + 1) % 7) / 6f),
        animationSpec = MotionTokens.spatialFast(),
        label = "cw0"
    )
    val p1State = animateFloatAsState(
        targetValue = 0.30f + 0.70f * (((step * 5 + 3) % 7) / 6f),
        animationSpec = MotionTokens.spatialFast(),
        label = "cw1"
    )
    val p2State = animateFloatAsState(
        targetValue = 0.36f + 0.64f * (((step * 2 + 5) % 7) / 6f),
        animationSpec = MotionTokens.spatialFast(),
        label = "cw2"
    )
    val p3State = animateFloatAsState(
        targetValue = 0.28f + 0.72f * (((step * 4 + 2) % 7) / 6f),
        animationSpec = MotionTokens.spatialFast(),
        label = "cw3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(14.dp)
    ) {
        listOf(p0State, p1State, p2State, p3State).forEach { fracState ->
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(13.dp)
                    .graphicsLayer { scaleY = fracState.value }
                    .clip(CircleShape)
                    .background(accentColor)
            )
        }
    }
}

@Composable
private fun MusicLiveEqualizer(isPlaying: Boolean, accentColor: Color, positionMs: Long = 0L) {
    val step = ((positionMs / 160L).toInt() and 0x7FFFFFFF)
    val targets = if (isPlaying) {
        listOf(
            0.30f + 0.64f * (((step * 3 + 0) % 8) / 7f),
            0.36f + 0.64f * (((step * 5 + 2) % 8) / 7f),
            0.32f + 0.68f * (((step * 7 + 4) % 8) / 7f),
            0.28f + 0.66f * (((step * 2 + 6) % 8) / 7f)
        )
    } else {
        listOf(0.22f, 0.28f, 0.24f, 0.20f)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(16.dp)
    ) {
        targets.forEachIndexed { idx, targetFrac ->
            val animFracState = animateFloatAsState(
                targetValue = targetFrac,
                animationSpec = MotionTokens.spatialFast(),
                label = "eq_$idx"
            )
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(15.dp)
                    .graphicsLayer { scaleY = animFracState.value }
                    .clip(CircleShape)
                    .background((if (isPlaying) accentColor else IslandColors.TextSecondary).copy(alpha = if (isPlaying) 0.88f else 0.36f))
            )
        }
    }
}

/**
 * Compact Music Card strictly capped at <= 78 dp height inside the 160 dp expanded pill.
 * Features rotating circular album art driven by track position (pauses when paused; toggleable via [rotateAlbumArt]),
 * RenderThread M3E WavyProgressBar seekbar, equalizer bars, and on-device lyrics + designed empty state.
 */
@Composable
fun MusicExpandedView(
    state: MediaUiState,
    accentColor: Color,
    rotateAlbumArt: Boolean = true,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    val effectiveAccent = remember(state.artworkAccentArgb, accentColor) {
        state.artworkAccentArgb?.let { Color(it) } ?: accentColor
    }
    var showLyricsPane by remember { mutableStateOf(false) }

    var lastArtDegrees by remember(state.title) { mutableFloatStateOf(0f) }
    if (state.isPlaying && rotateAlbumArt) {
        lastArtDegrees = (state.positionMs / 35f)
    }
    val artRotationState = animateFloatAsState(
        targetValue = if (rotateAlbumArt) lastArtDegrees else 0f,
        animationSpec = MotionTokens.effectsDefault(),
        label = "art_rotation"
    )

    val artScaleState = animateFloatAsState(
        targetValue = if (state.isPlaying) 1.0f else 0.94f,
        animationSpec = MotionTokens.spatialDefault(),
        label = "art_scale"
    )
    val playBtnScaleState = animateFloatAsState(
        targetValue = if (state.isPlaying) 1.05f else 1.0f,
        animationSpec = MotionTokens.spatialFast(),
        label = "play_btn_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 78.dp)
            .drawBehind {
                if (state.hasActiveSession) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                effectiveAccent.copy(alpha = if (state.isPlaying) 0.20f else 0.07f),
                                Color.Transparent
                            ),
                            center = Offset(28.dp.toPx(), 18.dp.toPx()),
                            radius = 88.dp.toPx()
                        ),
                        center = Offset(28.dp.toPx(), 18.dp.toPx()),
                        radius = 88.dp.toPx()
                    )
                }
            }
            .padding(horizontal = 12.dp, vertical = 1.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Row 1 (28dp): Album Art + Title/Artist + Visualizer + Lyrics Toggle + Like
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clickable(onClick = onOpenPlayer),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer {
                        scaleX = artScaleState.value
                        scaleY = artScaleState.value
                        rotationZ = if (rotateAlbumArt) (artRotationState.value % 360f) else 0f
                    }
                    .clip(if (rotateAlbumArt) CircleShape else RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(
                        1.dp,
                        effectiveAccent.copy(alpha = 0.72f),
                        if (rotateAlbumArt) CircleShape else RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (state.albumArt != null) {
                    Image(
                        bitmap = state.albumArt.asImageBitmap(),
                        contentDescription = "Album art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = "Music",
                        tint = effectiveAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (state.hasActiveSession) state.title else "No Active Music Session",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = IslandColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
                Text(
                    text = when {
                        !state.hasNotificationAccess -> "Allow Notification Access"
                        state.hasActiveSession -> listOf(state.artist, state.appLabel)
                            .filter { it.isNotBlank() }
                            .joinToString(" · ")
                        else -> "Play music in your selected player"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 10.sp,
                    color = IslandColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 12.sp
                )
            }

            Spacer(Modifier.width(4.dp))
            MusicLiveEqualizer(isPlaying = state.isPlaying, accentColor = effectiveAccent, positionMs = state.positionMs)

            IconButton(
                onClick = { showLyricsPane = !showLyricsPane },
                modifier = Modifier
                    .size(26.dp)
                    .testTag("music_lyrics_toggle")
            ) {
                Icon(
                    imageVector = Icons.Filled.Lyrics,
                    contentDescription = "Toggle on-device lyrics",
                    tint = if (showLyricsPane) effectiveAccent else IslandColors.TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }

            if (state.supportsLike) {
                IconButton(
                    onClick = onToggleLike,
                    modifier = Modifier
                        .size(26.dp)
                        .testTag("music_like_button")
                ) {
                    Icon(
                        imageVector = if (state.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like track",
                        tint = if (state.isLiked) IslandColors.CrimsonBright else IslandColors.TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Row 2 (16dp): Inline Wavy Seekbar OR On-Device Lyrics
        if (showLyricsPane) {
            if (state.lyrics.isNotEmpty()) {
                SyncedLyricsPanel(
                    lyrics = state.lyrics,
                    positionMs = state.positionMs,
                    accentColor = effectiveAccent,
                    onSeekTo = onSeekTo,
                    onClose = { showLyricsPane = false }
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { showLyricsPane = false }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Lyrics, contentDescription = null, tint = IslandColors.TextTertiary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "No lyrics on device",
                        color = IslandColors.TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            KnobSeekBar(
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                isPlaying = state.isPlaying,
                accentColor = effectiveAccent,
                onSeekTo = onSeekTo
            )
        }

        // Row 3 (28dp): Compact Transport Controls with Crimson Play/Pause center circle (matching mockup 2c)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.supportsShuffle) {
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("music_shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Toggle shuffle",
                        tint = if (state.isShuffled) effectiveAccent else IslandColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            IconButton(
                onClick = onPrevious,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("music_prev_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous track",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer {
                        scaleX = playBtnScaleState.value
                        scaleY = playBtnScaleState.value
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(effectiveAccent, effectiveAccent.copy(alpha = 0.72f))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                    .clickable(onClick = onTogglePlayPause)
                    .testTag("music_play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("music_next_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next track",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (state.supportsRepeat) {
                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("music_repeat_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Repeat,
                        contentDescription = "Cycle repeat (${state.repeatModeLabel})",
                        tint = if (state.repeatModeLabel != "Off") effectiveAccent else IslandColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
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
    accentColor: Color,
    onSeekTo: (Long) -> Unit,
    onClose: () -> Unit
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
            .height(20.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(onClick = onClose)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.2f to Color.Black,
                        0.8f to Color.Black,
                        1.0f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
            .padding(horizontal = 8.dp)
    ) {
        LazyColumn(state = listState) {
            itemsIndexed(lyrics) { index, line ->
                val isCurrent = index == activeIndex
                Text(
                    text = line.text,
                    color = if (isCurrent) accentColor else IslandColors.TextSecondary.copy(alpha = 0.6f),
                    fontSize = if (isCurrent) 11.sp else 9.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (line.timestampMs >= 0L) onSeekTo(line.timestampMs) else onClose()
                        }
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
    isPlaying: Boolean = true,
    onSeekTo: (Long) -> Unit
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    val liveFraction = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = TimerEngine.formatDuration(positionMs),
            style = TextStyle(fontSize = 9.sp, color = IslandColors.TextSecondary, fontFeatureSettings = "tnum")
        )
        WavyProgressBar(
            progress = liveFraction,
            isActiveWave = isPlaying,
            accentColor = accentColor,
            height = 12.dp,
            interactive = true,
            onSeekFraction = { frac -> onSeekTo((frac * safeDuration).toLong()) },
            modifier = Modifier
                .weight(1f)
                .testTag("music_knob_seekbar")
        )
        Text(
            text = TimerEngine.formatDuration(durationMs),
            style = TextStyle(fontSize = 9.sp, color = IslandColors.TextSecondary, fontFeatureSettings = "tnum")
        )
    }
}
