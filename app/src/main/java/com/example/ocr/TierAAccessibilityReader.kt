package com.example.ocr

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.example.island.OcrSelectionState
import com.example.island.OcrTextToken
import kotlin.math.hypot

sealed interface TierAResult {
    data class TextFound(
        val text: String,
        val bounds: Rect,
        val selectionState: OcrSelectionState
    ) : TierAResult

    data class NoText(val bounds: Rect?) : TierAResult
    data class Refused(val reason: String) : TierAResult
}

/**
 * Tier A OCR: Privacy-first Accessibility single-node reader.
 * Strictly reads ONLY the exact leaf element containing the user's pressed point (x, y).
 * Never invoked unless OcrPrivacyGate.isScreenReadingAllowed() is true.
 * Produces tokenized word boxes for interactive handle selection + magnifier loupe.
 */
object TierAAccessibilityReader {

    private data class CandidateNode(
        val node: AccessibilityNodeInfo,
        val text: String?,
        val contentDesc: String?,
        val bounds: Rect,
        val area: Long,
        val depth: Int
    )

    fun readNodeAtPoint(
        service: AccessibilityService?,
        screenReadingAllowed: Boolean,
        x: Int,
        y: Int,
        ownPackageName: String
    ): TierAResult {
        // HARD PRIVACY CONSTRAINT: Never inspect nodes when OFF
        if (!screenReadingAllowed) {
            return TierAResult.Refused("Screen reading is OFF in Privacy Settings")
        }
        if (service == null) {
            return TierAResult.NoText(null)
        }

        val orderedWindows = runCatching { service.windows }
            .getOrNull()
            .orEmpty()
            .filter { it.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY }
            .sortedByDescending { it.layer }

        val targetRoots = mutableListOf<AccessibilityNodeInfo>()
        for (win in orderedWindows) {
            val winBounds = Rect()
            win.getBoundsInScreen(winBounds)
            if (!winBounds.contains(x, y)) continue
            val root = runCatching { win.root }.getOrNull() ?: continue
            targetRoots.add(root)
        }

        if (targetRoots.isEmpty()) {
            val activeRoot = runCatching { service.rootInActiveWindow }.getOrNull()
                ?: return TierAResult.NoText(null)
            targetRoots.add(activeRoot)
        }

        val candidates = mutableListOf<CandidateNode>()
        var hitSensitive = false
        var smallestBounds: Rect? = null
        var smallestArea = Long.MAX_VALUE

        fun visit(node: AccessibilityNodeInfo, depth: Int) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (!rect.contains(x, y)) return

            if (isSensitiveOrPassword(node)) {
                hitSensitive = true
                return
            }

            val area = rect.width().toLong().coerceAtLeast(1L) * rect.height().toLong().coerceAtLeast(1L)
            if (area < smallestArea) {
                smallestArea = area
                smallestBounds = Rect(rect)
            }

            val text = node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            val desc = node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            if (text != null || desc != null) {
                candidates.add(CandidateNode(node, text, desc, Rect(rect), area, depth))
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                visit(child, depth + 1)
                if (hitSensitive) return
            }
        }

        for (root in targetRoots) {
            if (root.packageName?.toString() == ownPackageName && targetRoots.size > 1) continue
            visit(root, 0)
            if (hitSensitive) {
                return TierAResult.Refused("Refused: protected password or sensitive screen")
            }
            if (candidates.isNotEmpty()) break
        }

        if (candidates.isEmpty()) {
            return TierAResult.NoText(smallestBounds)
        }

        // Prefer nodes with explicit text over bare contentDescription, smallest bounding box (deepest leaf)
        val withText = candidates.filter { it.text != null }
        val chosen = (if (withText.isNotEmpty()) withText else candidates)
            .minWithOrNull(compareBy<CandidateNode> { it.area }.thenByDescending { it.depth })!!

        val extracted = chosen.text ?: chosen.contentDesc ?: return TierAResult.NoText(chosen.bounds)
        val charRects = if (chosen.text != null) {
            queryCharacterLocations(chosen.node, extracted)
        } else null

