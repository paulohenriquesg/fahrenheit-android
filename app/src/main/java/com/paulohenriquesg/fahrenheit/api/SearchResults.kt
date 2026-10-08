package com.paulohenriquesg.fahrenheit.api

data class SearchResults(
    val items: List<LibraryItem>,
    val authors: List<Author>,
    val itemsCut: Boolean = false,
    val authorsCut: Boolean = false
) {
    companion object {
        val None = SearchResults(emptyList(), emptyList())
    }
}
