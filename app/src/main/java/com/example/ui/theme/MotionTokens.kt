package com.example.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.example.system.AnimationStyle
import com.example.system.OpeningEffect

/**
 * Exact Material 3 Expressive spring motion specification for the Dynamic Island.
 *
 * M3 Expressive table:
 * - Island expand/collapse = spatial default (damping = 0.8f, stiffness = 380f, ~460 ms, 1.5% overshoot)
 * - Small pops             = spatial fast    (damping = 0.6f, stiffness = 800f, ~380 ms)
 * - Fades / glass          = effects default (damping = 1.0f, stiffness = 1600f, never overshoots)
 * - Pill morph             = StiffnessLow (200f), dampingRatio = 0.6f
 * - Collapse               = critically damped (1.0f, never overshoots on large surfaces), ~240 ms (faster than expand)
 * - Tap squish             = 0.97f at 0 ms
 * - Content enter          = fadeIn(tween(300, delayMillis = 300)), stagger step 200 ms, glass fades in 180 ms
 */
object MotionTokens {

    // Exact M3 Expressive Spring Table
    const val SPATIAL_DEFAULT_DAMPING = 0.8f
    const val SPATIAL_DEFAULT_STIFFNESS = 380f

    const val SPATIAL_FAST_DAMPING = 0.6f
    const val SPATIAL_FAST_STIFFNESS = 800f

    const val EFFECTS_DEFAULT_DAMPING = Spring.DampingRatioNoBouncy // 1.0f
    const val EFFECTS_DEFAULT_STIFFNESS = 1600f

    const val PILL_MORPH_DAMPING = 0.6f
    const val PILL_MORPH_STIFFNESS = Spring.StiffnessLow // 200f

    // Collapse settles in ~240 ms cleanly with zero overshoot on large surfaces
    const val COLLAPSE_DAMPING = Spring.DampingRatioNoBouncy // 1.0f
    const val COLLAPSE_STIFFNESS = 950f

    // Choreography timing constants
    const val TAP_SQUISH_SCALE = 0.97f
    const val BUTTON_PRESS_SCALE = TAP_SQUISH_SCALE
    const val GLASS_FADE_MS = 180
    const val CONTENT_ENTER_MS = 300
    const val CONTENT_ENTER_DELAY_MS = 300
    const val CONTENT_FADE_DELAY_MS = 150
    const val CONTENT_EXIT_MS = 80
    const val CONTENT_STAGGER_MS = 200
    const val TILE_STAGGER_MS = 40
    const val COLLAPSE_MS = 240
    const val CONTENT_CROSSFADE_MS = 180
    const val TIMER_TICK_CROSSFADE_MS = 140
    val ContentCrossfadeEasing = CubicBezierEasing(0.16f, 1.0f, 0.30f, 1.0f)
    val AppleEase = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

    // Round 3 Apple / per-feature spring constants
    const val APPLE_EXPAND_DAMPING = 0.75f
    const val APPLE_EXPAND_STIFFNESS = 400f
    const val ALERT_BOUNCE_DAMPING = 0.8f
    const val ALERT_BOUNCE_STIFFNESS = 800f
    const val TILE_POP_DAMPING = 0.6f
    const val TILE_POP_STIFFNESS = 800f

    fun <T> appleSpring(durationScale: Float = 1.0f): SpringSpec<T> = spring(
        dampingRatio = APPLE_EXPAND_DAMPING,
        stiffness = APPLE_EXPAND_STIFFNESS * durationScale.coerceIn(0.5f, 1.8f)
    )

    fun <T> alertBounceSpring(): SpringSpec<T> = spring(
        dampingRatio = ALERT_BOUNCE_DAMPING,
        stiffness = ALERT_BOUNCE_STIFFNESS
    )

    fun <T> tilePopSpring(): SpringSpec<T> = spring(
        dampingRatio = TILE_POP_DAMPING,
        stiffness = TILE_POP_STIFFNESS
    )

