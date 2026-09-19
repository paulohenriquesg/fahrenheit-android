package com.paulohenriquesg.fahrenheit.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Authentication endpoints, kept separate from [ApiService] so that the login flow
 * can be exercised without standing up the whole API surface.
 */
interface AuthApi {
    /**
     * Unauthenticated. Reports which sign-in methods the server offers, so the
     * login form can adapt instead of assuming a password always exists.
     */
    @GET("status")
    suspend fun status(): ServerStatus

    @POST("login")
    suspend fun login(
        @Header("x-return-tokens") returnTokens: String,
        @Body request: LoginRequest
    ): LoginResponse

    /**
     * Resolves the user behind a bearer token and returns the login payload.
     * Used for API-key sign-in, which has no username or password to post.
     */
    @POST("api/authorize")
    suspend fun authorize(@Header("Authorization") bearer: String): LoginResponse

    @POST("auth/refresh")
    suspend fun refresh(@Header("x-refresh-token") refreshToken: String): LoginResponse
}
