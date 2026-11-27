package com.example.ocr

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.example.island.OcrSelectionState
import com.example.island.OcrTextToken
import java.text.BreakIterator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.hypot

sealed interface TierAResult {
    data class DelegatedToHost(
        val bounds: Rect,
        val copiedText: String
    ) : TierAResult

    data class TextFound(
        val text: String,
        val bounds: Rect,
        val selectionState: OcrSelectionState
    ) : TierAResult

    data class NoText(val bounds: Rect?) : TierAResult
    data class Refused(val reason: String) : TierAResult
}

/**
 * Tier A Text Selection (§5):
 * - Re-reads fresh windows / rootInActiveWindow and skips huge containers (>= 75% screen area).
 * - Picks the smallest leaf element containing the pressed point (x, y).
 * - Path A (native selectable/editable text): delegates selection via ACTION_SET_SELECTION /
 *   ACTION_LONG_CLICK using EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY, strictly checks the
 *   performAction() boolean, refreshes, copies ONLY text[start, end), and draws NOTHING on top.
 * - Non-selectable leaf nodes return TextFound (word-token overlay) or NoText -> Path B (frozen ML Kit).
 */
object TierAAccessibilityReader {

    @Volatile
    private var selectionChangedSignal: CompletableDeferred<Unit>? = null

    fun onTextSelectionChanged() {
        selectionChangedSignal?.complete(Unit)
    }

    private data class CandidateNode(
        val node: AccessibilityNodeInfo,
        val text: String?,
        val contentDesc: String?,
        val bounds: Rect,
        val area: Long,
        val depth: Int
    )

    suspend fun readNodeAtPointAsync(
        service: AccessibilityService?,
        screenReadingAllowed: Boolean,
        x: Int,
        y: Int,
        ownPackageName: String,
        context: Context? = service
    ): TierAResult {
        if (!screenReadingAllowed) {
            return TierAResult.Refused("Screen reading is OFF in Privacy Settings")
        }
        if (service == null) {
            return TierAResult.NoText(null)
        }
        val collected = collectCandidateState(service, x, y, ownPackageName, context)
            ?: return TierAResult.NoText(null)
        if (collected.hitSensitive) return TierAResult.Refused("Refused: protected password or sensitive screen")
        if (collected.hitWebView != null) return TierAResult.NoText(collected.hitWebView)
        val chosen = collected.chosen ?: return TierAResult.NoText(collected.smallestBounds)
        val extracted = chosen.text ?: chosen.contentDesc ?: return TierAResult.NoText(chosen.bounds)
        val charRects = if (chosen.text != null) queryCharacterLocations(chosen.node, extracted) else null

        if (chosen.text != null && (chosen.node.isTextSelectable || chosen.node.isEditable)) {
            val delegated = delegateSelectionSuspend(
                node = chosen.node,
                text = extracted,
                bounds = chosen.bounds,
                charLocations = charRects,
                tapX = x,
                tapY = y,
                context = context
            )
            if (delegated != null) return delegated
        }

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

    private data class CollectionResult(
        val hitSensitive: Boolean = false,
        val hitWebView: Rect? = null,
        val smallestBounds: Rect? = null,
        val chosen: CandidateNode? = null
    )

    private fun collectCandidateState(
        service: AccessibilityService,
        x: Int,
        y: Int,
        ownPackageName: String,
        context: Context?
    ): CollectionResult? {
        val dm = (context ?: service).resources.displayMetrics
        val screenW = dm.widthPixels.coerceAtLeast(360)
        val screenH = dm.heightPixels.coerceAtLeast(640)

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
            val activeRoot = runCatching { service.rootInActiveWindow }.getOrNull() ?: return null
            targetRoots.add(activeRoot)
        }

        val candidates = mutableListOf<CandidateNode>()
        var hitSensitive = false
        var hitWebViewRect: Rect? = null
        var smallestBounds: Rect? = null
        var smallestArea = Long.MAX_VALUE

        fun visit(node: AccessibilityNodeInfo, depth: Int) {
            runCatching { node.refresh() }
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (!rect.contains(x, y)) return

            if (isSensitiveOrPassword(node)) {
                hitSensitive = true
                return
            }

            val className = node.className?.toString().orEmpty()
            if (className == "android.webkit.WebView" || className.endsWith(".WebView")) {
                hitWebViewRect = Rect(rect)
                return
            }

            val area = rect.width().toLong().coerceAtLeast(1L) * rect.height().toLong().coerceAtLeast(1L)
            if (area < smallestArea) {
                smallestArea = area
                smallestBounds = Rect(rect)
            }

            val text = node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            val isTextWidget = className.endsWith("TextView") ||
                className.endsWith("EditText") ||
                className.endsWith("Button")
            val isImageOrVideoWidget = className.endsWith("ImageView") ||
                className.endsWith("ImageButton") ||
                className == "android.widget.Image" ||
                className.contains("PhotoView", ignoreCase = true) ||
                className.contains("SurfaceView", ignoreCase = true) ||
                className.contains("TextureView", ignoreCase = true) ||
                className.contains("PlayerView", ignoreCase = true) ||
                className.contains("Video", ignoreCase = true)

            val isHugeContainer = SelectionCoordinateMapper.isOversizeSelection(rect, screenW, screenH, 0.75f) ||
                (!isTextWidget && (rect.width() > (screenW * 0.78f) || rect.height() > (screenH * 0.65f)))
            val isLargeNonTextDesc = text == null && !isTextWidget && (rect.height() > 280 || rect.width() > 700)

            val desc = if ((isImageOrVideoWidget && text == null) || isHugeContainer || isLargeNonTextDesc) {
                null
            } else {
                node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            }

            if (!isHugeContainer && (text != null || desc != null)) {
                candidates.add(CandidateNode(node, text, desc, Rect(rect), area, depth))
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                visit(child, depth + 1)
                if (hitSensitive || hitWebViewRect != null) return
            }
        }

        for (root in targetRoots) {
            if (root.packageName?.toString() == ownPackageName && targetRoots.size > 1) continue
            visit(root, 0)
            if (hitSensitive) return CollectionResult(hitSensitive = true)
            if (hitWebViewRect != null) return CollectionResult(hitWebView = hitWebViewRect)
            if (candidates.isNotEmpty()) break
        }

        val withText = candidates.filter { it.text != null }
        val chosen = (if (withText.isNotEmpty()) withText else candidates)
            .minWithOrNull(compareBy<CandidateNode> { it.area }.thenByDescending { it.depth })
        return CollectionResult(smallestBounds = smallestBounds, chosen = chosen)
    }

