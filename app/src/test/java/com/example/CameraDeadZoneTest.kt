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
@Config(sdk = [36])
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
                root = composeRule.onRoot().fetchSemanticsNode(),
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
                root = composeRule.onRoot().fetchSemanticsNode(),
                zone = zone,
                contextLabel = if (isExpanded) "ExpandedCard[holeW=$holeW]" else "MediumPill[holeW=$holeW]"
            )
        }
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
