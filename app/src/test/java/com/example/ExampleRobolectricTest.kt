package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.example.system.CalculatorEngine
import com.example.system.CalcOperator
import com.example.system.IslandSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Dynamic Island and manifest uses least-privilege permissions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Dynamic Island", context.getString(R.string.app_name))

        val pkgInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val permissions = pkgInfo.requestedPermissions?.toList().orEmpty()
        assertTrue(
            "INTERNET permission is declared solely for opt-in https://lrclib.net lyrics lookup",
            permissions.contains(Manifest.permission.INTERNET)
        )
        assertFalse(permissions.contains(Manifest.permission.READ_EXTERNAL_STORAGE))
        assertFalse(permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION))
        assertFalse(permissions.contains(Manifest.permission.SEND_SMS))
    }

    @Test
    fun `privacy and opt-in defaults match Round 3 contract`() {
        val defaults = IslandSettings()
        assertFalse(defaults.screenReadingEnabled)
        assertFalse(defaults.ocrTierBEnabled)
        assertTrue(defaults.lyricsFromInternet)
        assertFalse(defaults.appleWidthIdlePill)
        assertFalse(defaults.reelOverlayEnabled)
        assertFalse(defaults.sfxMorphEnabled)
    }

    @Test
    fun `calculator engine computes standard and inline math`() {
        val calc = CalculatorEngine()
        calc.pressDigit("1")
        calc.pressDigit("2")
        calc.pressOperator(CalcOperator.MULTIPLY)
        calc.pressDigit("3")
        calc.pressEquals()
        assertEquals("36", calc.state.value.display)

        assertEquals("42", CalculatorEngine.evaluateInlineExpression("6 * 7"))
        assertEquals("5", CalculatorEngine.evaluateInlineExpression("25 ÷ 5"))
        assertTrue(CalculatorEngine.evaluateInlineExpression("hello") == null)
    }
}
