package com.example.ocr

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * In-context MediaProjection prompt.
 * Never launched at startup and never kept running after single-frame capture.
 */
class MediaProjectionPromptActivity : ComponentActivity() {

    companion object {
        const val EXTRA_MODE = "capture_mode"
        const val MODE_VAULT_SCREENSHOT = "vault_screenshot"
        const val MODE_OCR_TIER_B = "ocr_tier_b"
        const val EXTRA_BOUNDS = "crop_bounds"

        fun launchForVaultScreenshot(context: Context) {
            val intent = Intent(context, MediaProjectionPromptActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_VAULT_SCREENSHOT)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }

        fun launchForTierBOcr(context: Context, bounds: Rect?) {
            val intent = Intent(context, MediaProjectionPromptActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_OCR_TIER_B)
                putExtra(EXTRA_BOUNDS, bounds)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_VAULT_SCREENSHOT
            @Suppress("DEPRECATION")
            val bounds = intent.getParcelableExtra<Rect>(EXTRA_BOUNDS)
            val serviceIntent = Intent(this, MediaProjectionCaptureService::class.java).apply {
                putExtra(MediaProjectionCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(MediaProjectionCaptureService.EXTRA_RESULT_DATA, result.data)
                putExtra(EXTRA_MODE, mode)
                putExtra(EXTRA_BOUNDS, bounds)
            }
            ContextCompat.startForegroundService(this, serviceIntent)
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        if (mgr == null) {
            finish()
            return
        }
        projectionLauncher.launch(mgr.createScreenCaptureIntent())
    }
}
