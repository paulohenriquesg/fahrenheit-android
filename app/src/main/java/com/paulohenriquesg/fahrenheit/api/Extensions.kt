package com.paulohenriquesg.fahrenheit.api

/**
 * Helper extensions for LibraryItemMetadata to handle both detailed and minified API response formats.
 * Detailed responses use array fields (authors, narrators, series).
 * Minified responses use string fields (authorName, narratorName, seriesName).
 */

fun LibraryItemMetadata.getAuthorsDisplay(): String =
    authors?.joinToString(", ") { it.name } ?: authorName.orEmpty()

fun LibraryItemMetadata.getNarratorsDisplay(): String =
    narrators?.joinToString(", ") ?: narratorName.orEmpty()

fun LibraryItemMetadata.getSeriesDisplay(): String =
    series?.joinToString(", ") { it.name } ?: seriesName.orEmpty()
