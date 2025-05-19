package com.example.ddma.data.api

import com.example.ddma.data.model.CarritoItemRequest
import com.example.ddma.data.model.CarritoItemResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface CarritoService {
    @POST("/api/Carritos/items")
    suspend fun agregarItemAlCarrito(
        @Body item: CarritoItemRequest
    ): Response<CarritoItemResponse>
}