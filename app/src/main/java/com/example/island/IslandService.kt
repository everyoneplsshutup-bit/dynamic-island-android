package com.example.island

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.DragEvent
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.DynamicIslandApp
import com.example.ocr.MediaProjectionPromptActivity
import com.example.ocr.OcrPrivacyGate
import com.example.ocr.TierAAccessibilityReader
import com.example.ocr.TierAResult
import com.example.system.HapticsStrength
import com.example.system.IslandGesture
import com.example.system.IslandSettings
import com.example.system.IslandTab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * AccessibilityService hosting the Dynamic Island TYPE_ACCESSIBILITY_OVERLAY.
 * Uses the two-window trick:
 *  1) Fixed full-width drawing window that never resizes (120Hz smooth)
 *  2) Invisible touch window that follows the pill bounds (or captures Select Mode long-press)
 *
 * HARD PRIVACY CONSTRAINT:
 * Never calls getRootInActiveWindow() or inspects nodes unless screenReadingEnabled == true
 * AND the user explicitly triggered Select Mode.
 */
class IslandService : AccessibilityService() {

    companion object {
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _detectedCutout = MutableStateFlow<Rect?>(null)
        val detectedCutout: StateFlow<Rect?> = _detectedCutout.asStateFlow()
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var windowManager: WindowManager? = null
    private var drawingComposeView: ComposeView? = null
    private var drawingParams: WindowManager.LayoutParams? = null
    private var touchView: View? = null
    private var touchParams: WindowManager.LayoutParams? = null

    private var overlayLifecycleOwner: OverlayLifecycleOwner? = null
    private var latestSettings: IslandSettings = IslandSettings()
    private var latestState: IslandState = IslandState()

    private var screenReceiverRegistered = false
    private val lockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val app = applicationContext as? DynamicIslandApp ?: return
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            app.islandController.setDeviceLocked(km?.isKeyguardLocked == true)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isRunning.value = true

        val app = applicationContext as? DynamicIslandApp ?: return
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager

        serviceScope.launch {
            app.settingsRepository.update { it.copy(accessibilityWasEnabled = true) }
        }

        app.mediaHub.start()
        setupOverlayWindows(app)
        registerLockReceiver()
        measureDisplayCutout()
        observeStateAndSettings(app)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // HARD PRIVACY CONSTRAINT:
        // We do NOT inspect window/node text on accessibility events!
        // Screen reading happens ONLY on explicit user long-press in Select Mode when opted in.
    }

