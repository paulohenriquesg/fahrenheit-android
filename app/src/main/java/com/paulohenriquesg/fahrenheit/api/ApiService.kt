package com.paulohenriquesg.fahrenheit.api

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @GET("api/items/{itemId}")
    fun getLibraryItem(
        @Path("itemId") itemId: String,
        @Query("expanded") expanded: Int = 1,
        @Query("include", encoded = true) include: String = "progress,rssfeed,authors,downloads"
    ): Call<LibraryItemResponse>

    @GET("api/items/{itemId}/cover")
    fun getItemCover(
        @Path("itemId") itemId: String,
        @Query("format") format: String = "jpg"
    ): Call<ResponseBody>

    @GET("/book_placeholder.jpg")
    fun getDefaultItemCover(): Call<ResponseBody>

    @POST("api/items/{libraryItemId}/play/{episodeId}")
    fun playLibraryItem(
        @Path("libraryItemId") libraryItemId: String,
        @Path("episodeId") episodeId: String? = null,
        @Body request: PlayLibraryItemRequest
    ): Call<PlayLibraryItemResponse>

    @PATCH("api/me/progress/{libraryItemId}/{episodeId}")
    fun userCreateOrUpdateMediaProgress(
        @Path("libraryItemId") libraryItemId: String,
        @Path("episodeId") episodeId: String,
        @Body request: MediaProgressRequest
    ): Call<Void>

    @GET("api/me/progress/{libraryItemId}/{episodeId}")
    fun userGetMediaProgress(
        @Path("libraryItemId") libraryItemId: String,
        @Path("episodeId") episodeId: String
    ): Call<MediaProgressResponse>

    @PATCH("api/me/progress/{libraryItemId}")
    fun userCreateOrUpdateMediaProgress(
        @Path("libraryItemId") libraryItemId: String,
        @Body request: MediaProgressRequest
    ): Call<Void>

    @GET("api/me/progress/{libraryItemId}")
    fun userGetMediaProgress(
        @Path("libraryItemId") libraryItemId: String,
    ): Call<MediaProgressResponse>

    @GET("api/libraries/{libraryId}/series/{seriesId}")
    fun getSeries(
        @Path("libraryId") libraryId: String,
        @Path("seriesId") seriesId: String
    ): Call<Series>

}