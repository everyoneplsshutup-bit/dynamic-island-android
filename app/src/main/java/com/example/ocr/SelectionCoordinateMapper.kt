package com.example.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.example.island.OcrTextToken
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Holds the latest frozen screen bitmap for Tier B OCR selection overlay
 * so OcrSelectionState keeps value equality and avoids large Bitmap copies in StateFlow.
 */
object OcrBitmapHolder {
    @Volatile
    private var frozenFrame: Bitmap? = null

    fun setFrozenFrame(bitmap: Bitmap?) {
        val prev = frozenFrame
        frozenFrame = bitmap
        if (prev != null && prev !== bitmap && !prev.isRecycled) {
            runCatching { prev.recycle() }
        }
    }

    fun getFrozenFrame(): Bitmap? = frozenFrame?.takeIf { !it.isRecycled }

    fun clear() {
        val prev = frozenFrame
        frozenFrame = null
        if (prev != null && !prev.isRecycled) {
            runCatching { prev.recycle() }
        }
    }
}

/**
 * Pure-Kotlin coordinate normalization, word hit-testing, <80% container guard,
 * and slice-only text copy helpers for Two-Path Text Selection (§5).
 */
object SelectionCoordinateMapper {

    const val OVERSIZE_SCREEN_RATIO = 0.80f

    fun isOversizeSelection(
        bounds: Rect,
        screenWidthPx: Int,
        screenHeightPx: Int,
        maxAreaFraction: Float = OVERSIZE_SCREEN_RATIO
    ): Boolean {
        if (bounds.isEmpty || screenWidthPx <= 0 || screenHeightPx <= 0) return false
        val rectArea = bounds.width().toDouble().coerceAtLeast(0.0) * bounds.height().toDouble().coerceAtLeast(0.0)
        val screenArea = screenWidthPx.toDouble() * screenHeightPx.toDouble()
        return (rectArea / screenArea) >= maxAreaFraction.toDouble()
    }

    fun mapScreenPointToBitmap(
        screenX: Float,
        screenY: Float,
        screenWidthPx: Float,
        screenHeightPx: Float,
        bitmapWidthPx: Int,
        bitmapHeightPx: Int
    ): Pair<Float, Float> {
        val safeW = screenWidthPx.coerceAtLeast(1f)
        val safeH = screenHeightPx.coerceAtLeast(1f)
        val bx = ((screenX / safeW) * bitmapWidthPx).coerceIn(0f, bitmapWidthPx.toFloat())
        val by = ((screenY / safeH) * bitmapHeightPx).coerceIn(0f, bitmapHeightPx.toFloat())
        return bx to by
    }

    fun mapBitmapRectToOverlay(
        bitmapRect: Rect,
        bitmapWidthPx: Int,
        bitmapHeightPx: Int,
        overlayWidthPx: Float,
        overlayHeightPx: Float
    ): Rect {
        val bw = bitmapWidthPx.coerceAtLeast(1).toFloat()
        val bh = bitmapHeightPx.coerceAtLeast(1).toFloat()
        return Rect(
            ((bitmapRect.left / bw) * overlayWidthPx).toInt(),
            ((bitmapRect.top / bh) * overlayHeightPx).toInt(),
            ((bitmapRect.right / bw) * overlayWidthPx).toInt(),
            ((bitmapRect.bottom / bh) * overlayHeightPx).toInt()
        )
    }

    fun findNearestTokenIndex(
        tokens: List<OcrTextToken>,
        x: Float,
        y: Float,
        maxDistancePx: Float = Float.MAX_VALUE,
        currentIdx: Int = -1
    ): Int {
        if (tokens.isEmpty()) return -1
        var bestIdx = -1
        var bestScore = Double.MAX_VALUE
        tokens.forEachIndexed { idx, tok ->
            val dx = when {
                x < tok.leftPx -> (tok.leftPx - x).toDouble()
                x > tok.rightPx -> (x - tok.rightPx).toDouble()
                else -> 0.0
            }
            val dy = when {
                y < tok.topPx -> (tok.topPx - y).toDouble()
                y > tok.bottomPx -> (y - tok.bottomPx).toDouble()
                else -> 0.0
            }
            var dist = hypot(dx, dy * 1.8)
            if (idx == currentIdx) {
                dist = (dist - 10.0).coerceAtLeast(0.0)
            }
            if (dist < bestScore) {
                bestScore = dist
                bestIdx = idx
            }
        }
        return if (bestScore <= maxDistancePx.toDouble()) bestIdx else -1
    }

    fun extractSelectedText(
        fullText: String,
        tokens: List<OcrTextToken>,
        startIdx: Int,
        endIdx: Int
    ): String {
        if (tokens.isEmpty()) return fullText.trim()
        val s = minOf(startIdx, endIdx).coerceIn(0, tokens.lastIndex)
        val e = maxOf(startIdx, endIdx).coerceIn(s, tokens.lastIndex)
        val cStart = tokens[s].startChar.coerceIn(0, fullText.length)
        val cEnd = tokens[e].endChar.coerceIn(cStart, fullText.length)
        return fullText.substring(cStart, cEnd).trim()
    }

    fun computeCharIndexAtPoint(
        text: String,
        nodeBounds: Rect,
        charLocations: Array<android.graphics.RectF>?,
        tapX: Int,
        tapY: Int
    ): Pair<Int, Int> {
        if (text.isEmpty()) return 0 to 0
        if (charLocations != null && charLocations.isNotEmpty()) {
            val hitChar = charLocations.indices.minByOrNull { idx ->
                val r = charLocations[idx]
                if (r.isEmpty) Double.MAX_VALUE
                else hypot((r.centerX() - tapX).toDouble(), (r.centerY() - tapY).toDouble())
            } ?: 0
            return expandToWordRange(text, hitChar)
        }
        val lineCount = text.count { it == '\n' } + 1
        val relY = if (nodeBounds.height() > 0) {
            ((tapY - nodeBounds.top).toFloat() / nodeBounds.height().toFloat()).coerceIn(0f, 0.999f)
        } else 0f
        val relX = if (nodeBounds.width() > 0) {
            ((tapX - nodeBounds.left).toFloat() / nodeBounds.width().toFloat()).coerceIn(0f, 0.999f)
        } else 0f
        val lines = text.split('\n')
        val lineIdx = (relY * lineCount).toInt().coerceIn(0, lines.lastIndex)
        var offset = 0
        for (i in 0 until lineIdx) {
            offset += lines[i].length + 1
        }
        val charInLine = (relX * lines[lineIdx].length).toInt().coerceIn(0, (lines[lineIdx].length - 1).coerceAtLeast(0))
        return expandToWordRange(text, (offset + charInLine).coerceIn(0, text.lastIndex))
    }

    private fun expandToWordRange(text: String, charIdx: Int): Pair<Int, Int> {
        if (text.isEmpty()) return 0 to 0
        val idx = charIdx.coerceIn(0, text.lastIndex)
        var s = idx
        while (s > 0 && !text[s - 1].isWhitespace()) s--
        var e = idx + 1
        while (e < text.length && !text[e].isWhitespace()) e++
        if (abs(e - s) == 0) return 0 to text.length
        return s to e
    }
}
