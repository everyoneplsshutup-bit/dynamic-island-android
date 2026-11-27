package com.example.island

/**
 * Authoritative Build #5 Checklist mapping each redesigned capability
 * to its concrete implementation symbols across packages.
 */
object Build4Checklist {

    data class ChecklistEntry(
        val id: Int,
        val area: String,
        val summary: String,
        val implementationRef: String
    )

    val entries: List<ChecklistEntry> = listOf(
        ChecklistEntry(
            id = 1,
            area = "120 Hz M3E Spring Choreography & Pull-Down",
            summary = "Two-stage WWDC23 + M3 Expressive spring physics, 30% hold + 200 ms / 8 dp content reveal, non-bouncing collapse, zero-recomposition pull-down tracking, and no-flicker throttling.",
            implementationRef = "MotionTokens.kt, IslandController.kt, IslandOverlayRoot.kt, IslandService.kt"
        ),
        ChecklistEntry(
            id = 2,
            area = "13 Cohesive Micro-Acoustic Sounds (<= 9.6 kHz LPF)",
            summary = "Regenerated 13 mono 44.1 kHz WAVs with click-free raised-cosine attacks, marimba-glass warmth, subtle +/-1.5% pitch variance, and 28 ms rapid replay debounce.",
            implementationRef = "res/raw/sfx_*.wav, IslandSoundManager.kt"
        ),
        ChecklistEntry(
            id = 3,
            area = "Compact Expansion Hard Caps & Three-Way Glass",
            summary = "Dark, Apple Glass, and real Kyant Backdrop Liquid Glass chrome with strict 320x120 dp Medium and 360x160 dp Full hard caps keeping the screen behind visible.",
            implementationRef = "DeadZoneGeometry.kt, GlassSurface.kt, Color.kt, IslandOverlayRoot.kt"
        ),
        ChecklistEntry(
            id = 4,
            area = "Camera Dead-Zone Geometry & Calibration",
            summary = "Strict >= 8 dp cutout exclusion zone with symmetrical 88 dp slots, landscape vertical pill rotation, red DEBUG overlap guard, 26x33 dp default, and Calibration preview/confirm.",
            implementationRef = "DeadZoneGeometry.kt, CalibrationScreen.kt, IslandOverlayRoot.kt, CameraDeadZoneTest.kt"
        ),
        ChecklistEntry(
            id = 5,
            area = "Notification Pill & Interactive Dwell Timer",
            summary = "Compact notification pill displays app icon + name only, replaces rather than stacks, shows 2 dp draining dwell underline paused on touch/expand, and groups Alerts by app with Undo.",
            implementationRef = "IslandNotificationListener.kt, IslandController.kt, IslandToolsAndModules.kt (AlertsIslandPanel)"
        ),
        ChecklistEntry(
            id = 6,
            area = "Music Spinning Art, Wavy Seek & PixelMusic Default",
            summary = "Rotating circular album art (toggleable), RenderThread M3E WavyProgressBar, 12 Hz bars, offline-only synced/static lyrics with designed empty state, and user music source picker.",
            implementationRef = "MusicIslandView.kt, MediaHub.kt, SettingsScreens.kt"
        ),
        ChecklistEntry(
            id = 7,
            area = "Shelf (10-Min Grid, Photo Cropping, Attach & Save)",
            summary = "Shelf with perimeter countdown rings, in-Shelf bitmap photo cropping, long-press delete, Attach to clipboard + Save to gallery (zero Share), and Clear all.",
            implementationRef = "VaultManager.kt, IslandToolsAndModules.kt (VaultIslandPanel), HomeHubScreen.kt"
        ),
        ChecklistEntry(
            id = 8,
            area = "Quick Toggles, Keep Awake & Uncapped Sidebar",
            summary = "Keep Awake toggle wiring FLAG_KEEP_SCREEN_ON, compact Torch Beam card, Screenshot -> Shelf action, and uncapped curved 56/280 dp Floating Sidebar with unread badges.",
            implementationRef = "QuickTogglesManager.kt, IslandToolsAndModules.kt (ToolsIslandPanel)"
        ),
        ChecklistEntry(
            id = 9,
            area = "Auto-Saving Notes, Pinned Apps & Clips",
            summary = "Single-column notes with 4 dp lime strip and inline auto-saving editor, Search Pinned Apps section persisted in DataStore, plus 40 ms staggered Clips.",
            implementationRef = "IslandToolsAndModules.kt (SearchIslandPanel, NotesIslandPanel, ClipsIslandPanel)"
        ),
        ChecklistEntry(
            id = 10,
            area = "Flicker-Free Interactive OCR Selection",
            summary = "Keyed pointerInput + RenderThread drag state eliminating flicker, 22 dp teardrop handles, clamped Magnifier Loupe, word/sentence/all tap cycle, and photo OCR crop widening.",
            implementationRef = "OcrSelectionOverlay.kt, TierAAccessibilityReader.kt, TierBMlKitOcr.kt"
        ),
        ChecklistEntry(
            id = 11,
            area = "When Idle Modes, Mini Capsule & Event Groups",
            summary = "Camera Only / Dual-Slot Info Pill (12 widgets) / Hidden idle modes, Mini Capsule Compact/Standard & duration controls, Lock-screen privacy presets, and grouped Event Switches.",
            implementationRef = "SettingsRepository.kt, IslandOverlayRoot.kt, SettingsScreens.kt"
        ),
        ChecklistEntry(
            id = 12,
            area = "Tips Onboarding, Settings Studio & Dock Ordering",
            summary = "6-group M3 Settings Studio with Try It demo, attached pill Timer, 5 main tabs + More single-row dock ordering with smooth 6-dot drag, and interactive Tips sheet.",
            implementationRef = "SettingsScreens.kt, FeaturePickerScreen.kt, HomeHubScreen.kt, SetupWizardScreen.kt"
        )
    )
}
