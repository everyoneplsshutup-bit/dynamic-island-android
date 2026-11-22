package com.example.ui.theme

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.system.PillStyle
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import kotlin.math.PI
import kotlin.math.sin

/**
 * In-app backdrop passed down from MainActivity root so Liquid Glass cards/previews
 * refract real underlying screen content on API 33+ (Redmi Note 10 Pro Android 13).
 * Null inside TYPE_ACCESSIBILITY_OVERLAY where cross-window blur + Apple Glass rim is used.
 */
val LocalAppBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/**
 * Unified Build #5 glass chrome modifier.
 * - Dark: pure opaque black pill/card, crisp thin outline.
 * - Apple Glass: density-driven translucent scrim + 1px specular top rim + inner edge band.
 * - Liquid Glass: Kyant Backdrop AGSL blur+lens refraction in-app (API 33+ when backdrop provided),
 *   gracefully matching Apple-Glass specular scrim + FLAG_BLUR_BEHIND on the system overlay.
 * Guardrails (732G): applied ONLY to island & dock chrome, never to lists/content cards.
 */
fun Modifier.islandGlassChrome(
    shape: Shape,
    pillStyle: PillStyle,
    glassDensity: Float,
    reduceGlass: Boolean,
    solidFallback: Boolean,
    accentColor: Color,
    isHighlighted: Boolean = false,
    backdrop: LayerBackdrop? = null,
    crossWindowBlurActive: Boolean = false
): Modifier {
    val effectiveStyle = if (solidFallback) PillStyle.BLACK else pillStyle
    if (effectiveStyle == PillStyle.BLACK) {
        val borderBrush = IslandColors.liquidBorderBrush(
            useGlass = false,
            isHighlighted = isHighlighted,
            accentColor = accentColor,
            isLiquidStyle = false
        )
        return this
            .clip(shape)
            .background(IslandColors.PillBlack)
            .border(if (isHighlighted) 1.5.dp else 1.dp, borderBrush, shape)
    }

    val density = glassDensity.coerceIn(0f, 1f)
    val isLiquid = effectiveStyle == PillStyle.LIQUID_GLASS
    val canUseBackdropRefraction =
        isLiquid && !reduceGlass && backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    val glassBrush = IslandColors.blendGlassBase(
        materialYouColor = accentColor,
        density = density,
        pillStyle = effectiveStyle,
        reduceGlass = reduceGlass || (isLiquid && backdrop == null && !crossWindowBlurActive)
    )
    val borderBrush = IslandColors.liquidBorderBrush(
        useGlass = true,
        isHighlighted = isHighlighted,
        accentColor = accentColor,
        isLiquidStyle = isLiquid
    )

    val baseMod = if (canUseBackdropRefraction && backdrop != null) {
        val blurPx = (6f + (1f - density) * 6f).coerceIn(6f, 12f)
        val refractionAmt = (12f + (1f - density) * 14f).coerceIn(10f, 26f)
        this
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    blur(radius = blurPx.dp.toPx())
                    lens(
                        refractionHeight = 10.dp.toPx(),
                        refractionAmount = refractionAmt.dp.toPx(),
                        depthEffect = true,
                        chromaticAberration = false
                    )
                },
                onDrawSurface = {
                    drawRect(brush = glassBrush)
                }
            )
    } else {
        this
            .clip(shape)
            .background(glassBrush)
    }

    return baseMod
        .drawWithContent {
            drawContent()
            // Top & bottom 1dp specular rim band inside edge (Liquid / Apple Glass signature)
            val topAlpha = if (isLiquid) 0.40f else 0.28f
            val botAlpha = if (isLiquid) 0.16f else 0.10f
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = topAlpha), Color.Transparent)
                ),
                start = Offset(size.width * 0.10f, 1.dp.toPx()),
                end = Offset(size.width * 0.90f, 1.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = botAlpha), Color.Transparent)
                ),
                start = Offset(size.width * 0.16f, size.height - 1.dp.toPx()),
                end = Offset(size.width * 0.84f, size.height - 1.dp.toPx()),
                strokeWidth = 0.8.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        .border(if (isHighlighted) 1.5.dp else 0.9.dp, borderBrush, shape)
}

