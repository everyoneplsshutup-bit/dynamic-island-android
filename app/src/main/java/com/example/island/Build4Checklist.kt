package com.example.island

/**
 * Authoritative Build #4 Redesign Checklist mapping each redesigned capability
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
            area = "Motion & Two-Stage Physics",
            summary = "Apple WWDC23 + M3 Expressive springs (Springy/Standard/Calm/Off), Opening Effect vs Content Effect split, Stage 1 Medium pill, non-bouncing collapse, & expanded swipe gestures.",
            implementationRef = "MotionTokens.kt, IslandController.kt (openFromPill, no-flicker rules), IslandOverlayRoot.kt, IslandService.kt"
        ),
        ChecklistEntry(
            id = 2,
            area = "13 Cohesive Micro-Acoustic Sounds",
            summary = "Deleted and regenerated all 13 WAVs from one warm marimba-glass FM family with pitch/gain randomization and SoundPool gating.",
            implementationRef = "res/raw/sfx_*.wav, IslandSoundManager.kt"
        ),
        ChecklistEntry(
            id = 3,
            area = "Liquid Glass & Concentric Chrome",
            summary = "Material You wallpaper accent tinting on outer shell (Solid Black + Apple Glass preserved), 44dp concentric outer radius, 408dp max width, 220dp unified panel height cap, Live-First expanded home with shortcuts.",
            implementationRef = "Color.kt (extractMaterialYouAccent, blendGlassBase), IslandOverlayRoot.kt, HomeHubScreen.kt"
        ),
        ChecklistEntry(
            id = 4,
            area = "Camera Alignment, Low-Battery & Calibration",
            summary = "Strict >= 8dp cutout clearance, low-battery 20/15/10% capsule & red <=15% idle glyph, vivid #FF3B30 Calibration ring + crosshairs.",
            implementationRef = "CalibrationScreen.kt, IslandOverlayRoot.IdlePillView, BatteryMonitor.kt, IslandNotificationListener.kt"
        ),
        ChecklistEntry(
            id = 5,
            area = "User-Ordered Dock & Customise Dragging",
            summary = "BottomChooserBar strictly respects user-ordered enabledTabs (main 5 + More overflow), split Active vs Disabled sections, immediate 6-dot drag reorder.",
            implementationRef = "IslandOverlayRoot.BottomChooserBar, FeaturePickerScreen.kt"
        ),
        ChecklistEntry(
            id = 6,
            area = "Interactive iOS-Style OCR Selection",
            summary = "Word bounding-box tokenization, blue start/end lollipop drag handles, Magnifier Loupe above active finger, Callout Copy/All/Share — Copy-only (never auto-copies).",
            implementationRef = "OcrSelectionOverlay.kt, TierAAccessibilityReader.kt, TierBMlKitOcr.kt, OcrPrivacyGate.kt"
        ),
        ChecklistEntry(
            id = 7,
            area = "Tools Panel & Volumetric Beam Card",
            summary = "Removed legacy Floating Apps button, added Screenshot -> Vault trigger and interactive Torch Brightness & Beam Card overlay.",
            implementationRef = "IslandToolsAndModules.kt (ToolsIslandPanel, FlashlightBeamOverlayCard)"
        ),
        ChecklistEntry(
            id = 8,
            area = "Uncapped A-Z App Opener & Search",
            summary = "Removed .take(8) app cap, added A-Z section headers + right-edge touchable A-Z scroller + long-press floating freeform launch + BroadcastReceiver refresh.",
            implementationRef = "SearchEngine.kt, IslandToolsAndModules.kt (SearchIslandPanel)"
        ),
        ChecklistEntry(
            id = 9,
            area = "Vault 10-Min Grid, Filter Chips & Preview",
            summary = "8dp filter chips (All/Screenshots/Photos/Trash), 3-column thumbnail grid with Canvas countdown rings, swipe-to-trash, drag-out, and full-screen preview.",
            implementationRef = "IslandToolsAndModules.kt (VaultIslandPanel, VaultFullScreenPreviewOverlay), VaultManager.kt"
        ),
        ChecklistEntry(
            id = 10,
            area = "Music Artwork Glow, On-Device Lyrics & Sleep Dial",
            summary = "Artwork-derived accent glow & strictly offline MediaMetadata/sidecar .lrc lyrics in Music; 24-hour draggable Bedtime/Wake circular SleepDialView in Timer tab.",
            implementationRef = "MusicIslandView.kt, MediaHub.kt, TimerStopwatchView.kt (SleepDialView), TimerEngine.startSleepTimer"
        ),
        ChecklistEntry(
            id = 11,
            area = "Settings Studio, Motion Subtabs & Gestures",
            summary = "Motion subtabs (Background/Animations/Gestures), Opening vs Content effects, Try It demo with event chips, Pill/Expanded swipe pickers, Companion Circle side, Per-Event rules.",
            implementationRef = "SettingsScreens.kt, SettingsRepository.kt"
        )
    )
}
