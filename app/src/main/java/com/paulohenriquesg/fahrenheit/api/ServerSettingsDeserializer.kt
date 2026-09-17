package com.paulohenriquesg.fahrenheit.api

import com.google.gson.*
import java.lang.reflect.Type
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Custom deserializer for ServerSettings.
 *
 * This object arrives inside the login response, so anything that makes it
 * unparseable blocks sign-in entirely. That is issue #1: the server sent
 * maxBackupSize as 0.5 against an Int field, and nobody could log in.
 *
 * Nothing in the app reads ServerSettings. Refusing to sign a user in over a
 * field we never look at is indefensible, so every read here tolerates the
 * field being absent, null, or the wrong type, and falls back to a default.
 *
 * Numbers keep their original rounding: sizes round up (conservative for a
 * limit), everything else to nearest.
 */
class ServerSettingsDeserializer : JsonDeserializer<ServerSettings> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): ServerSettings {
        val obj = if (json.isJsonObject) json.asJsonObject else JsonObject()

        return ServerSettings(
            id = obj.string("id"),
            scannerFindCovers = obj.boolean("scannerFindCovers"),
            scannerCoverProvider = obj.string("scannerCoverProvider"),
            scannerParseSubtitle = obj.boolean("scannerParseSubtitle"),
            scannerPreferMatchedMetadata = obj.boolean("scannerPreferMatchedMetadata"),
            scannerDisableWatcher = obj.boolean("scannerDisableWatcher"),
            storeCoverWithItem = obj.boolean("storeCoverWithItem"),
            storeMetadataWithItem = obj.boolean("storeMetadataWithItem"),
            metadataFileFormat = obj.string("metadataFileFormat"),
            rateLimitLoginRequests = obj.int("rateLimitLoginRequests"),
            rateLimitLoginWindow = obj.long("rateLimitLoginWindow"),
            backupSchedule = obj.string("backupSchedule"),
            backupsToKeep = obj.int("backupsToKeep"),
            maxBackupSize = obj.int("maxBackupSize", roundUp = true),
            loggerDailyLogsToKeep = obj.int("loggerDailyLogsToKeep"),
            loggerScannerLogsToKeep = obj.int("loggerScannerLogsToKeep"),
            homeBookshelfView = obj.int("homeBookshelfView"),
            bookshelfView = obj.int("bookshelfView"),
            sortingIgnorePrefix = obj.boolean("sortingIgnorePrefix"),
            sortingPrefixes = obj.stringList("sortingPrefixes"),
            chromecastEnabled = obj.boolean("chromecastEnabled"),
            dateFormat = obj.string("dateFormat"),
            language = obj.string("language"),
            logLevel = obj.int("logLevel"),
            version = obj.string("version")
        )
    }

    /** Present, non-null and a primitive - anything else is treated as absent. */
    private fun JsonObject.usable(field: String): JsonPrimitive? =
        get(field)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asJsonPrimitive

    private fun JsonObject.string(field: String, default: String = "") =
        usable(field)?.asString ?: default

    private fun JsonObject.boolean(field: String, default: Boolean = false) =
        usable(field)?.takeIf { it.isBoolean }?.asBoolean ?: default

    private fun JsonObject.long(field: String, default: Long = 0L) =
        usable(field)?.takeIf { it.isNumber }?.asLong ?: default

    private fun JsonObject.int(field: String, roundUp: Boolean = false, default: Int = 0): Int {
        val number = usable(field)?.takeIf { it.isNumber }?.asDouble ?: return default
        return if (roundUp) ceil(number).toInt() else number.roundToInt()
    }

    private fun JsonObject.stringList(field: String): List<String> =
        get(field)?.takeIf { it.isJsonArray }?.asJsonArray
            ?.mapNotNull { it.takeIf { e -> !e.isJsonNull && e.isJsonPrimitive }?.asString }
            ?: emptyList()
}
