package com.example.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntSize
import com.example.system.AnimationStyle
import com.example.system.OpeningEffect
import kotlin.math.PI

/**
 * Blended Apple WWDC23 Live Activities + M3 Expressive Spring Motion Tokens.
 * Drives GPU RenderThread graphicsLayer transforms (scale, translation, alpha, blur) at 120Hz.
 * Dynamic Ledge rule: open springs gently overshoot once; collapse NEVER bounces (damping = 1.0).
 */
object MotionTokens {

    const val CONTENT_CROSSFADE_MS = 185
    const val TIMER_TICK_CROSSFADE_MS = 140
    const val STAGGER_STEP_MS = 40
    val ContentCrossfadeEasing = CubicBezierEasing(0.16f, 1.0f, 0.30f, 1.0f)

    const val BUTTON_PRESS_SCALE = 0.965f

    fun pressRadiusDp(pressed: Boolean, baseDp: Float, squishedDp: Float = baseDp + 6f): Float =
        if (pressed) squishedDp else baseDp

    /**
     * Converts Apple-style (response seconds, bounce 0..1) into Compose SpringSpec(dampingRatio, stiffness).
     * Springy (FRUIT) produces a fluid crisp single overshoot; Standard (SPRING) produces balanced M3E settle;
     * Calm (SMOOTH) is critically damped (no overshoot).
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
        val scaledResp = (responseSec / durationScale.coerceIn(0.45f, 2.0f)).coerceIn(0.10f, 1.1f)
        val omega = (2.0 * PI / scaledResp).toFloat()
        val stiffness = (omega * omega).coerceIn(160f, 4400f)

        val rawDamping = when (style) {
            AnimationStyle.FRUIT -> {
                // "Springy": Apple Live Activity fluid single-overshoot physics
                val springyBounce = (bounce + 0.14f + 0.12f * elasticity).coerceIn(0.20f, 0.36f)
                1f - springyBounce
            }
            AnimationStyle.SPRING -> {
                // "Standard": M3 Expressive spatial spring (gentle settle)
                val stdBounce = (bounce * (0.50f + 0.40f * elasticity)).coerceIn(0.05f, 0.18f)
                1f - stdBounce
            }
            AnimationStyle.SMOOTH -> 1.0f // "Calm": critically damped, zero bounce
            AnimationStyle.NONE -> 1.0f
        }.coerceIn(0.56f, 1.0f)

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
        responseSec = 0.31f,
        bounce = 0.14f,
        style = style,
        elasticity = elasticity,
        durationScale = durationScale
    )

    fun <T> mediumToFullSpec(
        style: AnimationStyle,
        elasticity: Float,
        durationScale: Float
    ): FiniteAnimationSpec<T> = responseSpring(
        responseSec = 0.35f,
        bounce = 0.16f,
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
            OpeningEffect.SMOOTH -> responseSpring(0.30f, 0f, AnimationStyle.SMOOTH, 0f, durationScale)
            OpeningEffect.POP -> responseSpring(0.25f, 0.22f, style, elasticity.coerceAtLeast(0.65f), durationScale)
        }
    }

    fun <T> collapseSpec(
        style: AnimationStyle,
        durationScale: Float
    ): FiniteAnimationSpec<T> = if (style == AnimationStyle.NONE) {
        snap()
    } else {
        // Dynamic Ledge rule: collapse NEVER bounces (damping = 1.0)
        responseSpring(
            responseSec = 0.27f,
            bounce = 0f,
            style = AnimationStyle.SMOOTH,
            elasticity = 0f,
            durationScale = durationScale
        )
    }

    fun sizeTransformSpec(
        style: AnimationStyle,
        elasticity: Float,
        durationScale: Float
    ): FiniteAnimationSpec<IntSize> = if (style == AnimationStyle.NONE) {
        snap()
    } else {
        responseSpring(
            responseSec = 0.28f,
            bounce = 0.10f,
            style = style,
            elasticity = elasticity,
            durationScale = durationScale
        )
    }

    fun <T> spatialDefault(
        style: AnimationStyle,
        elasticity: Float,
        durationScale: Float
    ): SpringSpec<T> {
        val damping = when (style) {
            AnimationStyle.FRUIT -> (0.72f - elasticity * 0.14f).coerceIn(0.54f, 0.80f)
            AnimationStyle.SPRING -> (0.84f - elasticity * 0.10f).coerceIn(0.70f, 0.92f)
            AnimationStyle.SMOOTH, AnimationStyle.NONE -> Spring.DampingRatioNoBouncy
        }
        val stiffness = if (style == AnimationStyle.NONE) 5000f else (440f * durationScale.coerceIn(0.5f, 1.8f))
        return spring(dampingRatio = damping, stiffness = stiffness)
    }

    fun <T> spatialFast(
        style: AnimationStyle,
        elasticity: Float,
        durationScale: Float
    ): SpringSpec<T> {
        val damping = when (style) {
            AnimationStyle.FRUIT -> (0.64f - elasticity * 0.12f).coerceIn(0.50f, 0.76f)
            AnimationStyle.SPRING -> 0.74f
            AnimationStyle.SMOOTH, AnimationStyle.NONE -> Spring.DampingRatioNoBouncy
        }
        val stiffness = if (style == AnimationStyle.NONE) 5000f else (920f * durationScale.coerceIn(0.5f, 1.8f))
        return spring(dampingRatio = damping, stiffness = stiffness)
    }

    fun <T> effectsDefault(durationScale: Float): SpringSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 1800f * durationScale.coerceIn(0.5f, 1.8f))

    fun <T> effectsFast(durationScale: Float): SpringSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 3800f * durationScale.coerceIn(0.5f, 1.8f))

    fun <T> buttonPressSpec(style: AnimationStyle): FiniteAnimationSpec<T> =
        if (style == AnimationStyle.NONE) snap() else responseSpring(0.14f, 0f, AnimationStyle.SMOOTH)

    fun <T> buttonReleaseSpec(style: AnimationStyle, elasticity: Float): FiniteAnimationSpec<T> =
        if (style == AnimationStyle.NONE) snap() else responseSpring(0.28f, 0.22f, style, elasticity)

    fun contentCrossfadeTween(style: AnimationStyle, durationScale: Float): TweenSpec<Float> =
        tween(
            durationMillis = if (style == AnimationStyle.NONE) 0 else (CONTENT_CROSSFADE_MS / durationScale).toInt().coerceIn(70, 340),
            easing = ContentCrossfadeEasing
        )
}
