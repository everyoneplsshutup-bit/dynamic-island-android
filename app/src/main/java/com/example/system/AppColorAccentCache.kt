package com.example.system

import android.content.Context
import android.graphics.Color
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class AppColorAccentCache(private val context: Context) {

    private val cache = object : LruCache<String, Int>(64) {}

    suspend fun getAccentColorArgb(packageName: String): Int? {
        if (packageName.isBlank()) return null
        synchronized(cache) {
            cache.get(packageName)?.let { return if (it == 0) null else it }
        }
        val computed = withTimeoutOrNull(500L) {
            withContext(Dispatchers.Default) {
                extractDominantAccentArgb(packageName)
            }
        }
        synchronized(cache) {
            cache.put(packageName, computed ?: 0)
        }
        return computed
    }

    private fun extractDominantAccentArgb(packageName: String): Int? = runCatching {
        val drawable = context.packageManager.getApplicationIcon(packageName)
        val bmp = drawable.toBitmap(width = 48, height = 48)
        val hsv = FloatArray(3)
        var bestScore = -1f
        var bestArgb: Int? = null
        val step = 2
        for (y in 0 until bmp.height step step) {
            for (x in 0 until bmp.width step step) {
                val pixel = bmp.getPixel(x, y)
                val alpha = Color.alpha(pixel)
                if (alpha < 180) continue
                Color.colorToHSV(pixel, hsv)
                val sat = hsv[1]
                val valB = hsv[2]
                // Reject near-white, near-black, and near-gray colors
                if (sat < 0.22f || valB < 0.20f || valB > 0.98f && sat < 0.25f) continue
                val score = sat * 0.72f + valB * 0.28f
                if (score > bestScore) {
                    bestScore = score
                    bestArgb = Color.rgb(Color.red(pixel), Color.green(pixel), Color.blue(pixel))
                }
            }
        }
        if (!bmp.isRecycled) bmp.recycle()
        bestArgb
    }.getOrNull()
}
