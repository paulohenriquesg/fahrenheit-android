package com.paulohenriquesg.fahrenheit.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.paulohenriquesg.fahrenheit.TestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * serverSettings arrives inside the login response, so anything that makes it
 * unparseable blocks sign-in entirely - that is issue #1, where maxBackupSize
 * came back as 0.5 against an Int.
 *
 * Nothing in the app reads serverSettings. Refusing to log a user in over a
 * field we never look at is indefensible, so parsing must survive whatever the
 * server sends, including fields that simply are not there.
 */
class ServerSettingsToleranceTest {
    private lateinit var gson: Gson

    @Before
    fun setup() {
        gson = GsonBuilder()
            .registerTypeAdapter(ServerSettings::class.java, ServerSettingsDeserializer())
            .create()
    }

    @Test
    fun `a payload missing string fields still parses`() {
        // A future release renaming or dropping a field must not lock everyone out.
        val settings = gson.fromJson(
            """{ "id": "server-settings", "version": "2.99.0" }""",
            ServerSettings::class.java
        )

        assertNotNull(settings)
        assertEquals("server-settings", settings.id)
        assertEquals("2.99.0", settings.version)
    }

    @Test
    fun `explicit nulls are tolerated the same as absent fields`() {
        val settings = gson.fromJson(
            """
            {
                "id": "server-settings",
                "scannerCoverProvider": null,
                "metadataFileFormat": null,
                "dateFormat": null,
                "language": null,
                "chromecastEnabled": null,
                "maxBackupSize": null,
                "sortingPrefixes": null
            }
            """.trimIndent(),
            ServerSettings::class.java
        )

        assertNotNull(settings)
        assertEquals(0, settings.maxBackupSize)
    }

    @Test
    fun `an empty object parses rather than throwing`() {
        assertNotNull(gson.fromJson("{}", ServerSettings::class.java))
    }

    @Test
    fun `a login response whose serverSettings is unusable still yields a session`() {
        // The whole point: sign-in must not depend on a block nothing reads.
        val response = gson.fromJson(
            """
            {
                "user": {
                    "id": "u1", "username": "testuser", "type": "root",
                    "token": "legacy", "accessToken": "access", "refreshToken": "refresh",
                    "mediaProgress": [], "seriesHideFromContinueListening": [],
                    "bookmarks": [], "isActive": true, "isLocked": false,
                    "lastSeen": 0, "createdAt": 0,
                    "permissions": {
                        "download": true, "update": true, "delete": true, "upload": true,
                        "accessAllLibraries": true, "accessAllTags": true,
                        "accessExplicitContent": true
                    },
                    "librariesAccessible": [], "itemTagsAccessible": []
                },
                "userDefaultLibraryId": "lib-1",
                "serverSettings": {},
                "Source": "test"
            }
            """.trimIndent(),
            LoginResponse::class.java
        )

        val session = response.toAuthSession()
        assertEquals("access", session.accessToken)
        assertEquals("refresh", session.refreshToken)
        assertEquals("testuser", session.username)
    }

    @Test
    fun `the fixture payload is unaffected`() {
        // Guards against the tolerant path quietly changing normal parsing.
        assertEquals(1, TestFixtures.createMockServerSettings(1.0).maxBackupSize)
    }
}