/**
 * RenderThread-driven Material 3 Expressive Wavy Progress Bar.
 * Used by MusicIslandView (interactive seek) and TimerStopwatchView (determinate countdown).
 * Zero allocations inside Canvas draw path when calm; smooth 120Hz sinusoidal wave while active.
 */
@Composable
fun WavyProgressBar(
    progress: Float,
    isActiveWave: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 18.dp,
    interactive: Boolean = false,
    onSeekFraction: ((Float) -> Unit)? = null
) {
    val clamped = progress.coerceIn(0f, 1f)
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(clamped) }
    var widthPx by remember { mutableFloatStateOf(1f) }

    val displayFraction = if (isDragging) dragFraction else clamped

    val wavePhase = if (isActiveWave) {
        val infinite = rememberInfiniteTransition(label = "m3e_wavy_phase")
        val phase by infinite.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(1150, easing = LinearEasing), RepeatMode.Restart),
            label = "wave_phase"
        )
        phase
    } else {
        0f
    }

    val wavePath = remember { Path() }

    val interactionMod = if (interactive && onSeekFraction != null) {
        Modifier
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(onSeekFraction) {
                detectTapGestures { offset ->
                    val frac = (offset.x / widthPx).coerceIn(0f, 1f)
                    onSeekFraction(frac)
                }
            }
            .pointerInput(onSeekFraction) {
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
                        onSeekFraction(dragFraction)
                        isDragging = false
                    },
                    onDragCancel = { isDragging = false }
                )
            }
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .then(interactionMod)
    ) {
        val cy = size.height / 2f
        val pad = 7.dp.toPx()
        val trackStart = pad
        val trackEnd = (size.width - pad).coerceAtLeast(pad + 1f)
        val knobX = trackStart + (trackEnd - trackStart) * displayFraction

        // Calm trailing track
        drawLine(
            color = Color.White.copy(alpha = 0.18f),
            start = Offset(knobX, cy),
            end = Offset(trackEnd, cy),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // M3 Expressive sinusoidal active segment
        val activeLen = (knobX - trackStart).coerceAtLeast(0f)
        if (activeLen > 2f) {
            val amplitude = if (isActiveWave) 2.4.dp.toPx() else 0f
            if (amplitude > 0.1f) {
                wavePath.reset()
                val wavelengthPx = 18.dp.toPx()
                val step = 4f
                var x = trackStart
                var first = true
                while (x <= knobX) {
                    val norm = (x - trackStart) / wavelengthPx
                    val y = cy + sin(norm * (2f * PI.toFloat()) - wavePhase) * amplitude
                    if (first) {
                        wavePath.moveTo(x, y)
                        first = false
                    } else {
                        wavePath.lineTo(x, y)
                    }
                    x += step
                }
                val endY = cy + sin(((knobX - trackStart) / wavelengthPx) * (2f * PI.toFloat()) - wavePhase) * amplitude
                wavePath.lineTo(knobX, endY)
                drawPath(
                    path = wavePath,
                    color = accentColor,
                    style = Stroke(width = if (isDragging) 4.dp.toPx() else 3.2.dp.toPx(), cap = StrokeCap.Round)
                )
            } else {
                drawLine(
                    color = accentColor,
                    start = Offset(trackStart, cy),
                    end = Offset(knobX, cy),
                    strokeWidth = if (isDragging) 4.dp.toPx() else 3.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        if (isDragging) {
            drawCircle(
                color = accentColor.copy(alpha = 0.28f),
                radius = 11.dp.toPx(),
                center = Offset(knobX, cy)
            )
        }
        drawCircle(
            color = Color.White,
            radius = if (isDragging) 6.5.dp.toPx() else if (interactive) 5.dp.toPx() else 3.5.dp.toPx(),
            center = Offset(knobX, cy)
        )
    }
}
