package com.example.system

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

enum class IslandSoundEvent {
    EXPAND,
    COLLAPSE,
    TAP,
    TIMER_SET,
    TIMER_DONE,
    TOGGLE_ON,
    TOGGLE_OFF,
    MUSIC_PLAY,
    MUSIC_PAUSE,
    MUSIC_NEXT,
    SHELF_ADD,
    SEARCH_OPEN,
    NOTIFICATION,
    ERROR,
    SUCCESS,
    SCREENSHOT,
    CHARGING,
    LOW_BATTERY,
    COPY,
    MORPH
}

/**
 * Low-latency SoundPool earcon engine for the C-major mallet-pluck family (19 core + 1 default-OFF morph).
 * - Preloads all 20 WAVs at startup; maxStreams = 6; never requests AudioFocus.
 * - Plays ONLY after SoundPool.OnLoadCompleteListener confirms the sample is loaded.
 * - Target levels (-21 dBFS to -6 dBFS) are baked directly into the WAV files so play() uses
 *   `1.0f * userVolume * volJitter` without double-attenuation.
 * - Rate jitter 0.97..1.03 and volume jitter +/- 1 dB (0.891..1.122).
 * - 45 ms retrigger cooldown on rapid ticks/toggles/copy/screenshot.
 * - Paired haptic choreography: haptics first, sound second.
 */
