package com.example.ui.theme

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.os.Build
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
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.system.PillStyle
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur as backdropBlur
import com.kyant.backdrop.effects.lens
import kotlin.math.PI
import kotlin.math.sin

val LocalAppBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

private const val LIQUID_EDGE_AGSL = """
    uniform shader content;
    uniform float2 iResolution;
    uniform float2 iLightPos;
    uniform float iRefractStrength;
    uniform float iBendScale;

    float sdSuperellipse(float2 p, float2 b) {
        float2 d = abs(p) / max(b, float2(1.0));
        float k = pow(pow(d.x, 3.0) + pow(d.y, 3.0), 1.0 / 3.0);
        return (k - 1.0) * min(b.x, b.y);
    }

    half4 main(float2 fragCoord) {
        float2 center = iResolution * 0.5;
        float2 p = fragCoord - center;
        float dist = sdSuperellipse(p, center * 0.96);
        float edgeNorm = clamp((dist + 24.0) / 24.0, 0.0, 1.0);
        float edgeMask = pow(edgeNorm, 2.3);
        float2 dir = normalize(p + float2(0.001));
        float bendMag = clamp(-iBendScale / 112.0, 0.5, 1.8);
        float2 disp = dir * edgeMask * iRefractStrength * bendMag;

        // Chromatic RGB dispersion confined to optical rim; flat center remains undistorted
        half4 baseCol = content.eval(fragCoord - disp);
        half4 redCol = content.eval(fragCoord - disp * 1.24);
        half4 blueCol = content.eval(fragCoord - disp * 0.76);
        half3 refracted = half3(redCol.r, baseCol.g, blueCol.b);

        // Dual opposed specular caustic rim oriented to primary and counter light
        float2 lightDir1 = normalize(iLightPos - center + float2(0.001));
        float2 lightDir2 = -lightDir1;
        float rimDot1 = pow(clamp(dot(dir, lightDir1) * 0.5 + 0.5, 0.0, 1.0), 1.8);
        float rimDot2 = pow(clamp(dot(dir, lightDir2) * 0.5 + 0.5, 0.0, 1.0), 2.4);
        float rimBand = smoothstep(-14.0, -0.8, dist);
        float fresnel = rimBand * (0.10 + 0.28 * rimDot1 + 0.14 * rimDot2);

        return half4(clamp(refracted + half3(fresnel), 0.0, 1.0), baseCol.a);
    }
"""

/**
 * Optional API 31+ RenderEffect metaball chain (gaussian soften + alpha threshold) on the parent layer
 * so companion circles merge/split cleanly without CPU field marching.
 */
fun Modifier.metaballParentLayer(enabled: Boolean): Modifier {
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return this
    return this.graphicsLayer {
        runCatching {
            val blurEffect = AndroidRenderEffect.createBlurEffect(
                14f,
                14f,
                android.graphics.Shader.TileMode.CLAMP
            )
            val matrix = android.graphics.ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, 0f,
                    0f, 0f, 0f, 38f, -3800f
                )
            )
            val thresholdEffect = AndroidRenderEffect.createColorFilterEffect(
                android.graphics.ColorMatrixColorFilter(matrix),
                blurEffect
            )
            renderEffect = thresholdEffect.asComposeRenderEffect()
        }
    }
}

