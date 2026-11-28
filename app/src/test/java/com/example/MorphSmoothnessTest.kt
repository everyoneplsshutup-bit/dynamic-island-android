package com.example

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.island.DeadZoneGeometry
import com.example.system.AnimationStyle
import com.example.system.PillStyle
import com.example.system.WhenIdleMode
import com.example.ui.theme.MotionTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MorphSmoothnessTest {

    @Test
    fun `round 5 handover threshold and spring physics match specification`() {
        assertEquals(0.85f, MotionTokens.ROUND5_HANDOVER_THRESHOLD, 0.0001f)

        val openSpec = MotionTokens.round5MorphSpec(expanding = true, reducedMotion = false, style = AnimationStyle.SPRING)
        assertTrue(openSpec is SpringSpec<Float>)
        val openSpring = openSpec as SpringSpec<Float>
        assertEquals(MotionTokens.ROUND5_OPEN_DAMPING, openSpring.dampingRatio, 0.001f)
        assertEquals(MotionTokens.ROUND5_OPEN_STIFFNESS, openSpring.stiffness, 0.01f)

        val closeSpec = MotionTokens.round5MorphSpec(expanding = false, reducedMotion = false, style = AnimationStyle.SPRING)
        assertTrue(closeSpec is SpringSpec<Float>)
        val closeSpring = closeSpec as SpringSpec<Float>
        assertEquals(MotionTokens.ROUND5_CLOSE_DAMPING, closeSpring.dampingRatio, 0.001f)
        assertEquals(MotionTokens.ROUND5_CLOSE_STIFFNESS, closeSpring.stiffness, 0.01f)

        val heightSpec = MotionTokens.round5HeightSpec(expanding = true, reducedMotion = false, style = AnimationStyle.SPRING)
        assertTrue(heightSpec is SpringSpec<Float>)
        val heightSpring = heightSpec as SpringSpec<Float>
        assertEquals(Spring.DampingRatioNoBouncy, heightSpring.dampingRatio, 0.001f)

        assertTrue(MotionTokens.DOCK_LEAD_STIFFNESS > MotionTokens.DOCK_TRAIL_STIFFNESS)
        assertEquals(1000f, MotionTokens.DOCK_LEAD_STIFFNESS, 0.01f)
        assertEquals(50f, MotionTokens.DOCK_TRAIL_STIFFNESS, 0.01f)
    }

    @Test
    fun `round 5 settings enums and camera collar circle invariants hold`() {
        assertEquals(listOf(PillStyle.BLACK, PillStyle.APPLE_LIQUID), PillStyle.entries.toList())
        assertEquals(listOf(WhenIdleMode.CAMERA_ONLY, WhenIdleMode.HIDDEN), WhenIdleMode.entries.toList())

        val idle = DeadZoneGeometry.idleCameraPillMetrics(holeWidthDp = 26f, holeHeightDp = 33f)
        assertTrue("Idle circle width must be 32..44 dp, got ${idle.widthDp}", idle.widthDp in 32f..44f)
        assertEquals(idle.widthDp, idle.heightDp, 0.01f)
        assertEquals(idle.widthDp / 2f, idle.cornerRadiusDp, 0.01f)

        val shape = DeadZoneGeometry.CameraCollarCardShape(holeWidthDp = 26f, holeHeightDp = 33f)
        val outline = shape.createOutline(
            size = Size(356f * 2.75f, 220f * 2.75f),
            layoutDirection = LayoutDirection.Ltr,
            density = Density(2.75f)
        )
        assertTrue(outline is Outline.Generic)
        assertFalse((outline as Outline.Generic).path.isEmpty)
    }

    @Test
    fun `round 5 codebase purge leaves zero forbidden APIs or removed features`() {
        val candidateRoots = listOf(
            File("src/main/java/com/example"),
            File("app/src/main/java/com/example")
        )
        val sourceRoot = candidateRoots.firstOrNull { it.exists() && it.isDirectory }
            ?: error("Could not locate main source directory")

        val ktFiles = sourceRoot.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue(ktFiles.isNotEmpty())

        val forbiddenPatterns = listOf(
            "QrCodeGenerator",
            "GlyfStrokeRecognizer",
            "WhenIdleMode.INFO_PILL",
            "animateDpAsState",
            "SizeTransform",
            ".blur("
        )

        ktFiles.forEach { file ->
            val content = file.readText()
            forbiddenPatterns.forEach { pattern ->
                assertFalse(
                    "Forbidden symbol '$pattern' found in ${file.path}",
                    content.contains(pattern)
                )
            }
            if (file.name != "MusicIslandView.kt") {
                assertFalse(
                    "Infinite animation found outside MusicIslandView.kt in ${file.path}",
                    content.contains("rememberInfiniteTransition") || content.contains("infiniteRepeatable")
                )
            }
        }

        val toolsFile = ktFiles.first { it.name == "IslandToolsAndModules.kt" }
        val toolsText = toolsFile.readText()
        val toolsPanelIndex = toolsText.indexOf("fun ToolsIslandPanel(")
        assertTrue(toolsPanelIndex >= 0)
        val panelSlice = toolsText.substring(toolsPanelIndex, (toolsPanelIndex + 4000).coerceAtMost(toolsText.length))
        assertFalse("ToolsIslandPanel must not scroll", panelSlice.contains("verticalScroll"))
    }
}