class IslandSoundManager(
    context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val loadedSampleIds = ConcurrentHashMap.newKeySet<Int>()
    private val soundIds = ConcurrentHashMap<IslandSoundEvent, Int>()
    private val activeStreams = ConcurrentHashMap<IslandSoundEvent, Int>()
    private val lastPlayedMs = ConcurrentHashMap<IslandSoundEvent, Long>()

    @Volatile
    private var soundEnabled: Boolean = true

    @Volatile
    private var soundVolume: Float = 0.75f

    @Volatile
    private var sfxMorphEnabled: Boolean = false

    @Volatile
    private var hapticsStrength: HapticsStrength = HapticsStrength.LIGHT

    private var soundPool: SoundPool? = null

    init {
        initSoundPool()
        observeSettings()
    }

    private fun initSoundPool() {
        runCatching {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val pool = SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(attrs)
                .build()

            pool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0 && sampleId > 0) {
                    loadedSampleIds.add(sampleId)
                }
            }

            soundIds[IslandSoundEvent.EXPAND] = pool.load(appContext, R.raw.sfx_expand, 1)
            soundIds[IslandSoundEvent.COLLAPSE] = pool.load(appContext, R.raw.sfx_collapse, 1)
            soundIds[IslandSoundEvent.TAP] = pool.load(appContext, R.raw.sfx_tap, 1)
            soundIds[IslandSoundEvent.TIMER_SET] = pool.load(appContext, R.raw.sfx_timer_set, 1)
            soundIds[IslandSoundEvent.TIMER_DONE] = pool.load(appContext, R.raw.sfx_timer_done, 1)
            soundIds[IslandSoundEvent.TOGGLE_ON] = pool.load(appContext, R.raw.sfx_toggle, 1)
            soundIds[IslandSoundEvent.TOGGLE_OFF] = pool.load(appContext, R.raw.sfx_toggle_off, 1)
            soundIds[IslandSoundEvent.MUSIC_PLAY] = pool.load(appContext, R.raw.sfx_music_play, 1)
            soundIds[IslandSoundEvent.MUSIC_PAUSE] = pool.load(appContext, R.raw.sfx_music_pause, 1)
            soundIds[IslandSoundEvent.MUSIC_NEXT] = pool.load(appContext, R.raw.sfx_music_next, 1)
            soundIds[IslandSoundEvent.SHELF_ADD] = pool.load(appContext, R.raw.sfx_shelf_add, 1)
            soundIds[IslandSoundEvent.SEARCH_OPEN] = pool.load(appContext, R.raw.sfx_search_open, 1)
            soundIds[IslandSoundEvent.NOTIFICATION] = pool.load(appContext, R.raw.sfx_notification, 1)
            soundIds[IslandSoundEvent.ERROR] = pool.load(appContext, R.raw.sfx_error, 1)
            soundIds[IslandSoundEvent.SUCCESS] = pool.load(appContext, R.raw.sfx_success, 1)
            soundIds[IslandSoundEvent.SCREENSHOT] = pool.load(appContext, R.raw.sfx_screenshot, 1)
            soundIds[IslandSoundEvent.CHARGING] = pool.load(appContext, R.raw.sfx_charging, 1)
            soundIds[IslandSoundEvent.LOW_BATTERY] = pool.load(appContext, R.raw.sfx_low_battery, 1)
            soundIds[IslandSoundEvent.COPY] = pool.load(appContext, R.raw.sfx_copy, 1)
            soundIds[IslandSoundEvent.MORPH] = pool.load(appContext, R.raw.sfx_morph, 1)

            soundPool = pool
        }
    }

    private fun observeSettings() {
        scope.launch {
            settingsRepository.settingsFlow.collectLatest { s ->
                soundEnabled = s.soundEnabled
                soundVolume = s.soundVolume.coerceIn(0f, 1f)
                sfxMorphEnabled = s.sfxMorphEnabled
                hapticsStrength = s.hapticsStrength
            }
        }
    }

    fun isSoundEnabled(): Boolean = soundEnabled

    fun currentVolume(): Float = soundVolume

    fun updateCachedAudioSettings(
        enabled: Boolean,
        volume: Float = soundVolume,
        morphEnabled: Boolean = sfxMorphEnabled
    ) {
        soundEnabled = enabled
        soundVolume = volume.coerceIn(0f, 1f)
        sfxMorphEnabled = morphEnabled
    }

    fun play(event: IslandSoundEvent): Boolean {
        if (!soundEnabled) return false
        if (event == IslandSoundEvent.MORPH && !sfxMorphEnabled) return false
        val baseVol = soundVolume.coerceIn(0f, 1f)
        if (baseVol <= 0f) return false

        val pool = soundPool ?: return false
        val sampleId = soundIds[event] ?: return false
        if (sampleId == 0) return false

        if (!loadedSampleIds.contains(sampleId)) {
            return false
        }

        // 45 ms retrigger cooldown on rapid ticks & micro-feedback
        val nowMs = android.os.SystemClock.uptimeMillis()
        if (event == IslandSoundEvent.TAP ||
            event == IslandSoundEvent.MUSIC_NEXT ||
            event == IslandSoundEvent.TOGGLE_ON ||
            event == IslandSoundEvent.TOGGLE_OFF ||
            event == IslandSoundEvent.SCREENSHOT ||
            event == IslandSoundEvent.COPY
        ) {
            val prevMs = lastPlayedMs[event] ?: 0L
            if (nowMs - prevMs < 45L) {
                return false
            }
        }
        lastPlayedMs[event] = nowMs

        if (event == IslandSoundEvent.TAP) {
            activeStreams[event]?.let { prevStream ->
                if (prevStream != 0) runCatching { pool.stop(prevStream) }
            }
        }

        val priority = when (event) {
            IslandSoundEvent.TIMER_DONE,
            IslandSoundEvent.SUCCESS,
            IslandSoundEvent.SHELF_ADD -> 10
            IslandSoundEvent.NOTIFICATION,
            IslandSoundEvent.LOW_BATTERY,
            IslandSoundEvent.ERROR -> 8
            IslandSoundEvent.MUSIC_PLAY,
            IslandSoundEvent.CHARGING -> 6
            IslandSoundEvent.EXPAND,
            IslandSoundEvent.COLLAPSE,
            IslandSoundEvent.SEARCH_OPEN,
            IslandSoundEvent.TIMER_SET,
            IslandSoundEvent.TOGGLE_ON,
            IslandSoundEvent.TOGGLE_OFF,
            IslandSoundEvent.MUSIC_PAUSE,
            IslandSoundEvent.MORPH -> 4
            IslandSoundEvent.TAP,
            IslandSoundEvent.MUSIC_NEXT,
            IslandSoundEvent.SCREENSHOT,
            IslandSoundEvent.COPY -> 1
        }

        val rate = 0.97f + (Math.random().toFloat() * 0.06f)
        val volJitter = 0.891f + (Math.random().toFloat() * 0.231f)
        // Baked WAV peaks handle hierarchy cleanly; play at 1.0 * userVolume * jitter
        val vol = (baseVol * volJitter).coerceIn(0.01f, 1f)

        var streamId = runCatching {
            pool.play(sampleId, vol, vol, priority, 0, rate)
        }.getOrDefault(0)

        if (streamId == 0 && priority >= 6) {
            val lowest = activeStreams[IslandSoundEvent.TAP] ?: activeStreams[IslandSoundEvent.MUSIC_NEXT]
            if (lowest != null && lowest != 0) {
                runCatching { pool.stop(lowest) }
                streamId = runCatching { pool.play(sampleId, vol, vol, priority, 0, rate) }.getOrDefault(0)
            }
        }

        if (streamId != 0) {
            activeStreams[event] = streamId
        }
        return streamId != 0
    }

    fun playWithHaptic(
        event: IslandSoundEvent,
        view: View? = null,
        fallbackHaptic: (() -> Unit)? = null
    ): Boolean {
        val triggerHaptic: (Int) -> Unit = { constant ->
            if (hapticsStrength != HapticsStrength.OFF) {
                if (view != null) {
                    view.performHapticFeedback(constant)
                } else {
                    fallbackHaptic?.invoke() ?: triggerSystemHaptic(constant)
                }
            }
        }

        when (event) {
            IslandSoundEvent.TOGGLE_ON, IslandSoundEvent.TOGGLE_OFF -> {
                triggerHaptic(HapticFeedbackConstants.VIRTUAL_KEY)
                scope.launch {
                    delay(10L)
                    play(event)
                }
                return soundEnabled && soundVolume > 0f
            }

            IslandSoundEvent.TIMER_DONE,
            IslandSoundEvent.SHELF_ADD,
            IslandSoundEvent.SUCCESS -> {
                val played = play(event)
                triggerHaptic(HapticFeedbackConstants.CLOCK_TICK)
                scope.launch {
                    delay(135L)
                    triggerHaptic(HapticFeedbackConstants.CLOCK_TICK)
                    delay(135L)
                    triggerHaptic(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
                        else HapticFeedbackConstants.CONTEXT_CLICK
                    )
                }
                return played
            }

            IslandSoundEvent.EXPAND, IslandSoundEvent.COLLAPSE, IslandSoundEvent.MORPH -> {
                triggerHaptic(HapticFeedbackConstants.CONTEXT_CLICK)
                return play(event)
            }

            IslandSoundEvent.ERROR -> {
                val rejectConst = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.REJECT
                } else {
                    HapticFeedbackConstants.LONG_PRESS
                }
                triggerHaptic(rejectConst)
                return play(event)
            }

            IslandSoundEvent.MUSIC_NEXT -> {
                triggerHaptic(HapticFeedbackConstants.KEYBOARD_TAP)
                return play(event)
            }

            IslandSoundEvent.TIMER_SET, IslandSoundEvent.CHARGING -> {
                triggerHaptic(HapticFeedbackConstants.CLOCK_TICK)
                return play(event)
            }

            IslandSoundEvent.COPY -> {
                triggerHaptic(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
                    else HapticFeedbackConstants.KEYBOARD_TAP
                )
                return play(event)
            }

            else -> {
                triggerHaptic(HapticFeedbackConstants.VIRTUAL_KEY)
                return play(event)
            }
        }
    }

    private fun triggerSystemHaptic(constant: Int) {
        if (hapticsStrength == HapticsStrength.OFF) return
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effect = when {
                    hapticsStrength == HapticsStrength.MEDIUM -> VibrationEffect.EFFECT_CLICK
                    constant == HapticFeedbackConstants.LONG_PRESS -> VibrationEffect.EFFECT_HEAVY_CLICK
                    else -> VibrationEffect.EFFECT_TICK
                }
                vibrator.vibrate(VibrationEffect.createPredefined(effect))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(if (hapticsStrength == HapticsStrength.MEDIUM) 16L else 8L)
            }
        }
    }

    fun playExpand(view: View? = null) { playWithHaptic(IslandSoundEvent.EXPAND, view) }
    fun playCollapse(view: View? = null) { playWithHaptic(IslandSoundEvent.COLLAPSE, view) }
    fun playTap(view: View? = null) { playWithHaptic(IslandSoundEvent.TAP, view) }
    fun playTimerSet(view: View? = null) { playWithHaptic(IslandSoundEvent.TIMER_SET, view) }
    fun playTimerDone(view: View? = null) { playWithHaptic(IslandSoundEvent.TIMER_DONE, view) }
    fun playToggle(isOn: Boolean = true, view: View? = null) {
        playWithHaptic(if (isOn) IslandSoundEvent.TOGGLE_ON else IslandSoundEvent.TOGGLE_OFF, view)
    }
    fun playMusicPlay(view: View? = null) { playWithHaptic(IslandSoundEvent.MUSIC_PLAY, view) }
    fun playMusicPause(view: View? = null) { playWithHaptic(IslandSoundEvent.MUSIC_PAUSE, view) }
    fun playMusicNext(view: View? = null) { playWithHaptic(IslandSoundEvent.MUSIC_NEXT, view) }
    fun playShelfAdd(view: View? = null) { playWithHaptic(IslandSoundEvent.SHELF_ADD, view) }
    fun playSearchOpen(view: View? = null) { playWithHaptic(IslandSoundEvent.SEARCH_OPEN, view) }
    fun playNotification(view: View? = null) { playWithHaptic(IslandSoundEvent.NOTIFICATION, view) }
    fun playError(view: View? = null) { playWithHaptic(IslandSoundEvent.ERROR, view) }
    fun playSuccess(view: View? = null) { playWithHaptic(IslandSoundEvent.SUCCESS, view) }
    fun playScreenshot(view: View? = null) { playWithHaptic(IslandSoundEvent.SCREENSHOT, view) }
    fun playCharging(view: View? = null) { playWithHaptic(IslandSoundEvent.CHARGING, view) }
    fun playLowBattery(view: View? = null) { playWithHaptic(IslandSoundEvent.LOW_BATTERY, view) }
    fun playCopy(view: View? = null) { playWithHaptic(IslandSoundEvent.COPY, view) }
    fun playMorph(view: View? = null) {
        if (sfxMorphEnabled) playWithHaptic(IslandSoundEvent.MORPH, view)
    }

    fun release() {
        runCatching { soundPool?.release() }
        soundPool = null
        loadedSampleIds.clear()
        soundIds.clear()
        activeStreams.clear()
        lastPlayedMs.clear()
    }
}