/**
 * Optical Liquid Glass chrome modifier (§9 Two Looks ONLY: Dark vs Apple Liquid):
 * - Clear Glass on compact pills / chips / dock (`isRegularCard = false`)
 * - Regular Glass on expanded cards (`isRegularCard = true`) for crisp text legibility
 * - Cached AGSL RuntimeShader uniforms updated ONLY when size changes (zero idle invalidations)
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
    crossWindowBlurActive: Boolean = false,
    allowDarkBorder: Boolean = true,
    performanceMode: Boolean = false,
    isRegularCard: Boolean = false,
    bendScale: Float = -112f
): Modifier {
    val effectiveStyle = if (solidFallback) PillStyle.BLACK else pillStyle
    if (effectiveStyle == PillStyle.BLACK) {
        val base = this
            .clip(shape)
            .background(IslandColors.PillBlack)
        if (!allowDarkBorder && !isHighlighted) return base
        val borderBrush = IslandColors.liquidBorderBrush(
            useGlass = false,
            isHighlighted = isHighlighted,
            accentColor = accentColor,
            isLiquidStyle = false
        )
        return base.border(if (isHighlighted) 1.5.dp else 1.dp, borderBrush, shape)
    }

    val density = glassDensity.coerceIn(0f, 1f)
    val isLiquid = effectiveStyle == PillStyle.APPLE_LIQUID
    val glassBrush = IslandColors.blendGlassBase(
        materialYouColor = accentColor,
        density = density,
        pillStyle = effectiveStyle,
        reduceGlass = reduceGlass || performanceMode,
        isRegularCard = isRegularCard
    )
    val borderBrush = IslandColors.liquidBorderBrush(
        useGlass = true,
        isHighlighted = isHighlighted,
        accentColor = accentColor,
        isLiquidStyle = isLiquid
    )

    val kyantBackdropMod = if (backdrop != null && !reduceGlass && !performanceMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                if (isRegularCard) {
                    backdropBlur(radius = 20.dp.toPx())
                }
                lens(
                    refractionHeight = if (isRegularCard) 14.dp.toPx() else 18.dp.toPx(),
                    refractionAmount = if (isRegularCard) 24.dp.toPx() else 32.dp.toPx(),
                    depthEffect = true
                )
            }
        )
    } else {
        Modifier
    }

    val agslRefractMod = if (isLiquid && !reduceGlass && !performanceMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = runCatching { RuntimeShader(LIQUID_EDGE_AGSL) }.getOrNull()
        if (shader != null) {
            var cachedW = -1f
            var cachedH = -1f
            var cachedEffect: androidx.compose.ui.graphics.RenderEffect? = null
            Modifier.graphicsLayer {
                if (size.width >= 8f && size.height >= 8f) {
                    if (size.width != cachedW || size.height != cachedH || cachedEffect == null) {
                        cachedW = size.width
                        cachedH = size.height
                        runCatching {
                            shader.setFloatUniform("iResolution", size.width, size.height)
                            shader.setFloatUniform("iLightPos", size.width * 0.25f, size.height * 0.12f)
                            shader.setFloatUniform("iRefractStrength", if (isRegularCard) 4.2f else 5.5f)
                            shader.setFloatUniform("iBendScale", bendScale.coerceIn(-180f, -60f))
                            cachedEffect = AndroidRenderEffect
                                .createRuntimeShaderEffect(shader, "content")
                                .asComposeRenderEffect()
                        }
                    }
                    renderEffect = cachedEffect
                }
            }
        } else {
            Modifier
        }
    } else {
        Modifier
    }

    return this
        .clip(shape)
        .then(kyantBackdropMod)
        .background(glassBrush)
        .then(agslRefractMod)
        .drawWithContent {
            drawContent()
            if (isLiquid && !reduceGlass && !performanceMode) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (crossWindowBlurActive) 0.18f else 0.14f),
                            accentColor.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.22f, size.height * 0.14f),
                        radius = (maxOf(size.width, size.height) * 0.88f).coerceAtLeast(1f)
                    )
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x2890E0FF),
                            accentColor.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.82f, size.height * 0.86f),
                        radius = (maxOf(size.width, size.height) * 0.72f).coerceAtLeast(1f)
                    )
                )
            }
            val topAlpha = if (isLiquid) 0.74f else 0.34f
            val botAlpha = if (isLiquid) 0.38f else 0.14f
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = topAlpha), Color.Transparent)
                ),
                start = Offset(size.width * 0.06f, 1.dp.toPx()),
                end = Offset(size.width * 0.94f, 1.dp.toPx()),
                strokeWidth = (if (isLiquid) 1.5 else 1.1).dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = botAlpha), Color.Transparent)
                ),
                start = Offset(size.width * 0.12f, size.height - 1.dp.toPx()),
                end = Offset(size.width * 0.88f, size.height - 1.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        .border(
            width = when {
                isHighlighted -> 1.6.dp
                isLiquid -> 1.3.dp
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
