package com.example

import android.content.ComponentName
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.example.floating.FloatingWindowManager
import com.example.island.Build4Checklist
import com.example.island.IslandActivity
import com.example.island.IslandController
import com.example.island.IslandMode
import com.example.island.OcrSelectionState
import com.example.island.OcrTextToken
import com.example.ocr.OcrPrivacyGate
import com.example.ocr.TierAAccessibilityReader
import com.example.ocr.TierAResult
import com.example.system.AnimationStyle
import com.example.system.CalculatorEngine
import com.example.system.CompanionPosition
import com.example.system.EventKind
import com.example.system.IslandSettings
import com.example.system.IslandSoundEvent
import com.example.system.IslandTab
import com.example.system.SettingsRepository
import com.example.system.TimerEngine
import com.example.ui.theme.MotionTokens
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DynamicIslandRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `island state machine transitions Idle Compact Split Expanded and emits distinct sounds`() {
        val events = mutableListOf<IslandSoundEvent>()
        val controller = IslandController(onSoundEvent = { events.add(it) })
        assertEquals(IslandMode.IDLE, controller.state.value.mode)

        controller.upsertActivity(
            IslandActivity.Music(
                packageName = "com.unshoo.pixelmusic",
                title = "Starlight",
                artist = "Muse",
                isPlaying = true,
                positionMs = 1000L,
                durationMs = 180_000L
            )
        )
        assertEquals(IslandMode.COMPACT, controller.state.value.mode)

        controller.upsertActivity(
            IslandActivity.Timer(
                label = "Timer",
                remainingMs = 60_000L,
                totalMs = 60_000L,
                isRunning = true
            )
        )
        assertEquals(IslandMode.SPLIT, controller.state.value.mode)

        assertTrue(IslandTab.entries.none { it.name == "CALC" })

        controller.expand(IslandTab.TOOLS)
        assertEquals(IslandMode.EXPANDED, controller.state.value.mode)
        assertEquals(IslandTab.TOOLS, controller.state.value.activeTab)

        controller.selectTab(IslandTab.TIMER)
        controller.selectTab(IslandTab.SEARCH)
        assertTrue(controller.state.value.needsKeyboardFocus)

        controller.toggleMoreRow()
        controller.collapse()
        assertEquals(IslandMode.SPLIT, controller.state.value.mode)
        assertFalse(controller.state.value.needsKeyboardFocus)

        // Timer always attaches directly to the Dynamic Island pill rather than a floating mini window
        controller.setFloatingMiniTab(IslandTab.TIMER)
        assertEquals(IslandMode.EXPANDED, controller.state.value.mode)
        assertEquals(IslandTab.TIMER, controller.state.value.activeTab)
        assertNull(controller.state.value.activeFloatingMiniTab)

        assertEquals(
            listOf(
                IslandSoundEvent.EXPAND,
                IslandSoundEvent.TAP,
                IslandSoundEvent.SEARCH_OPEN,
                IslandSoundEvent.TAP,
                IslandSoundEvent.COLLAPSE,
                IslandSoundEvent.EXPAND
            ),
            events
        )
    }

    @Test
    fun `two-stage open, companion swap, MotionTokens, and Build4Checklist work cleanly`() = runTest {
        assertEquals(12, Build4Checklist.entries.size)

        val controller = IslandController()
        controller.upsertActivity(
            IslandActivity.Music(
                packageName = "com.unshoo.pixelmusic",
                title = "Track",
                artist = "Artist",
                isPlaying = true,
                positionMs = 0L,
                durationMs = 120_000L
            )
        )
        controller.upsertActivity(
            IslandActivity.Timer(
                label = "Timer",
                remainingMs = 30_000L,
                totalMs = 30_000L,
                isRunning = true
            )
        )
        val initialPrimaryId = controller.state.value.primaryActivity?.id
        val initialSecondaryId = controller.state.value.secondaryActivity?.id
        assertNotNull(initialPrimaryId)
        assertNotNull(initialSecondaryId)
        controller.swapPrimaryAndSecondary()
        assertEquals(initialSecondaryId, controller.state.value.primaryActivity?.id)

        // Two-stage open: Compact/Split -> MEDIUM -> EXPANDED
        controller.openFromPill(twoStageOpen = true)
        assertEquals(IslandMode.MEDIUM, controller.state.value.mode)
        controller.openFromPill(twoStageOpen = true)
        assertEquals(IslandMode.EXPANDED, controller.state.value.mode)
        controller.collapse()
        controller.openFromPill(twoStageOpen = false)
        assertEquals(IslandMode.EXPANDED, controller.state.value.mode)

        // MotionTokens curves & NONE snap (including OpeningEffect & non-bouncing collapse)
        assertTrue(MotionTokens.idleToMediumSpec<Float>(AnimationStyle.NONE, 0.5f, 1.0f) is SnapSpec<*>)
        assertTrue(MotionTokens.collapseSpec<Float>(AnimationStyle.NONE, 1.0f) is SnapSpec<*>)
        assertTrue(MotionTokens.openingEffectSpec<Float>(com.example.system.OpeningEffect.LIQUID, AnimationStyle.NONE, 0.5f, 1.0f) is SnapSpec<*>)
        assertFalse(MotionTokens.idleToMediumSpec<Float>(AnimationStyle.FRUIT, 0.8f, 1.0f) is SnapSpec<*>)
        assertFalse(MotionTokens.openingEffectSpec<Float>(com.example.system.OpeningEffect.POP, AnimationStyle.FRUIT, 0.8f, 1.0f) is SnapSpec<*>)

        // No-flicker rule: duplicate activity updates keep order and do not steal front
        val sameNotif = IslandActivity.NotificationPreview(
            id = "notif_1",
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            title = "Alex",
            text = "On my way"
        )
        val secondNotif = IslandActivity.NotificationPreview(
            id = "notif_2",
            packageName = "com.telegram",
            appName = "Telegram",
            title = "Sam",
            text = "See you at 5"
        )
        controller.upsertActivity(sameNotif)
        controller.upsertActivity(secondNotif)
        val queueSizeBeforeDup = controller.state.value.activityQueue.size
        controller.upsertActivity(sameNotif) // Visually identical: ignored
        assertEquals(queueSizeBeforeDup, controller.state.value.activityQueue.size)

        // Notification cycling & dismiss + 15s reaper pruneStaleNotifications
        controller.nextNotification()
        controller.dismissActiveNotification()
        controller.pruneStaleNotifications(emptySet())
        assertTrue(controller.state.value.activityQueue.none { it is IslandActivity.NotificationPreview })

        // Low-battery warning <= 15% sets red glyph state and resets above threshold
        controller.updateBatteryTelemetry(levelPercent = 15, isCharging = false, isLowBatteryWarning = true)
        assertEquals(15, controller.state.value.batteryLevelPercent)
        assertTrue(controller.state.value.isLowBatteryWarning)
        controller.updateBatteryTelemetry(levelPercent = 30, isCharging = true, isLowBatteryWarning = false)
        assertFalse(controller.state.value.isLowBatteryWarning)

        // Settings persistence for Build #4 options + Opening/Content effects & Swipe actions
        val repo = SettingsRepository(composeRule.activity)
        repo.resetSettings()
        repo.update {
            it.copy(
                animationStyle = AnimationStyle.SPRING,
                openingEffect = com.example.system.OpeningEffect.POP,
                contentEffect = com.example.system.ContentEffect.SLIDE_UP,
                pillSwipeLeftGesture = com.example.system.PillSwipeAction.VOLUME,
                pillSwipeRightGesture = com.example.system.PillSwipeAction.SEARCH,
                expandedSwipeLeftGesture = com.example.system.ExpandedSwipeAction.COLLAPSE,
                expandedSwipeRightGesture = com.example.system.ExpandedSwipeAction.DISMISS,
                companionPosition = CompanionPosition.LEFT,
                twoStageOpen = false,
                animationElasticity = 0.9f
            )
        }
        val updated = repo.settingsFlow.first()
        assertEquals(AnimationStyle.SPRING, updated.animationStyle)
        assertEquals("Standard", updated.animationStyle.label)
        assertEquals("Springy", AnimationStyle.FRUIT.label)
        assertEquals("Calm", AnimationStyle.SMOOTH.label)
        assertEquals(com.example.system.OpeningEffect.POP, updated.openingEffect)
        assertEquals(com.example.system.ContentEffect.SLIDE_UP, updated.contentEffect)
        assertEquals(com.example.system.PillSwipeAction.VOLUME, updated.pillSwipeLeftGesture)
        assertEquals(com.example.system.PillSwipeAction.SEARCH, updated.pillSwipeRightGesture)
        assertEquals(com.example.system.ExpandedSwipeAction.COLLAPSE, updated.expandedSwipeLeftGesture)
        assertEquals(com.example.system.ExpandedSwipeAction.DISMISS, updated.expandedSwipeRightGesture)
        assertEquals(CompanionPosition.LEFT, updated.companionPosition)
        assertFalse(updated.twoStageOpen)
        assertEquals(0.9f, updated.animationElasticity, 0.01f)
        assertTrue(updated.eventConfig(EventKind.CHARGING).enabled)
        repo.resetSettings()
    }

    @Test
    fun `search engine returns more than 8 apps uncapped and OCR selection copies only explicit range`() = runTest {
        val app = composeRule.activity.application as DynamicIslandApp
        val shadowPm = Shadows.shadowOf(composeRule.activity.packageManager)
        val letters = listOf("Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta", "Theta", "Iota", "Kappa", "Lambda", "Mu")
        letters.forEachIndexed { idx, name ->
            val pkg = "com.example.fakeapp.$idx"
            val actName = "$pkg.MainActivity"
            val comp = ComponentName(pkg, actName)
            shadowPm.addActivityIfNotPresent(comp)
            val filter = IntentFilter(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
            shadowPm.addIntentFilterForActivity(comp, filter)
            val ri = ResolveInfo().apply {
                activityInfo = ActivityInfo().apply {
                    packageName = pkg
                    this.name = actName
                }
                nonLocalizedLabel = name
            }
            shadowPm.addResolveInfoForIntent(
                android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER),
                ri
            )
        }

        val allApps = app.searchEngine.getInstalledApps(forceRefresh = true)
        assertTrue("Expected > 8 launchable apps uncapped, got ${allApps.size}", allApps.size >= 12)
        assertTrue(allApps.any { it.sectionLetter == 'A' })
        assertTrue(allApps.any { it.sectionLetter == 'Z' })

        // Verify OCR selection state computes only the selected token slice (never auto-copies)
        val full = "Order 48291 confirmed today"
        val tokens = listOf(
            OcrTextToken("Order", 0, 5, 10, 100, 90, 140),
            OcrTextToken("48291", 6, 11, 100, 100, 190, 140),
            OcrTextToken("confirmed", 12, 21, 200, 100, 340, 140),
            OcrTextToken("today", 22, 27, 350, 100, 440, 140)
        )
        val sel = OcrSelectionState(
            fullText = full,
            elementBounds = Rect(0, 80, 500, 160),
            tokens = tokens,
            startTokenIndex = 1,
            endTokenIndex = 1,
            anchorX = 140f,
            anchorY = 120f
        )
        assertEquals("48291", sel.selectedText)
        val expandedSel = sel.copy(startTokenIndex = 1, endTokenIndex = 2)
        assertEquals("48291 confirmed", expandedSel.selectedText)
    }

    @Test
    fun `all 14 unique UI sound assets exist, pass mastering checks, and respect toggle and volume gating`() {
        assertEquals(14, IslandSoundEvent.entries.size)
        val rawResources = listOf(
            R.raw.sfx_expand,
            R.raw.sfx_collapse,
            R.raw.sfx_tap,
            R.raw.sfx_timer_set,
            R.raw.sfx_timer_done,
            R.raw.sfx_toggle,
            R.raw.sfx_toggle_off,
            R.raw.sfx_notification,
            R.raw.sfx_music_play,
            R.raw.sfx_music_pause,
            R.raw.sfx_music_next,
            R.raw.sfx_vault_add,
            R.raw.sfx_search_open,
            R.raw.sfx_error
        )
        assertEquals(14, rawResources.distinct().size)

        val rmsDbValues = mutableListOf<Double>()
        val tickIds = setOf(R.raw.sfx_tap, R.raw.sfx_music_next)

        rawResources.forEach { resId ->
            val bytes = composeRule.activity.resources.openRawResource(resId).use { it.readBytes() }
            assertTrue("WAV too short for $resId", bytes.size > 44)
            assertEquals("RIFF", String(bytes, 0, 4))
            assertEquals("WAVE", String(bytes, 8, 4))

            fun leShort(off: Int): Int =
                (bytes[off].toInt() and 0xFF) or ((bytes[off + 1].toInt() and 0xFF) shl 8)
            fun leInt(off: Int): Int =
                (bytes[off].toInt() and 0xFF) or
                    ((bytes[off + 1].toInt() and 0xFF) shl 8) or
                    ((bytes[off + 2].toInt() and 0xFF) shl 16) or
                    ((bytes[off + 3].toInt() and 0xFF) shl 24)

            val audioFormat = leShort(20)
            val channels = leShort(22)
            val sampleRate = leInt(24)
            val bitsPerSample = leShort(34)
            assertEquals("PCM format required", 1, audioFormat)
            assertEquals("Mono required", 1, channels)
            assertEquals("44.1 kHz required", 44100, sampleRate)
            assertEquals("16-bit PCM required", 16, bitsPerSample)

            // Locate 'data' chunk
            var pos = 12
            var dataStart = -1
            var dataSize = 0
            while (pos + 8 <= bytes.size) {
                val id = String(bytes, pos, 4)
                val sz = leInt(pos + 4)
                if (id == "data") {
                    dataStart = pos + 8
                    dataSize = sz.coerceAtMost(bytes.size - dataStart)
                    break
                }
                pos += 8 + sz
            }
            assertTrue("Missing data chunk", dataStart > 0 && dataSize > 0)

            val sampleCount = dataSize / 2
            val durationSec = sampleCount / 44100.0
            assertTrue("WAV duration ${durationSec}s exceeds 1.0s", durationSec <= 1.0)

            val samples = DoubleArray(sampleCount) { i ->
                val o = dataStart + i * 2
                val raw = (bytes[o].toInt() and 0xFF) or (bytes[o + 1].toInt() shl 8)
                raw / 32768.0
            }

            // Leading silence <= 5 ms
            val firstAudibleIdx = samples.indexOfFirst { kotlin.math.abs(it) >= (256.0 / 32768.0) }
            assertTrue("No audible audio in $resId", firstAudibleIdx >= 0)
            val leadMs = (firstAudibleIdx / 44.1)
            assertTrue("Leading silence ${leadMs}ms > 5ms", leadMs <= 5.0)

            val peak = samples.maxOf { kotlin.math.abs(it) }
            val peakDbfs = 20.0 * kotlin.math.log10(peak.coerceAtLeast(1e-9))
            val rms = kotlin.math.sqrt(samples.sumOf { it * it } / sampleCount)
            val rmsDbfs = 20.0 * kotlin.math.log10(rms.coerceAtLeast(1e-9))
            rmsDbValues.add(rmsDbfs)

            if (resId in tickIds) {
                assertTrue("Tick peak ${peakDbfs} dBFS > -12 dBFS", peakDbfs <= -12.0)
            }

            // Ensure negligible energy below 300 Hz (speaker safety on Redmi Note 10 Pro)
            val fftN = minOf(sampleCount, 4096)
            var lowBandEnergy = 0.0
            var totalSampleEnergy = 0.0
            for (i in 0 until fftN) totalSampleEnergy += samples[i] * samples[i]
            for (freqHz in listOf(60.0, 100.0, 150.0, 200.0, 250.0, 280.0)) {
                var re = 0.0
                var im = 0.0
                val w = 2.0 * Math.PI * freqHz / 44100.0
                for (i in 0 until fftN) {
                    re += samples[i] * kotlin.math.cos(w * i)
                    im -= samples[i] * kotlin.math.sin(w * i)
                }
                lowBandEnergy += (re * re + im * im) / fftN
            }
            val lowRatio = lowBandEnergy / totalSampleEnergy.coerceAtLeast(1e-9)
            assertTrue("Low-frequency (<300 Hz) energy ratio $lowRatio too high for $resId", lowRatio < 0.05)
        }

        val familyMeanRms = rmsDbValues.average()
        assertTrue(
            "Overall family RMS ${familyMeanRms} dBFS should be within -18.5..-12.0 dBFS",
            familyMeanRms in -18.5..-12.0
        )

        val defaults = IslandSettings()
        assertTrue(defaults.soundEnabled)
        assertEquals(0.75f, defaults.soundVolume, 0.001f)

        var timerDoneFired = false
        val controller = IslandController()
        val engine = TimerEngine(composeRule.activity, controller) {
            timerDoneFired = true
        }
        engine.onTimerCompletedFromAlarm()
        assertTrue(timerDoneFired)

        val app = composeRule.activity.application as DynamicIslandApp
        app.soundManager.updateCachedAudioSettings(enabled = false, volume = 0.5f)
        assertFalse(app.soundManager.play(IslandSoundEvent.EXPAND))
        app.soundManager.updateCachedAudioSettings(enabled = true, volume = 0f)
        assertFalse(app.soundManager.play(IslandSoundEvent.ERROR))
        app.soundManager.updateCachedAudioSettings(enabled = true, volume = 0.75f)
    }

    @Test
    fun `motion tokens match exact M3 Expressive table and source contains zero banned animation APIs`() {
        val sd = MotionTokens.m3SpatialDefault<Float>()
        assertEquals(0.8f, sd.dampingRatio, 0.0001f)
        assertEquals(380f, sd.stiffness, 0.0001f)

        val sf = MotionTokens.m3SpatialFast<Float>()
        assertEquals(0.6f, sf.dampingRatio, 0.0001f)
        assertEquals(800f, sf.stiffness, 0.0001f)

        val ed = MotionTokens.m3EffectsDefault<Float>()
        assertEquals(1.0f, ed.dampingRatio, 0.0001f)
        assertEquals(1600f, ed.stiffness, 0.0001f)

        val pm = MotionTokens.m3PillMorph<Float>()
        assertEquals(0.6f, pm.dampingRatio, 0.0001f)
        assertEquals(Spring.StiffnessLow, pm.stiffness, 0.0001f)

        val collapse = MotionTokens.collapseSpec<Float>(AnimationStyle.SPRING, 1.0f) as SpringSpec<Float>
        assertEquals(1.0f, collapse.dampingRatio, 0.0001f)

        assertEquals(0.97f, MotionTokens.TAP_SQUISH_SCALE, 0.0001f)
        assertEquals(180, MotionTokens.GLASS_FADE_MS)
        assertEquals(300, MotionTokens.CONTENT_ENTER_MS)
        assertEquals(300, MotionTokens.CONTENT_ENTER_DELAY_MS)
        assertEquals(200, MotionTokens.CONTENT_STAGGER_MS)
        assertEquals(240, MotionTokens.COLLAPSE_MS)

        // Audit main source files for banned animation APIs
        val candidateRoots = listOf(
            java.io.File("src/main/java/com/example"),
            java.io.File("app/src/main/java/com/example")
        )
        val srcRoot = candidateRoots.firstOrNull { it.exists() && it.isDirectory }
        assertNotNull("Could not locate main Kotlin source directory for animation audit", srcRoot)
        val bannedPatterns = listOf(
            "rememberInfiniteTransition",
            "infiniteRepeatable",
            "animateDpAsState",
            "SizeTransform",
            ".blur("
        )
        srcRoot!!.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val text = file.readText()
                bannedPatterns.forEach { banned ->
                    assertFalse(
                        "Found banned animation API '$banned' in ${file.name}",
                        text.contains(banned)
                    )
                }
            }
    }

    @Test
    fun `ajitpal rule preserves activity order on updates and promotes only new pop events`() {
        val controller = IslandController()
        val timer = IslandActivity.Timer(
            id = "timer_1",
            label = "Focus",
            remainingMs = 120_000L,
            totalMs = 120_000L,
            isRunning = true
        )
        val n1 = IslandActivity.NotificationPreview(
            id = "notif_a",
            packageName = "com.pkg.a",
            appName = "AppA",
            title = "Alice",
            text = "Ping 1"
        )
        val n2 = IslandActivity.NotificationPreview(
            id = "notif_b",
            packageName = "com.pkg.b",
            appName = "AppB",
            title = "Bob",
            text = "Ping 2"
        )
        controller.upsertActivity(timer)
        controller.upsertActivity(n1)
        // New pop n1 promotes in front of persistent timer; timer remains in queue
        assertEquals("notif_a", controller.state.value.primaryActivity?.id)

        // Updating persistent timer updates content in place WITHOUT stealing front from n1
        controller.upsertActivity(timer.copy(remainingMs = 119_000L))
        assertEquals("notif_a", controller.state.value.primaryActivity?.id)
        val updatedTimer = controller.state.value.activityQueue.first { it.id == "timer_1" } as IslandActivity.Timer
        assertEquals(119_000L, updatedTimer.remainingMs)

        // New pop n2 replaces n1 at the front while moving n1 to notificationsHistory
        controller.upsertActivity(n2)
        assertEquals("notif_b", controller.state.value.primaryActivity?.id)
        assertTrue(controller.state.value.notificationsHistory.any { it.id == "notif_a" })

        // Re-posting visually unchanged displaced n1 is ignored (no redraw, does not steal front)
        controller.upsertActivity(n1.copy())
        assertEquals("notif_b", controller.state.value.primaryActivity?.id)
    }

    @Test
    fun `ocr privacy gate defaults OFF and blocks reading when disabled`() = runTest {
        val repo = SettingsRepository(composeRule.activity)
        repo.resetSettings()
        assertFalse(OcrPrivacyGate.isScreenReadingAllowed(repo))
        assertFalse(OcrPrivacyGate.isTierBAllowed(repo))

        val initial = repo.settingsFlow.first()
        assertEquals(IslandSettings.MAX_ISLAND_FEATURES, initial.enabledTabs.size)

        // Adding a 9th tab (MEMO) must be rejected while at 8/8
        val added9th = repo.toggleTabInIsland(IslandTab.MEMO)
        assertFalse(added9th)
        assertEquals(8, repo.settingsFlow.first().enabledTabs.size)

        // Removing one allows adding another
        assertTrue(repo.toggleTabInIsland(IslandTab.ALERTS))
        assertEquals(7, repo.settingsFlow.first().enabledTabs.size)
        assertTrue(repo.toggleTabInIsland(IslandTab.MEMO))
        assertEquals(8, repo.settingsFlow.first().enabledTabs.size)

        // Tier A refuses without consent even if called directly
        val refused = TierAAccessibilityReader.readNodeAtPoint(
            service = null,
            screenReadingAllowed = false,
            x = 100,
            y = 100,
            ownPackageName = composeRule.activity.packageName
        )
        assertTrue(refused is TierAResult.Refused)
    }

    @Test
    fun `floating window manager builds freeform windowing mode 5 bundle`() {
        val app = composeRule.activity.application as DynamicIslandApp
        val bundle = app.floatingWindowManager.buildFreeformOptionsBundle(Rect(40, 120, 680, 960))
        assertEquals(
            FloatingWindowManager.WINDOWING_MODE_FREEFORM,
            bundle.getInt(FloatingWindowManager.KEY_LAUNCH_WINDOWING_MODE)
        )
    }

    @Test
    fun `calculator swipe backspace removes trailing digits cleanly`() {
        val calc = CalculatorEngine()
        calc.pressDigit("1")
        calc.pressDigit("2")
        calc.pressDigit("3")
        assertEquals("123", calc.state.value.display)
        calc.pressBackspace()
        assertEquals("12", calc.state.value.display)
        calc.pressBackspace()
        assertEquals("1", calc.state.value.display)
        calc.pressBackspace()
        assertEquals("0", calc.state.value.display)
        calc.pressDigit("5")
        calc.pressToggleSign()
        assertEquals("-5", calc.state.value.display)
        calc.pressBackspace()
        assertEquals("0", calc.state.value.display)
    }

    @Test
    fun `shelf photo crop creates cropped PNG and updates item file path`() = kotlinx.coroutines.runBlocking {
        val app = composeRule.activity.application as DynamicIslandApp
        val bmp = Bitmap.createBitmap(100, 80, Bitmap.Config.ARGB_8888)
        val saved = app.vaultManager.saveBitmap(bmp, sourceType = "PHOTO_TEST", namePrefix = "crop_src")
        bmp.recycle()
        assertNotNull("VaultManager.saveBitmap returned null", saved)
        val savedItem = saved!!

        val croppedItem = app.vaultManager.cropVaultItem(
            itemId = savedItem.id,
            leftFrac = 0.1f,
            topFrac = 0.2f,
            rightFrac = 0.7f,
            bottomFrac = 0.8f
        )
        assertNotNull(croppedItem)
        val updated = croppedItem!!
        assertTrue(updated.filePath != savedItem.filePath)
        val outFile = java.io.File(updated.filePath)
        assertTrue(outFile.exists())
        val decoded = BitmapFactory.decodeFile(outFile.absolutePath)
        assertNotNull(decoded)
        assertEquals(60, decoded.width)
        assertEquals(48, decoded.height)
        decoded.recycle()

        val dbItem = app.database.vaultDao().getById(savedItem.id)
        assertNotNull(dbItem)
        assertEquals(updated.filePath, dbItem!!.filePath)
        app.vaultManager.purgeItemNow(savedItem.id)
    }

    @Test
    fun `main screen opens settings 6 groups, setup wizard, tools screenshot vault, sleep dial, search pinned apps, and no calc tab`() {
        val app = composeRule.activity.application as DynamicIslandApp
        val shadowPm = Shadows.shadowOf(composeRule.activity.packageManager)
        val calcPkg = "com.android.calculator2"
        val calcAct = "$calcPkg.Calculator"
        val calcComp = ComponentName(calcPkg, calcAct)
        shadowPm.addActivityIfNotPresent(calcComp)
        shadowPm.addIntentFilterForActivity(
            calcComp,
            IntentFilter(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
        )
        shadowPm.addResolveInfoForIntent(
            android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER),
            ResolveInfo().apply {
                activityInfo = ActivityInfo().apply {
                    packageName = calcPkg
                    name = calcAct
                }
                nonLocalizedLabel = "Calculator"
            }
        )
        val installed = kotlinx.coroutines.runBlocking {
            app.searchEngine.getInstalledApps(forceRefresh = true)
        }
        assertTrue("Expected Calculator in installed apps, got ${installed.map { it.packageName }}", installed.any { it.packageName == calcPkg })
        app.searchEngine.refreshApps()

        composeRule.waitForIdle()

        // Calculator tab/feature is removed from chooser dock
        composeRule.onNodeWithTag("chooser_tab_calc").assertDoesNotExist()

        // Switch sandbox tab to Tools: Screenshot->Vault exists and Floating Apps button does NOT exist
        composeRule.onNodeWithTag("chooser_tab_tools").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tools_screenshot_vault_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("tools_floating_apps_btn").assertDoesNotExist()

        // Switch sandbox tab to Timer and verify Sleep Dial tab renders
        composeRule.onNodeWithTag("chooser_tab_timer").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("timer_mode_sleep").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("sleep_start_button").assertIsDisplayed()

        // Switch sandbox tab to Search, pin Calculator, and verify PINNED APPS section renders
        composeRule.onNodeWithTag("chooser_tab_search").performClick()
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            runCatching {
                composeRule.onNodeWithTag("search_app_opener_list")
                    .performScrollToNode(hasTestTag("search_pin_$calcPkg"))
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("search_pin_$calcPkg").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("search_pinned_section").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("search_pinned_section").assertIsDisplayed()
        composeRule.onNodeWithTag("search_pinned_chip_$calcPkg").assertIsDisplayed()

        // Verify Shelf tab is directly accessible in main chooser bar
        composeRule.onNodeWithTag("chooser_tab_vault").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("vault_filter_all").assertIsDisplayed()

        // Open Settings and verify all 6 groups render
        composeRule.onNodeWithTag("open_settings_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_island_master_switch").assertIsDisplayed().assertIsOn()
        composeRule.onNodeWithTag("settings_home_list").performScrollToNode(hasTestTag("settings_group_look"))
        composeRule.onNodeWithTag("settings_group_look").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_home_list").performScrollToNode(hasTestTag("settings_group_motion"))
        composeRule.onNodeWithTag("settings_group_motion").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_home_list").performScrollToNode(hasTestTag("settings_group_pill"))
        composeRule.onNodeWithTag("settings_group_pill").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_home_list").performScrollToNode(hasTestTag("settings_group_features"))
        composeRule.onNodeWithTag("settings_group_features").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_home_list").performScrollToNode(hasTestTag("settings_group_privacy"))
        composeRule.onNodeWithTag("settings_group_privacy").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_home_list").performScrollToNode(hasTestTag("settings_group_about"))
        composeRule.onNodeWithTag("settings_group_about").assertIsDisplayed()

        // Open Settings > Look (Appearance) and tap all 3 live previews + size presets (must never crash)
        composeRule.onNodeWithTag("settings_home_list").performScrollToNode(hasTestTag("settings_group_look"))
        composeRule.onNodeWithTag("settings_group_look").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("look_preview_black").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("look_preview_apple_glass").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("look_preview_liquid_glass").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("look_size_large").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_detail_back").performClick()
        composeRule.waitForIdle()

        // Open Motion settings and verify Build #4 Motion controls + subtabs + Sound Effects + Try It demo
        composeRule.onNodeWithTag("settings_home_list").performScrollToNode(hasTestTag("settings_group_motion"))
        composeRule.onNodeWithTag("settings_group_motion").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("motion_subtab_background").assertIsDisplayed()
        composeRule.onNodeWithTag("motion_subtab_animations").assertIsDisplayed()
        composeRule.onNodeWithTag("motion_subtab_gestures").assertIsDisplayed()
        composeRule.onNodeWithTag("motion_style_fruit").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_detail_list").performScrollToNode(hasTestTag("opening_effect_liquid"))
        composeRule.onNodeWithTag("opening_effect_liquid").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_detail_list").performScrollToNode(hasTestTag("content_effect_blur_in"))
        composeRule.onNodeWithTag("content_effect_blur_in").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_detail_list").performScrollToNode(hasTestTag("motion_sound_switch"))
        composeRule.onNodeWithTag("motion_sound_switch").assertIsDisplayed().assertIsOn()
        composeRule.onNodeWithTag("settings_detail_list").performScrollToNode(hasTestTag("motion_sound_volume_slider"))
        composeRule.onNodeWithTag("motion_sound_volume_slider").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("settings_detail_list").performScrollToNode(hasTestTag("motion_try_it_btn"))
        composeRule.onNodeWithTag("motion_try_it_btn").assertIsDisplayed()

        // Toggling Sound Effects OFF disables the volume slider; toggling back ON restores it
        composeRule.onNodeWithTag("settings_detail_list").performScrollToNode(hasTestTag("motion_sound_switch"))
        composeRule.onNodeWithTag("motion_sound_switch").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            runCatching {
                composeRule.onNodeWithTag("motion_sound_switch").assertIsOff()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("settings_detail_list").performScrollToNode(hasTestTag("motion_sound_volume_slider"))
        composeRule.onNodeWithTag("motion_sound_volume_slider").assertIsNotEnabled()
        composeRule.onNodeWithTag("settings_detail_list").performScrollToNode(hasTestTag("motion_sound_switch"))
        composeRule.onNodeWithTag("motion_sound_switch").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            runCatching {
                composeRule.onNodeWithTag("motion_sound_switch").assertIsOn()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("settings_detail_list").performScrollToNode(hasTestTag("motion_sound_volume_slider"))
        composeRule.onNodeWithTag("motion_sound_volume_slider").assertIsEnabled()

        // Navigate back, open Pill group & verify Companion Circle Left/Right side toggle
        composeRule.onNodeWithTag("settings_detail_back").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_home_list").performScrollToNode(hasTestTag("settings_group_pill"))
        composeRule.onNodeWithTag("settings_group_pill").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_detail_list").performScrollToNode(hasTestTag("pill_companion_left"))
        composeRule.onNodeWithTag("pill_companion_left").assertIsDisplayed()
        composeRule.onNodeWithTag("pill_companion_right").assertIsDisplayed()

        // Navigate back to Settings, test all 9 Floating Glass Dock tabs, and open Setup Wizard
        composeRule.onNodeWithTag("settings_detail_back").performClick()
        composeRule.waitForIdle()
        listOf(
            "dock_tab_notes",
            "dock_tab_alerts",
            "dock_tab_shelf",
            "dock_tab_tools",
            "dock_tab_music",
            "dock_tab_timer",
            "dock_tab_search",
            "dock_tab_settings",
            "dock_tab_home"
        ).forEach { tabTag ->
            composeRule.onNodeWithTag(tabTag).assertIsDisplayed().performClick()
            composeRule.waitForIdle()
        }

        composeRule.onNodeWithTag("open_wizard_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("wizard_acc_btn").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("wizard_nl_btn").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("wizard_overlay_btn").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("wizard_screenshots_btn").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("wizard_finish_btn").performScrollTo().performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun `islandEnabled, pillStyle, and pillSizePreset persist cleanly in SettingsRepository`() = runTest {
        val repo = SettingsRepository(composeRule.activity)
        repo.resetSettings()
        repo.update {
            it.copy(
                islandEnabled = false,
                pillStyle = com.example.system.PillStyle.APPLE_GLASS,
                pillSizePreset = com.example.system.PillSizePreset.LARGE
            )
        }
        val saved = repo.settingsFlow.first()
        assertFalse(saved.islandEnabled)
        assertEquals(com.example.system.PillStyle.APPLE_GLASS, saved.pillStyle)
        assertEquals(com.example.system.PillSizePreset.LARGE, saved.pillSizePreset)
        repo.resetSettings()
    }
}
