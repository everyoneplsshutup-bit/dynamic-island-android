package com.example.system

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "dynamic_island_settings")

enum class PillStyle(val label: String) {
    BLACK("Dark"),
    APPLE_LIQUID("Apple Liquid")
}

enum class PillSizePreset(val label: String, val slotWidthDp: Float) {
    SMALL("Small", 72f),
    MEDIUM("Medium", 80f),
    LARGE("Large", 96f)
}

enum class WhenIdleMode(val label: String, val subtitle: String) {
    CAMERA_ONLY("Circle Ring", "Thin circular ring around the front camera"),
    HIDDEN("Hidden", "Completely invisible until an activity or alert arrives")
}

enum class MiniCapsuleSize(val label: String, val widthDp: Float, val heightDp: Float) {
    COMPACT("Compact", 204f, 48f),
    STANDARD("Standard", 252f, 58f)
}

enum class AccentTheme(val label: String, val swatchHex: Long) {
    BLUE("Blue", 0xFF5E9EFFL),
    PURPLE("Purple", 0xFFB085F5L),
    CYAN("Cyan", 0xFF4DD0E1L),
    ORANGE("Amber", 0xFFFFB74DL),
    CRIMSON("Coral", 0xFFE57373L),
    GREEN("Mint", 0xFF66BB6AL),
    DYNAMIC("Auto", 0xFF64D2FFL)
}

enum class SpringFeel { SNAPPY, BALANCED, GENTLE }

enum class AnimationStyle(val label: String, val subtitle: String) {
    FRUIT("Springy", "Playful bouncy overshoot"),
    SPRING("Standard", "Balanced M3 Expressive spring"),
    SMOOTH("Calm", "Minimal bounce, fastest settle"),
    NONE("Off", "Instant snap with zero animation")
}

enum class OpeningEffect(val label: String) {
    LIQUID("Liquid"),
    SPRING("Spring"),
    SMOOTH("Smooth"),
    POP("Pop")
}

enum class ContentEffect(val label: String) {
    FADE("Fade"),
    SLIDE_UP("Slide Up"),
    SCALE_IN("Scale In"),
    BLUR_IN("Blur In")
}

enum class PillSwipeAction(val label: String) {
    TRACK_OR_NOTIF("Track / Cycle Notif"),
    VOLUME("Media Volume"),
    SEARCH("Open Search"),
    EXPAND("Expand Island"),
    COLLAPSE("Collapse / Dismiss"),
    TORCH("Toggle Torch"),
    OPEN_SHELF("Open Shelf"),
    NONE("Off")
}

enum class ExpandedSwipeAction(val label: String) {
    NEXT_NOTIFICATION("Next Notif"),
    DISMISS("Dismiss"),
    COLLAPSE("Collapse"),
    NONE("Off")
}

enum class CompanionPosition { LEFT, RIGHT }
enum class HapticsStrength { OFF, LIGHT, MEDIUM }
enum class CompactPreference { AUTO, MUSIC, TIMER, BATTERY, NOTIFICATIONS }

enum class IdleWidgetType(val label: String) {
    NONE("Nothing"),
    TIME("Time"),
    BATTERY("Battery"),
    DATE("Date"),
    NEXT_ALARM("Next Alarm"),
    UNREAD_COUNT("Unread Count"),
    DND_STATUS("DND Status"),
    NET_SPEED("Net Speed"),
    CLOCK("Clock"),
    BATTERY_RING("Battery Ring"),
    SEARCH_SHORTCUT("Search"),
    QUICK_NOTE("Quick Note")
}

enum class LockScreenPreset { MINIMAL, PRIVACY_HIDE_TEXT, HIDDEN }
enum class IslandGesture { OPEN_APP, EXPAND, TOGGLE_PLAY, OPEN_CUSTOMISE, SELECT_OCR }

enum class EventGroup(val label: String) {
    MUSIC("Music"),
    SYSTEM("System"),
    NOTIFICATIONS("Notifications"),
    TIMERS_SHELF("Timers & Shelf")
}

enum class EventKind(val label: String, val subtitle: String, val group: EventGroup) {
    CHARGING("Charging Mini Capsule", "Show single-value live battery & wattage glimpse when plugged in", EventGroup.SYSTEM),
    LOW_BATTERY("Low Battery Warning", "Transient warning at 20%, 15%, and 10% + subtle alert", EventGroup.SYSTEM),
    TIMER("Timer & Sleep", "Show countdown ring in compact/split pill", EventGroup.TIMERS_SHELF),
    MUSIC("Music Playback", "Show art + animated visualizer in compact pill", EventGroup.MUSIC),
    NOTIFICATIONS("Notifications", "Show incoming notification preview pill (app icon + name only)", EventGroup.NOTIFICATIONS),
    VAULT("Shelf Expiry & Capture", "Show confirmation & expiry alerts from 10-min Shelf", EventGroup.TIMERS_SHELF),
    VOICE_MEMO("Voice Memo", "Show live recording elapsed badge in island", EventGroup.SYSTEM)
}

@Serializable
data class EventBehaviorConfig(
    val enabled: Boolean = true,
    val autoExpand: Boolean = false
)

enum class IslandTab(
    val title: String,
    val description: String,
    val isMainRow: Boolean
) {
    SEARCH("Search", "Apps, contacts, notes & inline math", true),
    MUSIC("Music", "Media controls, local & LRCLIB lyrics", true),
    TIMER("Timer", "Countdown rings, sleep dial & stopwatch", true),
    TOOLS("Tools", "Control Centre sliders, toggles, recorder & OCR", true),
    VAULT("Shelf", "10-minute auto-expiring photo shelf", true),
    CLIPS("Clips", "Foreground-only saved clip history", false),
    NOTES("Notes", "On-device auto-saved quick notes", false),
    MEMO("Memo", "One-tap voice memo recorder", false),
    ALERTS("Alerts", "Searchable, app-grouped inbox & OTP copy", false)
}

