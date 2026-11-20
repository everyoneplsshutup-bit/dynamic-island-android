package com.example.notes

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.DynamicIslandApp
import com.example.island.AlertKind
import com.example.island.IslandActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Transparent entry point for:
 * (a) ACTION_SEND (text, image, link, file, vCard, video, audio) + direct-share targets ("Save to Vault", "Save link")
 * (b) ACTION_PROCESS_TEXT ("Add to island" in text-selection toolbar via EXTRA_PROCESS_TEXT)
 * Works reliably even if the Accessibility overlay service is currently killed.
 */
class ShareToIslandActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) {
            finish()
            return
        }
        val app = applicationContext as? DynamicIslandApp
        if (app == null) {
            finish()
            return
        }

        when (intent.action) {
            Intent.ACTION_PROCESS_TEXT -> {
                val selectedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()?.trim().orEmpty()
                if (selectedText.isNotEmpty()) {
                    lifecycleScope.launch {
                        saveTextOrLink(app, selectedText, titlePrefix = "Selected Text")
                        toastAndFinish("Added to Dynamic Island Notes")
                    }
                } else {
                    finish()
                }
            }

            Intent.ACTION_SEND -> {
                val mime = intent.type.orEmpty()
                val streamUri = getStreamUri(intent)
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
                val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.trim().orEmpty()

                lifecycleScope.launch {
                    when {
                        mime.startsWith("image/") && streamUri != null -> {
                            app.vaultManager.saveFromUri(streamUri, sourceType = "SHARED")
                            toastAndFinish("Saved to 10-min Photo Vault")
                        }

                        streamUri != null -> {
                            val copiedPath = copySharedFileToPrivate(streamUri, mime)
                            val label = subject.ifEmpty { streamUri.lastPathSegment ?: "Shared File" }
                            val typeTag = when {
                                mime.contains("vcard") -> "VCARD"
                                mime.startsWith("video/") -> "VIDEO"
                                mime.startsWith("audio/") -> "AUDIO"
                                else -> "FILE"
                            }
                            withContext(Dispatchers.IO) {
                                app.database.savedLinkDao().insert(
                                    SavedLinkEntity(
                                        urlOrUri = copiedPath ?: streamUri.toString(),
                                        title = label,
                                        itemType = typeTag
                                    )
                                )
                            }
                            app.islandController.upsertActivity(
                                IslandActivity.Alert(
                                    id = "shared_file_${System.currentTimeMillis()}",
                                    kind = AlertKind.VAULT_EXPIRING,
                                    title = "Saved $typeTag to Island",
                                    subtitle = label
                                ),
                                autoShowCompact = true,
                                autoDismissMs = 3500L
                            )
                            toastAndFinish("Saved $typeTag to Island")
                        }

                        sharedText.isNotEmpty() -> {
                            saveTextOrLink(app, sharedText, titlePrefix = subject.ifEmpty { "Shared" })
                            val isUrl = sharedText.startsWith("http://") || sharedText.startsWith("https://")
                            toastAndFinish(if (isUrl) "Link saved to Island" else "Note saved to Island")
                        }

                        else -> finish()
                    }
                }
            }

            else -> finish()
        }
    }

    private suspend fun saveTextOrLink(app: DynamicIslandApp, content: String, titlePrefix: String) {
        withContext(Dispatchers.IO) {
            val isUrl = content.startsWith("http://") || content.startsWith("https://")
            if (isUrl) {
                app.database.savedLinkDao().insert(
                    SavedLinkEntity(
                        urlOrUri = content,
                        title = titlePrefix,
                        itemType = "LINK"
                    )
                )
            }
            app.database.noteDao().upsert(
                NoteEntity(
                    title = if (isUrl) "Link: $titlePrefix" else content.take(36),
                    content = content
                )
            )
        }
        app.islandController.upsertActivity(
            IslandActivity.Alert(
                id = "shared_note_${System.currentTimeMillis()}",
                kind = AlertKind.OCR_COPIED,
                title = "Added to Island Notes",
                subtitle = content.take(48)
            ),
            autoShowCompact = true,
            autoDismissMs = 3000L
        )
    }

    private suspend fun copySharedFileToPrivate(uri: Uri, mime: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val sharedDir = File(filesDir, "shared").apply { if (!exists()) mkdirs() }
            val ext = mime.substringAfter('/', "bin").substringBefore(';').take(8)
            val dest = File(sharedDir, "${UUID.randomUUID()}.$ext")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
            dest.absolutePath
        }.getOrNull()
    }

    @Suppress("DEPRECATION")
    private fun getStreamUri(intent: Intent): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }

    private fun toastAndFinish(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        finish()
    }
}
