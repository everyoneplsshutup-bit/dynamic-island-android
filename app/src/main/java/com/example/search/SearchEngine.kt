package com.example.search

import android.Manifest
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.floating.FloatingWindowManager
import com.example.notes.NoteDao
import com.example.notes.NoteEntity
import com.example.system.CalculatorEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LaunchableApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val iconBitmap: ImageBitmap? = null
)

data class ContactMatch(
    val name: String,
    val phone: String
)

data class SearchResults(
    val inlineMathResult: String? = null,
    val apps: List<LaunchableApp> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val contacts: List<ContactMatch> = emptyList(),
    val hasContactsPermission: Boolean = false
)

class SearchEngine(
    private val context: Context,
    private val noteDao: NoteDao,
    private val floatingWindowManager: FloatingWindowManager
) {
    @Volatile
    private var cachedApps: List<LaunchableApp>? = null

    suspend fun getInstalledApps(forceRefresh: Boolean = false): List<LaunchableApp> = withContext(Dispatchers.Default) {
        if (!forceRefresh && cachedApps != null) return@withContext cachedApps!!
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, 0)
        val list = resolved
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .map { info ->
                val drawable = runCatching { info.loadIcon(pm) }.getOrNull()
                val bmp = runCatching { drawable?.toBitmap(64, 64)?.asImageBitmap() }.getOrNull()
                LaunchableApp(
                    packageName = info.activityInfo.packageName,
                    label = info.loadLabel(pm).toString(),
                    icon = drawable,
                    iconBitmap = bmp
                )
            }
            .sortedBy { it.label.lowercase() }
        cachedApps = list
        list
    }

    suspend fun search(query: String): SearchResults = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        val hasContactsPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (trimmed.isEmpty()) {
            val apps = getInstalledApps().take(8)
            return@withContext SearchResults(
                apps = apps,
                hasContactsPermission = hasContactsPerm
            )
        }

        val math = CalculatorEngine.evaluateInlineExpression(trimmed)
        val apps = getInstalledApps()
            .filter { it.label.contains(trimmed, ignoreCase = true) || it.packageName.contains(trimmed, ignoreCase = true) }
            .take(8)
        val notes = noteDao.search(trimmed).take(5)
        val contacts = if (hasContactsPerm && trimmed.length >= 2) {
            queryContacts(trimmed)
        } else {
            emptyList()
        }

        SearchResults(
            inlineMathResult = math,
            apps = apps,
            notes = notes,
            contacts = contacts,
            hasContactsPermission = hasContactsPerm
        )
    }

    private fun queryContacts(query: String): List<ContactMatch> {
        val matches = mutableListOf<ContactMatch>()
        runCatching {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val args = arrayOf("%$query%")
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                args,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext() && matches.size < 5) {
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else ""
                    val num = if (numIdx >= 0) cursor.getString(numIdx).orEmpty() else ""
                    if (name.isNotBlank()) {
                        matches.add(ContactMatch(name, num))
                    }
                }
            }
        }
        return matches
    }

    fun launchApp(packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun launchWebSearch(query: String, preferFloating: Boolean = false) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        val googleUrl = Uri.parse("https://www.google.com/search?q=${Uri.encode(trimmed)}")
        val browserIntent = Intent(Intent.ACTION_VIEW, googleUrl).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pm = context.packageManager
        val browserHandlers = pm.queryIntentActivities(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .filter { it.activityInfo.packageName != context.packageName }

        val webSearchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, trimmed)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val targetIntent = when {
            browserHandlers.isNotEmpty() -> {
                val chosenPkg = browserHandlers.firstOrNull {
                    it.activityInfo.packageName.contains("chrome", ignoreCase = true) ||
                        it.activityInfo.packageName.contains("browser", ignoreCase = true)
                }?.activityInfo?.packageName ?: browserHandlers.first().activityInfo.packageName
                Intent(browserIntent).setPackage(chosenPkg)
            }
            webSearchIntent.resolveActivity(pm) != null -> webSearchIntent
            else -> browserIntent
        }

        if (preferFloating) {
            floatingWindowManager.launchIntentFloatingOrFallback(targetIntent)
            return
        }

        val directResult = runCatching { context.startActivity(targetIntent) }
        if (directResult.isFailure) {
            val fallbackChooser = Intent.createChooser(browserIntent, "Search Google").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(fallbackChooser) }
        }
    }
}