    fun <T> m3SpatialDefault(): SpringSpec<T> = spring(
        dampingRatio = SPATIAL_DEFAULT_DAMPING,
        stiffness = SPATIAL_DEFAULT_STIFFNESS
    )

    fun <T> m3SpatialFast(): SpringSpec<T> = spring(
        dampingRatio = SPATIAL_FAST_DAMPING,
        stiffness = SPATIAL_FAST_STIFFNESS
    )

    fun <T> m3EffectsDefault(): SpringSpec<T> = spring(
        dampingRatio = EFFECTS_DEFAULT_DAMPING,
        stiffness = EFFECTS_DEFAULT_STIFFNESS
    )

    fun <T> m3PillMorph(): SpringSpec<T> = spring(
        dampingRatio = PILL_MORPH_DAMPING,
        stiffness = PILL_MORPH_STIFFNESS
    )

    fun <T> liquidOpenSpec(
        style: AnimationStyle = AnimationStyle.FRUIT,
        elasticity: Float = 0.65f,
        durationScale: Float = 1.0f
    ): SpringSpec<T> {
        val scale = durationScale.coerceIn(0.5f, 1.8f)
        val damping = if (style == AnimationStyle.SMOOTH || style == AnimationStyle.NONE) {
            Spring.DampingRatioNoBouncy
        } else {
            (0.76f - elasticity * 0.14f).coerceIn(0.58f, 0.85f)
        }
        return spring(
            dampingRatio = damping,
            stiffness = 320f * scale
        )
    }

