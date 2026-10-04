package com.paulohenriquesg.fahrenheit.api

/**
 * How the library screen asks for its items (#146): the whole library by
 * title, as the rail opens it, or the view a Home shelf's "See all" stands
 * for. "progress" sorts by when each item was last played.
 */
enum class LibraryQuery(val sort: String, val newestFirst: Boolean) {
    Everything("media.metadata.title", newestFirst = false),
    // "recent" is the server's last 60 days, the same window as the Recently
    // added shelf counts, so "See all N" opens N items.
    RecentlyAdded("addedAt", newestFirst = true),
    InProgress("progress", newestFirst = true),
    Finished("progress", newestFirst = true);

    /** The server's filter, or null for none. */
    val filter: String?
        get() = when (this) {
            Everything -> null
            RecentlyAdded -> "recent"
            InProgress -> itemsFilter("progress", "in-progress")
            Finished -> itemsFilter("progress", "finished")
        }
}
