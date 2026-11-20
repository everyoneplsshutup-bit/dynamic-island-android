# Dynamic Island (Android — Redmi Note 10 Pro / MIUI 14)

Native Kotlin + Jetpack Compose + Material 3 Dynamic Island app built around a centred punch-hole front camera with zero `INTERNET` permission and strict on-device privacy.

## Architecture (`com.example.*`)
- `island/` — `IslandService` (`AccessibilityService` with `TYPE_ACCESSIBILITY_OVERLAY` two-window trick: fixed full-width drawing window + bounds-tracking invisible touch/drag window), `IslandController` (`StateFlow<IslandState>` with `Idle / Compact / Split / Expanded` + activity queue), `IslandOverlayRoot`, `CalibrationScreen`, `FeaturePickerScreen` (8/8 cap).
- `media/` — `MediaHub` (PixelMusic `com.unshoo.pixelmusic` priority + generic `MediaController` fallback, conditional `TOGGLE_SHUFFLE` / `CYCLE_REPEAT` / `LIKE` commands, live synced lyrics, draggable knob seek bar).
- `notifications/` — `IslandNotificationListener`, Room inbox (`NotificationDao`) & per-app preview rules (`AppRuleDao`), Clock timer mirroring, MIUI screenshot notification listener.
- `ocr/` — `OcrPrivacyGate` (`screenReadingEnabled` default `false`), `TierAAccessibilityReader` (deepest node at press point, refuses passwords & `accessibilityDataSensitive`), `TierBMlKitOcr` (opt-in cropped single-frame ML Kit Latin + Devanagari via in-context `MediaProjection`).
- `vault/` — `VaultManager` & `VaultExpiryWorker` (app-private 10-minute Photo Vault, WorkManager auto-Trash + 24-hour permanent purge, Restore gives fresh 10 minutes, FileProvider one-tap chat attach & drag-out).
- `floating/` — `FloatingWindowManager` & `FloatingSidebarOverlay` (ranked MIUI sidebar / Android freeform `LaunchBounds` paths).
- `system/` — `BatteryMonitor`, `QuickTogglesManager` (Flashlight + Android 13 torch strength, DND policy, Brightness, honest Wi-Fi/Bluetooth system panels), `CalculatorEngine` & `CalculatorIslandView` (live viewport geometry & iOS tokens), `TimerEngine` & `TimerStopwatchView`, `AppDatabase`, `SettingsRepository`.
- `search/` — `SearchEngine` (installed apps, Room notes, optional contacts, inline math evaluator, `ACTION_WEB_SEARCH` Google handoff).
- `notes/` — `NoteEntities`, `VoiceMemoService` (`foregroundServiceType="microphone"` + `MediaRecorder` + Media3 `ExoPlayer` playback), foreground-only `ClipboardMonitor`, `ShareToIslandActivity` (`ACTION_SEND`, Direct Share targets `"Save to Vault"` / `"Save link"`, `ACTION_PROCESS_TEXT`).
- `setup/` — `HomeHubScreen`, `SetupWizardScreen` (ordered permissions + Android 13 sideload "Allow restricted settings" + 6-item MIUI 14 survival checklist), `SettingsScreens` (6 groups + search with breadcrumbs + Danger Zone).
- `keepalive/` — `IslandKeepAliveService` & `BootReceiver` (MIUI force-stop Accessibility disconnect detector + reboot recovery).

## Build APK
```bash
gradle :app:assembleDebug
```
Output APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Testing Status (Honest Report)
1. **Local JVM + Robolectric Tests (`gradle :app:testDebugUnitTest`)**: Verified in this environment (app metadata, zero-internet manifest check, `screenReadingEnabled == false` default, `CalculatorEngine`, `IslandController` state machine, 8/8 feature cap, `MainActivity` Compose UI navigation into 6-group Settings & Setup Wizard, live Calculator `7 × 8 = 56`).
2. **Browser Emulator (Appetize.io)**: Build `:app:assembleDebug` and upload `app/build/outputs/apk/debug/app-debug.apk` to Appetize.io to verify Setup Wizard, Settings 6 groups, 8/8 Feature Picker, Calculator digits, and Timer wheel pickers interactively.
3. **Real-Device Only Tests (Redmi Note 10 Pro, MIUI 14, Android 13)** — cannot run in cloud container without ADB/hardware:
   - Accessibility `TYPE_ACCESSIBILITY_OVERLAY` over status bar & lock screen + two-window rotation check
   - Punch-hole `DisplayCutout` calibration & portrait/landscape clearance across 3 apps
   - Real PixelMusic (`com.unshoo.pixelmusic`) playback, custom `TOGGLE_SHUFFLE`/`CYCLE_REPEAT`/`LIKE` commands, and force-stop recovery
   - Tier A/B Select Mode OCR against Instagram caption & password field refusal in airplane mode
   - 3-finger MIUI screenshot auto-Vault, 10-min WorkManager expiry while killed, and chat attachment
   - MIUI 14 Recents lock, reboot survival, force-stop Accessibility disconnect detection, and 120 Hz scroll smoothness
   - MIUI 14 floating window ranked launch path verification
