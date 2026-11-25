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
 * Roomy, high-contrast Music Card for Medium/Expanded Dynamic Island.
 * Features 56 dp album art on the left, 16 sp / 13 sp 2-line title + artist, live seekbar,
 * 44 dp+ transport controls, and a collapsed-by-default synced lyrics drawer.
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
        targetValue = if (state.isPlaying) 1.0f else 0.95f,
        animationSpec = MotionTokens.spatialDefault(),
        label = "art_scale"
    )
    val playBtnScaleState = animateFloatAsState(
        targetValue = if (state.isPlaying) 1.04f else 1.0f,
        animationSpec = MotionTokens.spatialFast(),
        label = "play_btn_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (state.hasActiveSession) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                effectiveAccent.copy(alpha = if (state.isPlaying) 0.22f else 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(44.dp.toPx(), 34.dp.toPx()),
                            radius = 120.dp.toPx()
                        ),
                        center = Offset(44.dp.toPx(), 34.dp.toPx()),
                        radius = 120.dp.toPx()
                    )
                }
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: 56dp Album Art + 2-line Title/Artist + Equalizer + Lyrics Toggle + Like
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenPlayer),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val artShape = if (rotateAlbumArt && state.isPlaying) CircleShape else RoundedCornerShape(14.dp)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer {
                        scaleX = artScaleState.value
                        scaleY = artScaleState.value
                        rotationZ = if (rotateAlbumArt) (artRotationState.value % 360f) else 0f
                    }
                    .clip(artShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.2.dp, effectiveAccent.copy(alpha = 0.78f), artShape),
                contentAlignment = Alignment.Center
            ) {
                if (state.albumArt != null) {
                    Image(
                        bitmap = state.albumArt.asImageBitmap(),
                        contentDescription = "Album art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = "Music",
                        tint = effectiveAccent,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = if (state.hasActiveSession) state.title else "No Active Music Session",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
                Text(
                    text = when {
                        !state.hasNotificationAccess -> "Tap to allow Notification Access"
                        state.hasActiveSession -> listOf(state.artist, state.appLabel)
                            .filter { it.isNotBlank() }
                            .joinToString(" · ")
                        else -> "Tap to open PixelMusic"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.width(8.dp))
            MusicLiveEqualizer(isPlaying = state.isPlaying, accentColor = effectiveAccent, positionMs = state.positionMs)

            IconButton(
                onClick = { showLyricsPane = !showLyricsPane },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("music_lyrics_toggle")
            ) {
                Icon(
                    imageVector = Icons.Filled.Lyrics,
                    contentDescription = "Toggle on-device lyrics",
                    tint = if (showLyricsPane) effectiveAccent else Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.size(20.dp)
                )
            }

            if (state.supportsLike) {
                IconButton(
                    onClick = onToggleLike,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("music_like_button")
                ) {
                    Icon(
                        imageVector = if (state.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like track",
                        tint = if (state.isLiked) IslandColors.CrimsonBright else Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Row 2: Live Seekbar OR Collapsed-by-default Synced Lyrics Drawer
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
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                        .clickable { showLyricsPane = false }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Lyrics, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "No lyrics found on device · Tap to hide",
                        color = Color.White.copy(alpha = 0.76f),
                        fontSize = 12.sp,
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

        // Row 3: Roomy 44dp+ Transport Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.supportsShuffle) {
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("music_shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Toggle shuffle",
                        tint = if (state.isShuffled) effectiveAccent else Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            IconButton(
                onClick = onPrevious,
                modifier = Modifier
                    .size(46.dp)
                    .testTag("music_prev_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous track",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        scaleX = playBtnScaleState.value
                        scaleY = playBtnScaleState.value
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(effectiveAccent, effectiveAccent.copy(alpha = 0.76f))
                        )
                    )
                    .border(1.2.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                    .clickable(onClick = onTogglePlayPause)
                    .testTag("music_play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(46.dp)
                    .testTag("music_next_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next track",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            if (state.supportsRepeat) {
                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("music_repeat_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Repeat,
                        contentDescription = "Cycle repeat (${state.repeatModeLabel})",
                        tint = if (state.repeatModeLabel != "Off") effectiveAccent else Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(20.dp)
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
            .height(68.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .clickable(onClick = onClose)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.18f to Color.Black,
                        0.82f to Color.Black,
                        1.0f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            itemsIndexed(lyrics) { index, line ->
                val isCurrent = index == activeIndex
                Text(
                    text = line.text,
                    color = if (isCurrent) accentColor else Color.White.copy(alpha = 0.62f),
                    fontSize = if (isCurrent) 13.sp else 11.sp,
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
            .height(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = TimerEngine.formatDuration(positionMs),
            style = TextStyle(fontSize = 11.sp, color = Color.White.copy(alpha = 0.72f), fontFeatureSettings = "tnum")
        )
        WavyProgressBar(
            progress = liveFraction,
            isActiveWave = isPlaying,
            accentColor = accentColor,
            height = 18.dp,
            interactive = true,
            onSeekFraction = { frac -> onSeekTo((frac * safeDuration).toLong()) },
            modifier = Modifier
                .weight(1f)
                .testTag("music_knob_seekbar")
        )
        Text(
            text = TimerEngine.formatDuration(durationMs),
            style = TextStyle(fontSize = 11.sp, color = Color.White.copy(alpha = 0.72f), fontFeatureSettings = "tnum")
        )
    }
}
