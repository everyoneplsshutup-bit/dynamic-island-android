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
    fun `app name matches Dynamic Island and internet permission is absent`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Dynamic Island", context.getString(R.string.app_name))

        val pkgInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val permissions = pkgInfo.requestedPermissions?.toList().orEmpty()
        assertFalse(
            "App must NOT declare INTERNET permission",
            permissions.contains(Manifest.permission.INTERNET)
        )
    }

    @Test
    fun `screenReadingEnabled defaults to false`() {
        val defaults = IslandSettings()
        assertFalse(defaults.screenReadingEnabled)
        assertFalse(defaults.ocrTierBEnabled)
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
