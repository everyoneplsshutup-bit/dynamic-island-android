package com.example.media

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
import kotlinx.coroutines.delay

/**
 * Pure-Canvas visualizer bars for the short music pill (no title text, zero layout passes).
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun MusicCompactWaveform(isPlaying: Boolean, accentColor: Color, positionMs: Long = 0L) {
    val heights: FloatArray = if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "compact_waveform")
        val f0 by transition.animateFloat(
            initialValue = 0.28f,
            targetValue = 0.98f,
            animationSpec = infiniteRepeatable(tween(520, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "cw0"
        )
        val f1 by transition.animateFloat(
            initialValue = 0.88f,
            targetValue = 0.30f,
            animationSpec = infiniteRepeatable(tween(640, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "cw1"
        )
        val f2 by transition.animateFloat(
            initialValue = 0.34f,
            targetValue = 0.94f,
            animationSpec = infiniteRepeatable(tween(580, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "cw2"
        )
        val f3 by transition.animateFloat(
            initialValue = 0.78f,
            targetValue = 0.26f,
            animationSpec = infiniteRepeatable(tween(720, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "cw3"
        )
        floatArrayOf(f0, f1, f2, f3)
    } else {
        floatArrayOf(0.24f, 0.28f, 0.26f, 0.22f)
    }

    Canvas(
        modifier = Modifier
            .width(22.dp)
            .height(14.dp)
    ) {
        val barW = 2.6.dp.toPx()
        val gap = (size.width - barW * 4f) / 3f
        val color = if (isPlaying) accentColor else accentColor.copy(alpha = 0.55f)
        for (i in 0 until 4) {
            val h = (size.height * heights[i]).coerceAtLeast(3.dp.toPx())
            val x = i * (barW + gap)
            val y = (size.height - h) / 2f
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
            )
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun MusicLiveEqualizer(isPlaying: Boolean, accentColor: Color, positionMs: Long = 0L) {
    val heights: FloatArray = if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "live_eq")
        val f0 by transition.animateFloat(
            initialValue = 0.26f,
            targetValue = 0.96f,
            animationSpec = infiniteRepeatable(tween(540, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "eq0"
        )
        val f1 by transition.animateFloat(
            initialValue = 0.90f,
            targetValue = 0.28f,
            animationSpec = infiniteRepeatable(tween(660, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "eq1"
        )
        val f2 by transition.animateFloat(
            initialValue = 0.32f,
            targetValue = 0.92f,
            animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "eq2"
        )
        val f3 by transition.animateFloat(
            initialValue = 0.80f,
            targetValue = 0.24f,
            animationSpec = infiniteRepeatable(tween(780, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "eq3"
        )
        floatArrayOf(f0, f1, f2, f3)
    } else {
        floatArrayOf(0.22f, 0.28f, 0.24f, 0.20f)
    }

    Canvas(
        modifier = Modifier
            .width(20.dp)
            .height(16.dp)
    ) {
        val barW = 2.5.dp.toPx()
        val gap = (size.width - barW * 4f) / 3f
        val color = (if (isPlaying) accentColor else IslandColors.TextSecondary)
            .copy(alpha = if (isPlaying) 0.90f else 0.40f)
        for (i in 0 until 4) {
            val h = (size.height * heights[i]).coerceAtLeast(3.dp.toPx())
            val x = i * (barW + gap)
            val y = (size.height - h) / 2f
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
            )
        }
    }
}

/**
 * Expanded Music Card (§0, §1, §4):
 * - 56 dp album art with -12° -> 0° spin-settle when track starts
 * - 16 sp semibold white title up to 2 lines, artist on its own 13 sp line
 * - Opt-in marquee ONLY inside expanded music card (1500 ms dwell, 40 dp/s, 2000 ms end pause, stops on pause/touch)
 * - Lyrics pane collapsed by default (toggled ONLY by the Lyrics icon button)
 * - Honest LRCLIB vs offline/missing lyrics copy
 */
