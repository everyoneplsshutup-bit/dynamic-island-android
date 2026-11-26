package com.example.island

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.Rect
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.accessibility.AccessibilityEvent
import androidx.compose.runtime.mutableFloatStateOf
import com.example.DynamicIslandApp
import com.example.keepalive.IslandKeepAliveService
import com.example.ocr.MediaProjectionPromptActivity
import com.example.ocr.OcrBitmapHolder
import com.example.ocr.OcrPrivacyGate
import com.example.ocr.TierAAccessibilityReader
import com.example.ocr.TierAResult
import com.example.ocr.TierBMlKitOcr
import com.example.system.IslandSettings
import com.example.system.WhenIdleMode
import android.graphics.Bitmap
import android.os.Build
import android.view.Display
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * AccessibilityService hosting the Dynamic Island TYPE_ACCESSIBILITY_OVERLAY (with automatic
 * TYPE_APPLICATION_OVERLAY fallback when Settings.canDrawOverlays is granted).
 */
class IslandService : AccessibilityService() {

    companion object {
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _isHostingOverlay = MutableStateFlow(false)
        val isHostingOverlay: StateFlow<Boolean> = _isHostingOverlay.asStateFlow()

        private val _detectedCutout = MutableStateFlow<Rect?>(null)
        val detectedCutout: StateFlow<Rect?> = _detectedCutout.asStateFlow()

        private val _liveExclusionZone = MutableStateFlow<ExclusionZone?>(null)
        val liveExclusionZone: StateFlow<ExclusionZone?> = _liveExclusionZone.asStateFlow()

        private val _crossWindowBlurSupported = MutableStateFlow(false)
        val crossWindowBlurSupported: StateFlow<Boolean> = _crossWindowBlurSupported.asStateFlow()

        private val _currentForegroundApp = MutableStateFlow<String?>(null)
        val currentForegroundApp: StateFlow<String?> = _currentForegroundApp.asStateFlow()

        private val ignoredForegroundPackages = setOf(
            "com.android.systemui",
            "com.miui.home",
            "com.miui.securitycenter",
            "com.google.android.inputmethod.latin",
            "com.emoji.keyboard.touchpal",
            "com.sohu.inputmethod.sogou.xiaomi"
        )
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val pullDownFractionState = mutableFloatStateOf(0f)

    private var overlayHost: OverlayWindowHost? = null
    private var latestSettings: IslandSettings = IslandSettings()
    private var latestState: IslandState = IslandState()
    private var latestKeepAwake: Boolean = false

    private var screenshotObserver: ContentObserver? = null
    private var screenshotCheckJob: Job? = null
    private var lastTreePromptTimeMs: Long = 0L
    private var postedInitialWelcome = false

    private var screenReceiverRegistered = false
    private val lockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val app = applicationContext as? DynamicIslandApp ?: return
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            app.islandController.setDeviceLocked(km?.isKeyguardLocked == true)
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    app.islandController.setDwellPaused(DwellPauseReason.SCREEN_OFF, true)
                    app.islandController.setFloatingSidebarOpen(false)
                    app.islandController.setFlashlightCardOpen(false)
                }
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    app.islandController.setDwellPaused(DwellPauseReason.SCREEN_OFF, false)
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isRunning.value = true

        val app = applicationContext as? DynamicIslandApp ?: return
        overlayHost = OverlayWindowHost(
            serviceContext = this,
            scope = serviceScope,
            preferAccessibilityOverlay = true,
            pullDownFractionState = pullDownFractionState,
            liveExclusionZone = liveExclusionZone,
            crossWindowBlurSupported = crossWindowBlurSupported,
            onBlurSupportedChanged = { _crossWindowBlurSupported.value = it },
            onSelectModePoint = { x, y -> handleSelectModePoint(app, x, y) }
        )

        app.mediaHub.start()
        measureDisplayCutout()
        registerLockReceiver()
        registerScreenshotObserver(app)
        IslandKeepAliveService.start(this)

