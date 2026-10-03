package com.paulohenriquesg.fahrenheit.api

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

/**
 * A book's series, as the server sends them: a list on an item, but one
 * object on each book of a list filtered by series (the player's About row,
 * #107). Read as a list either way; anything else is no series.
 */
class SeriesListDeserializer : JsonDeserializer<List<Series>> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): List<Series> =
        when {
            json.isJsonArray -> json.asJsonArray.map { context.deserialize<Series>(it, Series::class.java) }
            json.isJsonObject -> listOf(context.deserialize(json, Series::class.java))
            else -> emptyList()
        }
}