    fun readNodeAtPoint(
        service: AccessibilityService?,
        screenReadingAllowed: Boolean,
        x: Int,
        y: Int,
        ownPackageName: String,
        context: Context? = service
    ): TierAResult {
        if (!screenReadingAllowed) {
            return TierAResult.Refused("Screen reading is OFF in Privacy Settings")
        }
        if (service == null) {
            return TierAResult.NoText(null)
        }
        val collected = collectCandidateState(service, x, y, ownPackageName, context)
            ?: return TierAResult.NoText(null)
        if (collected.hitSensitive) return TierAResult.Refused("Refused: protected password or sensitive screen")
        if (collected.hitWebView != null) return TierAResult.NoText(collected.hitWebView)
        val chosen = collected.chosen ?: return TierAResult.NoText(collected.smallestBounds)
        val extracted = chosen.text ?: chosen.contentDesc ?: return TierAResult.NoText(chosen.bounds)
        val charRects = if (chosen.text != null) queryCharacterLocations(chosen.node, extracted) else null

        if (chosen.text != null && (chosen.node.isTextSelectable || chosen.node.isEditable)) {
            val delegated = tryNativeDelegateSelection(
                node = chosen.node,
                text = extracted,
                bounds = chosen.bounds,
                charLocations = charRects,
                tapX = x,
                tapY = y,
                context = context
            )
            if (delegated != null) return delegated
        }

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

    /**
     * Attempts native host text selection delegation.
     * Returns DelegatedToHost ONLY if performAction() returned true and a valid non-empty
     * substring slice was obtained; otherwise returns null so caller falls back cleanly.
     */
    suspend fun delegateSelectionSuspend(
        node: AccessibilityNodeInfo,
        text: String,
        bounds: Rect,
        charLocations: Array<RectF>?,
        tapX: Int,
        tapY: Int,
        context: Context?
    ): TierAResult.DelegatedToHost? {
        if (text.isEmpty()) return null
        val (wordStart, wordEnd) = SelectionCoordinateMapper.computeCharIndexAtPoint(
            text = text,
            nodeBounds = bounds,
            charLocations = charLocations,
            tapX = tapX,
            tapY = tapY
        )

        val setSelectionArgs = Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, wordStart)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, wordEnd)
        }

        val signal = CompletableDeferred<Unit>()
        selectionChangedSignal = signal
        val actionAccepted = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, Bundle.EMPTY)
            }
            val setOk = node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, setSelectionArgs)
            if (setOk) true else node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
        }.getOrDefault(false)

        if (!actionAccepted) {
            selectionChangedSignal = null
            return null
        }

        withTimeoutOrNull(300L) { signal.await() }
        selectionChangedSignal = null
        runCatching { node.refresh() }
        val selStart = node.textSelectionStart
        val selEnd = node.textSelectionEnd
        if (selStart < 0 || selEnd <= selStart || selEnd > text.length) {
            return null
        }

        val slice = text.substring(selStart, selEnd).trim()
        if (slice.isEmpty()) return null

        if (context != null) {
            runCatching {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                cm?.setPrimaryClip(ClipData.newPlainText("Selected text", slice))
            }
        }
        return TierAResult.DelegatedToHost(bounds = bounds, copiedText = slice)
    }

    fun tryNativeDelegateSelection(
        node: AccessibilityNodeInfo,
        text: String,
        bounds: Rect,
        charLocations: Array<RectF>?,
        tapX: Int,
        tapY: Int,
        context: Context?
    ): TierAResult.DelegatedToHost? {
        if (text.isEmpty()) return null
        val (wordStart, wordEnd) = SelectionCoordinateMapper.computeCharIndexAtPoint(
            text = text,
            nodeBounds = bounds,
            charLocations = charLocations,
            tapX = tapX,
            tapY = tapY
        )

        val setSelectionArgs = Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, wordStart)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, wordEnd)
        }

        val actionAccepted = runCatching {
            val setOk = node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, setSelectionArgs)
            if (setOk) {
                true
            } else {
                node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
            }
        }.getOrDefault(false)

        if (!actionAccepted) return null

        runCatching { node.refresh() }
        val selStart = node.textSelectionStart
        val selEnd = node.textSelectionEnd
        val (copyStart, copyEnd) = if (selStart >= 0 && selEnd > selStart && selEnd <= text.length) {
            selStart to selEnd
        } else {
            wordStart to wordEnd
        }

        val slice = text.substring(copyStart.coerceIn(0, text.length), copyEnd.coerceIn(0, text.length)).trim()
        if (slice.isEmpty()) return null

        if (context != null) {
            runCatching {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                cm?.setPrimaryClip(ClipData.newPlainText("Selected text", slice))
            }
        }
        return TierAResult.DelegatedToHost(bounds = bounds, copiedText = slice)
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
        val iterator = BreakIterator.getWordInstance().apply { setText(text) }
        val wordSpans = mutableListOf<IntRange>()
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            val slice = text.substring(start, end)
            if (slice.any { !it.isWhitespace() }) {
                wordSpans.add(start until end)
            }
            start = end
            end = iterator.next()
        }

        val tokens = if (wordSpans.isEmpty()) {
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
            val rawLines = text.split('\n')
            val lineCount = rawLines.size.coerceAtLeast(1)
            val lineHeight = (safeBounds.height().toFloat() / lineCount).coerceIn(24f, 84f)
            val width = safeBounds.width().coerceAtLeast(40)

            val lineStarts = IntArray(lineCount)
            var running = 0
            for (li in 0 until lineCount) {
                lineStarts[li] = running
                running += rawLines[li].length + 1
            }

            wordSpans.map { span ->
                val sIdx = span.first
                val eIdx = span.last + 1
                val word = text.substring(sIdx, eIdx)

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
                        text = word,
                        startChar = sIdx,
                        endChar = eIdx,
                        leftPx = exactRect.left,
                        topPx = exactRect.top,
                        rightPx = exactRect.right,
                        bottomPx = exactRect.bottom
                    )
                } else {
                    var li = 0
                    while (li + 1 < lineCount && lineStarts[li + 1] <= sIdx) {
                        li++
                    }
                    val lineStrLen = rawLines[li].length.coerceAtLeast(1)
                    val relStart = (sIdx - lineStarts[li]).coerceIn(0, lineStrLen)
                    val relEnd = (eIdx - lineStarts[li]).coerceIn(relStart + 1, lineStrLen)
                    val lFrac = relStart.toFloat() / lineStrLen.toFloat()
                    val rFrac = relEnd.toFloat() / lineStrLen.toFloat()
                    val tokTop = (safeBounds.top + (li * lineHeight)).toInt()
                    val tokBot = (tokTop + lineHeight).toInt().coerceAtMost(safeBounds.bottom.coerceAtLeast(tokTop + 24))
                    val tokLeft = safeBounds.left + (width * lFrac).toInt()
                    val tokRight = (safeBounds.left + (width * rFrac).toInt()).coerceAtLeast(tokLeft + 18)
                    OcrTextToken(
                        text = word,
                        startChar = sIdx,
                        endChar = eIdx,
                        leftPx = tokLeft,
                        topPx = tokTop,
                        rightPx = tokRight,
                        bottomPx = tokBot
                    )
                }
            }
        }

        val avgLineH = tokens.map { (it.bottomPx - it.topPx).coerceAtLeast(18) }.average().toInt().coerceAtLeast(24)
        val lineTol = (avgLineH * 0.5f).toInt().coerceAtLeast(12)
        val hitIdx = tokens.indexOfFirst { tok ->
            tapX in (tok.leftPx - 12)..(tok.rightPx + 12) && tapY in (tok.topPx - lineTol)..(tok.bottomPx + lineTol)
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
