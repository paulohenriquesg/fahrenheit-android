package com.paulohenriquesg.fahrenheit.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Pins the exact serverSettings shape a real Audiobookshelf 2.36.0 server sends.
 *
 * Captured from a live instance rather than invented, because issue #1 was a field
 * whose real type did not match what the model declared, and login parses this whole
 * object before the user gets anywhere.
 *
 * Note backupSchedule: the model types it String, but a server with backups disabled
 * sends boolean false. Gson coerces that rather than throwing, so login survives - but
 * the value arrives as the string "false", not a cron expression.
 */
class ServerSettingsLiveShapeTest {
    private lateinit var gson: Gson

    @Before
    fun setup() {
        gson = GsonBuilder()
            .registerTypeAdapter(ServerSettings::class.java, ServerSettingsDeserializer())
            .create()
    }

    /** Verbatim values from a 2.36.0 instance, backups disabled. */
    private val liveJson = """
        {
            "id": "server-settings",
            "scannerFindCovers": false,
            "scannerCoverProvider": "google",
            "scannerParseSubtitle": false,
            "scannerPreferMatchedMetadata": false,
            "scannerDisableWatcher": false,
            "storeCoverWithItem": true,
            "storeMetadataWithItem": true,
            "metadataFileFormat": "json",
            "rateLimitLoginRequests": 10,
            "rateLimitLoginWindow": 600000,
            "backupSchedule": false,
            "backupsToKeep": 2,
            "maxBackupSize": 1,
            "loggerDailyLogsToKeep": 7,
            "loggerScannerLogsToKeep": 2,
            "homeBookshelfView": 0,
            "bookshelfView": 0,
            "sortingIgnorePrefix": false,
            "sortingPrefixes": ["the", "a"],
            "chromecastEnabled": false,
            "dateFormat": "dd/MM/yyyy",
            "language": "en-us",
            "logLevel": 2,
            "version": "2.36.0"
        }
    """.trimIndent()

    @Test
    fun `a real 2_36_0 payload deserializes without throwing`() {
        val settings = gson.fromJson(liveJson, ServerSettings::class.java)

        assertEquals("server-settings", settings.id)
        assertEquals("2.36.0", settings.version)
        assertEquals(1, settings.maxBackupSize)
        assertEquals(listOf("the", "a"), settings.sortingPrefixes)
    }

    @Test
    fun `a boolean backupSchedule is coerced rather than crashing login`() {
        // Regression guard: if the model is ever tightened so this throws, login
        // breaks for every user who has backups switched off.
        val settings = gson.fromJson(liveJson, ServerSettings::class.java)

        assertEquals("false", settings.backupSchedule)
    }
}