    fun <T> swipeDismissSpec(style: AnimationStyle = AnimationStyle.SPRING): FiniteAnimationSpec<T> =
        if (style == AnimationStyle.NONE) snap()
        else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 1250f)

    fun <T> spatialDefault(
        style: AnimationStyle = AnimationStyle.SPRING,
        @Suppress("UNUSED_PARAMETER") elasticity: Float = 0.65f,
        durationScale: Float = 1.0f
    ): SpringSpec<T> {
        val scale = durationScale.coerceIn(0.5f, 1.8f)
        val damping = if (style == AnimationStyle.SMOOTH || style == AnimationStyle.NONE) {
            Spring.DampingRatioNoBouncy
        } else {
            SPATIAL_DEFAULT_DAMPING
        }
        return spring(
            dampingRatio = damping,
            stiffness = SPATIAL_DEFAULT_STIFFNESS * scale
        )
    }

    fun <T> spatialFast(
        style: AnimationStyle = AnimationStyle.SPRING,
        @Suppress("UNUSED_PARAMETER") elasticity: Float = 0.65f,
        durationScale: Float = 1.0f
    ): SpringSpec<T> {
        val scale = durationScale.coerceIn(0.5f, 1.8f)
        val damping = if (style == AnimationStyle.SMOOTH || style == AnimationStyle.NONE) {
            Spring.DampingRatioNoBouncy
        } else {
            SPATIAL_FAST_DAMPING
        }
        return spring(
            dampingRatio = damping,
            stiffness = SPATIAL_FAST_STIFFNESS * scale
        )
    }

    fun <T> effectsDefault(durationScale: Float = 1.0f): SpringSpec<T> = spring(
        dampingRatio = EFFECTS_DEFAULT_DAMPING,
        stiffness = EFFECTS_DEFAULT_STIFFNESS * durationScale.coerceIn(0.5f, 1.8f)
    )

    fun <T> effectsFast(durationScale: Float = 1.0f): SpringSpec<T> =
        effectsDefault(durationScale)

    fun <T> pillMorphSpring(
        style: AnimationStyle = AnimationStyle.SPRING,
        durationScale: Float = 1.0f
    ): SpringSpec<T> {
        val damping = if (style == AnimationStyle.SMOOTH || style == AnimationStyle.NONE) {
            Spring.DampingRatioNoBouncy
        } else {
            PILL_MORPH_DAMPING
        }
        return spring(
            dampingRatio = damping,
            stiffness = PILL_MORPH_STIFFNESS * durationScale.coerceIn(0.5f, 1.8f)
        )
    }

    fun <T> pillMorphSpec(
        style: AnimationStyle,
        durationScale: Float = 1.0f
    ): FiniteAnimationSpec<T> = if (style == AnimationStyle.NONE) {
        snap()
    } else {
        pillMorphSpring(style, durationScale)
    }

    fun <T> idleToMediumSpec(
        style: AnimationStyle,
        elasticity: Float,
        durationScale: Float
    ): FiniteAnimationSpec<T> = if (style == AnimationStyle.NONE) {
        snap()
    } else {
        spatialDefault(style, elasticity, durationScale)
    }

    fun <T> mediumToFullSpec(
        style: AnimationStyle,
        elasticity: Float,
        durationScale: Float
    ): FiniteAnimationSpec<T> = if (style == AnimationStyle.NONE) {
        snap()
    } else {
        spatialDefault(style, elasticity, durationScale)
    }

    fun <T> openingEffectSpec(
        openingEffect: OpeningEffect,
        style: AnimationStyle,
        elasticity: Float,
        durationScale: Float
    ): FiniteAnimationSpec<T> {
        if (style == AnimationStyle.NONE) return snap()
        return when (openingEffect) {
            OpeningEffect.POP -> spatialFast(style, elasticity, durationScale)
            OpeningEffect.SMOOTH -> spatialDefault(AnimationStyle.SMOOTH, elasticity, durationScale)
            OpeningEffect.LIQUID -> liquidOpenSpec(style, elasticity, durationScale)
            OpeningEffect.SPRING -> spatialDefault(style, elasticity, durationScale)
        }
    }

    fun <T> collapseSpec(
        style: AnimationStyle,
        durationScale: Float
    ): FiniteAnimationSpec<T> = if (style == AnimationStyle.NONE) {
        snap()
    } else {
        spring(
            dampingRatio = COLLAPSE_DAMPING,
            stiffness = COLLAPSE_STIFFNESS * durationScale.coerceIn(0.5f, 1.8f)
        )
    }

    fun <T> buttonPressSpec(style: AnimationStyle): FiniteAnimationSpec<T> =
        if (style == AnimationStyle.NONE) snap() else spatialFast(AnimationStyle.SMOOTH)

    fun <T> buttonReleaseSpec(style: AnimationStyle, elasticity: Float): FiniteAnimationSpec<T> =
        if (style == AnimationStyle.NONE) snap() else spatialFast(style, elasticity)

    fun contentEnterTransition(style: AnimationStyle): EnterTransition =
        if (style == AnimationStyle.NONE) {
            fadeIn(tween(0))
        } else {
            fadeIn(tween(durationMillis = CONTENT_ENTER_MS, delayMillis = CONTENT_ENTER_DELAY_MS))
        }

    fun contentExitTransition(style: AnimationStyle): ExitTransition =
        if (style == AnimationStyle.NONE) {
            fadeOut(tween(0))
        } else {
            fadeOut(tween(durationMillis = COLLAPSE_MS / 2))
        }

    fun tabSwitchEnterTransition(style: AnimationStyle): EnterTransition =
        if (style == AnimationStyle.NONE) {
            fadeIn(tween(0))
        } else {
            fadeIn(tween(durationMillis = GLASS_FADE_MS, easing = ContentCrossfadeEasing))
        }

    fun tabSwitchExitTransition(style: AnimationStyle): ExitTransition =
        if (style == AnimationStyle.NONE) {
            fadeOut(tween(0))
        } else {
            fadeOut(tween(durationMillis = 100))
        }

    fun contentCrossfadeTween(style: AnimationStyle, durationScale: Float): TweenSpec<Float> =
        tween(
            durationMillis = if (style == AnimationStyle.NONE) 0 else (GLASS_FADE_MS / durationScale).toInt().coerceIn(80, 300),
            easing = ContentCrossfadeEasing
        )
}
