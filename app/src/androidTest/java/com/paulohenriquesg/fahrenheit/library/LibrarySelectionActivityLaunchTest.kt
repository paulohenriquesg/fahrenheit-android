package com.paulohenriquesg.fahrenheit.library

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test for LibrarySelectionActivity launch
 * This test reproduces the crash when clicking "Switch Library"
 */
@RunWith(AndroidJUnit4::class)
class LibrarySelectionActivityLaunchTest {

    @Test
    fun librarySelectionActivity_canBeLaunchedFromIntent() {
        // Given
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = LibrarySelectionActivity.createIntent(context)

        // When - This will crash if activity is not in manifest
        val scenario = ActivityScenario.launch<LibrarySelectionActivity>(intent)

        // Then - Activity should be created successfully
        scenario.use {
            assertNotNull("Activity should be created", it)
        }
    }

    @Test
    fun librarySelectionActivity_isDeclaredInManifest() {
        // Given
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageManager = context.packageManager
        val packageName = context.packageName

        // When
        val packageInfo = packageManager.getPackageInfo(
            packageName,
            android.content.pm.PackageManager.GET_ACTIVITIES
        )

        // Then - Check if LibrarySelectionActivity is declared
        val activityNames = packageInfo.activities?.map { it.name } ?: emptyList()
        val libraryActivityName = "com.paulohenriquesg.fahrenheit.library.LibrarySelectionActivity"

        assertTrue(
            "LibrarySelectionActivity must be declared in AndroidManifest.xml",
            activityNames.contains(libraryActivityName)
        )
    }

    @Test
    fun createIntent_returnsValidIntent() {
        // Given
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // When
        val intent = LibrarySelectionActivity.createIntent(context)

        // Then
        assertNotNull("Intent should not be null", intent)
        assertEquals(
            "Intent should target LibrarySelectionActivity",
            "com.paulohenriquesg.fahrenheit.library.LibrarySelectionActivity",
            intent.component?.className
        )
    }

    @Test
    fun librarySelectionActivity_hasCorrectIntentFlags() {
        // Given
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // When
        val intent = LibrarySelectionActivity.createIntent(context)

        // Then - For TV apps, we might want specific flags
        assertNotNull("Intent should be created", intent)
        // Activity should be launchable
        val resolveInfo = context.packageManager.resolveActivity(
            intent,
            android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
        )
        assertNotNull("Activity should be resolvable", resolveInfo)
    }
}