    override fun onInterrupt() = Unit

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        measureDisplayCutout()
        updateTouchWindowBounds()
    }

    override fun onDestroy() {
        _isRunning.value = false
        unregisterLockReceiver()
        teardownOverlayWindows()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun measureDisplayCutout() {
        val wm = windowManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val cutout = wm.maximumWindowMetrics.windowInsets.displayCutout
            val rect = cutout?.boundingRects?.firstOrNull()
            _detectedCutout.value = rect
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupOverlayWindows(app: DynamicIslandApp) {
        val wm = windowManager ?: return
        teardownOverlayWindows()

        val owner = OverlayLifecycleOwner()
        owner.onCreate()
        owner.onStart()
        owner.onResume()
        overlayLifecycleOwner = owner

        val density = resources.displayMetrics.density
        val fixedDrawingHeightPx = (560 * density).toInt()

        // Window 1: Fixed full-width drawing window that never resizes
        val dParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            fixedDrawingHeightPx,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
        drawingParams = dParams

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                IslandOverlayRoot(
                    app = app,
                    onRequestHaptic = { performSelectionHaptic() },
                    onOpenAppSettings = {
                        val i = Intent(this@IslandService, com.example.MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        runCatching { startActivity(i) }
                    }
                )
            }
        }
        drawingComposeView = composeView

        // Window 2: Invisible touch window that follows the pill bounds
        val tParams = WindowManager.LayoutParams(
            (120 * density).toInt(),
            (40 * density).toInt(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = (6 * density).toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
        touchParams = tParams

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (latestState.isSelectModeActive) {
                    handleSelectModePoint(app, e.rawX.toInt(), e.rawY.toInt())
                    return true
                }
                performSelectionHaptic()
                handlePillTap(app)
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                performSelectionHaptic()
                if (latestState.isSelectModeActive) {
                    handleSelectModePoint(app, e.rawX.toInt(), e.rawY.toInt())
                } else {
                    handlePillLongPress(app)
                }
            }
        })

        val invisibleTouch = View(this).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                true
            }
            // Accept images dragged onto the Dynamic Island into the 10-minute Photo Vault
            setOnDragListener { _, dragEvent ->
                when (dragEvent.action) {
                    DragEvent.ACTION_DRAG_STARTED -> true
                    DragEvent.ACTION_DROP -> {
                        val clip = dragEvent.clipData ?: return@setOnDragListener false
                        var savedAny = false
                        for (i in 0 until clip.itemCount) {
                            val uri = clip.getItemAt(i).uri
                            if (uri != null) {
                                savedAny = true
                                serviceScope.launch {
                                    app.vaultManager.saveFromUri(uri, sourceType = "DRAGGED")
                                }
                            }
                        }
                        if (savedAny) performSelectionHaptic()
                        savedAny
                    }
                    else -> true
                }
            }
        }
        touchView = invisibleTouch

        runCatching {
            wm.addView(composeView, dParams)
            wm.addView(invisibleTouch, tParams)
        }
    }

    private fun observeStateAndSettings(app: DynamicIslandApp) {
        serviceScope.launch {
            app.settingsRepository.settingsFlow.collectLatest { settings ->
                latestSettings = settings
                updateTouchWindowBounds()
            }
        }
        serviceScope.launch {
            app.islandController.state.collectLatest { state ->
                latestState = state
                app.clipboardMonitor.setForegroundActive(
                    state.mode == IslandMode.EXPANDED && latestSettings.clipboardHistoryEnabled
                )
                updateDrawingWindowInteractivity(state)
                updateTouchWindowBounds()
            }
        }
    }

    private fun updateDrawingWindowInteractivity(state: IslandState) {
        val wm = windowManager ?: return
        val view = drawingComposeView ?: return
        val params = drawingParams ?: return

        val isExpanded = state.mode == IslandMode.EXPANDED || state.isFloatingSidebarOpen
        val needsIme = isExpanded && state.needsKeyboardFocus

        var flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH

        if (!isExpanded) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        if (!needsIme) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }

        if (params.flags != flags) {
            params.flags = flags
            runCatching { wm.updateViewLayout(view, params) }
        }
    }

    private fun updateTouchWindowBounds() {
        val wm = windowManager ?: return
        val view = touchView ?: return
        val params = touchParams ?: return
        val density = resources.displayMetrics.density

        if (latestState.isSelectModeActive) {
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            params.height = WindowManager.LayoutParams.MATCH_PARENT
            params.x = 0
            params.y = 0
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            view.visibility = View.VISIBLE
            runCatching { wm.updateViewLayout(view, params) }
            return
        }

        if (latestState.mode == IslandMode.EXPANDED || latestState.isFloatingSidebarOpen) {
            // Drawing window handles touches directly when expanded
            view.visibility = View.GONE
            return
        }

        view.visibility = View.VISIBLE
        val pillWidthDp = when (latestState.mode) {
            IslandMode.IDLE -> (latestSettings.cutoutWidthDp + 24f).coerceAtLeast(48f)
            IslandMode.COMPACT -> 210f
            IslandMode.SPLIT -> 276f
            IslandMode.EXPANDED -> 340f
        }
        val pillHeightDp = when (latestState.mode) {
            IslandMode.IDLE -> (latestSettings.cutoutHeightDp + 18f).coerceAtLeast(48f)
            IslandMode.COMPACT, IslandMode.SPLIT -> 48f
            IslandMode.EXPANDED -> 48f
        }

        params.width = (pillWidthDp * density).toInt()
        params.height = (pillHeightDp * density).toInt()
        params.x = (latestSettings.cutoutXOffsetDp * density).toInt()
        params.y = (latestSettings.cutoutYOffsetDp * density).toInt().coerceAtLeast(0)
        runCatching { wm.updateViewLayout(view, params) }
    }

    private fun handlePillTap(app: DynamicIslandApp) {
        val primary = latestState.primaryActivity
        if (latestState.mode == IslandMode.IDLE) {
            app.islandController.expand()
            return
        }
        when (latestSettings.tapGesture) {
            IslandGesture.OPEN_APP -> {
                when (primary) {
                    is IslandActivity.Music -> app.mediaHub.openPlayerApp()
                    is IslandActivity.NotificationPreview -> {
                        val i = packageManager.getLaunchIntentForPackage(primary.packageName)
                        i?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let { runCatching { startActivity(it) } }
                    }
                    is IslandActivity.Timer -> app.islandController.expand(IslandTab.TIMER)
                    is IslandActivity.VoiceRecording -> app.islandController.expand(IslandTab.MEMO)
                    else -> app.islandController.expand()
                }
            }
            IslandGesture.TOGGLE_PLAY -> {
                if (primary is IslandActivity.Music) app.mediaHub.togglePlayPause()
                else app.islandController.expand()
            }
            IslandGesture.EXPAND, IslandGesture.OPEN_CUSTOMISE, IslandGesture.SELECT_OCR -> {
                app.islandController.expand()
            }
        }
    }

    private fun handlePillLongPress(app: DynamicIslandApp) {
        when (latestSettings.longPressGesture) {
            IslandGesture.SELECT_OCR -> triggerSelectMode(app)
            IslandGesture.OPEN_CUSTOMISE -> {
                val i = Intent(this, com.example.MainActivity::class.java).apply {
                    putExtra("open_route", "feature_picker")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { startActivity(i) }
            }
            else -> app.islandController.expand()
        }
    }

    private fun triggerSelectMode(app: DynamicIslandApp) {
        serviceScope.launch {
            val allowed = OcrPrivacyGate.isScreenReadingAllowed(app.settingsRepository)
            if (!allowed) {
                app.islandController.showTransientBanner("Screen Reading is OFF in Privacy Settings")
            } else {
                app.islandController.setSelectMode(true)
            }
        }
    }

    private fun handleSelectModePoint(app: DynamicIslandApp, rawX: Int, rawY: Int) {
        serviceScope.launch {
            val allowed = OcrPrivacyGate.isScreenReadingAllowed(app.settingsRepository)
            app.islandController.setSelectMode(false)
            if (!allowed) {
                app.islandController.showTransientBanner("Refused: Screen Reading is OFF")
                return@launch
            }

            // Temporarily hide touch window so it doesn't interfere, then inspect deepest node at (rawX, rawY)
            touchView?.visibility = View.GONE
            val result = TierAAccessibilityReader.readNodeAtPoint(
                service = this@IslandService,
                screenReadingAllowed = true,
                x = rawX,
                y = rawY,
                ownPackageName = packageName
            )

            when (result) {
                is TierAResult.TextFound -> {
                    app.clipboardMonitor.copyToClipboard(result.text)
                    app.islandController.upsertActivity(
                        IslandActivity.Alert(
                            id = "ocr_copied_${System.currentTimeMillis()}",
                            kind = AlertKind.OCR_COPIED,
                            title = "Copied via Select Mode",
                            subtitle = result.text.take(56)
                        ),
                        autoShowCompact = true,
                        autoDismissMs = 3500L
                    )
                }
                is TierAResult.Refused -> {
                    app.islandController.showTransientBanner(result.reason)
                }
                is TierAResult.NoText -> {
                    val tierBAllowed = OcrPrivacyGate.isTierBAllowed(app.settingsRepository)
                    if (tierBAllowed) {
                        MediaProjectionPromptActivity.launchForTierBOcr(this@IslandService, result.bounds)
                    } else {
                        app.islandController.showTransientBanner("No accessibility text found (Tier B OCR is off)")
                    }
                }
            }
        }
    }

    private fun performSelectionHaptic() {
        val s = latestSettings
        if (s.hapticsStrength == HapticsStrength.OFF) return
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = if (s.hapticsStrength == HapticsStrength.MEDIUM) {
                VibrationEffect.EFFECT_CLICK
            } else {
                VibrationEffect.EFFECT_TICK
            }
            vibrator.vibrate(VibrationEffect.createPredefined(effect))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(if (s.hapticsStrength == HapticsStrength.MEDIUM) 16L else 8L)
        }
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

    private fun teardownOverlayWindows() {
        val wm = windowManager ?: return
        drawingComposeView?.let { runCatching { wm.removeView(it) } }
        touchView?.let { runCatching { wm.removeView(it) } }
        drawingComposeView = null
        touchView = null
        overlayLifecycleOwner?.onDestroy()
        overlayLifecycleOwner = null
    }

    private class OverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val store = ViewModelStore()
        private val savedStateController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val viewModelStore: ViewModelStore get() = store
        override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

        fun onCreate() {
            savedStateController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }

        fun onStart() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }

        fun onResume() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun onDestroy() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            store.clear()
        }
    }
}
