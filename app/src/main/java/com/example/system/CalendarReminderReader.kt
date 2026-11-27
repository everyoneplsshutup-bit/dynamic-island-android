package com.example.system

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CalendarReminderInfo(
    val eventId: Long,
    val title: String,
    val startMs: Long,
    val formattedStart: String,
    val joinUrl: String?
)

object CalendarReminderReader {
    private val meetingUrlRegex = Regex(
        """https?://(?:meet\.google\.com|[\w.-]*zoom\.us|teams\.microsoft\.com)/[^\s<>"]+""",
        RegexOption.IGNORE_CASE
    )

    fun hasCalendarPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun getNextUpcomingEvent(context: Context, nowMs: Long = System.currentTimeMillis()): CalendarReminderInfo? {
        if (!hasCalendarPermission(context)) return null
        val windowStart = nowMs - 10L * 60_000L
        val windowEnd = nowMs + 12L * 60L * 60_000L

        return runCatching {
            val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(uriBuilder, windowStart)
            ContentUris.appendId(uriBuilder, windowEnd)
            val projection = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.EVENT_LOCATION,
                CalendarContract.Instances.DESCRIPTION
            )
            context.contentResolver.query(
                uriBuilder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val titleCol = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                val beginCol = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                val locCol = cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
                val descCol = cursor.getColumnIndex(CalendarContract.Instances.DESCRIPTION)
                val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
                while (cursor.moveToNext()) {
                    val eventId = if (idCol >= 0) cursor.getLong(idCol) else 0L
                    val title = if (titleCol >= 0) cursor.getString(titleCol).orEmpty().trim() else ""
                    val beginMs = if (beginCol >= 0) cursor.getLong(beginCol) else 0L
                    if (title.isBlank() || beginMs <= 0L) continue
                    val location = if (locCol >= 0) cursor.getString(locCol).orEmpty() else ""
                    val desc = if (descCol >= 0) cursor.getString(descCol).orEmpty() else ""
                    val joinUrl = meetingUrlRegex.find(location)?.value ?: meetingUrlRegex.find(desc)?.value
                    return@runCatching CalendarReminderInfo(
                        eventId = eventId,
                        title = title,
                        startMs = beginMs,
                        formattedStart = timeFmt.format(Date(beginMs)),
                        joinUrl = joinUrl
                    )
                }
                null
            }
        }.getOrNull()
    }
}
