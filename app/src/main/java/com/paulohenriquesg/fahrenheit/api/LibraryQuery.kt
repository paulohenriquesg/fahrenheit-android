package com.paulohenriquesg.fahrenheit.api

/**
 * How the library screen asks for its items (#146): the whole library by
 * title, as the rail opens it, or the view a Home shelf's "See all" stands
 * for. "progress" sorts by when each item was last played.
 */
enum class LibraryQuery(val sort: String, val newestFirst: Boolean, private val progress: String?) {
    Everything("media.metadata.title", newestFirst = false, progress = null),
    RecentlyAdded("addedAt", newestFirst = true, progress = null),
    InProgress("progress", newestFirst = true, progress = "in-progress"),
    Finished("progress", newestFirst = true, progress = "finished");

    /** The server's filter, or null for none. */
    val filter: String? get() = progress?.let { itemsFilter("progress", it) }
}
