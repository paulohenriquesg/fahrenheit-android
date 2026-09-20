package com.paulohenriquesg.fahrenheit.api

import retrofit2.http.GET
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

    @GET("api/libraries/{libraryId}/items")
    suspend fun getLibraryItems(
        @Path("libraryId") libraryId: String,
        @Query("sort") sort: String = "media.metadata.title",
        @Query("limit") limit: Int? = null,
        @Query("page") page: Int? = null,
        @Query("desc") desc: Boolean? = null,
        @Query("include", encoded = true) include: String = "rssfeed,numEpisodesIncomplete",
        @Query("minified") minified: Int = 0
    ): LibraryItemsResponse

    @GET("api/libraries/{libraryId}/personalized")
    suspend fun getPersonalizedView(
        @Path("libraryId") libraryId: String,
        @Query("limit") limit: Int = 10,
        @Query("include") include: String = "rssfeed"
    ): List<Shelf>
}
