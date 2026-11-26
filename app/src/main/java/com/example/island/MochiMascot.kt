package com.example.island

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IslandColors
import com.example.ui.theme.MotionTokens
import java.util.Calendar
import kotlinx.coroutines.delay

enum class MochiMove {
    PERCH,
    PEEK,
    RUN,
    DANCE,
    SLEEP,
    HAPPY,
    SLEEPY
}

enum class MochiFrameId {
    PERCH,
    BLINK,
    PEEK_1,
    PEEK_2,
    RUN_1,
    RUN_2,
    RUN_3,
    RUN_4,
    DANCE_1,
    DANCE_2,
    DANCE_3,
    DANCE_4,
    SLEEP_1,
    SLEEP_2,
    SLEEP_3,
    HAPPY_1,
    HAPPY_2,
    SLEEPY
}

object MochiSpriteAtlas {
    private const val SIZE = 32
    private val frames: Map<MochiFrameId, ImageBitmap> by lazy { buildAtlas() }

    fun frame(id: MochiFrameId): ImageBitmap = frames[id] ?: frames.getValue(MochiFrameId.PERCH)

    fun frameCount(): Int = frames.size

    private fun buildAtlas(): Map<MochiFrameId, ImageBitmap> =
        MochiFrameId.entries.associateWith { renderFrame(it) }

    private fun renderFrame(id: MochiFrameId): ImageBitmap {
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val cOutline = 0xFF141418.toInt()
        val cFur = 0xFFF7F7FA.toInt()
        val cFurShade = 0xFFD9DCE6.toInt()
        val cDark = 0xFF20222A.toInt()
        val cEar = 0xFF181A20.toInt()
        val cEyeWhite = 0xFFFFFFFF.toInt()
        val cBlush = 0xFFFF8AA6.toInt()
        val cNote = 0xFF4DD0E1.toInt()

        fun px(x: Int, y: Int, col: Int) {
            if (x in 0 until SIZE && y in 0 until SIZE) bmp.setPixel(x, y, col)
        }

        fun fillRect(x0: Int, y0: Int, w: Int, h: Int, col: Int) {
            for (y in y0 until (y0 + h)) {
                for (x in x0 until (x0 + w)) px(x, y, col)
            }
        }

        val bobY = when (id) {
            MochiFrameId.DANCE_1, MochiFrameId.HAPPY_1 -> -1
            MochiFrameId.DANCE_3, MochiFrameId.HAPPY_2 -> 1
            MochiFrameId.RUN_1, MochiFrameId.RUN_3 -> -1
            MochiFrameId.PEEK_1 -> 4
            MochiFrameId.PEEK_2 -> 1
            MochiFrameId.SLEEP_1, MochiFrameId.SLEEP_2, MochiFrameId.SLEEP_3 -> 3
            else -> 0
        }
        val leanX = when (id) {
            MochiFrameId.DANCE_2 -> -1
            MochiFrameId.DANCE_4 -> 1
            MochiFrameId.RUN_2 -> 1
            MochiFrameId.RUN_4 -> -1
            else -> 0
        }

        val isSleeping = id == MochiFrameId.SLEEP_1 || id == MochiFrameId.SLEEP_2 || id == MochiFrameId.SLEEP_3
        val isBlink = id == MochiFrameId.BLINK || isSleeping || id == MochiFrameId.SLEEPY
        val isHappy = id == MochiFrameId.HAPPY_1 || id == MochiFrameId.HAPPY_2

        // Ears
        val hx = 8 + leanX
        val hy = 6 + bobY
        fillRect(hx + 1, hy - 2, 4, 4, cEar)
        fillRect(hx + 11, hy - 2, 4, 4, cEar)
        px(hx + 2, hy - 1, cDark)
        px(hx + 12, hy - 1, cDark)

        // Head outline + white face
        fillRect(hx + 1, hy, 14, 1, cOutline)
        fillRect(hx, hy + 1, 16, 10, cOutline)
        fillRect(hx + 1, hy + 1, 14, 10, cFur)
        fillRect(hx + 2, hy + 11, 12, 1, cOutline)
        fillRect(hx + 1, hy + 9, 14, 2, cFurShade)

        // Eye patches
        val eyeGazeDx = when (id) {
            MochiFrameId.PEEK_2 -> 1
            MochiFrameId.DANCE_2 -> -1
            MochiFrameId.DANCE_4 -> 1
            else -> 0
        }
        fillRect(hx + 2, hy + 4, 4, 4, cDark)
        fillRect(hx + 10, hy + 4, 4, 4, cDark)

        // Eyes / expressions
        if (isBlink) {
            fillRect(hx + 3, hy + 6, 2, 1, cFur)
            fillRect(hx + 11, hy + 6, 2, 1, cFur)
            if (id == MochiFrameId.SLEEPY) {
                px(hx + 3, hy + 5, cFurShade)
                px(hx + 12, hy + 5, cFurShade)
            }
        } else if (isHappy) {
            px(hx + 3, hy + 5, cEyeWhite)
            px(hx + 4, hy + 5, cEyeWhite)
            px(hx + 11, hy + 5, cEyeWhite)
            px(hx + 12, hy + 5, cEyeWhite)
        } else {
            px(hx + 3 + eyeGazeDx, hy + 5, cEyeWhite)
            px(hx + 11 + eyeGazeDx, hy + 5, cEyeWhite)
        }

        // Cheeks & nose
        px(hx + 2, hy + 8, cBlush)
        px(hx + 13, hy + 8, cBlush)
        fillRect(hx + 7, hy + 7, 2, 1, cOutline)
        px(hx + 6, hy + 8, cOutline)
        px(hx + 9, hy + 8, cOutline)

        // Body (when not tucked deep in PEEK_1)
        if (id != MochiFrameId.PEEK_1) {
            val by = hy + 12
            fillRect(hx + 2, by, 12, 8, cOutline)
            fillRect(hx + 3, by, 10, 7, cFur)
            // Dark shoulder collar
            fillRect(hx + 2, by, 12, 2, cDark)
            // Left / right paws
            val leftPawLift = when (id) {
                MochiFrameId.DANCE_1, MochiFrameId.DANCE_2, MochiFrameId.HAPPY_1 -> -3
                MochiFrameId.RUN_1, MochiFrameId.RUN_2 -> -1
                else -> 0
            }
            val rightPawLift = when (id) {
                MochiFrameId.DANCE_3, MochiFrameId.DANCE_4, MochiFrameId.HAPPY_2 -> -3
                MochiFrameId.RUN_3, MochiFrameId.RUN_4 -> -1
                else -> 0
            }
            fillRect(hx, by + 2 + leftPawLift, 3, 4, cDark)
            fillRect(hx + 13, by + 2 + rightPawLift, 3, 4, cDark)

            // Feet
            val stepL = if (id == MochiFrameId.RUN_1 || id == MochiFrameId.RUN_4) 1 else 0
            val stepR = if (id == MochiFrameId.RUN_2 || id == MochiFrameId.RUN_3) 1 else 0
            fillRect(hx + 3, by + 7 - stepL, 4, 2, cDark)
            fillRect(hx + 9, by + 7 - stepR, 4, 2, cDark)
        }

        // Tiny music sparkle pixel on dance frames
        if (id == MochiFrameId.DANCE_2 || id == MochiFrameId.DANCE_4) {
            px(27, 4, cNote)
            px(27, 5, cNote)
            px(26, 6, cNote)
        }

        return bmp.asImageBitmap()
    }
}