        val selectionState = buildSelectionState(
            text = extracted,
            bounds = chosen.bounds,
            tapX = x,
            tapY = y,
            charLocations = charRects,
            isFromTierB = false
        )
        return TierAResult.TextFound(extracted, chosen.bounds, selectionState)
    }

    private fun queryCharacterLocations(node: AccessibilityNodeInfo, text: String): Array<RectF>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || text.isEmpty()) return null
        return runCatching {
            val args = Bundle().apply {
                putInt(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX, 0)
                putInt(
                    AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH,
                    text.length.coerceAtMost(256)
                )
            }
            val ok = node.refreshWithExtraData(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY, args)
            if (!ok) return null
            val raw = node.extras?.getParcelableArray(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY)
                ?: return null
            raw.mapNotNull { it as? RectF }.takeIf { it.isNotEmpty() }?.toTypedArray()
        }.getOrNull()
    }

    fun buildSelectionState(
        text: String,
        bounds: Rect,
        tapX: Int,
        tapY: Int,
        charLocations: Array<RectF>? = null,
        isFromTierB: Boolean = false
    ): OcrSelectionState {
        val safeBounds = if (bounds.isEmpty) Rect(tapX - 120, tapY - 30, tapX + 120, tapY + 30) else bounds
        val tokenRegex = Regex("""\S+""")
        val matches = tokenRegex.findAll(text).toList()

        val tokens = if (matches.isEmpty()) {
            listOf(
                OcrTextToken(
                    text = text,
                    startChar = 0,
                    endChar = text.length,
                    leftPx = safeBounds.left,
                    topPx = safeBounds.top,
                    rightPx = safeBounds.right,
                    bottomPx = safeBounds.bottom
                )
            )
        } else {
            val totalLen = text.length.coerceAtLeast(1)
            val width = safeBounds.width().coerceAtLeast(40)
            matches.map { match ->
                val sIdx = match.range.first
                val eIdx = match.range.last + 1

                val exactRect: Rect? = if (charLocations != null && sIdx < charLocations.size) {
                    val lastLocIdx = (eIdx - 1).coerceAtMost(charLocations.lastIndex)
                    var l = Float.MAX_VALUE
                    var t = Float.MAX_VALUE
                    var r = -Float.MAX_VALUE
                    var b = -Float.MAX_VALUE
                    for (ci in sIdx..lastLocIdx) {
                        val cr = charLocations[ci]
                        if (!cr.isEmpty) {
                            l = minOf(l, cr.left)
                            t = minOf(t, cr.top)
                            r = maxOf(r, cr.right)
                            b = maxOf(b, cr.bottom)
                        }
                    }
                    if (l < r && t < b) Rect(l.toInt(), t.toInt(), r.toInt(), b.toInt()) else null
                } else null

                if (exactRect != null) {
                    OcrTextToken(
                        text = match.value,
                        startChar = sIdx,
                        endChar = eIdx,
                        leftPx = exactRect.left,
                        topPx = exactRect.top,
                        rightPx = exactRect.right,
                        bottomPx = exactRect.bottom
                    )
                } else {
                    val lFrac = sIdx.toFloat() / totalLen.toFloat()
                    val rFrac = eIdx.toFloat() / totalLen.toFloat()
                    OcrTextToken(
                        text = match.value,
                        startChar = sIdx,
                        endChar = eIdx,
                        leftPx = safeBounds.left + (width * lFrac).toInt(),
                        topPx = safeBounds.top,
                        rightPx = safeBounds.left + (width * rFrac).toInt().coerceAtLeast(safeBounds.left + (width * lFrac).toInt() + 18),
                        bottomPx = safeBounds.bottom
                    )
                }
            }
        }

        // Find token nearest to tap point
        val hitIdx = tokens.indexOfFirst { tok ->
            tapX in (tok.leftPx - 12)..(tok.rightPx + 12) && tapY in (tok.topPx - 18)..(tok.bottomPx + 18)
        }.takeIf { it >= 0 } ?: tokens.indices.minByOrNull { idx ->
            val tok = tokens[idx]
            val cx = (tok.leftPx + tok.rightPx) / 2.0
            val cy = (tok.topPx + tok.bottomPx) / 2.0
            hypot(cx - tapX, cy - tapY)
        } ?: 0

        return OcrSelectionState(
            fullText = text,
            elementBounds = safeBounds,
            tokens = tokens,
            startTokenIndex = hitIdx,
            endTokenIndex = hitIdx,
            anchorX = tapX.toFloat(),
            anchorY = tapY.toFloat(),
            isFromTierB = isFromTierB
        )
    }

    private fun isSensitiveOrPassword(node: AccessibilityNodeInfo): Boolean {
        if (node.isPassword) return true
        val inputVariation = node.inputType and InputType.TYPE_MASK_VARIATION
        if (inputVariation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            inputVariation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            inputVariation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            inputVariation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        ) {
            return true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (node.isAccessibilityDataSensitive) return true
        }
        return false
    }
}
