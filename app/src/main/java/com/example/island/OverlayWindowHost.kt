package com.example.island

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Display
import android.view.DragEvent
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
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
import com.example.MainActivity
import com.example.R
import com.example.ocr.MediaProjectionPromptActivity
import com.example.ocr.OcrPrivacyGate
import com.example.system.HapticsStrength
import com.example.system.IslandGesture
import com.example.system.IslandSettings
import com.example.system.IslandTab
import com.example.system.LockScreenPreset
import com.example.system.PillStyle
import com.example.system.PillSwipeAction
import com.example.system.WhenIdleMode
import com.example.ui.theme.DynamicIslandTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.function.Consumer

/**
 * Reusable overlay window host used by [IslandService] (TYPE_ACCESSIBILITY_OVERLAY with fallback
 * to TYPE_APPLICATION_OVERLAY) and [com.example.keepalive.IslandKeepAliveService]
 * (TYPE_APPLICATION_OVERLAY fallback when MIUI hasn't attached Accessibility yet).
 */
class OverlayWindowHost(
    private val serviceContext: Context,
    private val scope: CoroutineScope,
    private val preferAccessibilityOverlay: Boolean,
    private val pullDownFractionState: MutableFloatState,
    private val liveExclusionZone: StateFlow<ExclusionZone?>,
    private val crossWindowBlurSupported: StateFlow<Boolean>,
    private val onBlurSupportedChanged: (Boolean) -> Unit,
    private val onSelectModePoint: (Int, Int) -> Unit
) {
    private val _isAttached = MutableStateFlow(false)
    val isAttached: StateFlow<Boolean> = _isAttached.asStateFlow()

    private var windowManager: WindowManager? = null
    private var drawingComposeView: ComposeView? = null
    private var drawingParams: WindowManager.LayoutParams? = null
    private var touchView: View? = null
    private var touchParams: WindowManager.LayoutParams? = null
    private var overlayLifecycleOwner: OverlayLifecycleOwner? = null
    private var blurEnabledListener: Consumer<Boolean>? = null

    private var lastTouchSignature: String = ""
    private var lastDrawingSignature: String = ""
    private var lastDrawingWasFullCanvas: Boolean = true
    private var shrinkDrawingJob: kotlinx.coroutines.Job? = null
    private var orientationListener: OrientationEventListener? = null
    private val pillPressedState = mutableStateOf(false)
    private val windowOffsetDpState = mutableStateOf(Offset.Zero)
    private var activeWindowType: Int = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

    private var latestSettings: IslandSettings = IslandSettings()
    private var latestState: IslandState = IslandState()
    private var latestKeepAwake: Boolean = false

    private fun resolveOverlayUiContext(windowType: Int): Context {
        if (preferAccessibilityOverlay && windowType == WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY) {
            return ContextThemeWrapper(serviceContext, R.style.Theme_DynamicIsland)
        }
        val dm = serviceContext.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val display = dm?.getDisplay(Display.DEFAULT_DISPLAY)
        val base = if (display != null) {
            val dispCtx = serviceContext.createDisplayContext(display)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                runCatching { dispCtx.createWindowContext(windowType, null) }.getOrDefault(dispCtx)
            } else {
                dispCtx
            }
        } else {
            serviceContext
        }
        return ContextThemeWrapper(base, R.style.Theme_DynamicIsland)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun attach(
        app: DynamicIslandApp,
        initialSettings: IslandSettings,
        initialState: IslandState,
        initialKeepAwake: Boolean
    ): Boolean {
        latestSettings = initialSettings
        latestState = initialState
        latestKeepAwake = initialKeepAwake

        if (!preferAccessibilityOverlay && !Settings.canDrawOverlays(serviceContext)) {
            return false
        }

        val initialType = if (preferAccessibilityOverlay) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }

        val triedTypes = buildList {
            add(initialType)
            if (preferAccessibilityOverlay && Settings.canDrawOverlays(serviceContext)) {
                add(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            }
        }

        for (windowType in triedTypes) {
            if (tryAttachWithType(app, windowType)) {
                return true
            }
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun tryAttachWithType(app: DynamicIslandApp, windowType: Int): Boolean {
        detach()
        val uiCtx = resolveOverlayUiContext(windowType)
        val wm = if (windowType == WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY) {
            (serviceContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
                ?: (uiCtx.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
        } else {
            (uiCtx.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
                ?: (serviceContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
        } ?: return false
        windowManager = wm
        activeWindowType = windowType

        val owner = OverlayLifecycleOwner()
        owner.onCreate()
        owner.onStart()
        owner.onResume()
        overlayLifecycleOwner = owner

        val density = uiCtx.resources.displayMetrics.density.coerceAtLeast(1f)
        val cutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        } else {
            0
        }

        val dragListener = View.OnDragListener { _, dragEvent ->
            when (dragEvent.action) {
                DragEvent.ACTION_DRAG_STARTED -> true
                DragEvent.ACTION_DRAG_ENTERED -> {
                    app.islandController.setDragOverIsland(true)
                    true
                }
                DragEvent.ACTION_DRAG_EXITED, DragEvent.ACTION_DRAG_ENDED -> {
                    app.islandController.setDragOverIsland(false)
                    true
                }
                DragEvent.ACTION_DROP -> {
                    app.islandController.setDragOverIsland(false)
                    val clip = dragEvent.clipData ?: return@OnDragListener false
                    var savedAny = false
                    for (i in 0 until clip.itemCount) {
                        val item = clip.getItemAt(i)
                        val uri = item.uri ?: item.text?.toString()
                            ?.takeIf { it.startsWith("content://") }
                            ?.let { runCatching { Uri.parse(it) }.getOrNull() }
                        if (uri != null) {
                            savedAny = true
                            scope.launch {
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

        val dParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = cutoutMode
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                preferredRefreshRate = 120f
            }
        }
        drawingParams = dParams

        val composeView = ComposeView(uiCtx).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setOnDragListener(dragListener)
            setContent {
                val liveZone by liveExclusionZone.collectAsState()
                val blurSupported by crossWindowBlurSupported.collectAsState()
                DynamicIslandTheme(darkTheme = true, dynamicColor = false) {
                    IslandOverlayRoot(
                        app = app,
                        exclusionZone = liveZone ?: DeadZoneGeometry.measureLive(uiCtx, latestSettings),
                        crossWindowBlurSupported = blurSupported,
                        windowOriginDp = windowOffsetDpState.value,
                        pullDownProgressProvider = { pullDownFractionState.floatValue },
                        isPillPressedProvider = { pillPressedState.value },
                        onRequestHaptic = { performSelectionHaptic() },
                        onOpenAppSettings = {
                            val i = Intent(serviceContext, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            runCatching { serviceContext.startActivity(i) }
                        }
                    )
                }
            }
        }
        drawingComposeView = composeView

        val tParams = WindowManager.LayoutParams(
            (136 * density).toInt(),
            (48 * density).toInt(),
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = (4 * density).toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = cutoutMode
            }
        }
        touchParams = tParams

        val gestureDetector = GestureDetector(uiCtx, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                pillPressedState.value = false
                if (latestState.isSelectModeActive) {
                    onSelectModePoint(e.rawX.toInt(), e.rawY.toInt())
                    return true
                }
                handlePillTap(app)
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                performSelectionHaptic()
                if (latestState.isSelectModeActive) {
                    onSelectModePoint(e.rawX.toInt(), e.rawY.toInt())
                } else {
                    handlePillLongPress(app)
                }
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (latestState.isSelectModeActive) return false
                val absX = kotlin.math.abs(velocityX)
                val absY = kotlin.math.abs(velocityY)
                val sensitivity = latestSettings.gestureSensitivity.coerceIn(0.5f, 1.5f)
                val xThreshold = (420f / sensitivity)
                val yThreshold = (500f / sensitivity)
                if (absX > absY && absX > xThreshold) {
                    performSelectionHaptic()
                    val isLeft = velocityX < 0f
                    val swipeAction = if (isLeft) latestSettings.pillSwipeLeftGesture else latestSettings.pillSwipeRightGesture
                    executePillSwipeAction(app, swipeAction, isLeft = isLeft)
                    return true
                }
                if (velocityY > yThreshold && absY > absX) {
                    performSelectionHaptic()
                    executePillSwipeAction(app, latestSettings.pillSwipeDownGesture, isLeft = false)
                    return true
                }
                if (velocityY < -yThreshold && absY > absX && latestSettings.swipeUpDismissEnabled) {
                    performSelectionHaptic()
                    executePillSwipeAction(app, latestSettings.pillSwipeUpGesture, isLeft = true)
                    return true
                }
                return false
            }
        })

        var downRawY = 0f
        var isPullingDown = false
        val pullThresholdPx = 46f * density
        val invisibleTouch = View(uiCtx).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                if (!latestState.isSelectModeActive) {
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            downRawY = event.rawY
                            isPullingDown = false
                            pillPressedState.value = true
                            performSelectionHaptic(lightPress = true)
                            app.soundManager.playTap()
                            app.islandController.setDwellPaused(DwellPauseReason.TOUCH, true)
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dy = (event.rawY - downRawY).coerceAtLeast(0f)
                            if (dy > 14f * density) {
                                isPullingDown = true
                                pillPressedState.value = false
                                pullDownFractionState.floatValue = (dy / (110f * density)).coerceIn(0f, 1f)
                            }
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            pillPressedState.value = false
                            app.islandController.setDwellPaused(DwellPauseReason.TOUCH, false)
                            if (isPullingDown) {
                                val dy = event.rawY - downRawY
                                isPullingDown = false
                                pullDownFractionState.floatValue = 0f
                                if (dy >= pullThresholdPx) {
                                    performSelectionHaptic()
                                    app.islandController.openFromPill(latestSettings.twoStageOpen)
                                }
                            }
                        }
                    }
                }
                true
            }
            setOnDragListener(dragListener)
        }
        touchView = invisibleTouch

        lastDrawingSignature = ""
        lastTouchSignature = ""

        val added = runCatching {
            wm.addView(composeView, dParams)
            wm.addView(invisibleTouch, tParams)
        }.onFailure { err ->
            Log.e("OverlayWindowHost", "Failed adding overlay windowType=$windowType", err)
            detach()
        }.isSuccess

        if (added) {
            registerBlurListener(wm)
            registerOrientationListener(app, uiCtx)
            _isAttached.value = true
            updateLayout(latestState, latestSettings, latestKeepAwake, liveExclusionZone.value, forceTouch = true)
        }
        return added
    }

    fun updateLayout(
        state: IslandState,
        settings: IslandSettings,
        keepAwake: Boolean,
        zone: ExclusionZone?,
        forceTouch: Boolean = false
    ) {
        latestState = state
        latestSettings = settings
        latestKeepAwake = keepAwake
        if (!_isAttached.value) return

        updateDrawingWindowAttributes(state, settings, keepAwake, zone)
        updateTouchWindowBounds(state, settings, zone, force = forceTouch)
    }

    fun hideTouchForSelectCapture() {
        touchView?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        touchView?.visibility = View.GONE
        lastTouchSignature = "GONE"
    }

    fun setOverlayHiddenForCapture(hidden: Boolean) {
        drawingComposeView?.visibility = if (hidden) View.INVISIBLE else View.VISIBLE
        if (hidden) {
            hideTouchForSelectCapture()
        } else {
            updateTouchWindowBounds(latestState, latestSettings, liveExclusionZone.value, force = true)
        }
    }

    fun detach() {
        shrinkDrawingJob?.cancel()
        shrinkDrawingJob = null
        orientationListener?.disable()
        orientationListener = null
        unregisterBlurListener()
        val wm = windowManager
        if (wm != null) {
            drawingComposeView?.let { runCatching { wm.removeViewImmediate(it) }.onFailure { _ -> runCatching { wm.removeView(it) } } }
            touchView?.let { runCatching { wm.removeViewImmediate(it) }.onFailure { _ -> runCatching { wm.removeView(it) } } }
        }
        drawingComposeView = null
        touchView = null
        drawingParams = null
        touchParams = null
        overlayLifecycleOwner?.onDestroy()
        overlayLifecycleOwner = null
        windowManager = null
        lastDrawingSignature = ""
        lastTouchSignature = ""
        lastDrawingWasFullCanvas = true
        windowOffsetDpState.value = Offset.Zero
        _isAttached.value = false
    }

    private fun registerOrientationListener(app: DynamicIslandApp, uiCtx: Context) {
        orientationListener?.disable()
        val listener = object : OrientationEventListener(serviceContext) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val isLandscapeHold = (orientation in 55..125) || (orientation in 235..305)
                val isPortraitHold = (orientation in 0..40) || (orientation in 320..359)
                if (!isLandscapeHold && !isPortraitHold) return

                if (latestSettings.landscapeQuietMode) {
                    app.islandController.setLandscapeQuiet(isLandscapeHold)
                } else if (latestState.isLandscapeQuiet) {
                    app.islandController.setLandscapeQuiet(false)
                }

                if (latestSettings.autoRotateSuggestionEnabled && isLandscapeHold) {
                    val accelOff = runCatching {
                        Settings.System.getInt(
                            serviceContext.contentResolver,
                            Settings.System.ACCELEROMETER_ROTATION,
                            1
                        ) == 0
                    }.getOrDefault(false)
                    val canWrite = runCatching { Settings.System.canWrite(serviceContext) }.getOrDefault(false)
                    if (accelOff && canWrite) {
                        val targetRot = if (orientation in 55..125) Surface.ROTATION_270 else Surface.ROTATION_90
                        val currentRot = runCatching { uiCtx.display?.rotation }.getOrNull() ?: Surface.ROTATION_0
                        if (currentRot != targetRot) {
                            app.islandController.setAutoRotateSuggestion(targetRot)
                        } else {
                            app.islandController.setAutoRotateSuggestion(null)
                        }
                    } else {
                        app.islandController.setAutoRotateSuggestion(null)
                    }
                } else if (isPortraitHold || !latestSettings.autoRotateSuggestionEnabled) {
                    app.islandController.setAutoRotateSuggestion(null)
                }
            }
        }
        if (listener.canDetectOrientation()) {
            listener.enable()
            orientationListener = listener
        }
    }

    private fun registerBlurListener(wm: WindowManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        onBlurSupportedChanged(runCatching { wm.isCrossWindowBlurEnabled }.getOrDefault(false))
        val listener = Consumer<Boolean> { enabled ->
            onBlurSupportedChanged(enabled)
            updateDrawingWindowAttributes(latestState, latestSettings, latestKeepAwake, liveExclusionZone.value)
        }
        blurEnabledListener = listener
        runCatching { wm.addCrossWindowBlurEnabledListener(listener) }
    }

    private fun unregisterBlurListener() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val listener = blurEnabledListener ?: return
        runCatching { windowManager?.removeCrossWindowBlurEnabledListener(listener) }
        blurEnabledListener = null
    }

    private fun updateDrawingWindowAttributes(
        state: IslandState,
        settings: IslandSettings,
        keepAwake: Boolean,
        zoneInput: ExclusionZone?
    ) {
        val wm = windowManager ?: return
        val view = drawingComposeView ?: return
        val params = drawingParams ?: return

        val hideDrawingWindow = (!settings.islandEnabled && !state.isCalibrationPreview) ||
            (state.mode == IslandMode.IDLE &&
                !state.isCalibrationPreview &&
                !state.isDragOverIsland &&
                !state.privacyIndicator.anyActive &&
                (settings.whenIdleMode == WhenIdleMode.HIDDEN ||
                    (state.isLandscapeQuiet && settings.landscapeQuietMode) ||
                    (state.isDeviceLocked && settings.lockScreenPreset == LockScreenPreset.HIDDEN)))
        if (hideDrawingWindow) {
            shrinkDrawingJob?.cancel()
            shrinkDrawingJob = null
            if (lastDrawingSignature != "GONE") {
                lastDrawingSignature = "GONE"
                view.visibility = View.GONE
            }
            return
        }
        if (view.visibility != View.VISIBLE) {
            view.visibility = View.VISIBLE
        }

        val zone = zoneInput ?: DeadZoneGeometry.measureLive(serviceContext, settings)
        val density = zone.density

        val hasRotateChip = state.pendingAutoRotateSuggestion != null && settings.autoRotateSuggestionEnabled
        val isInteractive = state.mode == IslandMode.EXPANDED ||
            state.mode == IslandMode.MEDIUM ||
            state.ocrSelection != null ||
            state.isFlashlightCardOpen ||
            state.approvalPrompt != null ||
            hasRotateChip
        val needsIme = isInteractive && state.needsKeyboardFocus

        val effectiveStyle = if (settings.solidFallback) PillStyle.BLACK else settings.pillStyle
        val useCrossBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            crossWindowBlurSupported.value &&
            (state.mode == IslandMode.MEDIUM || state.mode == IslandMode.EXPANDED) &&
            !settings.reduceGlass &&
            !settings.solidFallback &&
            !settings.performanceMode &&
            effectiveStyle != PillStyle.BLACK
        val blurRadius = if (useCrossBlur) 28 else 0

        var flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        if (isInteractive) {
            flags = flags or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        } else {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        if (!needsIme) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        if (keepAwake) {
            flags = flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        }
        if (useCrossBlur) {
            flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
        }

        val requireFullCanvas = state.mode == IslandMode.EXPANDED ||
            state.mode == IslandMode.MEDIUM ||
            state.ocrSelection != null ||
            state.isFlashlightCardOpen ||
            state.approvalPrompt != null ||
            state.statusBanner != null ||
            state.isSelectModeActive ||
            !settings.islandEnabled ||
            zone.isLandscape ||
            state.privacyIndicator.anyActive

        val targetW: Int
        val targetH: Int
        val targetX: Int
        val targetY: Int
        val offsetDp: Offset

        if (requireFullCanvas) {
            targetW = WindowManager.LayoutParams.MATCH_PARENT
            targetH = WindowManager.LayoutParams.MATCH_PARENT
            targetX = 0
            targetY = 0
            offsetDp = Offset.Zero
        } else {
            val isMusicPrimary = state.primaryActivity is IslandActivity.Music && state.mode == IslandMode.COMPACT
            val metrics = if (isMusicPrimary) {
                DeadZoneGeometry.musicCompactPillMetrics(zone.holeWidthDp, zone.holeHeightDp, isLandscape = false)
            } else {
                DeadZoneGeometry.compactSlotMetrics(
                    holeWidthDp = zone.holeWidthDp,
                    holeHeightDp = zone.holeHeightDp,
                    isLandscape = false,
                    slotWidthDp = settings.pillSizePreset.slotWidthDp
                )
            }
            val idleCameraMetrics = DeadZoneGeometry.idleCameraPillMetrics(
                holeWidthDp = zone.holeWidthDp,
                holeHeightDp = zone.holeHeightDp
            )
            val idleWidthDp = idleCameraMetrics.widthDp
            val pillWidthDp = when (state.mode) {
                IslandMode.IDLE -> idleWidthDp
                IslandMode.COMPACT -> metrics.totalWidthDp
                IslandMode.SPLIT -> metrics.totalWidthDp + 48f
                else -> metrics.totalWidthDp
            }
            val sideMarginDp = if (hasRotateChip) 96f else 28f
            val boxWidthDp = (pillWidthDp + sideMarginDp * 2f).coerceAtMost(zone.screenWidthPx / density)
            val boxHeightDp = maxOf(metrics.pillHeightDp, zone.holeHeightDp) + 26f
            targetW = (boxWidthDp * density).toInt().coerceAtLeast(1)
            targetH = (boxHeightDp * density).toInt().coerceAtLeast(1)
            targetX = (zone.cutoutRectPx.centerX() - targetW / 2).coerceIn(0, (zone.screenWidthPx - targetW).coerceAtLeast(0))
            targetY = (zone.cutoutRectPx.top - (8f * density).toInt()).coerceAtLeast(0)
            offsetDp = Offset(targetX / density, targetY / density)
        }

        val sig = "$flags:$blurRadius:$targetW:$targetH:$targetX:$targetY"
        if (lastDrawingSignature == sig) return
        lastDrawingSignature = sig

        val applyParams = {
            params.flags = flags
            params.width = targetW
            params.height = targetH
            params.x = targetX
            params.y = targetY
            windowOffsetDpState.value = offsetDp
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                params.blurBehindRadius = blurRadius
            }
            runCatching { wm.updateViewLayout(view, params) }
        }

        if (requireFullCanvas) {
            shrinkDrawingJob?.cancel()
            shrinkDrawingJob = null
            lastDrawingWasFullCanvas = true
            applyParams()
        } else if (lastDrawingWasFullCanvas) {
            shrinkDrawingJob?.cancel()
            shrinkDrawingJob = scope.launch {
                kotlinx.coroutines.delay(340L)
                lastDrawingWasFullCanvas = false
                applyParams()
            }
        } else {
            applyParams()
        }
    }

    private fun updateTouchWindowBounds(
        state: IslandState,
        settings: IslandSettings,
        zoneInput: ExclusionZone?,
        force: Boolean = false
    ) {
        val wm = windowManager ?: return
        val view = touchView ?: return
        val params = touchParams ?: return
        val zone = zoneInput ?: DeadZoneGeometry.measureLive(serviceContext, settings)
        val density = zone.density

        if (!settings.islandEnabled && !state.isCalibrationPreview) {
            if (lastTouchSignature != "DISABLED") {
                lastTouchSignature = "DISABLED"
                view.visibility = View.GONE
            }
            return
        }

        if (state.isSelectModeActive) {
            val sig = "SELECT"
            if (!force && lastTouchSignature == sig) return
            lastTouchSignature = sig

            view.setBackgroundColor(android.graphics.Color.argb(1, 0, 0, 0))
            params.gravity = Gravity.TOP or Gravity.START
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

        val hideTouchWindow = state.mode == IslandMode.EXPANDED ||
            state.mode == IslandMode.MEDIUM ||
            state.ocrSelection != null ||
            state.isFlashlightCardOpen ||
            state.approvalPrompt != null

        if (hideTouchWindow) {
            val sig = "GONE"
            if (!force && lastTouchSignature == sig) return
            lastTouchSignature = sig
            view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            view.visibility = View.GONE
            return
        }

        val isMusicPrimary = state.primaryActivity is IslandActivity.Music && state.mode == IslandMode.COMPACT
        val metrics = if (isMusicPrimary) {
            DeadZoneGeometry.musicCompactPillMetrics(zone.holeWidthDp, zone.holeHeightDp, zone.isLandscape)
        } else {
            DeadZoneGeometry.compactSlotMetrics(
                holeWidthDp = zone.holeWidthDp,
                holeHeightDp = zone.holeHeightDp,
                isLandscape = zone.isLandscape,
                slotWidthDp = settings.pillSizePreset.slotWidthDp
            )
        }
        val idleCameraMetrics = DeadZoneGeometry.idleCameraPillMetrics(
            holeWidthDp = zone.holeWidthDp,
            holeHeightDp = zone.holeHeightDp
        )

        if (!zone.isLandscape) {
            val idleWidthDp = idleCameraMetrics.widthDp.coerceAtLeast(48f)
            val pillWidthDp = when (state.mode) {
                IslandMode.IDLE -> idleWidthDp
                IslandMode.COMPACT -> metrics.totalWidthDp
                IslandMode.SPLIT -> metrics.totalWidthDp + 48f
                IslandMode.MEDIUM -> DeadZoneGeometry.MEDIUM_MAX_WIDTH_DP
                IslandMode.EXPANDED -> DeadZoneGeometry.FULL_MAX_WIDTH_DP
            }
            val touchW = ((pillWidthDp + 12f) * density).toInt()
            val touchH = ((metrics.pillHeightDp + 12f).coerceAtLeast(48f) * density).toInt()
            val targetX = ((zone.cutoutRectPx.centerX() - zone.screenWidthPx / 2f)).toInt()
            val targetY = (zone.cutoutRectPx.top - (2f * density).toInt()).coerceAtLeast(0)

            val sig = "PORTRAIT:$touchW:$touchH:$targetX:$targetY"
            if (!force && lastTouchSignature == sig) return
            lastTouchSignature = sig

            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            params.width = touchW
            params.height = touchH
            params.x = targetX
            params.y = targetY
        } else {
            val verticalLenDp = when (state.mode) {
                IslandMode.IDLE -> (zone.holeHeightDp + 44f).coerceAtLeast(64f)
                else -> metrics.totalWidthDp
            }
            val touchW = ((metrics.pillHeightDp + 12f).coerceAtLeast(48f) * density).toInt()
            val touchH = ((verticalLenDp + 16f) * density).toInt()
            val targetX = if (zone.isRightEdgeInLandscape) 0 else (zone.cutoutRectPx.left).coerceAtLeast(0)
            val targetY = (zone.cutoutRectPx.centerY() - zone.screenHeightPx / 2f).toInt()

            val sig = "LANDSCAPE:${zone.isRightEdgeInLandscape}:$touchW:$touchH:$targetX:$targetY"
            if (!force && lastTouchSignature == sig) return
            lastTouchSignature = sig

            params.gravity = (if (zone.isRightEdgeInLandscape) Gravity.END else Gravity.START) or Gravity.CENTER_VERTICAL
            params.width = touchW
            params.height = touchH
            params.x = targetX
            params.y = targetY
        }

        view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        view.visibility = View.VISIBLE
        runCatching { wm.updateViewLayout(view, params) }
    }

    private fun handlePillTap(app: DynamicIslandApp) {
        val primary = latestState.primaryActivity
        if (latestState.mode == IslandMode.IDLE) {
            app.islandController.openFromPill(latestSettings.twoStageOpen)
            return
        }
        when (latestSettings.tapGesture) {
            IslandGesture.OPEN_APP -> {
                when (primary) {
                    is IslandActivity.Music -> {
                        app.soundManager.playTap()
                        app.mediaHub.openPlayerApp()
                    }
                    is IslandActivity.NotificationPreview -> {
                        app.soundManager.playTap()
                        val i = serviceContext.packageManager.getLaunchIntentForPackage(primary.packageName)
                        i?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let { runCatching { serviceContext.startActivity(it) } }
                    }
                    is IslandActivity.Timer -> app.islandController.openFromPill(latestSettings.twoStageOpen, IslandTab.TIMER)
                    is IslandActivity.VoiceRecording -> app.islandController.openFromPill(latestSettings.twoStageOpen, IslandTab.MEMO)
                    else -> app.islandController.openFromPill(latestSettings.twoStageOpen)
                }
            }
            IslandGesture.TOGGLE_PLAY -> {
                if (primary is IslandActivity.Music) {
                    app.soundManager.playTap()
                    app.mediaHub.togglePlayPause()
                } else {
                    app.islandController.openFromPill(latestSettings.twoStageOpen)
                }
            }
            IslandGesture.EXPAND, IslandGesture.OPEN_CUSTOMISE, IslandGesture.SELECT_OCR -> {
                app.islandController.openFromPill(latestSettings.twoStageOpen)
            }
        }
    }

    private fun executePillSwipeAction(
        app: DynamicIslandApp,
        action: PillSwipeAction,
        isLeft: Boolean
    ) {
        if (latestState.primaryActivity is IslandActivity.NotificationPreview &&
            action == PillSwipeAction.TRACK_OR_NOTIF
        ) {
            if (isLeft) app.islandController.dismissActiveNotification() else app.islandController.nextNotification()
            return
        }
        when (action) {
            PillSwipeAction.TRACK_OR_NOTIF -> {
                when (latestState.primaryActivity) {
                    is IslandActivity.Music -> {
                        app.soundManager.playMusicNext()
                        if (isLeft) app.mediaHub.skipNext() else app.mediaHub.skipPrevious()
                    }
                    is IslandActivity.NotificationPreview -> app.islandController.nextNotification()
                    else -> app.islandController.expand(IslandTab.SEARCH)
                }
            }
            PillSwipeAction.VOLUME -> {
                val am = serviceContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val dir = if (isLeft) AudioManager.ADJUST_LOWER else AudioManager.ADJUST_RAISE
                am?.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir, AudioManager.FLAG_SHOW_UI)
            }
            PillSwipeAction.SEARCH -> app.islandController.expand(IslandTab.SEARCH)
            PillSwipeAction.EXPAND -> app.islandController.openFromPill(latestSettings.twoStageOpen)
            PillSwipeAction.COLLAPSE -> {
                if (latestState.primaryActivity is IslandActivity.NotificationPreview) {
                    app.islandController.dismissActiveNotification()
                } else {
                    app.islandController.collapse()
                }
            }
            PillSwipeAction.TORCH -> {
                val ok = app.quickTogglesManager.toggleFlashlight()
                app.soundManager.playToggle(ok && app.quickTogglesManager.state.value.isTorchOn)
            }
            PillSwipeAction.OPEN_SHELF -> app.islandController.expand(IslandTab.VAULT)
            PillSwipeAction.NONE -> Unit
        }
    }

    private fun handlePillLongPress(app: DynamicIslandApp) {
        when (latestSettings.longPressGesture) {
            IslandGesture.SELECT_OCR -> {
                scope.launch {
                    val allowed = OcrPrivacyGate.isScreenReadingAllowed(app.settingsRepository)
                    if (!allowed) {
                        app.islandController.showTransientBanner("Screen Reading is OFF in Privacy Settings")
                    } else {
                        app.islandController.setSelectMode(true)
                    }
                }
            }
            IslandGesture.OPEN_CUSTOMISE -> {
                val i = Intent(serviceContext, MainActivity::class.java).apply {
                    putExtra("open_route", "feature_picker")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { serviceContext.startActivity(i) }
            }
            else -> app.islandController.expand()
        }
    }

    private fun performSelectionHaptic(lightPress: Boolean = false) {
        val s = latestSettings
        if (s.hapticsStrength == HapticsStrength.OFF) return
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (serviceContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            serviceContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = if (!lightPress && s.hapticsStrength == HapticsStrength.MEDIUM) {
                VibrationEffect.EFFECT_CLICK
            } else {
                VibrationEffect.EFFECT_TICK
            }
            vibrator.vibrate(VibrationEffect.createPredefined(effect))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(if (!lightPress && s.hapticsStrength == HapticsStrength.MEDIUM) 16L else 8L)
        }
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
