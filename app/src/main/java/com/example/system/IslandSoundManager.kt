package com.example.system

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
 * Low-latency SoundPool manager giving every Dynamic Island interaction its own unique sound.
 * Uses small preloaded WAV assets in res/raw/ and respects Settings (soundEnabled, soundVolume).
 */
class IslandSoundManager(
    context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val loadedSampleIds = ConcurrentHashMap.newKeySet<Int>()
    private val soundIds = ConcurrentHashMap<IslandSoundEvent, Int>()

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
        val vol = soundVolume.coerceIn(0f, 1f)
        if (vol <= 0f) return false

        val pool = soundPool ?: return false
        val sampleId = soundIds[event] ?: return false
        if (sampleId == 0) return false

        if (loadedSampleIds.isNotEmpty() && !loadedSampleIds.contains(sampleId)) {
            return false
        }

        val streamId = runCatching {
            pool.play(sampleId, vol, vol, 1, 0, 1.0f)
        }.getOrDefault(0)
        return streamId != 0
    }

    fun playExpand() {
        play(IslandSoundEvent.EXPAND)
    }

    fun playCollapse() {
        play(IslandSoundEvent.COLLAPSE)
    }

    fun playTap() {
        play(IslandSoundEvent.TAP)
    }

    fun playTimerSet() {
        play(IslandSoundEvent.TIMER_SET)
    }

    fun playTimerDone() {
        play(IslandSoundEvent.TIMER_DONE)
    }

    fun playToggle(isOn: Boolean = true) {
        play(if (isOn) IslandSoundEvent.TOGGLE_ON else IslandSoundEvent.TOGGLE_OFF)
    }

    fun playCalcKey() {
        play(IslandSoundEvent.CALC_KEY)
    }

    fun playMusicPlay() {
        play(IslandSoundEvent.MUSIC_PLAY)
    }

    fun playMusicPause() {
        play(IslandSoundEvent.MUSIC_PAUSE)
    }

    fun playMusicNext() {
        play(IslandSoundEvent.MUSIC_NEXT)
    }

    fun playVaultAdd() {
        play(IslandSoundEvent.VAULT_ADD)
    }

    fun playSearchOpen() {
        play(IslandSoundEvent.SEARCH_OPEN)
    }

    fun release() {
        runCatching { soundPool?.release() }
        soundPool = null
        loadedSampleIds.clear()
        soundIds.clear()
    }
}
