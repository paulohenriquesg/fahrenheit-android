package com.paulohenriquesg.fahrenheit.auth

import retrofit2.Call
import com.paulohenriquesg.fahrenheit.api.LoginResponse
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Blocking twin of [AuthApi.refresh], for use from OkHttp's Authenticator, which
 * runs on a worker thread that is already blocked waiting on the original request.
 */
interface AuthRefreshApi {
    @POST("auth/refresh")
    fun refresh(@Header("x-refresh-token") refreshToken: String): Call<LoginResponse>
}
