package com.paulohenriquesg.fahrenheit.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Library endpoints, as suspend functions.
 *
 * Narrow on purpose: a fake for a 21-method interface is unusable, which is a
 * large part of why none of these screens had tests. Each migrated slice moves
 * its endpoints out of [ApiService] into an interface small enough to fake.
 */
interface LibraryApi {
    @GET("api/libraries")
    suspend fun getLibraries(): LibrariesResponse

    @GET("api/libraries/{libraryId}")
    suspend fun getLibrary(@Path("libraryId") libraryId: String): Library

    @GET("api/libraries/{libraryId}/stats")
    suspend fun getLibraryStats(@Path("libraryId") libraryId: String): LibraryStats

    @GET("api/items/{itemId}")
    suspend fun getLibraryItem(
        @Path("itemId") itemId: String,
        @Query("expanded") expanded: Int = 1,
        @Query("include", encoded = true) include: String = "progress,rssfeed,authors,downloads"
    ): LibraryItemResponse

    @GET("api/libraries/{libraryId}/items")
    suspend fun getLibraryItems(
        @Path("libraryId") libraryId: String,
        @Query("sort") sort: String = "media.metadata.title",
        @Query("limit") limit: Int? = null,
        @Query("page") page: Int? = null,
        /** The server reads "1" as descending and anything else as ascending. */
        @Query("desc") desc: Int? = null,
        @Query("include", encoded = true) include: String = "rssfeed,numEpisodesIncomplete",
        @Query("minified") minified: Int = 0,
        /** "group.value", the value base64- then URL-encoded, as the server decodes it. */
        @Query("filter", encoded = true) filter: String? = null
    ): LibraryItemsResponse

    /** Marks a book finished or not (#107). */
    @PATCH("api/me/progress/{itemId}")
    suspend fun markFinished(@Path("itemId") itemId: String, @Body body: ProgressMark)

    /** Marks an episode finished or not. */
    @PATCH("api/me/progress/{itemId}/{episodeId}")
    suspend fun markFinished(@Path("itemId") itemId: String, @Path("episodeId") episodeId: String, @Body body: ProgressMark)

    @GET("api/libraries/{libraryId}/personalized")
    suspend fun getPersonalizedView(
        @Path("libraryId") libraryId: String,
        @Query("limit") limit: Int = 10,
        @Query("include") include: String = "rssfeed"
    ): List<Shelf>
}

/**
 * Finished or not, and where: nothing else, so nothing else is overwritten.
 * [currentTime] is the position the closing report will carry, so that report
 * does not un-finish the item (the server does that when currentTime moves).
 */
data class ProgressMark(val isFinished: Boolean? = null, val currentTime: Double? = null)
