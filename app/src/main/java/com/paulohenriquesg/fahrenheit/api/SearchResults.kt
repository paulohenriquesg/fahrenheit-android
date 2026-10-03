package com.paulohenriquesg.fahrenheit.api

data class SearchResults(val items: List<LibraryItem>, val authors: List<Author>) {
    companion object {
        val None = SearchResults(emptyList(), emptyList())
    }
}
