package com.paulohenriquesg.fahrenheit.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>
}