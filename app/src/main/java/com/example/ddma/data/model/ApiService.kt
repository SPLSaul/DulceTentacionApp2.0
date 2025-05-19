package com.example.ddma.data.model

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiService {
    @GET("api/Pasteles")
    suspend fun getPasteles(): Response<List<PastelesResponse>>

    @POST("api/Users/login")
    @Headers(
        "accept: text/plain",
        "Content-Type: application/json"
    )
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

}