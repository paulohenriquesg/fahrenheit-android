package com.paulohenriquesg.fahrenheit.update

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DownloadProgressBarTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `reports download percent as a fraction`() {
        compose.setContent { DownloadProgressBar(progress = 42) }

        compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.42f, 0f..1f))).assertExists()
    }

    // The bar moved off the deprecated Float overload without a device to look
    // at it; the old call is kept here as the reference for how it must look.
    @Suppress("DEPRECATION")
    @Test
    fun `looks the same as the deprecated indicator it replaced`() {
        // Tags go on fixed-size boxes: the indicator's own semantics bounds are
        // padded for accessibility and would not match the pixels it draws.
        val size = Modifier.width(300.dp).height(8.dp)
        compose.setContent {
            Column {
                Box(size.testTag("bar")) {
                    DownloadProgressBar(progress = 42, modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(8.dp))
                Box(size.testTag("reference")) {
                    LinearProgressIndicator(
                        progress = 0.42f,
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Drawn straight from the view rather than captureToImage, which waits
        // on a frame-commit callback Robolectric never delivers.
        compose.waitForIdle()
        val root = compose.activity.window.decorView
        val screen = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(screen))
        fun pixels(tag: String): IntArray {
            val b = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInWindow
            val w = b.width.toInt(); val h = b.height.toInt()
            return IntArray(w * h).also { screen.getPixels(it, 0, w, b.left.toInt(), b.top.toInt(), w, h) }
        }
        val bar = pixels("bar")
        val reference = pixels("reference")
        assertEquals(reference.size, bar.size)
        assertTrue("reference drew nothing", reference.distinct().size > 1)
        val differing = bar.indices.count { bar[it] != reference[it] }
        assertEquals("pixels differing from the reference", 0, differing)
    }
}
