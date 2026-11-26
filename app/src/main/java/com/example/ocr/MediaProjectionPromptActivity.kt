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
 * Never launched at startup and asks fresh user consent per capture / screen recording start.
 */
class MediaProjectionPromptActivity : ComponentActivity() {

    companion object {
        const val EXTRA_MODE = "capture_mode"
        const val MODE_VAULT_SCREENSHOT = "vault_screenshot"
        const val MODE_OCR_TIER_B = "ocr_tier_b"
        const val MODE_SCREEN_RECORD = "screen_record"
        const val EXTRA_BOUNDS = "crop_bounds"
        const val EXTRA_TAP_X = "tap_x"
        const val EXTRA_TAP_Y = "tap_y"

        fun launchForVaultScreenshot(context: Context) {
            val intent = Intent(context, MediaProjectionPromptActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_VAULT_SCREENSHOT)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }

        fun launchForTierBOcr(context: Context, bounds: Rect?, tapX: Int = -1, tapY: Int = -1) {
            val intent = Intent(context, MediaProjectionPromptActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_OCR_TIER_B)
                putExtra(EXTRA_BOUNDS, bounds)
                putExtra(EXTRA_TAP_X, tapX)
                putExtra(EXTRA_TAP_Y, tapY)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }

        fun launchForScreenRecord(context: Context) {
            val intent = Intent(context, MediaProjectionPromptActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_SCREEN_RECORD)
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
            val tapX = intent.getIntExtra(EXTRA_TAP_X, -1)
            val tapY = intent.getIntExtra(EXTRA_TAP_Y, -1)
            val serviceIntent = Intent(this, MediaProjectionCaptureService::class.java).apply {
                putExtra(MediaProjectionCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(MediaProjectionCaptureService.EXTRA_RESULT_DATA, result.data)
                putExtra(EXTRA_MODE, mode)
                putExtra(EXTRA_BOUNDS, bounds)
                putExtra(EXTRA_TAP_X, tapX)
                putExtra(EXTRA_TAP_Y, tapY)
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
