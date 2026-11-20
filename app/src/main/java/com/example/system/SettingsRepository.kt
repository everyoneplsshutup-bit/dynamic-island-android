package com.example.system

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "dynamic_island_settings")

enum class PillStyle { BLACK, GLASS }
enum class AccentTheme { ORANGE, CYAN, GREEN, DYNAMIC }
enum class SpringFeel { SNAPPY, BALANCED, GENTLE }
enum class HapticsStrength { OFF, LIGHT, MEDIUM }
enum class CompactPreference { AUTO, MUSIC, TIMER, BATTERY, NOTIFICATIONS }
enum class IdleWidgetType { NONE, CLOCK, BATTERY_RING, SEARCH_SHORTCUT, QUICK_NOTE }
enum class LockScreenPreset { MINIMAL, PRIVACY_HIDE_TEXT, HIDDEN }
enum class IslandGesture { OPEN_APP, EXPAND, TOGGLE_PLAY, OPEN_CUSTOMISE, SELECT_OCR }

enum class IslandTab(
    val title: String,
    val description: String,
    val isMainRow: Boolean
) {
    SEARCH("Search", "Apps, contacts, notes & inline math", true),
    MUSIC("Music", "PixelMusic-first player & live lyrics", true),
    CALC("Calc", "iOS-style working calculator", true),
    TIMER("Timer", "Countdown rings, Clock mirror & stopwatch", true),
    TOOLS("Tools", "Flashlight, DND, brightness & panels", true),
    VAULT("Vault", "10-minute auto-expiring photo vault", false),
    CLIPS("Clips", "Foreground-only clipboard history", false),
    NOTES("Notes", "On-device quick notes & links", false),
    MEMO("Memo", "One-tap voice memo recorder", false),
    ALERTS("Alerts", "Timer, charging & vault expiry alerts", false)
}

enum class QuickToggleType(val label: String) {
    FLASHLIGHT("Torch"),
    DND("DND"),
    BRIGHTNESS("Bright"),
    WIFI("Wi-Fi"),
    BLUETOOTH("Bluetooth"),
    SELECT_OCR("Select OCR"),
    VAULT_CAPTURE("Shot->Vault")
}

@Serializable
data class IslandSettings(
    // 1. Look
    val pillStyle: PillStyle = PillStyle.BLACK,
    val glassDensity: Float = 0.62f,
    val solidFallback: Boolean = false,
    val accentTheme: AccentTheme = AccentTheme.ORANGE,

    // 2. Motion
    val animationSpeed: Float = 1.0f,
    val springFeel: SpringFeel = SpringFeel.BALANCED,
    val reducedMotion: Boolean = false,
    val hapticsStrength: HapticsStrength = HapticsStrength.LIGHT,

    // 3. Pill & Calibration
    val cutoutXOffsetDp: Float = 0f,
    val cutoutYOffsetDp: Float = 6f,
    val cutoutWidthDp: Float = 26f,
    val cutoutHeightDp: Float = 26f,
    val compactPreference: CompactPreference = CompactPreference.AUTO,
    val idleWidget: IdleWidgetType = IdleWidgetType.NONE,
    val lockScreenPreset: LockScreenPreset = LockScreenPreset.MINIMAL,

    // 4. Features (Hard cap: max 8 enabled on island)
    val enabledTabs: List<IslandTab> = listOf(
        IslandTab.SEARCH,
        IslandTab.MUSIC,
        IslandTab.CALC,
        IslandTab.TIMER,
        IslandTab.TOOLS,
        IslandTab.VAULT,
        IslandTab.NOTES,
        IslandTab.ALERTS
    ),
    val musicEnabled: Boolean = true,
    val timerEnabled: Boolean = true,
    val calculatorEnabled: Boolean = true,
    val searchEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val batteryEnabled: Boolean = true,
    val togglesEnabled: Boolean = true,
    val vaultEnabled: Boolean = true,
    val notesEnabled: Boolean = true,
    val memoEnabled: Boolean = true,
    val clipsEnabled: Boolean = true,
    val ocrFeatureEnabled: Boolean = true,
    val floatingEnabled: Boolean = true,
    val alertsEnabled: Boolean = true,
    val includedQuickToggles: List<QuickToggleType> = listOf(
        QuickToggleType.FLASHLIGHT,
        QuickToggleType.DND,
        QuickToggleType.BRIGHTNESS,
        QuickToggleType.WIFI,
        QuickToggleType.BLUETOOTH,
        QuickToggleType.SELECT_OCR
    ),
    val pinnedAppPackages: List<String> = emptyList(),
    val floatingAppPackages: List<String> = emptyList(),
    val tapGesture: IslandGesture = IslandGesture.OPEN_APP,
    val longPressGesture: IslandGesture = IslandGesture.EXPAND,

    // 5. Privacy (HARD CONSTRAINT: screenReadingEnabled defaults to false)
    val screenReadingEnabled: Boolean = false,
    val ocrTierBEnabled: Boolean = false,
    val vaultAutoCapture3Finger: Boolean = true,
    val clipboardHistoryEnabled: Boolean = true,

    // Setup & Survival
    val wizardCompleted: Boolean = false,
    val accessibilityWasEnabled: Boolean = false,
    val verifiedFloatingPath: String = "UNTESTED"
) {
    companion object {
        const val MAX_ISLAND_FEATURES = 8
    }

    fun isTabMasterEnabled(tab: IslandTab): Boolean = when (tab) {
        IslandTab.SEARCH -> searchEnabled
        IslandTab.MUSIC -> musicEnabled
        IslandTab.CALC -> calculatorEnabled
        IslandTab.TIMER -> timerEnabled
        IslandTab.TOOLS -> togglesEnabled
        IslandTab.VAULT -> vaultEnabled
        IslandTab.CLIPS -> clipsEnabled
        IslandTab.NOTES -> notesEnabled
        IslandTab.MEMO -> memoEnabled
        IslandTab.ALERTS -> alertsEnabled
    }
}

class SettingsRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private object Keys {
        val PILL_STYLE = stringPreferencesKey("pill_style")
        val GLASS_DENSITY = floatPreferencesKey("glass_density")
        val SOLID_FALLBACK = booleanPreferencesKey("solid_fallback")
        val ACCENT_THEME = stringPreferencesKey("accent_theme")

        val ANIMATION_SPEED = floatPreferencesKey("animation_speed")
        val SPRING_FEEL = stringPreferencesKey("spring_feel")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val HAPTICS_STRENGTH = stringPreferencesKey("haptics_strength")

        val CUTOUT_X = floatPreferencesKey("cutout_x_dp")
        val CUTOUT_Y = floatPreferencesKey("cutout_y_dp")
        val CUTOUT_W = floatPreferencesKey("cutout_w_dp")
        val CUTOUT_H = floatPreferencesKey("cutout_h_dp")
        val COMPACT_PREF = stringPreferencesKey("compact_pref")
        val IDLE_WIDGET = stringPreferencesKey("idle_widget")
        val LOCK_SCREEN_PRESET = stringPreferencesKey("lock_screen_preset")

        val ENABLED_TABS = stringPreferencesKey("enabled_tabs_json")
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val TIMER_ENABLED = booleanPreferencesKey("timer_enabled")
        val CALC_ENABLED = booleanPreferencesKey("calc_enabled")
        val SEARCH_ENABLED = booleanPreferencesKey("search_enabled")
        val NOTIF_ENABLED = booleanPreferencesKey("notif_enabled")
        val BATTERY_ENABLED = booleanPreferencesKey("battery_enabled")
        val TOGGLES_ENABLED = booleanPreferencesKey("toggles_enabled")
        val VAULT_ENABLED = booleanPreferencesKey("vault_enabled")
        val NOTES_ENABLED = booleanPreferencesKey("notes_enabled")
        val MEMO_ENABLED = booleanPreferencesKey("memo_enabled")
        val CLIPS_ENABLED = booleanPreferencesKey("clips_enabled")
        val OCR_FEATURE_ENABLED = booleanPreferencesKey("ocr_feature_enabled")
        val FLOATING_ENABLED = booleanPreferencesKey("floating_enabled")
        val ALERTS_ENABLED = booleanPreferencesKey("alerts_enabled")
        val QUICK_TOGGLES = stringPreferencesKey("quick_toggles_json")
        val PINNED_APPS = stringPreferencesKey("pinned_apps_json")
        val FLOATING_APPS = stringPreferencesKey("floating_apps_json")
        val TAP_GESTURE = stringPreferencesKey("tap_gesture")
        val LONG_PRESS_GESTURE = stringPreferencesKey("long_press_gesture")