object MochiScheduler {
    fun isQuietHoursNow(hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): Boolean {
        return hourOfDay >= 23 || hourOfDay < 7
    }

    fun nextSevenAmMillis(nowMs: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            val h = get(Calendar.HOUR_OF_DAY)
            if (h >= 7) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            set(Calendar.HOUR_OF_DAY, 7)
        }
        return cal.timeInMillis
    }

    fun resolveMove(state: IslandState, idleElapsedSec: Int): MochiMove {
        val primary = state.primaryActivity
        if (state.isLowBatteryWarning && primary == null) return MochiMove.SLEEPY
        if (state.isBatteryCharging && primary is IslandActivity.Charging) return MochiMove.SLEEP
        if (primary is IslandActivity.Alert && primary.kind == AlertKind.TIMER_DONE) return MochiMove.HAPPY
        if (primary is IslandActivity.NotificationPreview) return MochiMove.PEEK
        if (primary is IslandActivity.Music && primary.isPlaying) return MochiMove.DANCE
        return when {
            idleElapsedSec >= 30 -> MochiMove.SLEEP
            (idleElapsedSec % 18) in 12..14 -> MochiMove.PEEK
            (idleElapsedSec % 24) in 19..22 -> MochiMove.RUN
            else -> MochiMove.PERCH
        }
    }
}

