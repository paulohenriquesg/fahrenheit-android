package com.paulohenriquesg.fahrenheit.screensaver

import android.view.View
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

/** Tests run with the screensaver off unless a test installs it (#156). */
@RunWith(AndroidJUnit4::class)
class ScreensaverInstallTest {

    @Test
    fun `a screen started under test has no screensaver`() {
        val screen = Robolectric.buildActivity(ComponentActivity::class.java).setup()

        assertNull(screen.get().window.decorView.findViewWithTag<View>(ScreensaverTags.OVERLAY))
        screen.destroy()
    }
}
