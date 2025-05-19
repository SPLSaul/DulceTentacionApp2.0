package com.example.ddma.data.api

import com.example.ddma.data.model.CarritoDto
import com.example.ddma.data.model.CarritoItemDto
import com.example.ddma.data.model.UpdateCartItemRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface CarritoApiService {
    @GET("api/Carritos")
    suspend fun getUserCart(@Query("userId") userId: Int): Response<CarritoDto>

    @PUT("api/Carritos/items/{itemId}")
    suspend fun updateCartItem(
        @Path("itemId") itemId: Int,
        @Body request: UpdateCartItemRequest
    ): Response<CarritoItemDto>

    @DELETE("api/Carritos/items/{itemId}")
    suspend fun removeFromCart(@Path("itemId") itemId: Int): Response<Unit>

    @DELETE("api/Carritos/clear")
    suspend fun clearCart(): Response<Unit>
}