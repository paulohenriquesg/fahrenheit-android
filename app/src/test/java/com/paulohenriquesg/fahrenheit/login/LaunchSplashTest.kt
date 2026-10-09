package com.paulohenriquesg.fahrenheit.login

import android.content.Context
import android.graphics.Rect
import android.util.TypedValue
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.paulohenriquesg.fahrenheit.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.roundToInt

/**
 * The splash's icon on the stick (#191). Below API 31 the splash library
 * draws it in a 288dp box and rings off everything outside a 192dp circle,
 * so the bare launcher icon came out three times the launch screen's,
 * blurred and with its corners cut. Inset, it is the launch screen's 96dp.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class LaunchSplashTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).roundToInt()

    @Test
    fun `the splash icon is the launch screen's 96dp, inside the visible circle`() {
        val themed = ContextThemeWrapper(context, R.style.Theme_Fahrenheit_Starting)
        val value = TypedValue()
        themed.theme.resolveAttribute(androidx.core.splashscreen.R.attr.windowSplashScreenAnimatedIcon, value, true)
        val icon = ContextCompat.getDrawable(themed, value.resourceId)!!

        val inset = Rect().also { icon.getPadding(it) }
        assertEquals(dp(288), icon.intrinsicWidth)
        assertEquals(dp(96), inset.left)
        assertEquals(dp(96), inset.top)
    }
}