        val SCREEN_READING_ENABLED = booleanPreferencesKey("screen_reading_enabled")
        val OCR_TIER_B_ENABLED = booleanPreferencesKey("ocr_tier_b_enabled")
        val VAULT_AUTO_CAPTURE = booleanPreferencesKey("vault_auto_capture")
        val CLIPBOARD_HISTORY_ENABLED = booleanPreferencesKey("clipboard_history_enabled")

        val WIZARD_COMPLETED = booleanPreferencesKey("wizard_completed")
        val ACCESSIBILITY_WAS_ENABLED = booleanPreferencesKey("accessibility_was_enabled")
        val VERIFIED_FLOATING_PATH = stringPreferencesKey("verified_floating_path")
    }

    val settingsFlow: Flow<IslandSettings> = context.settingsDataStore.data.map { prefs ->
        val default = IslandSettings()
        IslandSettings(
            pillStyle = prefs[Keys.PILL_STYLE]?.let { runCatching { PillStyle.valueOf(it) }.getOrNull() } ?: default.pillStyle,
            glassDensity = prefs[Keys.GLASS_DENSITY] ?: default.glassDensity,
            solidFallback = prefs[Keys.SOLID_FALLBACK] ?: default.solidFallback,
            accentTheme = prefs[Keys.ACCENT_THEME]?.let { runCatching { AccentTheme.valueOf(it) }.getOrNull() } ?: default.accentTheme,

            animationSpeed = prefs[Keys.ANIMATION_SPEED] ?: default.animationSpeed,
            springFeel = prefs[Keys.SPRING_FEEL]?.let { runCatching { SpringFeel.valueOf(it) }.getOrNull() } ?: default.springFeel,
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: default.reducedMotion,
            hapticsStrength = prefs[Keys.HAPTICS_STRENGTH]?.let { runCatching { HapticsStrength.valueOf(it) }.getOrNull() } ?: default.hapticsStrength,

            cutoutXOffsetDp = prefs[Keys.CUTOUT_X] ?: default.cutoutXOffsetDp,
            cutoutYOffsetDp = prefs[Keys.CUTOUT_Y] ?: default.cutoutYOffsetDp,
            cutoutWidthDp = prefs[Keys.CUTOUT_W] ?: default.cutoutWidthDp,
            cutoutHeightDp = prefs[Keys.CUTOUT_H] ?: default.cutoutHeightDp,
            compactPreference = prefs[Keys.COMPACT_PREF]?.let { runCatching { CompactPreference.valueOf(it) }.getOrNull() } ?: default.compactPreference,
            idleWidget = prefs[Keys.IDLE_WIDGET]?.let { runCatching { IdleWidgetType.valueOf(it) }.getOrNull() } ?: default.idleWidget,
            lockScreenPreset = prefs[Keys.LOCK_SCREEN_PRESET]?.let { runCatching { LockScreenPreset.valueOf(it) }.getOrNull() } ?: default.lockScreenPreset,

            enabledTabs = prefs[Keys.ENABLED_TABS]?.let {
                runCatching { json.decodeFromString<List<IslandTab>>(it) }.getOrNull()
            }?.take(IslandSettings.MAX_ISLAND_FEATURES) ?: default.enabledTabs,
            musicEnabled = prefs[Keys.MUSIC_ENABLED] ?: default.musicEnabled,
            timerEnabled = prefs[Keys.TIMER_ENABLED] ?: default.timerEnabled,
            calculatorEnabled = prefs[Keys.CALC_ENABLED] ?: default.calculatorEnabled,
            searchEnabled = prefs[Keys.SEARCH_ENABLED] ?: default.searchEnabled,
            notificationsEnabled = prefs[Keys.NOTIF_ENABLED] ?: default.notificationsEnabled,
            batteryEnabled = prefs[Keys.BATTERY_ENABLED] ?: default.batteryEnabled,
            togglesEnabled = prefs[Keys.TOGGLES_ENABLED] ?: default.togglesEnabled,
            vaultEnabled = prefs[Keys.VAULT_ENABLED] ?: default.vaultEnabled,
            notesEnabled = prefs[Keys.NOTES_ENABLED] ?: default.notesEnabled,
            memoEnabled = prefs[Keys.MEMO_ENABLED] ?: default.memoEnabled,
            clipsEnabled = prefs[Keys.CLIPS_ENABLED] ?: default.clipsEnabled,
            ocrFeatureEnabled = prefs[Keys.OCR_FEATURE_ENABLED] ?: default.ocrFeatureEnabled,
            floatingEnabled = prefs[Keys.FLOATING_ENABLED] ?: default.floatingEnabled,
            alertsEnabled = prefs[Keys.ALERTS_ENABLED] ?: default.alertsEnabled,
            includedQuickToggles = prefs[Keys.QUICK_TOGGLES]?.let {
                runCatching { json.decodeFromString<List<QuickToggleType>>(it) }.getOrNull()
            } ?: default.includedQuickToggles,
            pinnedAppPackages = prefs[Keys.PINNED_APPS]?.let {
                runCatching { json.decodeFromString<List<String>>(it) }.getOrNull()
            } ?: default.pinnedAppPackages,
            floatingAppPackages = prefs[Keys.FLOATING_APPS]?.let {
                runCatching { json.decodeFromString<List<String>>(it) }.getOrNull()
            } ?: default.floatingAppPackages,
            tapGesture = prefs[Keys.TAP_GESTURE]?.let { runCatching { IslandGesture.valueOf(it) }.getOrNull() } ?: default.tapGesture,
            longPressGesture = prefs[Keys.LONG_PRESS_GESTURE]?.let { runCatching { IslandGesture.valueOf(it) }.getOrNull() } ?: default.longPressGesture,

            // HARD PRIVACY CONSTRAINT: default false
            screenReadingEnabled = prefs[Keys.SCREEN_READING_ENABLED] ?: false,
            ocrTierBEnabled = prefs[Keys.OCR_TIER_B_ENABLED] ?: false,
            vaultAutoCapture3Finger = prefs[Keys.VAULT_AUTO_CAPTURE] ?: default.vaultAutoCapture3Finger,
            clipboardHistoryEnabled = prefs[Keys.CLIPBOARD_HISTORY_ENABLED] ?: default.clipboardHistoryEnabled,

            wizardCompleted = prefs[Keys.WIZARD_COMPLETED] ?: false,
            accessibilityWasEnabled = prefs[Keys.ACCESSIBILITY_WAS_ENABLED] ?: false,
            verifiedFloatingPath = prefs[Keys.VERIFIED_FLOATING_PATH] ?: default.verifiedFloatingPath
        )
    }

    suspend fun update(transform: (IslandSettings) -> IslandSettings) {
        val current = settingsFlow.first()
        val updated = transform(current)
        writeAll(updated)
    }

    private suspend fun writeAll(s: IslandSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.PILL_STYLE] = s.pillStyle.name
            prefs[Keys.GLASS_DENSITY] = s.glassDensity.coerceIn(0f, 1f)
            prefs[Keys.SOLID_FALLBACK] = s.solidFallback
            prefs[Keys.ACCENT_THEME] = s.accentTheme.name

            prefs[Keys.ANIMATION_SPEED] = s.animationSpeed.coerceIn(0.5f, 1.8f)
            prefs[Keys.SPRING_FEEL] = s.springFeel.name
            prefs[Keys.REDUCED_MOTION] = s.reducedMotion
            prefs[Keys.HAPTICS_STRENGTH] = s.hapticsStrength.name

            prefs[Keys.CUTOUT_X] = s.cutoutXOffsetDp
            prefs[Keys.CUTOUT_Y] = s.cutoutYOffsetDp
            prefs[Keys.CUTOUT_W] = s.cutoutWidthDp.coerceIn(14f, 80f)
            prefs[Keys.CUTOUT_H] = s.cutoutHeightDp.coerceIn(14f, 60f)
            prefs[Keys.COMPACT_PREF] = s.compactPreference.name
            prefs[Keys.IDLE_WIDGET] = s.idleWidget.name
            prefs[Keys.LOCK_SCREEN_PRESET] = s.lockScreenPreset.name

            prefs[Keys.ENABLED_TABS] = json.encodeToString(s.enabledTabs.take(IslandSettings.MAX_ISLAND_FEATURES))
            prefs[Keys.MUSIC_ENABLED] = s.musicEnabled
            prefs[Keys.TIMER_ENABLED] = s.timerEnabled
            prefs[Keys.CALC_ENABLED] = s.calculatorEnabled
            prefs[Keys.SEARCH_ENABLED] = s.searchEnabled
            prefs[Keys.NOTIF_ENABLED] = s.notificationsEnabled
            prefs[Keys.BATTERY_ENABLED] = s.batteryEnabled
            prefs[Keys.TOGGLES_ENABLED] = s.togglesEnabled
            prefs[Keys.VAULT_ENABLED] = s.vaultEnabled
            prefs[Keys.NOTES_ENABLED] = s.notesEnabled
            prefs[Keys.MEMO_ENABLED] = s.memoEnabled
            prefs[Keys.CLIPS_ENABLED] = s.clipsEnabled
            prefs[Keys.OCR_FEATURE_ENABLED] = s.ocrFeatureEnabled
            prefs[Keys.FLOATING_ENABLED] = s.floatingEnabled
            prefs[Keys.ALERTS_ENABLED] = s.alertsEnabled
            prefs[Keys.QUICK_TOGGLES] = json.encodeToString(s.includedQuickToggles)
            prefs[Keys.PINNED_APPS] = json.encodeToString(s.pinnedAppPackages)
            prefs[Keys.FLOATING_APPS] = json.encodeToString(s.floatingAppPackages)
            prefs[Keys.TAP_GESTURE] = s.tapGesture.name
            prefs[Keys.LONG_PRESS_GESTURE] = s.longPressGesture.name

            prefs[Keys.SCREEN_READING_ENABLED] = s.screenReadingEnabled
            prefs[Keys.OCR_TIER_B_ENABLED] = s.ocrTierBEnabled
            prefs[Keys.VAULT_AUTO_CAPTURE] = s.vaultAutoCapture3Finger
            prefs[Keys.CLIPBOARD_HISTORY_ENABLED] = s.clipboardHistoryEnabled

            prefs[Keys.WIZARD_COMPLETED] = s.wizardCompleted
            prefs[Keys.ACCESSIBILITY_WAS_ENABLED] = s.accessibilityWasEnabled
            prefs[Keys.VERIFIED_FLOATING_PATH] = s.verifiedFloatingPath
        }
    }

    suspend fun toggleTabInIsland(tab: IslandTab): Boolean {
        var accepted = true
        update { s ->
            val list = s.enabledTabs.toMutableList()
            if (list.contains(tab)) {
                list.remove(tab)
                s.copy(enabledTabs = list)
            } else if (list.size >= IslandSettings.MAX_ISLAND_FEATURES) {
                accepted = false
                s
            } else {
                list.add(tab)
                s.copy(enabledTabs = list)
            }
        }
        return accepted
    }

    suspend fun moveTab(tab: IslandTab, delta: Int) {
        update { s ->
            val list = s.enabledTabs.toMutableList()
            val idx = list.indexOf(tab)
            if (idx == -1) return@update s
            val target = (idx + delta).coerceIn(0, list.lastIndex)
            if (target != idx) {
                val item = list.removeAt(idx)
                list.add(target, item)
            }
            s.copy(enabledTabs = list)
        }
    }

    suspend fun reorderTabs(newOrder: List<IslandTab>) {
        update { s ->
            s.copy(enabledTabs = newOrder.distinct().take(IslandSettings.MAX_ISLAND_FEATURES))
        }
    }

    suspend fun exportBackupJson(): String {
        val current = settingsFlow.first()
        return json.encodeToString(current)
    }

    suspend fun importBackupJson(rawJson: String): Result<Unit> = runCatching {
        val decoded = json.decodeFromString<IslandSettings>(rawJson)
        writeAll(decoded.copy(screenReadingEnabled = false)) // Enforce safe default on restore
    }

    suspend fun resetSettings() {
        writeAll(IslandSettings())
    }
}
