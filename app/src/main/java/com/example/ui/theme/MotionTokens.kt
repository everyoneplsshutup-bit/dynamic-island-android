package com.example.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import com.example.system.AnimationStyle
import com.example.system.OpeningEffect
import kotlin.math.PI

/**
 * Blended Apple WWDC23 Live Activities + M3 Expressive Spring Motion Tokens.
 * Drives GPU RenderThread graphicsLayer transforms (scale, translation, alpha, blur) at 120Hz.
 * Dynamic Ledge rule: open springs gently overshoot once; collapse never bounces (bounce = 0).
 */
object MotionTokens {

    const val CONTENT_CROSSFADE_MS = 200
    const val STAGGER_STEP_MS = 45
    val ContentCrossfadeEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

    const val BUTTON_PRESS_SCALE = 0.97f

    /**
     * Converts Apple-style (response seconds, bounce 0..1) into Compose SpringSpec(dampingRatio, stiffness).
     */
    fun <T> responseSpring(
        responseSec: Float,
        bounce: Float,
        style: AnimationStyle,
        elasticity: Float = 0.65f,
        durationScale: Float = 1.0f,
        visibilityThreshold: T? = null
    ): FiniteAnimationSpec<T> {
        if (style == AnimationStyle.NONE) return snap()
        val scaledResp = (responseSec / durationScale.coerceIn(0.45f, 2.0f)).coerceIn(0.10f, 1.2f)
        val omega = (2.0 * PI / scaledResp).toFloat()
        val stiffness = (omega * omega).coerceIn(140f, 4200f)

        val rawDamping = when (style) {
            AnimationStyle.FRUIT -> {
                // "Springy": playful bounce overshoot modulated by elasticity slider (single crisp settle)
                val springyBounce = (bounce + 0.14f * (0.5f + elasticity)).coerceIn(0.12f, 0.36f)
                1f - springyBounce
            }
            AnimationStyle.SPRING -> {
                // "Standard": balanced spring
                val stdBounce = (bounce * (0.55f + 0.55f * elasticity)).coerceIn(0f, 0.22f)
                1f - stdBounce
            }
            AnimationStyle.SMOOTH -> 1.0f // "Calm": minimal bounce, fastest settle
            AnimationStyle.NONE -> 1.0f
        }.coerceIn(0.48f, 1.0f)

        return spring(
            dampingRatio = rawDamping,
            stiffness = stiffness,
            visibilityThreshold = visibilityThreshold
        )
    }

    fun <T> idleToMediumSpec(
        style: AnimationStyle,
        elasticity: Float,
        durationScale: Float
    ): FiniteAnimationSpec<T> = responseSpring(
        responseSec = 0.35f,
        bounce = 0.15f,
        style = style,
        elasticity = elasticity,
        durationScale = durationScale
    )

    fun <T> mediumToFullSpec(
        style: AnimationStyle,
        elasticity: Float,
        durationScale: Float
    ): FiniteAnimationSpec<T> = responseSpring(
        responseSec = 0.42f,
        bounce = 0.18f,
        style = style,
        elasticity = elasticity,
        durationScale = durationScale
    )

    fun <T> openingEffectSpec(
        openingEffect: OpeningEffect,
        style: AnimationStyle,
        elasticity: Float,
        durationScale: Float
    ): FiniteAnimationSpec<T> {
        if (style == AnimationStyle.NONE) return snap()
        return when (openingEffect) {
            OpeningEffect.LIQUID -> mediumToFullSpec(style, elasticity, durationScale)
            OpeningEffect.SPRING -> spatialDefault(style, elasticity, durationScale)
            OpeningEffect.SMOOTH -> responseSpring(0.34f, 0f, AnimationStyle.SMOOTH, 0f, durationScale)
            OpeningEffect.POP -> responseSpring(0.30f, 0.26f, style, elasticity.coerceAtLeast(0.6f), durationScale)
        }
    }

    fun <T> collapseSpec(
        style: AnimationStyle,
        durationScale: Float
    ): FiniteAnimationSpec<T> = if (style == AnimationStyle.NONE) {
        snap()
    } else {
        // Dynamic Ledge rule: collapse NEVER bounces (bounce = 0)
        responseSpring(
            responseSec = 0.32f,
            bounce = 0f,
            style = AnimationStyle.SMOOTH,
            elasticity = 0f,
            durationScale = durationScale
        )
    }

    fun <T> spatialDefault(
        style: AnimationStyle,
        elasticity: Float,
        durationScale: Float
    ): SpringSpec<T> {
        val damping = when (style) {
            AnimationStyle.FRUIT -> (0.74f - elasticity * 0.16f).coerceIn(0.52f, 0.85f)
            AnimationStyle.SPRING -> (0.84f - elasticity * 0.10f).coerceIn(0.68f, 0.92f)
            AnimationStyle.SMOOTH, AnimationStyle.NONE -> Spring.DampingRatioNoBouncy
        }
        val stiffness = if (style == AnimationStyle.NONE) 5000f else (380f * durationScale.coerceIn(0.5f, 1.8f))
        return spring(dampingRatio = damping, stiffness = stiffness)
    }

    fun <T> spatialFast(
        style: AnimationStyle,
        elasticity: Float,
        durationScale: Float
    ): SpringSpec<T> {
        val damping = when (style) {
            AnimationStyle.FRUIT -> (0.60f - elasticity * 0.12f).coerceIn(0.46f, 0.74f)
            AnimationStyle.SPRING -> 0.68f
            AnimationStyle.SMOOTH, AnimationStyle.NONE -> Spring.DampingRatioNoBouncy
        }
        val stiffness = if (style == AnimationStyle.NONE) 5000f else (800f * durationScale.coerceIn(0.5f, 1.8f))
        return spring(dampingRatio = damping, stiffness = stiffness)
    }

    fun <T> effectsDefault(durationScale: Float): SpringSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 1600f * durationScale.coerceIn(0.5f, 1.8f))

    fun <T> effectsFast(durationScale: Float): SpringSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 3800f * durationScale.coerceIn(0.5f, 1.8f))

    fun <T> buttonPressSpec(style: AnimationStyle): FiniteAnimationSpec<T> =
        if (style == AnimationStyle.NONE) snap() else responseSpring(0.16f, 0f, AnimationStyle.SMOOTH)

    fun <T> buttonReleaseSpec(style: AnimationStyle, elasticity: Float): FiniteAnimationSpec<T> =
        if (style == AnimationStyle.NONE) snap() else responseSpring(0.30f, 0.25f, style, elasticity)

    fun contentCrossfadeTween(style: AnimationStyle, durationScale: Float): TweenSpec<Float> =
        tween(
            durationMillis = if (style == AnimationStyle.NONE) 0 else (CONTENT_CROSSFADE_MS / durationScale).toInt().coerceIn(75, 360),
            easing = ContentCrossfadeEasing
        )
}
