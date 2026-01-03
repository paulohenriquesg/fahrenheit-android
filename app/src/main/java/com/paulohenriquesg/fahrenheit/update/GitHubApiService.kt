package com.paulohenriquesg.fahrenheit.update

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface GitHubApiService {
    @GET("repos/{owner}/{repo}/releases/latest")
    fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Call<GitHubRelease>
}
