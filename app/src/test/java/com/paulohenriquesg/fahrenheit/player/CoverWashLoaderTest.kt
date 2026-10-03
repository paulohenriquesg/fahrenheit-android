package com.paulohenriquesg.fahrenheit.player

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoverWashLoaderTest {
    private fun solid(argb: Int) = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(argb) }

    @Test fun `a green cover gives a green wash`() = runBlocking {
        val wash = coverWashOf(solid(0xFF5B8B3A.toInt()))!!
        assertTrue("greenish: $wash", wash.green > wash.red && wash.green > wash.blue)
    }

    // Review Focus 3.
    @Test fun `no cover is no wash`() = runBlocking { assertNull(coverWashOf(null)) }

    @Test fun `a grey cover is no wash`() = runBlocking { assertNull(coverWashOf(solid(0xFF808080.toInt()))) }
}
