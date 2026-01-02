package com.paulohenriquesg.fahrenheit.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ServerSettingsDeserializerTest {
    private lateinit var gson: Gson

    @Before
    fun setup() {
        gson = GsonBuilder()
            .registerTypeAdapter(ServerSettings::class.java, ServerSettingsDeserializer())
            .create()
    }

    @Test
    fun `deserialize maxBackupSize as float rounds up with ceiling`() {
        val json = """
            {
                "id": "test-id",
                "scannerFindCovers": true,
                "scannerCoverProvider": "google",
                "scannerParseSubtitle": false,
                "scannerPreferMatchedMetadata": true,
                "scannerDisableWatcher": false,
                "storeCoverWithItem": true,
                "storeMetadataWithItem": false,
                "metadataFileFormat": "json",
                "rateLimitLoginRequests": 10,
                "rateLimitLoginWindow": 600000,
                "backupSchedule": "0 0 * * *",
                "backupsToKeep": 5,
                "maxBackupSize": 0.5,
                "loggerDailyLogsToKeep": 7,
                "loggerScannerLogsToKeep": 3,
                "homeBookshelfView": 1,
                "bookshelfView": 1,
                "sortingIgnorePrefix": true,
                "sortingPrefixes": ["The", "A", "An"],
                "chromecastEnabled": false,
                "dateFormat": "MM/dd/yyyy",
                "language": "en",
                "logLevel": 2,
                "version": "2.5.0"
            }
        """.trimIndent()

        val settings = gson.fromJson(json, ServerSettings::class.java)

        assertEquals(1, settings.maxBackupSize) // 0.5 should round up to 1
    }

    @Test
    fun `deserialize maxBackupSize as integer`() {
        val json = """
            {
                "id": "test-id",
                "scannerFindCovers": true,
                "scannerCoverProvider": "google",
                "scannerParseSubtitle": false,
                "scannerPreferMatchedMetadata": true,
                "scannerDisableWatcher": false,
                "storeCoverWithItem": true,
                "storeMetadataWithItem": false,
                "metadataFileFormat": "json",
                "rateLimitLoginRequests": 10,
                "rateLimitLoginWindow": 600000,
                "backupSchedule": "0 0 * * *",
                "backupsToKeep": 5,
                "maxBackupSize": 2,
                "loggerDailyLogsToKeep": 7,
                "loggerScannerLogsToKeep": 3,
                "homeBookshelfView": 1,
                "bookshelfView": 1,
                "sortingIgnorePrefix": true,
                "sortingPrefixes": ["The", "A", "An"],
                "chromecastEnabled": false,
                "dateFormat": "MM/dd/yyyy",
                "language": "en",
                "logLevel": 2,
                "version": "2.5.0"
            }
        """.trimIndent()

        val settings = gson.fromJson(json, ServerSettings::class.java)

        assertEquals(2, settings.maxBackupSize)
    }

    @Test
    fun `deserialize backupsToKeep rounds to nearest integer`() {
        val json = """
            {
                "id": "test-id",
                "scannerFindCovers": true,
                "scannerCoverProvider": "google",
                "scannerParseSubtitle": false,
                "scannerPreferMatchedMetadata": true,
                "scannerDisableWatcher": false,
                "storeCoverWithItem": true,
                "storeMetadataWithItem": false,
                "metadataFileFormat": "json",
                "rateLimitLoginRequests": 10,
                "rateLimitLoginWindow": 600000,
                "backupSchedule": "0 0 * * *",
                "backupsToKeep": 2.6,
                "maxBackupSize": 1,
                "loggerDailyLogsToKeep": 7,
                "loggerScannerLogsToKeep": 3,
                "homeBookshelfView": 1,
                "bookshelfView": 1,
                "sortingIgnorePrefix": true,
                "sortingPrefixes": ["The", "A", "An"],
                "chromecastEnabled": false,
                "dateFormat": "MM/dd/yyyy",
                "language": "en",
                "logLevel": 2,
                "version": "2.5.0"
            }
        """.trimIndent()

        val settings = gson.fromJson(json, ServerSettings::class.java)

        assertEquals(3, settings.backupsToKeep) // 2.6 rounds to 3
    }

    @Test
    fun `deserialize all numeric fields as floats`() {
        val json = """
            {
                "id": "test-id",
                "scannerFindCovers": true,
                "scannerCoverProvider": "google",
                "scannerParseSubtitle": false,
                "scannerPreferMatchedMetadata": true,
                "scannerDisableWatcher": false,
                "storeCoverWithItem": true,
                "storeMetadataWithItem": false,
                "metadataFileFormat": "json",
                "rateLimitLoginRequests": 10.2,
                "rateLimitLoginWindow": 600000,
                "backupSchedule": "0 0 * * *",
                "backupsToKeep": 5.4,
                "maxBackupSize": 1.9,
                "loggerDailyLogsToKeep": 7.1,
                "loggerScannerLogsToKeep": 3.8,
                "homeBookshelfView": 1.5,
                "bookshelfView": 1.5,
                "sortingIgnorePrefix": true,
                "sortingPrefixes": ["The", "A", "An"],
                "chromecastEnabled": false,
                "dateFormat": "MM/dd/yyyy",
                "language": "en",
                "logLevel": 2.3,
                "version": "2.5.0"
            }
        """.trimIndent()

        val settings = gson.fromJson(json, ServerSettings::class.java)

        assertEquals(10, settings.rateLimitLoginRequests) // 10.2 rounds to 10
        assertEquals(5, settings.backupsToKeep) // 5.4 rounds to 5
        assertEquals(2, settings.maxBackupSize) // 1.9 rounds up to 2 (ceil)
        assertEquals(7, settings.loggerDailyLogsToKeep) // 7.1 rounds to 7
        assertEquals(4, settings.loggerScannerLogsToKeep) // 3.8 rounds to 4
        assertEquals(2, settings.homeBookshelfView) // 1.5 rounds to 2
        assertEquals(2, settings.bookshelfView) // 1.5 rounds to 2
        assertEquals(2, settings.logLevel) // 2.3 rounds to 2
    }
}
