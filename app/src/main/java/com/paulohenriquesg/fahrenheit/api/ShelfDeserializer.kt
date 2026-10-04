package com.paulohenriquesg.fahrenheit.api

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 * @param log where a shelf that cannot be read is reported; it is skipped,
 *   not drawn empty.
 */
class ShelfDeserializer(
    private val log: (String) -> Unit = { android.util.Log.w("ShelfDeserializer", it) }
) : JsonDeserializer<Shelf> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Shelf {
        val jsonObject = json.asJsonObject

        val id = jsonObject.get("id").asString
        val label = jsonObject.get("label").asString
        val labelStringKey = jsonObject.get("labelStringKey").asString
        val type = jsonObject.get("type").asString
        // `?.` only covers an absent key: a present-but-null field is a JsonNull
        // instance, and asInt throws UnsupportedOperationException on it.
        val total = jsonObject.get("total")?.takeIf { !it.isJsonNull }?.asInt

        val entities = jsonObject.get("entities")

        return when (type) {
            "book", "podcast", "episode" -> {
                val bookList = if (entities != null && !entities.isJsonNull) {
                    context.deserialize<List<LibraryItem>>(
                        entities,
                        object : TypeToken<List<LibraryItem>>() {}.type
                    )
                } else null

                Shelf(id, label, labelStringKey, type, bookList, total, null, null)
            }
            "authors" -> {
                val authorList = if (entities != null && !entities.isJsonNull) {
                    context.deserialize<List<Author>>(
                        entities,
                        object : TypeToken<List<Author>>() {}.type
                    )
                } else null

                Shelf(id, label, labelStringKey, type, null, total, authorList, null)
            }
            "series" -> {
                val seriesList = if (entities != null && !entities.isJsonNull) {
                    context.deserialize<List<Series>>(
                        entities,
                        object : TypeToken<List<Series>>() {}.type
                    )
                } else null

                Shelf(id, label, labelStringKey, type, null, total, null, seriesList)
            }
            else -> {
                // A type this version does not know (#147): the server adds and
                // renames shelves. If it holds library items, keep them, and
                // Home draws them as plain covers; anything else is skipped.
                Shelf(id, label, labelStringKey, type, unknownShelfItems(id, type, entities, context), total, null, null)
            }
        }
    }

    /**
     * The entities of a shelf of unknown type, if every one is a library item.
     * Checked by hand: Gson fills a Kotlin class without regard to its
     * non-null fields, so any object would "parse" and break later on screen.
     */
    private fun unknownShelfItems(
        id: String,
        type: String,
        entities: JsonElement?,
        context: JsonDeserializationContext
    ): List<LibraryItem>? {
        if (entities == null || !entities.isJsonArray || entities.asJsonArray.isEmpty) return null
        val readable = entities.asJsonArray.all { it.isLibraryItem() }
        if (!readable) {
            log("Skipped shelf \"$id\" of unknown type \"$type\": its entities are not library items")
            return null
        }
        return context.deserialize(entities, object : TypeToken<List<LibraryItem>>() {}.type)
    }

    private fun JsonElement.isLibraryItem(): Boolean {
        if (!isJsonObject) return false
        val item = asJsonObject
        val itemId = item.get("id")
        val media = item.get("media")
        return itemId != null && itemId.isJsonPrimitive &&
            media != null && media.isJsonObject &&
            media.asJsonObject.get("metadata")?.isJsonObject == true
    }
}
