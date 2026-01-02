package com.paulohenriquesg.fahrenheit.api

import com.google.gson.*
import java.lang.reflect.Type
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Custom deserializer for ServerSettings to handle numeric type mismatches.
 *
 * The Audiobookshelf API may return numeric values as either integers or floats,
 * but our Kotlin data class expects Int types. This deserializer handles both cases:
 *
 * - For size limits (maxBackupSize): Uses ceiling to round up conservatively
 * - For counts and other values: Uses standard rounding to nearest integer
 *
 * Example conversions:
 * - maxBackupSize: 0.5 → 1 (0.5 GB rounded up to 1 GB)
 * - backupsToKeep: 2.4 → 2 (2.4 rounded to 2)
 * - backupsToKeep: 2.6 → 3 (2.6 rounded to 3)
 */
class ServerSettingsDeserializer : JsonDeserializer<ServerSettings> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): ServerSettings {
        val obj = json.asJsonObject

        return ServerSettings(
            id = obj.get("id").asString,
            scannerFindCovers = obj.get("scannerFindCovers").asBoolean,
            scannerCoverProvider = obj.get("scannerCoverProvider").asString,
            scannerParseSubtitle = obj.get("scannerParseSubtitle").asBoolean,
            scannerPreferMatchedMetadata = obj.get("scannerPreferMatchedMetadata").asBoolean,
            scannerDisableWatcher = obj.get("scannerDisableWatcher").asBoolean,
            storeCoverWithItem = obj.get("storeCoverWithItem").asBoolean,
            storeMetadataWithItem = obj.get("storeMetadataWithItem").asBoolean,
            metadataFileFormat = obj.get("metadataFileFormat").asString,
            rateLimitLoginRequests = getIntFromNumber(obj, "rateLimitLoginRequests"),
            rateLimitLoginWindow = obj.get("rateLimitLoginWindow").asLong,
            backupSchedule = obj.get("backupSchedule").asString,
            backupsToKeep = getIntFromNumber(obj, "backupsToKeep"),
            maxBackupSize = getIntFromNumberCeil(obj, "maxBackupSize"),
            loggerDailyLogsToKeep = getIntFromNumber(obj, "loggerDailyLogsToKeep"),
            loggerScannerLogsToKeep = getIntFromNumber(obj, "loggerScannerLogsToKeep"),
            homeBookshelfView = getIntFromNumber(obj, "homeBookshelfView"),
            bookshelfView = getIntFromNumber(obj, "bookshelfView"),
            sortingIgnorePrefix = obj.get("sortingIgnorePrefix").asBoolean,
            sortingPrefixes = context.deserialize(
                obj.get("sortingPrefixes"),
                object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
            ),
            chromecastEnabled = obj.get("chromecastEnabled").asBoolean,
            dateFormat = obj.get("dateFormat").asString,
            language = obj.get("language").asString,
            logLevel = getIntFromNumber(obj, "logLevel"),
            version = obj.get("version").asString
        )
    }

    /**
     * Safely extracts Int from JSON number, handling both int and float values.
     * Uses rounding to nearest integer for fractional values.
     */
    private fun getIntFromNumber(obj: JsonObject, fieldName: String): Int {
        val element = obj.get(fieldName)
        return when {
            element.isJsonPrimitive && element.asJsonPrimitive.isNumber -> {
                element.asDouble.roundToInt()
            }
            else -> 0 // Default value if field is missing or invalid
        }
    }

    /**
     * Safely extracts Int from JSON number with ceiling rounding.
     * Used for size limits where we want to round up (conservative approach).
     */
    private fun getIntFromNumberCeil(obj: JsonObject, fieldName: String): Int {
        val element = obj.get(fieldName)
        return when {
            element.isJsonPrimitive && element.asJsonPrimitive.isNumber -> {
                ceil(element.asDouble).toInt()
            }
            else -> 0 // Default value if field is missing or invalid
        }
    }
}
