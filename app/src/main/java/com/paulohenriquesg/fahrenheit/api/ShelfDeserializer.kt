package com.paulohenriquesg.fahrenheit.api

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class ShelfDeserializer : JsonDeserializer<Shelf> {
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
        val total = jsonObject.get("total")?.asInt

        val entities = jsonObject.get("entities")

        return when (type) {
            "book", "podcast" -> {
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
                // Unknown type, return empty shelf
                Shelf(id, label, labelStringKey, type, null, total, null, null)
            }
        }
    }
}