/**
 * Mochi Pixel Panda Mascot (§12):
 * - Drawn programmatically via cached 32x32 ImageBitmaps with FilterQuality.None + withFrameNanos clock.
 * - Never obscures camera cutout, pill text, or sits inside Music/Alert cards.
 * - Long-press or small X badge opens Remove Mochi / Hide for today menu with poof fade.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MochiPandaPet(
    state: IslandState,
    quietHoursAnimate: Boolean,
    onTapMochi: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    var idleSeconds by remember { mutableIntStateOf(0) }
    var animTick by remember { mutableIntStateOf(0) }
    var isBlinking by remember { mutableStateOf(false) }
    var showHearts by remember { mutableStateOf(false) }
    val jumpOffset = remember { Animatable(0f) }
    val peekTranslateY = remember { Animatable(0f) }
    var runOffsetDp by remember { mutableFloatStateOf(0f) }

    val move = remember(state.primaryActivity, state.isBatteryCharging, state.isLowBatteryWarning, idleSeconds) {
        MochiScheduler.resolveMove(state, idleSeconds)
    }

    val isPaused = state.isDeviceLocked || (MochiScheduler.isQuietHoursNow() && !quietHoursAnimate)

    // Pop-up peek spring on PEEK move
    LaunchedEffect(move) {
        if (move == MochiMove.PEEK) {
            peekTranslateY.snapTo(8f)
            peekTranslateY.animateTo(0f, animationSpec = MotionTokens.appleSpring())
        } else {
            peekTranslateY.animateTo(0f, animationSpec = MotionTokens.appleSpring())
        }
    }

    // Jump + hearts reaction when tapped
    LaunchedEffect(state.mochiJumpTrigger) {
        if (state.mochiJumpTrigger > 0) {
            showHearts = true
            jumpOffset.snapTo(0f)
            jumpOffset.animateTo(-10f, animationSpec = MotionTokens.tilePopSpring())
            jumpOffset.animateTo(0f, animationSpec = MotionTokens.appleSpring())
            delay(750L)
            showHearts = false
        }
    }

    // Idle timer counter (1 Hz) resets on activity change or tap
    LaunchedEffect(state.primaryActivity?.id, state.mochiJumpTrigger, isPaused) {
        idleSeconds = 0
        if (isPaused) return@LaunchedEffect
        while (true) {
            delay(1000L)
            idleSeconds++
            isBlinking = (idleSeconds % 5 == 0)
        }
    }

    // Frame clock (12 fps dance / 8 fps otherwise) using compose withFrameNanos
    LaunchedEffect(move, isPaused) {
        if (isPaused) return@LaunchedEffect
        val targetFps = if (move == MochiMove.DANCE || move == MochiMove.RUN) 12 else 8
        val frameNs = 1_000_000_000L / targetFps
        var lastStepNs = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastStepNs == 0L || now - lastStepNs >= frameNs) {
                    lastStepNs = now
                    animTick++
                    runOffsetDp = if (move == MochiMove.RUN) {
                        val phase = (animTick % 16)
                        if (phase < 8) (phase - 4) * 1.4f else (12 - phase) * 1.4f
                    } else {
                        0f
                    }
                }
            }
        }
    }

    val activeFrameId = remember(move, animTick, isBlinking, isPaused) {
        when {
            isPaused && move == MochiMove.SLEEP -> MochiFrameId.SLEEP_1
            isPaused -> MochiFrameId.PERCH
            move == MochiMove.DANCE -> when (animTick % 4) {
                0 -> MochiFrameId.DANCE_1
                1 -> MochiFrameId.DANCE_2
                2 -> MochiFrameId.DANCE_3
                else -> MochiFrameId.DANCE_4
            }
            move == MochiMove.RUN -> when (animTick % 4) {
                0 -> MochiFrameId.RUN_1
                1 -> MochiFrameId.RUN_2
                2 -> MochiFrameId.RUN_3
                else -> MochiFrameId.RUN_4
            }
            move == MochiMove.SLEEP -> when (animTick % 3) {
                0 -> MochiFrameId.SLEEP_1
                1 -> MochiFrameId.SLEEP_2
                else -> MochiFrameId.SLEEP_3
            }
            move == MochiMove.PEEK -> if (animTick % 6 < 2) MochiFrameId.PEEK_1 else MochiFrameId.PEEK_2
            move == MochiMove.HAPPY -> if (animTick % 2 == 0) MochiFrameId.HAPPY_1 else MochiFrameId.HAPPY_2
            move == MochiMove.SLEEPY -> MochiFrameId.SLEEPY
            isBlinking -> MochiFrameId.BLINK
            else -> MochiFrameId.PERCH
        }
    }

    val bitmap = remember(activeFrameId) { MochiSpriteAtlas.frame(activeFrameId) }

    Box(
        modifier = modifier
            .size(width = 36.dp, height = 34.dp)
            .graphicsLayer {
                translationX = runOffsetDp * density
                translationY = (jumpOffset.value + peekTranslateY.value) * density
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTapMochi,
                onLongClick = onOpenMenu
            )
            .semantics { contentDescription = "Mochi panda mascot" }
            .testTag("mochi_panda_pet")
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawImage(
                image = bitmap,
                dstOffset = IntOffset(2.dp.roundToPx(), 4.dp.roundToPx()),
                dstSize = IntSize(28.dp.roundToPx(), 28.dp.roundToPx()),
                filterQuality = FilterQuality.None
            )

            // Pixel Zzz bubble while sleeping
            if (move == MochiMove.SLEEP && !isPaused) {
                val zStep = animTick % 4
                val zAlpha = 0.85f - zStep * 0.14f
                val zx = 22.dp.toPx() + zStep * 1.5.dp.toPx()
                val zy = 6.dp.toPx() - zStep * 1.6.dp.toPx()
                val zColor = Color(0xFFB6D7FF).copy(alpha = zAlpha.coerceIn(0.25f, 0.9f))
                drawRect(zColor, topLeft = Offset(zx, zy), size = Size(4.dp.toPx(), 1.5.dp.toPx()))
                drawRect(zColor, topLeft = Offset(zx + 1.5.dp.toPx(), zy + 1.5.dp.toPx()), size = Size(1.5.dp.toPx(), 1.5.dp.toPx()))
                drawRect(zColor, topLeft = Offset(zx, zy + 3.dp.toPx()), size = Size(4.dp.toPx(), 1.5.dp.toPx()))
            }

            // Tiny pixel hearts on tap
            if (showHearts) {
                val hColor = Color(0xFFFF6B93)
                val hx = 12.dp.toPx()
                val hy = 1.dp.toPx()
                drawRect(hColor, Offset(hx, hy), Size(2.dp.toPx(), 2.dp.toPx()))
                drawRect(hColor, Offset(hx + 3.dp.toPx(), hy), Size(2.dp.toPx(), 2.dp.toPx()))
                drawRect(hColor, Offset(hx + 1.5.dp.toPx(), hy + 2.dp.toPx()), Size(2.dp.toPx(), 2.dp.toPx()))
            }
        }

        // Small crisp X badge to remove/hide Mochi easily
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(14.dp)
                .clip(CircleShape)
                .background(IslandColors.ObsidianCardElevated.copy(alpha = 0.92f))
                .border(0.6.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                .clickable(onClick = onOpenMenu)
                .testTag("mochi_remove_badge"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Remove Mochi options",
                tint = IslandColors.TextSecondary,
                modifier = Modifier.size(9.dp)
            )
        }
    }
}

/**
 * Instant Remove / Hide menu for Mochi with tiny poof fade animation (§12 rule).
 */