        serviceScope.launch {
            val initialSettings = runCatching { app.settingsRepository.settingsFlow.first() }
                .getOrDefault(IslandSettings())
            latestSettings = initialSettings
            app.settingsRepository.update { it.copy(accessibilityWasEnabled = true) }

            measureDisplayCutout()
            if (initialSettings.islandEnabled) {
                val attached = overlayHost?.attach(app, latestSettings, latestState, latestKeepAwake) == true
                _isHostingOverlay.value = attached
                if (attached && !postedInitialWelcome) {
                    postedInitialWelcome = true
                    app.islandController.upsertActivity(
                        IslandActivity.Alert(
                            id = "island_connected_${System.currentTimeMillis()}",
                            kind = AlertKind.GENERAL,
                            title = "Dynamic Island Active",
                            subtitle = "Tap or pull down the pill"
                        ),
                        autoShowCompact = true,
                        autoDismissMs = 2000L
                    )
                }
            }
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            app.islandController.setDeviceLocked(km?.isKeyguardLocked == true)
            observeStateAndSettings(app)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString()
            if (!pkg.isNullOrBlank() && pkg != packageName && pkg !in ignoredForegroundPackages) {
                _currentForegroundApp.value = pkg
                (applicationContext as? DynamicIslandApp)?.islandController?.setForegroundPackage(pkg)
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        measureDisplayCutout()
        overlayHost?.updateLayout(
            state = latestState,
            settings = latestSettings,
            keepAwake = latestKeepAwake,
            zone = _liveExclusionZone.value,
            forceTouch = true
        )
    }

    override fun onDestroy() {
        _isRunning.value = false
        _isHostingOverlay.value = false
        unregisterLockReceiver()
        unregisterScreenshotObserver()
        overlayHost?.detach()
        overlayHost = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun measureDisplayCutout() {
        val zone = DeadZoneGeometry.measureLive(this, latestSettings)
        _liveExclusionZone.value = zone
        _detectedCutout.value = zone.cutoutRectPx
    }

    private fun observeStateAndSettings(app: DynamicIslandApp) {
        serviceScope.launch {
            app.settingsRepository.settingsFlow.collectLatest { settings ->
                latestSettings = settings
                measureDisplayCutout()
                val host = overlayHost ?: return@collectLatest
                if (settings.islandEnabled) {
                    if (!host.isAttached.value) {
                        val attached = host.attach(app, settings, latestState, latestKeepAwake)
                        _isHostingOverlay.value = attached
                        app.islandController.setHiddenReason(
                            if (attached) "Visible" else "Overlay permission blocked",
                            serviceAlive = true
                        )
                    } else {
                        host.updateLayout(latestState, settings, latestKeepAwake, _liveExclusionZone.value, forceTouch = true)
                        val hiddenMsg = when {
                            latestState.isDeviceLocked && settings.lockScreenPreset == com.example.system.LockScreenPreset.HIDDEN ->
                                "Hidden on lock screen"
                            latestState.mode == IslandMode.IDLE && settings.whenIdleMode == WhenIdleMode.HIDDEN ->
                                "Idle mode set to Hidden"
                            else -> "Visible"
                        }
                        app.islandController.setHiddenReason(hiddenMsg, serviceAlive = true)
                    }
                } else {
                    if (host.isAttached.value) {
                        host.detach()
                        _isHostingOverlay.value = false
                    }
                    app.islandController.setHiddenReason("Master switch turned OFF in Home Hub", serviceAlive = true)
                }
            }
        }
        serviceScope.launch {
            app.islandController.state.collectLatest { state ->
                latestState = state
                app.clipboardMonitor.setForegroundActive(
                    state.mode == IslandMode.EXPANDED && latestSettings.clipboardHistoryEnabled
                )
                overlayHost?.updateLayout(state, latestSettings, latestKeepAwake, _liveExclusionZone.value)
                val updatedReason = when {
                    !latestSettings.islandEnabled -> "Master switch turned OFF in Home Hub"
                    overlayHost?.isAttached?.value != true -> "Overlay permission blocked"
                    state.isDeviceLocked && latestSettings.lockScreenPreset == com.example.system.LockScreenPreset.HIDDEN ->
                        "Hidden on lock screen"
                    state.mode == IslandMode.IDLE && latestSettings.whenIdleMode == WhenIdleMode.HIDDEN ->
                        "Idle mode set to Hidden"
                    else -> "Visible"
                }
                app.islandController.setHiddenReason(updatedReason, serviceAlive = true)
            }
        }
        serviceScope.launch {
            app.quickTogglesManager.state.collectLatest { toggles ->
                if (latestKeepAwake != toggles.isKeepAwakeOn) {
                    latestKeepAwake = toggles.isKeepAwakeOn
                    overlayHost?.updateLayout(latestState, latestSettings, latestKeepAwake, _liveExclusionZone.value)
                }
            }
        }
    }

    private fun handleSelectModePoint(app: DynamicIslandApp, rawX: Int, rawY: Int) {
        serviceScope.launch {
            app.soundManager.playTap()
            val allowed = OcrPrivacyGate.isScreenReadingAllowed(app.settingsRepository)

            overlayHost?.hideTouchForSelectCapture()
            app.islandController.setSelectMode(false)

            if (!allowed) {
                app.islandController.showTransientBanner("Refused: Screen Reading is OFF")
                return@launch
            }

            delay(65L)

            val result = TierAAccessibilityReader.readNodeAtPoint(
                service = this@IslandService,
                screenReadingAllowed = true,
                x = rawX,
                y = rawY,
                ownPackageName = packageName,
                context = this@IslandService
            )

            when (result) {
                is TierAResult.DelegatedToHost -> {
                    app.soundManager.playCopy()
                    app.islandController.showTransientBanner("Copied: ${result.copiedText.take(40)}")
                }
                is TierAResult.TextFound -> {
                    OcrBitmapHolder.clear()
                    app.islandController.setOcrSelection(result.selectionState)
                }
                is TierAResult.Refused -> {
                    app.islandController.showTransientBanner(result.reason)
                }
                is TierAResult.NoText -> {
                    val tierBAllowed = OcrPrivacyGate.isTierBAllowed(app.settingsRepository)
                    if (!tierBAllowed) {
                        app.islandController.showTransientBanner("No text at pressed point (Enable Tier B OCR for image text)")
                        return@launch
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        overlayHost?.setOverlayHiddenForCapture(true)
                        delay(60L)
                        val captured = captureDisplayScreenshotR()
                        overlayHost?.setOverlayHiddenForCapture(false)
                        if (captured != null) {
                            val recognized = TierBMlKitOcr.recognizeSelectionAtPoint(
                                fullFrame = captured,
                                elementBounds = result.bounds,
                                tapX = rawX,
                                tapY = rawY,
                                preserveFrozenFrameForOverlay = true
                            )
                            if (recognized != null) {
                                app.soundManager.playTap()
                                app.islandController.setOcrSelection(recognized.selectionState)
                            } else {
                                OcrBitmapHolder.clear()
                                if (!captured.isRecycled) captured.recycle()
                                app.islandController.showTransientBanner("No text detected at pressed point")
                            }
                            return@launch
                        }
                    }

                    MediaProjectionPromptActivity.launchForTierBOcr(
                        context = this@IslandService,
                        bounds = result.bounds,
                        tapX = rawX,
                        tapY = rawY
                    )
                }
            }
        }
    }

    private suspend fun captureDisplayScreenshotR(): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return suspendCancellableCoroutine { cont ->
            runCatching {
                takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    mainExecutor,
                    object : TakeScreenshotCallback {
                        override fun onSuccess(screenshot: ScreenshotResult) {
                            val bmp = runCatching {
                                val hw = screenshot.hardwareBuffer
                                val cs = screenshot.colorSpace
                                val wrapped = Bitmap.wrapHardwareBuffer(hw, cs)
                                val soft = wrapped?.copy(Bitmap.Config.ARGB_8888, false)
                                wrapped?.recycle()
                                hw.close()
                                soft
                            }.getOrNull()
                            if (cont.isActive) cont.resume(bmp)
                        }

                        override fun onFailure(errorCode: Int) {
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                )
            }.onFailure {
                if (cont.isActive) cont.resume(null)
            }
        }
    }

    private fun registerScreenshotObserver(app: DynamicIslandApp) {
        unregisterScreenshotObserver()
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                val settings = latestSettings
                if (!settings.vaultEnabled || !settings.vaultAutoCapture3Finger) return

                screenshotCheckJob?.cancel()
                screenshotCheckJob = serviceScope.launch(Dispatchers.IO) {
                    delay(420L)

                    val treeStr = latestSettings.vaultScreenshotsTreeUri
                    if (!treeStr.isNullOrBlank()) {
                        val saved = app.vaultManager.ingestRecentScreenshotFromTree(Uri.parse(treeStr))
                        if (saved != null) return@launch
                    }

                    if (uri != null && uri != MediaStore.Images.Media.EXTERNAL_CONTENT_URI) {
                        val looksLikeScreenshot = checkUriIsReadableScreenshot(uri)
                        if (looksLikeScreenshot) {
                            app.vaultManager.saveFromUri(uri, sourceType = "SCREENSHOT_AUTO")
                            return@launch
                        }
                    }

                    if (treeStr.isNullOrBlank()) {
                        val now = System.currentTimeMillis()
                        if (now - lastTreePromptTimeMs > 300_000L) {
                            lastTreePromptTimeMs = now
                            app.islandController.showTransientBanner(
                                "Allow Screenshots folder in Shelf for 10m auto-store",
                                durationMs = 3200L
                            )
                        }
                    }
                }
            }
        }
        screenshotObserver = observer
        runCatching {
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
        }
    }

    private fun checkUriIsReadableScreenshot(uri: Uri): Boolean {
        return runCatching {
            val cols = arrayOf(
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.RELATIVE_PATH
            )
            contentResolver.query(uri, cols, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val name = c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME).let { if (it >= 0) c.getString(it).orEmpty() else "" }
                    val rel = c.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH).let { if (it >= 0) c.getString(it).orEmpty() else "" }
                    val combined = "$name $rel".lowercase()
                    combined.contains("screenshot")
                } else false
            } ?: false
        }.getOrDefault(false)
    }

    private fun unregisterScreenshotObserver() {
        screenshotCheckJob?.cancel()
        screenshotObserver?.let { runCatching { contentResolver.unregisterContentObserver(it) } }
        screenshotObserver = null
    }

    private fun registerLockReceiver() {
        if (screenReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(lockReceiver, filter)
        screenReceiverRegistered = true
    }

    private fun unregisterLockReceiver() {
        if (!screenReceiverRegistered) return
        runCatching { unregisterReceiver(lockReceiver) }
        screenReceiverRegistered = false
    }
}
