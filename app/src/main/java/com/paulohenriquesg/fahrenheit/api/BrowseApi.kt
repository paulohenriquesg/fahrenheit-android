package com.paulohenriquesg.fahrenheit.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Library browsing endpoints, as suspend functions. */
interface BrowseApi {
    @GET("api/libraries/{libraryId}/series")
    suspend fun getLibrarySeries(
        @Path("libraryId") libraryId: String,
        @Query("limit") limit: Int = 1000,
        @Query("minified") minified: Int = 1
    ): SeriesResponse

    @GET("api/libraries/{libraryId}/collections")
    suspend fun getLibraryCollections(
        @Path("libraryId") libraryId: String,
        @Query("limit") limit: Int = 1000,
        @Query("minified") minified: Int = 1
    ): CollectionsResponse

    @GET("api/libraries/{libraryId}/authors")
    suspend fun getLibraryAuthors(
        @Path("libraryId") libraryId: String,
        @Query("limit") limit: Int = 1000,
        @Query("minified") minified: Int = 1
    ): AuthorsResponse

    @GET("api/me/listening-stats")
    suspend fun getListeningStats(): ListeningStatsResponse

    @GET("api/libraries/{libraryId}/recent-episodes")
    suspend fun getRecentEpisodes(
        @Path("libraryId") libraryId: String,
        @Query("limit") limit: Int = 50
    ): RecentEpisodesResponse

    @GET("api/authors/{authorId}")
    suspend fun getAuthor(
        @Path("authorId") authorId: String,
        @Query("include") include: String = "items"
    ): AuthorDetailResponse

    @GET("api/libraries/{libraryId}/search")
    suspend fun searchLibraryItems(
        @Path("libraryId") libraryId: String,
        @Query("q") query: String,
        @Query("limit") limit: Int = 10
    ): SearchLibraryItemsResponse
}
