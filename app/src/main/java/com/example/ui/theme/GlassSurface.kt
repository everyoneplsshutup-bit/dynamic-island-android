package com.example.ui.theme

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
import kotlin.math.PI
import kotlin.math.sin

val LocalAppBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/**
 * Unified crash-free glass chrome modifier.
 * Avoids cyclic LayerBackdrop self-recording crashes on Android 13 / MIUI 14 while
 * rendering Dark, Apple Glass, and Liquid Glass with multi-stop gradients, inner refractive
 * sheen, and specular top/bottom edge rims.
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
    val glassBrush = IslandColors.blendGlassBase(
        materialYouColor = accentColor,
        density = density,
        pillStyle = effectiveStyle,
        reduceGlass = reduceGlass
    )
    val borderBrush = IslandColors.liquidBorderBrush(
        useGlass = true,
        isHighlighted = isHighlighted,
        accentColor = accentColor,
        isLiquidStyle = isLiquid
    )

    return this
        .clip(shape)
        .background(glassBrush)
        .drawWithContent {
            drawContent()
            if (isLiquid && !reduceGlass) {
                // Soft inner liquid refractive sheen + subtle edge halo
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.10f),
                            accentColor.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.26f, size.height * 0.16f),
                        radius = (maxOf(size.width, size.height) * 0.90f).coerceAtLeast(1f)
                    )
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.78f, size.height * 0.88f),
                        radius = (maxOf(size.width, size.height) * 0.72f).coerceAtLeast(1f)
                    )
                )
            }
            val topAlpha = if (isLiquid) 0.58f else 0.30f
            val botAlpha = if (isLiquid) 0.28f else 0.12f
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = topAlpha), Color.Transparent)
                ),
                start = Offset(size.width * 0.08f, 1.dp.toPx()),
                end = Offset(size.width * 0.92f, 1.dp.toPx()),
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = botAlpha), Color.Transparent)
                ),
                start = Offset(size.width * 0.14f, size.height - 1.dp.toPx()),
                end = Offset(size.width * 0.86f, size.height - 1.dp.toPx()),
                strokeWidth = 0.9.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        .border(
            width = when {
                isHighlighted -> 1.5.dp
                isLiquid -> 1.2.dp
                else -> 0.9.dp
            },
            brush = borderBrush,
            shape = shape
        )
}

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
    val wavePhase = if (isActiveWave) (displayFraction * 6f * PI.toFloat()) else 0f

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

        drawLine(
            color = Color.White.copy(alpha = 0.18f),
            start = Offset(knobX, cy),
            end = Offset(trackEnd, cy),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

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
