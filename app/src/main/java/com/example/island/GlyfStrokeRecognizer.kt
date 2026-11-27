package com.example.island

import kotlin.math.abs
import kotlin.math.hypot

data class StrokePoint(val x: Float, val y: Float)

enum class GlyfGesture(val symbol: Char, val label: String) {
    C('C', "Glyph C"),
    W('W', "Glyph W"),
    O('O', "Glyph O")
}

object GlyfStrokeRecognizer {

    fun recognize(points: List<StrokePoint>): GlyfGesture? {
        if (points.size < 8) return null
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }
        val width = maxX - minX
        val height = maxY - minY
        if (width < 28f && height < 28f) return null
        val diag = hypot(width, height).coerceAtLeast(1f)

        val start = points.first()
        val end = points.last()
        val startEndDist = hypot(end.x - start.x, end.y - start.y)

        // Normalized X coordinates in [0..1]
        val norm = points.map {
            StrokePoint(
                x = if (width > 1f) (it.x - minX) / width else 0.5f,
                y = if (height > 1f) (it.y - minY) / height else 0.5f
            )
        }

        // 'O': closed loop (start and end meet near each other, roughly spanning both X and Y)
        if (startEndDist <= diag * 0.34f && width >= 30f && height >= 30f) {
            // Check angular sweep around center
            val cx = 0.5f
            val cy = 0.5f
            var totalAngle = 0.0
            for (i in 1 until norm.size) {
                val a1 = kotlin.math.atan2((norm[i - 1].y - cy).toDouble(), (norm[i - 1].x - cx).toDouble())
                val a2 = kotlin.math.atan2((norm[i].y - cy).toDouble(), (norm[i].x - cx).toDouble())
                var d = a2 - a1
                while (d > Math.PI) d -= 2.0 * Math.PI
                while (d < -Math.PI) d += 2.0 * Math.PI
                totalAngle += d
            }
            if (abs(totalAngle) >= Math.PI * 1.35) {
                return GlyfGesture.O
            }
        }

        // 'W': monotonic or predominantly left-to-right stroke with two distinct valleys + central peak
        val deltaX = end.x - start.x
        if (abs(deltaX) >= width * 0.55f && width >= 48f) {
            var valleys = 0
            var peaks = 0
            val smoothedY = smoothY(norm)
            for (i in 1 until smoothedY.lastIndex) {
                val prev = smoothedY[i - 1]
                val cur = smoothedY[i]
                val next = smoothedY[i + 1]
                if (cur > prev + 0.04f && cur > next + 0.04f && cur > 0.58f) valleys++
                if (cur < prev - 0.04f && cur < next - 0.04f && cur < 0.55f) peaks++
            }
            if (valleys >= 2 && peaks >= 1) {
                return GlyfGesture.W
            }
        }

        // 'C': open curve on the right; start near top-right/top, swings left near mid, ends bottom-right
        val leftmostX = norm.minOf { it.x }
        val midStartEndX = (norm.first().x + norm.last().x) / 2f
        val verticalSpan = abs(end.y - start.y)
        if (midStartEndX - leftmostX >= 0.45f &&
            startEndDist > diag * 0.30f &&
            verticalSpan >= height * 0.35f &&
            norm.any { it.x < 0.25f && it.y in 0.20f..0.80f }
        ) {
            return GlyfGesture.C
        }

        return null
    }

    private fun smoothY(points: List<StrokePoint>): List<Float> {
        // Sample into 16 evenly spaced buckets along trajectory for clean extrema count
        val buckets = 16
        val out = ArrayList<Float>(buckets)
        for (b in 0 until buckets) {
            val idx = ((b.toFloat() / (buckets - 1)) * points.lastIndex).toInt().coerceIn(0, points.lastIndex)
            out.add(points[idx].y)
        }
        return out
    }
}
