package com.example.system

import android.app.AlarmManager
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NextAlarmInfo(
    val triggerTimeMs: Long,
    val formattedText: String
)

object NextAlarmReader {
    fun getNextAlarm(context: Context, nowMs: Long = System.currentTimeMillis()): NextAlarmInfo? {
        return runCatching {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return null
            val info = am.nextAlarmClock ?: return null
            val trigger = info.triggerTime
            if (trigger <= nowMs) return null
            val formatted = formatAlarm(trigger, nowMs)
            NextAlarmInfo(triggerTimeMs = trigger, formattedText = formatted)
        }.getOrNull()
    }

    fun formatAlarm(triggerTimeMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        val diffMin = ((triggerTimeMs - nowMs) / 60_000L).coerceAtLeast(1L)
        val hours = diffMin / 60L
        val mins = diffMin % 60L
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val clock = timeFmt.format(Date(triggerTimeMs))
        val eta = if (hours > 0L) "${hours}h ${mins}m" else "${mins}m"
        return "Wake up $clock · $eta"
    }
}