enum class QuickToggleType(val label: String) {
    FLASHLIGHT("Torch"),
    DND("DND"),
    KEEP_AWAKE("Keep Awake"),
    BRIGHTNESS("Bright"),
    WIFI("Wi-Fi"),
    BLUETOOTH("Bluetooth"),
    SELECT_OCR("Select OCR"),
    VAULT_CAPTURE("Shot->Shelf")
}

@Serializable
data class IslandSettings(
    // 0. Master Overlay Toggle
    val islandEnabled: Boolean = true,

    // 1. Look
    val pillStyle: PillStyle = PillStyle.APPLE_LIQUID,
    val pillSizePreset: PillSizePreset = PillSizePreset.MEDIUM,
    val glassDensity: Float = 0.65f,
    val reduceGlass: Boolean = false,
    val solidFallback: Boolean = false,
    val accentTheme: AccentTheme = AccentTheme.BLUE,
    val rotateAlbumArt: Boolean = true,
    val performanceMode: Boolean = false,
    val matchAppColor: Boolean = true,
    val appleWidthIdlePill: Boolean = false,
    val energyRingEnabled: Boolean = true,
    val privacyDotsEnabled: Boolean = true,

    // 2. Motion & Audio
    val animationSpeed: Float = 1.0f,
    val animationStyle: AnimationStyle = AnimationStyle.FRUIT,
    val openingEffect: OpeningEffect = OpeningEffect.LIQUID,
    val contentEffect: ContentEffect = ContentEffect.BLUR_IN,
    val animationElasticity: Float = 0.65f,
    val springFeel: SpringFeel = SpringFeel.BALANCED,
    val twoStageOpen: Boolean = true,
    val reducedMotion: Boolean = false,
    val metaballRenderEffect: Boolean = true,
    val hapticsStrength: HapticsStrength = HapticsStrength.LIGHT,
    val soundEnabled: Boolean = true,
    val soundVolume: Float = 0.75f,
    val sfxMorphEnabled: Boolean = false,
    val pillSwipeLeftGesture: PillSwipeAction = PillSwipeAction.TRACK_OR_NOTIF,
    val pillSwipeRightGesture: PillSwipeAction = PillSwipeAction.TRACK_OR_NOTIF,
    val pillSwipeUpGesture: PillSwipeAction = PillSwipeAction.COLLAPSE,
    val pillSwipeDownGesture: PillSwipeAction = PillSwipeAction.EXPAND,
    val gestureSensitivity: Float = 1.0f,
    val expandedSwipeLeftGesture: ExpandedSwipeAction = ExpandedSwipeAction.DISMISS,
    val expandedSwipeRightGesture: ExpandedSwipeAction = ExpandedSwipeAction.NEXT_NOTIFICATION,
    val expandedLongPressGesture: ExpandedSwipeAction = ExpandedSwipeAction.COLLAPSE,
    val miniCapsuleSize: MiniCapsuleSize = MiniCapsuleSize.COMPACT,
    val miniCapsuleDurationMs: Long = 3000L,

    // 3. Pill & Calibration (Redmi Note 10 Pro cutout 74x93 px = ~27x34 dp, top 0 dp)
    val cutoutXOffsetDp: Float = 0f,
    val cutoutYOffsetDp: Float = 0f,
    val cutoutWidthDp: Float = 27f,
    val cutoutHeightDp: Float = 34f,
    val cutoutCornerRadiusDp: Float = 17f,
    val calibrationConfirmed: Boolean = true,
    val compactPreference: CompactPreference = CompactPreference.AUTO,
    val companionPosition: CompanionPosition = CompanionPosition.RIGHT,
    val whenIdleMode: WhenIdleMode = WhenIdleMode.CAMERA_ONLY,
    val idleWidget: IdleWidgetType = IdleWidgetType.NONE,
    val idleLeftWidget: IdleWidgetType = IdleWidgetType.TIME,
    val idleRightWidget: IdleWidgetType = IdleWidgetType.BATTERY,
    val lockScreenPreset: LockScreenPreset = LockScreenPreset.MINIMAL,
    val alertsDwellMs: Long = 2000L,
    val reelOverlayEnabled: Boolean = false,
    val swipeUpDismissEnabled: Boolean = true,
    val hideNotifInForegroundApp: Boolean = true,
    val notifInboxGrouped: Boolean = true,

    // 4. Music & Lyrics
    val lyricsFromInternet: Boolean = true,
    val lyricsOffsetMs: Int = 0,
    val expandedLyricsMarquee: Boolean = false,

    // 5. Features (Hard cap: max 8 enabled on island)
    val enabledTabs: List<IslandTab> = listOf(
        IslandTab.SEARCH,
        IslandTab.MUSIC,
        IslandTab.TIMER,
        IslandTab.TOOLS,
        IslandTab.VAULT,
        IslandTab.NOTES,
        IslandTab.CLIPS,
        IslandTab.ALERTS
    ),
    val musicEnabled: Boolean = true,
    val timerEnabled: Boolean = true,
    val searchEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val batteryEnabled: Boolean = true,
    val togglesEnabled: Boolean = true,
    val vaultEnabled: Boolean = true,
    val notesEnabled: Boolean = true,
    val memoEnabled: Boolean = true,
    val clipsEnabled: Boolean = true,
    val ocrFeatureEnabled: Boolean = true,
    val alertsEnabled: Boolean = true,
    val landscapeQuietMode: Boolean = false,
    val autoRotateSuggestionEnabled: Boolean = true,
    val calendarRemindersEnabled: Boolean = true,
    val mutedNotificationChannels: Set<String> = emptySet(),
    val includedQuickToggles: List<QuickToggleType> = listOf(
        QuickToggleType.FLASHLIGHT,
        QuickToggleType.DND,
        QuickToggleType.KEEP_AWAKE,
        QuickToggleType.BRIGHTNESS,
        QuickToggleType.WIFI,
        QuickToggleType.BLUETOOTH,
        QuickToggleType.SELECT_OCR
    ),
    val allowedMusicPackages: Set<String> = setOf("com.unshoo.pixelmusic"),
    val pinnedAppPackages: List<String> = emptyList(),
    val tapGesture: IslandGesture = IslandGesture.OPEN_APP,
    val longPressGesture: IslandGesture = IslandGesture.EXPAND,

    // 6. Privacy (HARD CONSTRAINT: screenReadingEnabled defaults to false)
    val screenReadingEnabled: Boolean = false,
    val ocrTierBEnabled: Boolean = false,
    val vaultAutoCapture3Finger: Boolean = true,
    val clipboardHistoryEnabled: Boolean = true,

    // Setup, Tips & Survival
    val wizardCompleted: Boolean = false,
    val firstRunTipsShown: Boolean = false,
    val accessibilityWasEnabled: Boolean = false,
    val vaultScreenshotsTreeUri: String? = null,
    val perEventConfig: Map<String, EventBehaviorConfig> = defaultPerEventConfig()
) {
    companion object {
        const val MAX_ISLAND_FEATURES = 8

        fun defaultPerEventConfig(): Map<String, EventBehaviorConfig> =
            EventKind.entries.associate { it.name to EventBehaviorConfig(enabled = true, autoExpand = false) }
    }

    fun eventConfig(kind: EventKind): EventBehaviorConfig =
        perEventConfig[kind.name] ?: EventBehaviorConfig(enabled = true, autoExpand = false)

    fun isGroupEnabled(group: EventGroup): Boolean =
        EventKind.entries.filter { it.group == group }.any { eventConfig(it).enabled }

    fun isTabMasterEnabled(tab: IslandTab): Boolean = when (tab) {
        IslandTab.SEARCH -> searchEnabled
        IslandTab.MUSIC -> musicEnabled
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
        val ISLAND_ENABLED = booleanPreferencesKey("island_enabled")
        val PILL_STYLE = stringPreferencesKey("pill_style")
        val PILL_SIZE_PRESET = stringPreferencesKey("pill_size_preset")
        val GLASS_DENSITY = floatPreferencesKey("glass_density")
        val REDUCE_GLASS = booleanPreferencesKey("reduce_glass")
        val SOLID_FALLBACK = booleanPreferencesKey("solid_fallback")
        val ACCENT_THEME = stringPreferencesKey("accent_theme")
        val ROTATE_ALBUM_ART = booleanPreferencesKey("rotate_album_art")
        val PERFORMANCE_MODE = booleanPreferencesKey("performance_mode")
        val MATCH_APP_COLOR = booleanPreferencesKey("match_app_color")
        val APPLE_WIDTH_IDLE_PILL = booleanPreferencesKey("apple_width_idle_pill")
        val ENERGY_RING_ENABLED = booleanPreferencesKey("energy_ring_enabled")
        val PRIVACY_DOTS_ENABLED = booleanPreferencesKey("privacy_dots_enabled")

        val ANIMATION_SPEED = floatPreferencesKey("animation_speed")
        val ANIMATION_STYLE = stringPreferencesKey("animation_style")
        val OPENING_EFFECT = stringPreferencesKey("opening_effect")
        val CONTENT_EFFECT = stringPreferencesKey("content_effect")
        val ANIMATION_ELASTICITY = floatPreferencesKey("animation_elasticity")
        val SPRING_FEEL = stringPreferencesKey("spring_feel")
        val TWO_STAGE_OPEN = booleanPreferencesKey("two_stage_open")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val METABALL_RENDER_EFFECT = booleanPreferencesKey("metaball_render_effect")
        val HAPTICS_STRENGTH = stringPreferencesKey("haptics_strength")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val SOUND_VOLUME = floatPreferencesKey("sound_volume")
        val SFX_MORPH_ENABLED = booleanPreferencesKey("sfx_morph_enabled")
        val PILL_SWIPE_LEFT = stringPreferencesKey("pill_swipe_left")
        val PILL_SWIPE_RIGHT = stringPreferencesKey("pill_swipe_right")
        val PILL_SWIPE_UP = stringPreferencesKey("pill_swipe_up")
        val PILL_SWIPE_DOWN = stringPreferencesKey("pill_swipe_down")
        val GESTURE_SENSITIVITY = floatPreferencesKey("gesture_sensitivity")
        val EXPANDED_SWIPE_LEFT = stringPreferencesKey("expanded_swipe_left")
        val EXPANDED_SWIPE_RIGHT = stringPreferencesKey("expanded_swipe_right")
        val EXPANDED_LONG_PRESS = stringPreferencesKey("expanded_long_press")
        val MINI_CAPSULE_SIZE = stringPreferencesKey("mini_capsule_size")
        val MINI_CAPSULE_DURATION = longPreferencesKey("mini_capsule_duration_ms")

        val CUTOUT_X = floatPreferencesKey("cutout_x_dp")
        val CUTOUT_Y = floatPreferencesKey("cutout_y_dp")
        val CUTOUT_W = floatPreferencesKey("cutout_w_dp")
        val CUTOUT_H = floatPreferencesKey("cutout_h_dp")
        val CUTOUT_RADIUS = floatPreferencesKey("cutout_corner_radius_dp")
        val CALIBRATION_CONFIRMED = booleanPreferencesKey("calibration_confirmed")
        val COMPACT_PREF = stringPreferencesKey("compact_pref")
        val COMPANION_POSITION = stringPreferencesKey("companion_position")
        val WHEN_IDLE_MODE = stringPreferencesKey("when_idle_mode")
        val IDLE_WIDGET = stringPreferencesKey("idle_widget")
        val IDLE_LEFT_WIDGET = stringPreferencesKey("idle_left_widget")
        val IDLE_RIGHT_WIDGET = stringPreferencesKey("idle_right_widget")
        val LOCK_SCREEN_PRESET = stringPreferencesKey("lock_screen_preset")
        val ALERTS_DWELL_MS = longPreferencesKey("alerts_dwell_ms")
        val REEL_OVERLAY_ENABLED = booleanPreferencesKey("reel_overlay_enabled")
        val SWIPE_UP_DISMISS_ENABLED = booleanPreferencesKey("swipe_up_dismiss_enabled")
        val HIDE_NOTIF_IN_FG = booleanPreferencesKey("hide_notif_in_fg")
        val NOTIF_INBOX_GROUPED = booleanPreferencesKey("notif_inbox_grouped")
        val PER_EVENT_CONFIG = stringPreferencesKey("per_event_config_json")

        val LYRICS_FROM_INTERNET = booleanPreferencesKey("lyrics_from_internet")
        val LYRICS_OFFSET_MS = intPreferencesKey("lyrics_offset_ms")
        val EXPANDED_LYRICS_MARQUEE = booleanPreferencesKey("expanded_lyrics_marquee")

        val ENABLED_TABS = stringPreferencesKey("enabled_tabs_json")
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val TIMER_ENABLED = booleanPreferencesKey("timer_enabled")
        val SEARCH_ENABLED = booleanPreferencesKey("search_enabled")
        val NOTIF_ENABLED = booleanPreferencesKey("notif_enabled")
        val BATTERY_ENABLED = booleanPreferencesKey("battery_enabled")
        val TOGGLES_ENABLED = booleanPreferencesKey("toggles_enabled")
        val VAULT_ENABLED = booleanPreferencesKey("vault_enabled")
        val NOTES_ENABLED = booleanPreferencesKey("notes_enabled")
        val MEMO_ENABLED = booleanPreferencesKey("memo_enabled")
        val CLIPS_ENABLED = booleanPreferencesKey("clips_enabled")
        val OCR_FEATURE_ENABLED = booleanPreferencesKey("ocr_feature_enabled")
        val ALERTS_ENABLED = booleanPreferencesKey("alerts_enabled")
        val LANDSCAPE_QUIET_MODE = booleanPreferencesKey("landscape_quiet_mode")
        val AUTO_ROTATE_SUGGESTION = booleanPreferencesKey("auto_rotate_suggestion")
        val CALENDAR_REMINDERS = booleanPreferencesKey("calendar_reminders")
        val MUTED_CHANNELS = stringPreferencesKey("muted_channels_json")
        val QUICK_TOGGLES = stringPreferencesKey("quick_toggles_json")
        val ALLOWED_MUSIC_PACKAGES = stringPreferencesKey("allowed_music_packages_json")
        val PINNED_APPS = stringPreferencesKey("pinned_apps_json")
        val TAP_GESTURE = stringPreferencesKey("tap_gesture")
        val LONG_PRESS_GESTURE = stringPreferencesKey("long_press_gesture")

        val SCREEN_READING_ENABLED = booleanPreferencesKey("screen_reading_enabled")
        val OCR_TIER_B_ENABLED = booleanPreferencesKey("ocr_tier_b_enabled")
        val VAULT_AUTO_CAPTURE = booleanPreferencesKey("vault_auto_capture")
        val CLIPBOARD_HISTORY_ENABLED = booleanPreferencesKey("clipboard_history_enabled")

        val WIZARD_COMPLETED = booleanPreferencesKey("wizard_completed")
        val FIRST_RUN_TIPS_SHOWN = booleanPreferencesKey("first_run_tips_shown")
        val ACCESSIBILITY_WAS_ENABLED = booleanPreferencesKey("accessibility_was_enabled")
        val VAULT_SCREENSHOTS_TREE_URI = stringPreferencesKey("vault_screenshots_tree_uri")
    }

    val settingsFlow: Flow<IslandSettings> = context.settingsDataStore.data.map { prefs ->
        val default = IslandSettings()
        val parsedPillStyle = when (val raw = prefs[Keys.PILL_STYLE]) {
            "GLASS", "APPLE_GLASS", "LIQUID_GLASS" -> PillStyle.APPLE_LIQUID
            null -> default.pillStyle
            else -> runCatching { PillStyle.valueOf(raw) }.getOrNull() ?: default.pillStyle
        }
        val parsedLeftWidget = when (val raw = prefs[Keys.IDLE_LEFT_WIDGET]) {
            "NETWORK" -> IdleWidgetType.DND_STATUS
            null -> default.idleLeftWidget
            else -> runCatching { IdleWidgetType.valueOf(raw) }.getOrNull() ?: default.idleLeftWidget
        }
        val parsedRightWidget = when (val raw = prefs[Keys.IDLE_RIGHT_WIDGET]) {
            "NETWORK" -> IdleWidgetType.DND_STATUS
            null -> default.idleRightWidget
            else -> runCatching { IdleWidgetType.valueOf(raw) }.getOrNull() ?: default.idleRightWidget
        }
        IslandSettings(
            islandEnabled = prefs[Keys.ISLAND_ENABLED] ?: default.islandEnabled,
            pillStyle = parsedPillStyle,
            pillSizePreset = prefs[Keys.PILL_SIZE_PRESET]?.let { runCatching { PillSizePreset.valueOf(it) }.getOrNull() } ?: default.pillSizePreset,
            glassDensity = (prefs[Keys.GLASS_DENSITY] ?: default.glassDensity).coerceIn(0f, 1f),
            reduceGlass = prefs[Keys.REDUCE_GLASS] ?: default.reduceGlass,
            solidFallback = prefs[Keys.SOLID_FALLBACK] ?: default.solidFallback,
            accentTheme = prefs[Keys.ACCENT_THEME]?.let { runCatching { AccentTheme.valueOf(it) }.getOrNull() } ?: default.accentTheme,
            rotateAlbumArt = prefs[Keys.ROTATE_ALBUM_ART] ?: default.rotateAlbumArt,
            performanceMode = prefs[Keys.PERFORMANCE_MODE] ?: default.performanceMode,
            matchAppColor = prefs[Keys.MATCH_APP_COLOR] ?: default.matchAppColor,
            appleWidthIdlePill = prefs[Keys.APPLE_WIDTH_IDLE_PILL] ?: default.appleWidthIdlePill,
            energyRingEnabled = prefs[Keys.ENERGY_RING_ENABLED] ?: default.energyRingEnabled,
            privacyDotsEnabled = prefs[Keys.PRIVACY_DOTS_ENABLED] ?: default.privacyDotsEnabled,

            animationSpeed = prefs[Keys.ANIMATION_SPEED] ?: default.animationSpeed,
            animationStyle = prefs[Keys.ANIMATION_STYLE]?.let { runCatching { AnimationStyle.valueOf(it) }.getOrNull() } ?: default.animationStyle,
            openingEffect = prefs[Keys.OPENING_EFFECT]?.let { runCatching { OpeningEffect.valueOf(it) }.getOrNull() } ?: default.openingEffect,
            contentEffect = prefs[Keys.CONTENT_EFFECT]?.let { runCatching { ContentEffect.valueOf(it) }.getOrNull() } ?: default.contentEffect,
            animationElasticity = (prefs[Keys.ANIMATION_ELASTICITY] ?: default.animationElasticity).coerceIn(0f, 1f),
            springFeel = prefs[Keys.SPRING_FEEL]?.let { runCatching { SpringFeel.valueOf(it) }.getOrNull() } ?: default.springFeel,
            twoStageOpen = prefs[Keys.TWO_STAGE_OPEN] ?: default.twoStageOpen,
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: default.reducedMotion,
            metaballRenderEffect = prefs[Keys.METABALL_RENDER_EFFECT] ?: default.metaballRenderEffect,
            hapticsStrength = prefs[Keys.HAPTICS_STRENGTH]?.let { runCatching { HapticsStrength.valueOf(it) }.getOrNull() } ?: default.hapticsStrength,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: default.soundEnabled,
            soundVolume = (prefs[Keys.SOUND_VOLUME] ?: default.soundVolume).coerceIn(0f, 1f),
            sfxMorphEnabled = prefs[Keys.SFX_MORPH_ENABLED] ?: default.sfxMorphEnabled,
            pillSwipeLeftGesture = prefs[Keys.PILL_SWIPE_LEFT]?.let { runCatching { PillSwipeAction.valueOf(it) }.getOrNull() } ?: default.pillSwipeLeftGesture,
            pillSwipeRightGesture = prefs[Keys.PILL_SWIPE_RIGHT]?.let { runCatching { PillSwipeAction.valueOf(it) }.getOrNull() } ?: default.pillSwipeRightGesture,
            pillSwipeUpGesture = prefs[Keys.PILL_SWIPE_UP]?.let { runCatching { PillSwipeAction.valueOf(it) }.getOrNull() } ?: default.pillSwipeUpGesture,
            pillSwipeDownGesture = prefs[Keys.PILL_SWIPE_DOWN]?.let { runCatching { PillSwipeAction.valueOf(it) }.getOrNull() } ?: default.pillSwipeDownGesture,
            gestureSensitivity = (prefs[Keys.GESTURE_SENSITIVITY] ?: default.gestureSensitivity).coerceIn(0.5f, 1.5f),
            expandedSwipeLeftGesture = prefs[Keys.EXPANDED_SWIPE_LEFT]?.let { runCatching { ExpandedSwipeAction.valueOf(it) }.getOrNull() } ?: default.expandedSwipeLeftGesture,
            expandedSwipeRightGesture = prefs[Keys.EXPANDED_SWIPE_RIGHT]?.let { runCatching { ExpandedSwipeAction.valueOf(it) }.getOrNull() } ?: default.expandedSwipeRightGesture,
            expandedLongPressGesture = prefs[Keys.EXPANDED_LONG_PRESS]?.let { runCatching { ExpandedSwipeAction.valueOf(it) }.getOrNull() } ?: default.expandedLongPressGesture,
            miniCapsuleSize = prefs[Keys.MINI_CAPSULE_SIZE]?.let { runCatching { MiniCapsuleSize.valueOf(it) }.getOrNull() } ?: default.miniCapsuleSize,
            miniCapsuleDurationMs = (prefs[Keys.MINI_CAPSULE_DURATION] ?: default.miniCapsuleDurationMs).coerceIn(2000L, 4000L),

            cutoutXOffsetDp = prefs[Keys.CUTOUT_X] ?: default.cutoutXOffsetDp,
            cutoutYOffsetDp = prefs[Keys.CUTOUT_Y] ?: default.cutoutYOffsetDp,
            cutoutWidthDp = prefs[Keys.CUTOUT_W] ?: default.cutoutWidthDp,
            cutoutHeightDp = prefs[Keys.CUTOUT_H] ?: default.cutoutHeightDp,
            cutoutCornerRadiusDp = prefs[Keys.CUTOUT_RADIUS] ?: default.cutoutCornerRadiusDp,
            calibrationConfirmed = prefs[Keys.CALIBRATION_CONFIRMED] ?: default.calibrationConfirmed,
            compactPreference = prefs[Keys.COMPACT_PREF]?.let { runCatching { CompactPreference.valueOf(it) }.getOrNull() } ?: default.compactPreference,
            companionPosition = prefs[Keys.COMPANION_POSITION]?.let { runCatching { CompanionPosition.valueOf(it) }.getOrNull() } ?: default.companionPosition,
            whenIdleMode = when (val raw = prefs[Keys.WHEN_IDLE_MODE]) {
                "INFO_PILL", null -> default.whenIdleMode
                else -> runCatching { WhenIdleMode.valueOf(raw) }.getOrNull() ?: default.whenIdleMode
            },
            idleWidget = prefs[Keys.IDLE_WIDGET]?.let { runCatching { IdleWidgetType.valueOf(it) }.getOrNull() } ?: default.idleWidget,
            idleLeftWidget = parsedLeftWidget,
            idleRightWidget = parsedRightWidget,
            lockScreenPreset = prefs[Keys.LOCK_SCREEN_PRESET]?.let { runCatching { LockScreenPreset.valueOf(it) }.getOrNull() } ?: default.lockScreenPreset,
            alertsDwellMs = (prefs[Keys.ALERTS_DWELL_MS] ?: default.alertsDwellMs).coerceIn(1500L, 10000L),
            reelOverlayEnabled = prefs[Keys.REEL_OVERLAY_ENABLED] ?: default.reelOverlayEnabled,
            swipeUpDismissEnabled = prefs[Keys.SWIPE_UP_DISMISS_ENABLED] ?: default.swipeUpDismissEnabled,
            hideNotifInForegroundApp = prefs[Keys.HIDE_NOTIF_IN_FG] ?: default.hideNotifInForegroundApp,
            notifInboxGrouped = prefs[Keys.NOTIF_INBOX_GROUPED] ?: default.notifInboxGrouped,

            lyricsFromInternet = prefs[Keys.LYRICS_FROM_INTERNET] ?: default.lyricsFromInternet,
            lyricsOffsetMs = (prefs[Keys.LYRICS_OFFSET_MS] ?: default.lyricsOffsetMs).coerceIn(-5000, 5000),
            expandedLyricsMarquee = prefs[Keys.EXPANDED_LYRICS_MARQUEE] ?: default.expandedLyricsMarquee,

            enabledTabs = prefs[Keys.ENABLED_TABS]?.let { raw ->
                runCatching {
                    json.decodeFromString<List<String>>(raw).mapNotNull { name ->
                        runCatching { IslandTab.valueOf(name) }.getOrNull()
                    }
                }.getOrNull()
            }?.take(IslandSettings.MAX_ISLAND_FEATURES) ?: default.enabledTabs,
            musicEnabled = prefs[Keys.MUSIC_ENABLED] ?: default.musicEnabled,
            timerEnabled = prefs[Keys.TIMER_ENABLED] ?: default.timerEnabled,
            searchEnabled = prefs[Keys.SEARCH_ENABLED] ?: default.searchEnabled,
            notificationsEnabled = prefs[Keys.NOTIF_ENABLED] ?: default.notificationsEnabled,
            batteryEnabled = prefs[Keys.BATTERY_ENABLED] ?: default.batteryEnabled,
            togglesEnabled = prefs[Keys.TOGGLES_ENABLED] ?: default.togglesEnabled,
            vaultEnabled = prefs[Keys.VAULT_ENABLED] ?: default.vaultEnabled,
            notesEnabled = prefs[Keys.NOTES_ENABLED] ?: default.notesEnabled,
            memoEnabled = prefs[Keys.MEMO_ENABLED] ?: default.memoEnabled,
            clipsEnabled = prefs[Keys.CLIPS_ENABLED] ?: default.clipsEnabled,
            ocrFeatureEnabled = prefs[Keys.OCR_FEATURE_ENABLED] ?: default.ocrFeatureEnabled,
            alertsEnabled = prefs[Keys.ALERTS_ENABLED] ?: default.alertsEnabled,
            landscapeQuietMode = prefs[Keys.LANDSCAPE_QUIET_MODE] ?: default.landscapeQuietMode,
            autoRotateSuggestionEnabled = prefs[Keys.AUTO_ROTATE_SUGGESTION] ?: default.autoRotateSuggestionEnabled,
            calendarRemindersEnabled = prefs[Keys.CALENDAR_REMINDERS] ?: default.calendarRemindersEnabled,
            mutedNotificationChannels = prefs[Keys.MUTED_CHANNELS]?.let {
                runCatching { json.decodeFromString<Set<String>>(it) }.getOrNull()
            } ?: default.mutedNotificationChannels,
            includedQuickToggles = prefs[Keys.QUICK_TOGGLES]?.let {
                runCatching { json.decodeFromString<List<QuickToggleType>>(it) }.getOrNull()
            } ?: default.includedQuickToggles,
            allowedMusicPackages = prefs[Keys.ALLOWED_MUSIC_PACKAGES]?.let {
                runCatching { json.decodeFromString<Set<String>>(it) }.getOrNull()
            } ?: default.allowedMusicPackages,
            pinnedAppPackages = prefs[Keys.PINNED_APPS]?.let {
                runCatching { json.decodeFromString<List<String>>(it) }.getOrNull()
            } ?: default.pinnedAppPackages,
            tapGesture = prefs[Keys.TAP_GESTURE]?.let { runCatching { IslandGesture.valueOf(it) }.getOrNull() } ?: default.tapGesture,
            longPressGesture = prefs[Keys.LONG_PRESS_GESTURE]?.let { runCatching { IslandGesture.valueOf(it) }.getOrNull() } ?: default.longPressGesture,

            // HARD PRIVACY CONSTRAINT: default false
            screenReadingEnabled = prefs[Keys.SCREEN_READING_ENABLED] ?: false,
            ocrTierBEnabled = prefs[Keys.OCR_TIER_B_ENABLED] ?: false,
            vaultAutoCapture3Finger = prefs[Keys.VAULT_AUTO_CAPTURE] ?: default.vaultAutoCapture3Finger,
            clipboardHistoryEnabled = prefs[Keys.CLIPBOARD_HISTORY_ENABLED] ?: default.clipboardHistoryEnabled,

            wizardCompleted = prefs[Keys.WIZARD_COMPLETED] ?: false,
            firstRunTipsShown = prefs[Keys.FIRST_RUN_TIPS_SHOWN] ?: false,
            accessibilityWasEnabled = prefs[Keys.ACCESSIBILITY_WAS_ENABLED] ?: false,
            vaultScreenshotsTreeUri = prefs[Keys.VAULT_SCREENSHOTS_TREE_URI],
            perEventConfig = prefs[Keys.PER_EVENT_CONFIG]?.let {
                runCatching { json.decodeFromString<Map<String, EventBehaviorConfig>>(it) }.getOrNull()
            } ?: default.perEventConfig
        )
    }

    suspend fun update(transform: (IslandSettings) -> IslandSettings) {
        val current = settingsFlow.first()
        val updated = transform(current)
        writeAll(updated)
    }

    private suspend fun writeAll(s: IslandSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.ISLAND_ENABLED] = s.islandEnabled
            prefs[Keys.PILL_STYLE] = s.pillStyle.name
            prefs[Keys.PILL_SIZE_PRESET] = s.pillSizePreset.name
            prefs[Keys.GLASS_DENSITY] = s.glassDensity.coerceIn(0f, 1f)
            prefs[Keys.REDUCE_GLASS] = s.reduceGlass
            prefs[Keys.SOLID_FALLBACK] = s.solidFallback
            prefs[Keys.ACCENT_THEME] = s.accentTheme.name
            prefs[Keys.ROTATE_ALBUM_ART] = s.rotateAlbumArt
            prefs[Keys.PERFORMANCE_MODE] = s.performanceMode
            prefs[Keys.MATCH_APP_COLOR] = s.matchAppColor
            prefs[Keys.APPLE_WIDTH_IDLE_PILL] = s.appleWidthIdlePill
            prefs[Keys.ENERGY_RING_ENABLED] = s.energyRingEnabled
            prefs[Keys.PRIVACY_DOTS_ENABLED] = s.privacyDotsEnabled

            prefs[Keys.ANIMATION_SPEED] = s.animationSpeed.coerceIn(0.5f, 1.8f)
            prefs[Keys.ANIMATION_STYLE] = s.animationStyle.name
            prefs[Keys.OPENING_EFFECT] = s.openingEffect.name
            prefs[Keys.CONTENT_EFFECT] = s.contentEffect.name
            prefs[Keys.ANIMATION_ELASTICITY] = s.animationElasticity.coerceIn(0f, 1f)
            prefs[Keys.SPRING_FEEL] = s.springFeel.name
            prefs[Keys.TWO_STAGE_OPEN] = s.twoStageOpen
            prefs[Keys.REDUCED_MOTION] = s.reducedMotion
            prefs[Keys.METABALL_RENDER_EFFECT] = s.metaballRenderEffect
            prefs[Keys.HAPTICS_STRENGTH] = s.hapticsStrength.name
            prefs[Keys.SOUND_ENABLED] = s.soundEnabled
            prefs[Keys.SOUND_VOLUME] = s.soundVolume.coerceIn(0f, 1f)
            prefs[Keys.SFX_MORPH_ENABLED] = s.sfxMorphEnabled
            prefs[Keys.PILL_SWIPE_LEFT] = s.pillSwipeLeftGesture.name
            prefs[Keys.PILL_SWIPE_RIGHT] = s.pillSwipeRightGesture.name
            prefs[Keys.PILL_SWIPE_UP] = s.pillSwipeUpGesture.name
            prefs[Keys.PILL_SWIPE_DOWN] = s.pillSwipeDownGesture.name
            prefs[Keys.GESTURE_SENSITIVITY] = s.gestureSensitivity.coerceIn(0.5f, 1.5f)
            prefs[Keys.EXPANDED_SWIPE_LEFT] = s.expandedSwipeLeftGesture.name
            prefs[Keys.EXPANDED_SWIPE_RIGHT] = s.expandedSwipeRightGesture.name
            prefs[Keys.EXPANDED_LONG_PRESS] = s.expandedLongPressGesture.name
            prefs[Keys.MINI_CAPSULE_SIZE] = s.miniCapsuleSize.name
            prefs[Keys.MINI_CAPSULE_DURATION] = s.miniCapsuleDurationMs.coerceIn(2000L, 4000L)

            prefs[Keys.CUTOUT_X] = s.cutoutXOffsetDp
            prefs[Keys.CUTOUT_Y] = s.cutoutYOffsetDp
            prefs[Keys.CUTOUT_W] = s.cutoutWidthDp.coerceIn(14f, 80f)
            prefs[Keys.CUTOUT_H] = s.cutoutHeightDp.coerceIn(14f, 60f)
            prefs[Keys.CUTOUT_RADIUS] = s.cutoutCornerRadiusDp.coerceIn(6f, 28f)
            prefs[Keys.CALIBRATION_CONFIRMED] = s.calibrationConfirmed
            prefs[Keys.COMPACT_PREF] = s.compactPreference.name
            prefs[Keys.COMPANION_POSITION] = s.companionPosition.name
            prefs[Keys.WHEN_IDLE_MODE] = s.whenIdleMode.name
            prefs[Keys.IDLE_WIDGET] = s.idleWidget.name
            prefs[Keys.IDLE_LEFT_WIDGET] = s.idleLeftWidget.name
            prefs[Keys.IDLE_RIGHT_WIDGET] = s.idleRightWidget.name
            prefs[Keys.LOCK_SCREEN_PRESET] = s.lockScreenPreset.name
            prefs[Keys.ALERTS_DWELL_MS] = s.alertsDwellMs.coerceIn(1500L, 10000L)
            prefs[Keys.REEL_OVERLAY_ENABLED] = s.reelOverlayEnabled
            prefs[Keys.SWIPE_UP_DISMISS_ENABLED] = s.swipeUpDismissEnabled
            prefs[Keys.HIDE_NOTIF_IN_FG] = s.hideNotifInForegroundApp
            prefs[Keys.NOTIF_INBOX_GROUPED] = s.notifInboxGrouped
            prefs[Keys.PER_EVENT_CONFIG] = json.encodeToString(s.perEventConfig)

            prefs[Keys.LYRICS_FROM_INTERNET] = s.lyricsFromInternet
            prefs[Keys.LYRICS_OFFSET_MS] = s.lyricsOffsetMs.coerceIn(-5000, 5000)
            prefs[Keys.EXPANDED_LYRICS_MARQUEE] = s.expandedLyricsMarquee

            prefs[Keys.ENABLED_TABS] = json.encodeToString(s.enabledTabs.take(IslandSettings.MAX_ISLAND_FEATURES))
            prefs[Keys.MUSIC_ENABLED] = s.musicEnabled
            prefs[Keys.TIMER_ENABLED] = s.timerEnabled
            prefs[Keys.SEARCH_ENABLED] = s.searchEnabled
            prefs[Keys.NOTIF_ENABLED] = s.notificationsEnabled
            prefs[Keys.BATTERY_ENABLED] = s.batteryEnabled
            prefs[Keys.TOGGLES_ENABLED] = s.togglesEnabled
            prefs[Keys.VAULT_ENABLED] = s.vaultEnabled
            prefs[Keys.NOTES_ENABLED] = s.notesEnabled
            prefs[Keys.MEMO_ENABLED] = s.memoEnabled
            prefs[Keys.CLIPS_ENABLED] = s.clipsEnabled
            prefs[Keys.OCR_FEATURE_ENABLED] = s.ocrFeatureEnabled
            prefs[Keys.ALERTS_ENABLED] = s.alertsEnabled
            prefs[Keys.LANDSCAPE_QUIET_MODE] = s.landscapeQuietMode
            prefs[Keys.AUTO_ROTATE_SUGGESTION] = s.autoRotateSuggestionEnabled
            prefs[Keys.CALENDAR_REMINDERS] = s.calendarRemindersEnabled
            prefs[Keys.MUTED_CHANNELS] = json.encodeToString(s.mutedNotificationChannels)
            prefs[Keys.QUICK_TOGGLES] = json.encodeToString(s.includedQuickToggles)
            prefs[Keys.ALLOWED_MUSIC_PACKAGES] = json.encodeToString(s.allowedMusicPackages)
            prefs[Keys.PINNED_APPS] = json.encodeToString(s.pinnedAppPackages)
            prefs[Keys.TAP_GESTURE] = s.tapGesture.name
            prefs[Keys.LONG_PRESS_GESTURE] = s.longPressGesture.name

            prefs[Keys.SCREEN_READING_ENABLED] = s.screenReadingEnabled
            prefs[Keys.OCR_TIER_B_ENABLED] = s.ocrTierBEnabled
            prefs[Keys.VAULT_AUTO_CAPTURE] = s.vaultAutoCapture3Finger
            prefs[Keys.CLIPBOARD_HISTORY_ENABLED] = s.clipboardHistoryEnabled

            prefs[Keys.WIZARD_COMPLETED] = s.wizardCompleted
            prefs[Keys.FIRST_RUN_TIPS_SHOWN] = s.firstRunTipsShown
            prefs[Keys.ACCESSIBILITY_WAS_ENABLED] = s.accessibilityWasEnabled
            if (s.vaultScreenshotsTreeUri != null) {
                prefs[Keys.VAULT_SCREENSHOTS_TREE_URI] = s.vaultScreenshotsTreeUri
            } else {
                prefs.remove(Keys.VAULT_SCREENSHOTS_TREE_URI)
            }
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

    suspend fun setEventGroupEnabled(group: EventGroup, enabled: Boolean) {
        update { s ->
            val next = s.perEventConfig.toMutableMap()
            EventKind.entries.filter { it.group == group }.forEach { kind ->
                val cur = next[kind.name] ?: EventBehaviorConfig()
                next[kind.name] = cur.copy(enabled = enabled)
            }
            s.copy(perEventConfig = next)
        }
    }

    suspend fun exportBackupJson(): String {
        val current = settingsFlow.first()
        return json.encodeToString(current)
    }

    suspend fun importBackupJson(rawJson: String): Result<Unit> = runCatching {
        val decoded = json.decodeFromString<IslandSettings>(rawJson)
        writeAll(decoded.copy(screenReadingEnabled = false))
    }

    suspend fun resetSettings() {
        writeAll(IslandSettings())
    }
}
