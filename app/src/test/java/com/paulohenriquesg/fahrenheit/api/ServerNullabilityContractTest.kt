package com.paulohenriquesg.fahrenheit.api

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.reflect.full.memberProperties

/**
 * Fields an Audiobookshelf server sends as null or leaves out. Gson ignores
 * Kotlin nullability, so declaring any of these non-null lets a null through
 * to code the compiler promised it would never reach.
 *
 * Observed on a live server (2.x, 2026-09-19) by parsing every endpoint the
 * app calls. The PlayLibraryItem* entries are inferred: the play response
 * embeds the same library item and episode objects.
 */
class ServerNullabilityContractTest {

    private val nullable = mapOf(
        LibraryItemResponse::class to listOf("scanVersion"),
        LibraryItemMedia::class to listOf("coverPath", "audioFiles", "tracks"),
        LibraryItemMetadata::class to listOf("titleIgnorePrefix", "publishedYear", "publisher", "description", "asin"),
        Episode::class to listOf("episodeType", "description", "pubDate"),
        LibraryItemsResponse::class to listOf("filterBy"),
        Media::class to listOf("coverPath"),
        User::class to listOf("itemTagsAccessible"),
        PlayLibraryItemResponse::class to listOf("coverPath"),
        PlayLibraryItemMediaMetadata::class to listOf("description"),
        PlayLibraryItemLibraryItem::class to listOf("scanVersion"),
        PlayLibraryItemMedia::class to listOf("coverPath"),
        PlayLibraryItemEpisode::class to listOf("episodeType", "description", "pubDate"),
    )

    @Test
    fun `fields the server leaves null are declared nullable`() {
        val declaredNonNull = nullable.flatMap { (cls, names) ->
            names.map { name ->
                val prop = cls.memberProperties.firstOrNull { it.name == name }
                    ?: error("${cls.simpleName} has no property $name")
                prop to "${cls.simpleName}.$name"
            }
        }.filterNot { (prop, _) -> prop.returnType.isMarkedNullable }.map { it.second }

        assertEquals(emptyList<String>(), declaredNonNull)
    }
}
