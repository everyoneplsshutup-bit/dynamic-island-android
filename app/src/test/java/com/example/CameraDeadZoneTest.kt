package com.example

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.island.AlertKind
import com.example.island.CompactPillView
import com.example.island.DeadZoneGeometry
import com.example.island.ExclusionZone
import com.example.island.ExpandedIslandCard
import com.example.island.IslandActivity
import com.example.island.IslandMode
import com.example.island.IslandState
import com.example.island.MediumPillView
import com.example.media.MediaUiState
import com.example.system.CompanionPosition
import com.example.system.IslandSettings
import com.example.system.IslandTab
import com.example.system.TimerUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-440dpi")
class CameraDeadZoneTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `DeadZoneGeometry isSafe enforces symmetry and 8dp clearance`() {
        listOf(14f, 26f, 80f).forEach { holeW ->
            val metrics = DeadZoneGeometry.compactSlotMetrics(holeWidthDp = holeW, holeHeightDp = 33f)
            assertTrue(
                "CompactSlotMetrics must be safe for holeW=$holeW",
                DeadZoneGeometry.isSafe(metrics.leftSlotDp, metrics.rightSlotDp, holeW)
            )
            assertTrue(
                "Left and right slots must match within 0.5dp for holeW=$holeW",
                kotlin.math.abs(metrics.leftSlotDp - metrics.rightSlotDp) <= 0.5f
            )
        }
        assertFalse(DeadZoneGeometry.isSafe(leftSlotDp = 88f, rightSlotDp = 80f, holeWidthDp = 26f))
        assertFalse(DeadZoneGeometry.isSafe(leftSlotDp = 0f, rightSlotDp = 0f, holeWidthDp = 26f))
    }

    @Test
    fun `compact pill never intersects camera exclusion rect across worst-case payloads and hole widths`() {
        val longTitle = "Very Long Track Title Name With Sixty Characters Exact Length"
        val longAppName = "ExtremelyLongApplicationName30"
        val longAlertTitle = "Emergency System Alert Title FortyChars!"

        val scenarios: List<Pair<String, IslandState>> = listOf(
            "Music60" to IslandState(
                mode = IslandMode.COMPACT,
                activityQueue = listOf(
                    IslandActivity.Music(
                        packageName = "com.unshoo.pixelmusic",
                        title = longTitle,
                        artist = "Artist Name",
                        isPlaying = true,
                        positionMs = 15_000L,
                        durationMs = 240_000L
                    )
                )
            ),
            "Notif30LeftCompanion" to IslandState(
                mode = IslandMode.SPLIT,
                activityQueue = listOf(
                    IslandActivity.NotificationPreview(
                        id = "notif_long",
                        priority = 90,
                        packageName = "com.example.chat",
                        appName = longAppName,
                        title = "Sixty Character Notification Title Should Never Appear In Pill",
                        text = "Body"
                    ),
                    IslandActivity.Timer(
                        priority = 80,
                        label = "Timer",
                        remainingMs = 99_000L,
                        totalMs = 120_000L,
                        isRunning = true
                    )
                )
            ),
            "Charging100_99W" to IslandState(
                mode = IslandMode.COMPACT,
                activityQueue = listOf(
                    IslandActivity.Charging(
                        levelPercent = 100,
                        isCharging = true,
                        isFull = true,
                        wattageWatts = 99f,
                        pluggedLabel = "AC"
                    )
                )
            ),
            "Timer235959" to IslandState(
                mode = IslandMode.COMPACT,
                activityQueue = listOf(
                    IslandActivity.Timer(
                        label = "Timer",
                        remainingMs = (23 * 3600L + 59 * 60L + 59L) * 1000L,
                        totalMs = (24 * 3600L) * 1000L,
                        isRunning = true
                    )
                )
            ),
            "Alert40" to IslandState(
                mode = IslandMode.COMPACT,
                activityQueue = listOf(
                    IslandActivity.Alert(
                        id = "alert_40",
                        kind = AlertKind.LOW_BATTERY,
                        title = longAlertTitle,
                        subtitle = "10% remaining"
                    )
                )
            )
        )

        val holeWidths = listOf(14f, 26f, 80f)
        val densityVal = 2.75f
        val screenWidthDp = 393f

        val cases = holeWidths.flatMap { holeW ->
            val zone = DeadZoneGeometry.fromSyntheticHole(
                holeWidthDp = holeW,
                holeHeightDp = 33f,
                screenWidthDp = screenWidthDp,
                density = densityVal
            )
            scenarios.map { (scenarioName, islandState) ->
                val settings = IslandSettings(
                    cutoutWidthDp = holeW,
                    cutoutHeightDp = 33f,
                    companionPosition = if (scenarioName.contains("Left")) CompanionPosition.LEFT else CompanionPosition.RIGHT
                )
                Triple("CompactPill[$scenarioName, holeW=$holeW]", islandState, Pair(settings, zone))
            }
        }

        val activeCase = androidx.compose.runtime.mutableStateOf(cases.first())
        composeRule.setContent {
            val (_, islandState, pair) = activeCase.value
            val (settings, zone) = pair
            CompositionLocalProvider(LocalDensity provides Density(densityVal, fontScale = 1f)) {
                Box(
                    modifier = Modifier.width(screenWidthDp.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    CompactPillView(
                        state = islandState,
                        settings = settings,
                        exclusionZone = zone,
                        accentColor = Color(0xFFFF9F0A),
                        isPlaying = true
                    )
                }
            }
        }

        cases.forEach { case ->
            composeRule.runOnUiThread { activeCase.value = case }
            composeRule.waitForIdle()
            assertNoTextIntersectsExclusionZone(
                root = composeRule.onRoot(useUnmergedTree = true).fetchSemanticsNode(),
                zone = case.third.second,
                contextLabel = case.first
            )
        }
    }

    @Test
    fun `medium pill and expanded card headers clear camera exclusion rect at 14, 26, and 80 dp`() {
        val app = composeRule.activity.application as DynamicIslandApp
        val densityVal = 2.75f
        val screenWidthDp = 393f

        val holeWidths = listOf(14f, 26f, 80f)
        val holeStates = holeWidths.map { holeW ->
            val zone = DeadZoneGeometry.fromSyntheticHole(
                holeWidthDp = holeW,
                holeHeightDp = 33f,
                screenWidthDp = screenWidthDp,
                density = densityVal
            )
            val settings = IslandSettings(cutoutWidthDp = holeW, cutoutHeightDp = 33f)
            val state = IslandState(
                mode = IslandMode.MEDIUM,
                activeTab = IslandTab.TOOLS,
                activityQueue = listOf(
                    IslandActivity.Music(
                        packageName = "com.unshoo.pixelmusic",
                        title = "Starlight Extended Symphony Edition",
                        artist = "Muse",
                        isPlaying = true,
                        positionMs = 10_000L,
                        durationMs = 180_000L
                    )
                )
            )
            Triple(holeW, zone, Pair(settings, state))
        }

        // Pair(isExpanded, Triple(holeW, zone, Pair(settings, state)))
        val renderCases = holeStates.flatMap { triple ->
            listOf(false to triple, true to triple)
        }
        val activeRender = androidx.compose.runtime.mutableStateOf(renderCases.first())

        composeRule.setContent {
            val (isExpanded, triple) = activeRender.value
            val (_, zone, pair) = triple
            val (settings, state) = pair
            CompositionLocalProvider(LocalDensity provides Density(densityVal, fontScale = 1f)) {
                Box(
                    modifier = Modifier.fillMaxWidth().width(screenWidthDp.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (!isExpanded) {
                        MediumPillView(
                            app = app,
                            state = state,
                            settings = settings,
                            exclusionZone = zone,
                            accentColor = Color(0xFFFF9F0A),
                            mediaState = MediaUiState(
                                title = "Starlight Extended Symphony Edition",
                                artist = "Muse",
                                isPlaying = true,
                                appLabel = "VeryLongPlayerApplicationName"
                            ),
                            timerState = TimerUiState()
                        )
                    } else {
                        ExpandedIslandCard(
                            app = app,
                            state = state.copy(mode = IslandMode.EXPANDED),
                            settings = settings,
                            exclusionZone = zone,
                            accentColor = Color(0xFFFF9F0A)
                        )
                    }
                }
            }
        }

        renderCases.forEach { renderCase ->
            val (isExpanded, triple) = renderCase
            val (holeW, zone, _) = triple
            composeRule.runOnUiThread { activeRender.value = renderCase }
            composeRule.waitForIdle()
            assertNoTextIntersectsExclusionZone(
                root = composeRule.onRoot(useUnmergedTree = true).fetchSemanticsNode(),
                zone = zone,
                contextLabel = if (isExpanded) "ExpandedCard[holeW=$holeW]" else "MediumPill[holeW=$holeW]"
            )
        }
    }

    @Test
    fun `round 2 roomy expansion cards fit within 380x240 Medium and 380x340 Full with 12dp margins at 1080x2400`() {
        val app = composeRule.activity.application as DynamicIslandApp
        val densityVal = 2.75f
        val screenWidthDp = 1080f / densityVal // 392.73dp on Redmi Note 10 Pro (1080x2400)
        val zone = DeadZoneGeometry.fromSyntheticHole(
            holeWidthDp = 27f,
            holeHeightDp = 34f,
            screenWidthDp = screenWidthDp,
            density = densityVal
        )
        val settings = IslandSettings(cutoutWidthDp = 27f, cutoutHeightDp = 34f)

        // Pair(isExpanded, Pair(tab, showMoreRow))
        val cases = mutableListOf<Pair<Boolean, Pair<IslandTab, Boolean>>>()
        cases.add(false to (IslandTab.MUSIC to false))
        IslandTab.entries.forEach { tab ->
            cases.add(true to (tab to false))
            cases.add(true to (tab to true))
        }
        val activeCase = androidx.compose.runtime.mutableStateOf(cases.first())

        composeRule.setContent {
            val (isExpanded, pair) = activeCase.value
            val (tab, showMore) = pair
            val state = IslandState(
                mode = if (isExpanded) IslandMode.EXPANDED else IslandMode.MEDIUM,
                activeTab = tab,
                isMoreRowOpen = showMore,
                activityQueue = listOf(
                    IslandActivity.Music(
                        packageName = "com.unshoo.pixelmusic",
                        title = "Track",
                        artist = "Artist",
                        isPlaying = true,
                        positionMs = 10_000L,
                        durationMs = 180_000L
                    )
                )
            )
            CompositionLocalProvider(LocalDensity provides Density(densityVal, fontScale = 1f)) {
                Box(
                    modifier = Modifier.width(screenWidthDp.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (!isExpanded) {
                        MediumPillView(
                            app = app,
                            state = state,
                            settings = settings,
                            exclusionZone = zone,
                            accentColor = Color(0xFFFF9F0A),
                            mediaState = MediaUiState(
                                title = "Track",
                                artist = "Artist",
                                isPlaying = true,
                                appLabel = "Music"
                            ),
                            timerState = TimerUiState()
                        )
                    } else {
                        ExpandedIslandCard(
                            app = app,
                            state = state,
                            settings = settings,
                            exclusionZone = zone,
                            accentColor = Color(0xFFFF9F0A)
                        )
                    }
                }
            }
        }

        cases.forEach { case ->
            val (isExpanded, pair) = case
            val (tab, showMore) = pair
            composeRule.runOnUiThread { activeCase.value = case }
            composeRule.waitForIdle()
            val tag = if (isExpanded) "expanded_island_card" else "medium_stage1_pill"
            val bounds = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            val wDp = bounds.width / densityVal
            val hDp = bounds.height / densityVal
            assertTrue(
                "Roomy expansion cap violated for isExpanded=$isExpanded tab=$tab showMore=$showMore: ${wDp}dp x ${hDp}dp",
                DeadZoneGeometry.isCompactExpansionWithinCap(wDp, hDp, isExpanded = isExpanded)
            )
            assertTrue(
                "Expected >=12dp horizontal margins on 1080x2400 (max ${(screenWidthDp - 24f)}dp) but got ${wDp}dp",
                wDp <= (screenWidthDp - 23.5f)
            )
            if (!isExpanded) {
                assertTrue("Medium card height ${hDp}dp should be >= 170dp", hDp >= 169.5f)
                assertTrue("Medium card width ${wDp}dp should be >= 340dp", wDp >= 339.5f)
            }
        }
    }

    @Test
    fun `idle camera-only pill tightly hugs camera cutout without wide bar and card anchor clears cutout`() {
        val zone = DeadZoneGeometry.fromSyntheticHole(
            holeWidthDp = 27f,
            holeHeightDp = 34f,
            screenWidthDp = 1080f / 2.75f,
            density = 2.75f
        )
        val idleMetrics = DeadZoneGeometry.idleCameraPillMetrics(zone.holeWidthDp, zone.holeHeightDp)
        assertTrue("Idle camera pill must be narrow (<60dp), got ${idleMetrics.widthDp}", idleMetrics.widthDp < 60f)
        assertTrue("Card top anchor must clear camera bottom (${zone.cutoutRectDp.bottom}dp)", DeadZoneGeometry.cardTopAnchorDp(zone) > zone.cutoutRectDp.bottom)

        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2.75f, fontScale = 1f)) {
                Box(modifier = Modifier.width(393.dp), contentAlignment = Alignment.TopCenter) {
                    com.example.island.IdlePillView(
                        settings = IslandSettings(whenIdleMode = com.example.system.WhenIdleMode.CAMERA_ONLY),
                        exclusionZone = zone,
                        accentColor = Color(0xFF0A84FF),
                        isCalibrationPreview = false,
                        isDragOverIsland = false,
                        isLowBatteryWarning = false
                    )
                }
            }
        }
        composeRule.waitForIdle()
        val idleBounds = composeRule.onNodeWithTag("idle_camera_ring").fetchSemanticsNode().boundsInRoot
        val idleWidthDp = idleBounds.width / 2.75f
        assertTrue("Rendered idle_camera_ring must be tight around camera (<65dp), got $idleWidthDp", idleWidthDp < 65f)

        val appleIdle = DeadZoneGeometry.idleCameraPillMetrics(zone.holeWidthDp, zone.holeHeightDp, appleWidth = true)
        assertTrue("Apple-width idle pill must be 125dp, got ${appleIdle.widthDp}", kotlin.math.abs(appleIdle.widthDp - 125f) <= 0.5f)
    }

    @Test
    fun `active music compact pill stays under 210dp and never renders song title text`() {
        val longTitle = "VeryLongTrackTitleShouldNeverStretchPill"
        val zone = DeadZoneGeometry.fromSyntheticHole(
            holeWidthDp = 27f,
            holeHeightDp = 34f,
            screenWidthDp = 393f,
            density = 2.75f
        )
        val musicMetrics = DeadZoneGeometry.musicCompactPillMetrics(zone.holeWidthDp, zone.holeHeightDp)
        assertTrue(
            "Music compact pill totalWidthDp must be <= 210dp, got ${musicMetrics.totalWidthDp}",
            musicMetrics.totalWidthDp <= 210f
        )

        val state = IslandState(
            mode = IslandMode.COMPACT,
            activityQueue = listOf(
                IslandActivity.Music(
                    packageName = "com.unshoo.pixelmusic",
                    title = longTitle,
                    artist = "Muse",
                    isPlaying = true,
                    positionMs = 15_000L,
                    durationMs = 240_000L
                )
            )
        )
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2.75f, fontScale = 1f)) {
                Box(modifier = Modifier.width(393.dp), contentAlignment = Alignment.TopCenter) {
                    CompactPillView(
                        state = state,
                        settings = IslandSettings(cutoutWidthDp = 27f, cutoutHeightDp = 34f),
                        exclusionZone = zone,
                        accentColor = Color(0xFF4A9EFF),
                        isPlaying = true
                    )
                }
            }
        }
        composeRule.waitForIdle()

        val pillBounds = composeRule.onNodeWithTag("compact_pill_portrait").fetchSemanticsNode().boundsInRoot
        val pillWidthDp = pillBounds.width / 2.75f
        assertTrue("Rendered music compact_pill_portrait must stay <= 210dp, got $pillWidthDp", pillWidthDp <= 210f)

        val textNodes = mutableListOf<String>()
        fun collectTexts(node: SemanticsNode) {
            if (node.config.contains(SemanticsProperties.Text)) {
                textNodes += node.config[SemanticsProperties.Text].joinToString { it.text }
            }
            node.children.forEach { collectTexts(it) }
        }
        collectTexts(composeRule.onRoot(useUnmergedTree = true).fetchSemanticsNode())
        assertFalse(
            "Music compact pill must NEVER render track title text, found: $textNodes",
            textNodes.any { it.contains("VeryLongTrack") || it == longTitle }
        )
    }

    private fun assertNoTextIntersectsExclusionZone(
        root: SemanticsNode,
        zone: ExclusionZone,
        contextLabel: String
    ) {
        val textNodes = mutableListOf<SemanticsNode>()
        fun collect(node: SemanticsNode) {
            if (node.config.contains(SemanticsProperties.Text)) {
                textNodes.add(node)
            }
            node.children.forEach { collect(it) }
        }
        collect(root)

        val ex = zone.exclusionRectPx
        textNodes.forEach { node ->
            val bounds = node.boundsInRoot
            if (bounds.width <= 0f || bounds.height <= 0f) return@forEach
            val label = node.config[SemanticsProperties.Text].joinToString { it.text }
            val intersects = DeadZoneGeometry.rectsIntersect(
                aLeft = bounds.left,
                aTop = bounds.top,
                aRight = bounds.right,
                aBottom = bounds.bottom,
                bLeft = ex.left.toFloat(),
                bTop = ex.top.toFloat(),
                bRight = ex.right.toFloat(),
                bBottom = ex.bottom.toFloat()
            )
            assertFalse(
                "$contextLabel text '$label' bounds=$bounds intersected camera exclusionRectPx=$ex",
                intersects
            )
        }
    }
}