@Composable
fun MochiActionMenuCard(
    visible: Boolean,
    onRemovePermanently: () -> Unit,
    onHideForToday: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var poofing by remember(visible) { mutableStateOf(false) }

    LaunchedEffect(poofing) {
        if (poofing) {
            delay(220L)
            onRemovePermanently()
            poofing = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(MotionTokens.effectsFast()) + scaleIn(initialScale = 0.9f, animationSpec = MotionTokens.appleSpring()),
        exit = fadeOut(MotionTokens.effectsFast()) + scaleOut(targetScale = 0.85f, animationSpec = MotionTokens.effectsFast()),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 210.dp, max = 248.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(IslandColors.ObsidianCard.copy(alpha = 0.97f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                .padding(14.dp)
                .testTag("mochi_action_menu"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (poofing) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Poof! Mochi tucked away",
                        color = IslandColors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mochi Panda",
                        color = IslandColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close Mochi menu",
                        tint = IslandColors.TextSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(onClick = onDismiss)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(IslandColors.ObsidianCardElevated)
                        .clickable { poofing = true }
                        .padding(horizontal = 12.dp)
                        .testTag("mochi_remove_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Remove Mochi",
                        color = IslandColors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(IslandColors.ObsidianCardElevated)
                        .clickable(onClick = onHideForToday)
                        .padding(horizontal = 12.dp)
                        .testTag("mochi_hide_today_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                        tint = IslandColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Hide for today",
                        color = IslandColors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
