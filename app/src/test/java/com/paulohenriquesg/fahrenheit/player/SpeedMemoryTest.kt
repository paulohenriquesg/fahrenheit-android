package com.paulohenriquesg.fahrenheit.player

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Speed is remembered per book or show, on this device (#107). */
@RunWith(AndroidJUnit4::class)
class SpeedMemoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun `a book never played at a speed plays at normal speed`() =
        assertEquals(1f, SpeedMemory(context).of("b1"))

    @Test fun `a chosen speed is remembered for that book only`() {
        SpeedMemory(context).remember("b1", 1.25f)
        assertEquals(1.25f, SpeedMemory(context).of("b1"))
        assertEquals(1f, SpeedMemory(context).of("b2"))
    }

    @Test fun `a speed that is not on offer reads as normal`() {
        SpeedMemory(context).remember("b1", 7f)
        assertEquals(1f, SpeedMemory(context).of("b1"))
    }
}
