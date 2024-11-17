package com.paulohenriquesg.fahrenheit.api

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @GET("api/libraries")
    fun getLibraries(): Call<LibrariesResponse>

    @GET("api/libraries/{libraryId}/items")
    fun getLibraryItems(
        @Path("libraryId") libraryId: String,
        @Query("sort") sort: String = "media.metadata.title",
        @Query("limit") limit: Int? = null,
        @Query("page") page: Int? = null,
        @Query("desc") desc: Boolean? = null,
        @Query("include") include: String = "rssfeed,numEpisodesIncomplete"
    ): Call<LibraryItemsResponse>

    @GET("api/items/{itemId}")
    fun getLibraryItem(
        @Path("itemId") itemId: String,
        @Query("expanded") expanded: Int = 1,
        @Query("include") include: String = "progress,rssfeed,authors,downloads"
    ): Call<LibraryItemResponse>

    @GET("api/items/{itemId}/cover")
    fun getItemCover(
        @Path("itemId") itemId: String,
        @Query("format") format: String = "jpg"
    ): Call<ResponseBody>

    @POST("api/items/{libraryItemId}/play/{episodeId}")
    fun playLibraryItem(
        @Path("libraryItemId") libraryItemId: String,
        @Path("episodeId") episodeId: String? = null,
        @Body request: PlayLibraryItemRequest
    ): Call<PlayLibraryItemResponse>
}