package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName

data class SearchLibraryItemsResponse(
    @SerializedName("book") val book: List<SearchBookItem>? = null,
    @SerializedName("podcast") val podcast: List<SearchBookItem>? = null,
    @SerializedName("tags") val tags: List<Any>? = null,
    @SerializedName("genres") val genres: List<Any>? = null,
    @SerializedName("authors") val authors: List<Author>? = null,
    @SerializedName("narrators") val narrators: List<Any>? = null,
    @SerializedName("series") val series: List<Any>? = null
)

/**
 * A search match. The item it carries is the same LibraryItem the rest of the
 * app parses; the parallel Search* classes that once described it were never
 * referenced by anything.
 */
data class SearchBookItem(
    @SerializedName("libraryItem") val libraryItem: LibraryItem? = null,
    @SerializedName("matchKey") val matchKey: String? = null,
    @SerializedName("matchText") val matchText: String? = null
)