@Composable
fun MusicExpandedView(
    state: MediaUiState,
    accentColor: Color,
    rotateAlbumArt: Boolean = true,
    expandedMarqueeEnabled: Boolean = false,
    lyricsOffsetMs: Int = 0,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenLyricsSettings: () -> Unit = {}
) {
    val effectiveAccent = remember(state.artworkAccentArgb, accentColor) {
        state.artworkAccentArgb?.let { Color(it) } ?: accentColor
    }
    var showLyricsPane by remember(state.hasActiveSession) { mutableStateOf(false) }
    var showLearnMoreDialog by remember { mutableStateOf(false) }
    var userTouchingTitle by remember { mutableStateOf(false) }

    // Track-start -12° -> 0° spin-settle + optional continuous disc rotation
    val introSpin = remember(state.title, state.isPlaying) { Animatable(if (state.isPlaying) -12f else 0f) }
    LaunchedEffect(state.title, state.isPlaying) {
        if (state.isPlaying) {
            introSpin.animateTo(0f, animationSpec = MotionTokens.appleSpring())
        } else {
            introSpin.snapTo(0f)
        }
    }

    var lastArtDegrees by remember(state.title) { mutableFloatStateOf(0f) }
    if (state.isPlaying && rotateAlbumArt) {
        lastArtDegrees = (state.positionMs / 35f)
    }
    val artRotationState = animateFloatAsState(
        targetValue = if (rotateAlbumArt) (lastArtDegrees + introSpin.value) else introSpin.value,
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
                                effectiveAccent.copy(alpha = if (state.isPlaying) 0.20f else 0.08f),
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                        rotationZ = artRotationState.value % 360f
                    }
                    .clip(artShape)
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(1.dp, effectiveAccent.copy(alpha = 0.72f), artShape),
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
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(state.title) {
                        awaitPointerEventScope {
                            while (true) {
                                val ev = awaitPointerEvent()
                                userTouchingTitle = ev.changes.any { it.pressed }
                            }
                        }
                    },
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                ExpandedTitleText(
                    title = if (state.hasActiveSession) state.title else "No Active Music Session",
                    marqueeEnabled = expandedMarqueeEnabled && state.hasActiveSession && state.isPlaying && !userTouchingTitle
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
                    color = Color.White.copy(alpha = 0.76f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
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
                    contentDescription = "Toggle lyrics",
                    tint = if (showLyricsPane) effectiveAccent else Color.White.copy(alpha = 0.74f),
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
                        tint = if (state.isLiked) IslandColors.CrimsonBright else Color.White.copy(alpha = 0.74f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (showLyricsPane) {
            if (state.lyrics.isNotEmpty()) {
                SyncedLyricsPanel(
                    lyrics = state.lyrics,
                    positionMs = (state.positionMs + lyricsOffsetMs).coerceAtLeast(0L),
                    accentColor = effectiveAccent,
                    onSeekTo = onSeekTo,
                    onClose = { showLyricsPane = false }
                )
            } else {
                val (statusMessage, showLearnMore) = when (state.lyricsStatus) {
                    LyricsLookupStatus.LOADING -> "Checking lyrics..." to false
                    LyricsLookupStatus.NEEDS_INTERNET_STREAMED ->
                        "Lyrics need internet — PixelMusic downloads them from LRCLIB" to true
                    LyricsLookupStatus.RATE_LIMITED ->
                        "LRCLIB busy — try again in a moment" to false
                    else -> "No lyrics for this track" to false
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { showLyricsPane = false }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Lyrics,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.68f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = statusMessage,
                            color = Color.White.copy(alpha = 0.86f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (showLearnMore) {
                            Text(
                                text = "Learn more",
                                color = effectiveAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        showLearnMoreDialog = !showLearnMoreDialog
                                        onOpenLyricsSettings()
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (showLearnMoreDialog) {
                        Text(
                            text = "Only title, artist, album & length are sent to lrclib.net when \"Lyrics from internet\" is enabled in Settings.",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
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
                        tint = if (state.isShuffled) effectiveAccent else Color.White.copy(alpha = 0.72f),
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
                    .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
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
                        tint = if (state.repeatModeLabel != "Off") effectiveAccent else Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedTitleText(
    title: String,
    marqueeEnabled: Boolean
) {
    if (!marqueeEnabled || title.length <= 26) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 20.sp
        )
        return
    }

    val density = LocalDensity.current
    val scrollOffset = remember(title) { Animatable(0f) }
    var viewportWidthPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(title, marqueeEnabled, viewportWidthPx) {
        scrollOffset.snapTo(0f)
        if (!marqueeEnabled || viewportWidthPx <= 0f) return@LaunchedEffect
        val travelDp = ((title.length - 20).coerceIn(6, 48)) * 7f
        val travelPx = with(density) { travelDp.dp.toPx() }
        val durationMs = ((travelDp / 40f) * 1000f).toInt().coerceIn(800, 4500)
        delay(1500L)
        scrollOffset.animateTo(-travelPx, tween(durationMillis = durationMs, easing = LinearEasing))
        delay(2000L)
        scrollOffset.animateTo(0f, tween(durationMillis = 450))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .onSizeChanged { viewportWidthPx = it.width.toFloat() }
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.graphicsLayer { translationX = scrollOffset.value }
        )
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
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose
            )
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
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(lyrics) { index, line ->
                val isCurrent = index == activeIndex
                Text(
                    text = line.text,
                    color = if (isCurrent) accentColor else Color.White.copy(alpha = 0.66f),
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
            style = TextStyle(fontSize = 11.sp, color = Color.White.copy(alpha = 0.74f), fontFeatureSettings = "tnum")
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
            style = TextStyle(fontSize = 11.sp, color = Color.White.copy(alpha = 0.74f), fontFeatureSettings = "tnum")
        )
    }
}
