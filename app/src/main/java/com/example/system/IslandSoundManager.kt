package com.example.system

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
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
    CALC_KEY,
    MUSIC_PLAY,
    MUSIC_PAUSE,
    MUSIC_NEXT,
    VAULT_ADD,
    SEARCH_OPEN
}

/**
 * Low-latency SoundPool earcon engine for Build #5.
 * - Preloads all 13 single-family assets at startup; maxStreams = 4; never requests AudioFocus.
 * - Priority hierarchy: HERO (TIMER_DONE = 10, fixed 1.0x pitch) > Confirm (6) > Nav (4) > Micro-taps (1).
 * - Stop-before-replay for TAP and CALC_KEY so rapid taps stay crisp without stacking into a wash.
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

    @Volatile
    private var soundEnabled: Boolean = true

    @Volatile
    private var soundVolume: Float = 0.75f

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
                .setMaxStreams(4)
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
            soundIds[IslandSoundEvent.CALC_KEY] = pool.load(appContext, R.raw.sfx_calc_key, 1)
            soundIds[IslandSoundEvent.MUSIC_PLAY] = pool.load(appContext, R.raw.sfx_music_play, 1)
            soundIds[IslandSoundEvent.MUSIC_PAUSE] = pool.load(appContext, R.raw.sfx_music_pause, 1)
            soundIds[IslandSoundEvent.MUSIC_NEXT] = pool.load(appContext, R.raw.sfx_music_next, 1)
            soundIds[IslandSoundEvent.VAULT_ADD] = pool.load(appContext, R.raw.sfx_vault_add, 1)
            soundIds[IslandSoundEvent.SEARCH_OPEN] = pool.load(appContext, R.raw.sfx_search_open, 1)

            soundPool = pool
        }
    }

    private fun observeSettings() {
        scope.launch {
            settingsRepository.settingsFlow.collectLatest { s ->
                soundEnabled = s.soundEnabled
                soundVolume = s.soundVolume.coerceIn(0f, 1f)
            }
        }
    }

    fun isSoundEnabled(): Boolean = soundEnabled

    fun currentVolume(): Float = soundVolume

    fun updateCachedAudioSettings(enabled: Boolean, volume: Float = soundVolume) {
        soundEnabled = enabled
        soundVolume = volume.coerceIn(0f, 1f)
    }

    fun play(event: IslandSoundEvent): Boolean {
        if (!soundEnabled) return false
        val baseVol = soundVolume.coerceIn(0f, 1f)
        if (baseVol <= 0f) return false

        val pool = soundPool ?: return false
        val sampleId = soundIds[event] ?: return false
        if (sampleId == 0) return false

        if (loadedSampleIds.isNotEmpty() && !loadedSampleIds.contains(sampleId)) {
            return false
        }

        // Stop-before-replay for high-frequency taps so 10x calc taps stay crisp
        if (event == IslandSoundEvent.TAP || event == IslandSoundEvent.CALC_KEY) {
            activeStreams[event]?.let { prevStream ->
                if (prevStream != 0) runCatching { pool.stop(prevStream) }
            }
        }

        val priority = when (event) {
            IslandSoundEvent.TIMER_DONE -> 10
            IslandSoundEvent.MUSIC_PLAY, IslandSoundEvent.VAULT_ADD -> 6
            IslandSoundEvent.EXPAND,
            IslandSoundEvent.COLLAPSE,
            IslandSoundEvent.MUSIC_NEXT,
            IslandSoundEvent.SEARCH_OPEN,
            IslandSoundEvent.TOGGLE_ON,
            IslandSoundEvent.TOGGLE_OFF -> 4
            IslandSoundEvent.TAP,
            IslandSoundEvent.CALC_KEY,
            IslandSoundEvent.TIMER_SET,
            IslandSoundEvent.MUSIC_PAUSE -> 1
        }

        // Pitch rate per Point 5 recipes
        val rate = when (event) {
            IslandSoundEvent.TIMER_DONE -> 1.0f // Fixed 1.00x hero chime
            IslandSoundEvent.CALC_KEY, IslandSoundEvent.MUSIC_NEXT -> 0.97f + (Math.random().toFloat() * 0.06f) // +-3%
            IslandSoundEvent.TAP, IslandSoundEvent.TOGGLE_ON, IslandSoundEvent.TOGGLE_OFF -> 0.98f + (Math.random().toFloat() * 0.04f) // +-2%
            else -> 0.99f + (Math.random().toFloat() * 0.02f) // +-1%
        }

        // Gain jitter: +-0.5 dB for hero/nav, +-1 dB for frequent micro-taps
        val gainJitter = when (event) {
            IslandSoundEvent.TAP, IslandSoundEvent.CALC_KEY, IslandSoundEvent.MUSIC_NEXT,
            IslandSoundEvent.TOGGLE_ON, IslandSoundEvent.TOGGLE_OFF ->
                0.891f + (Math.random().toFloat() * 0.231f)
            else ->
                0.944f + (Math.random().toFloat() * 0.118f)
        }
        val vol = (baseVol * gainJitter).coerceIn(0.01f, 1f)

        var streamId = runCatching {
            pool.play(sampleId, vol, vol, priority, 0, rate)
        }.getOrDefault(0)

        // Handle streamID == 0 gracefully for high-priority alerts by freeing lowest stream once
        if (streamId == 0 && priority >= 6) {
            val lowest = activeStreams[IslandSoundEvent.CALC_KEY] ?: activeStreams[IslandSoundEvent.TAP]
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

    /**
     * Fires sound + paired haptic from the same call site (unified within <20ms).
     * Timer Done adds a second confirm light tick at 300 ms.
     */
    fun playWithHaptic(
        event: IslandSoundEvent,
        view: View? = null,
        fallbackHaptic: (() -> Unit)? = null
    ): Boolean {
        val played = play(event)
        if (view != null) {
            val constant = when (event) {
                IslandSoundEvent.TAP, IslandSoundEvent.TOGGLE_ON, IslandSoundEvent.TOGGLE_OFF, IslandSoundEvent.MUSIC_PAUSE ->
                    HapticFeedbackConstants.VIRTUAL_KEY
                IslandSoundEvent.CALC_KEY, IslandSoundEvent.MUSIC_NEXT ->
                    HapticFeedbackConstants.KEYBOARD_TAP
                IslandSoundEvent.TIMER_SET ->
                    HapticFeedbackConstants.CLOCK_TICK
                IslandSoundEvent.EXPAND, IslandSoundEvent.COLLAPSE, IslandSoundEvent.SEARCH_OPEN ->
                    HapticFeedbackConstants.CONTEXT_CLICK
                IslandSoundEvent.VAULT_ADD ->
                    HapticFeedbackConstants.LONG_PRESS
                IslandSoundEvent.MUSIC_PLAY, IslandSoundEvent.TIMER_DONE ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
                    else HapticFeedbackConstants.LONG_PRESS
            }
            view.performHapticFeedback(constant)
            if (event == IslandSoundEvent.TIMER_DONE) {
                scope.launch {
                    delay(300L)
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
            }
        } else {
            fallbackHaptic?.invoke()
            if (event == IslandSoundEvent.TIMER_DONE && fallbackHaptic != null) {
                scope.launch {
                    delay(300L)
                    fallbackHaptic()
                }
            }
        }
        return played
    }

    fun playExpand() { play(IslandSoundEvent.EXPAND) }
    fun playCollapse() { play(IslandSoundEvent.COLLAPSE) }
    fun playTap() { play(IslandSoundEvent.TAP) }
    fun playTimerSet() { play(IslandSoundEvent.TIMER_SET) }
    fun playTimerDone() { play(IslandSoundEvent.TIMER_DONE) }
    fun playToggle(isOn: Boolean = true) {
        play(if (isOn) IslandSoundEvent.TOGGLE_ON else IslandSoundEvent.TOGGLE_OFF)
    }
    fun playCalcKey() { play(IslandSoundEvent.CALC_KEY) }
    fun playMusicPlay() { play(IslandSoundEvent.MUSIC_PLAY) }
    fun playMusicPause() { play(IslandSoundEvent.MUSIC_PAUSE) }
    fun playMusicNext() { play(IslandSoundEvent.MUSIC_NEXT) }
    fun playVaultAdd() { play(IslandSoundEvent.VAULT_ADD) }
    fun playSearchOpen() { play(IslandSoundEvent.SEARCH_OPEN) }

    fun release() {
        runCatching { soundPool?.release() }
        soundPool = null
        loadedSampleIds.clear()
        soundIds.clear()
        activeStreams.clear()
    }
}
