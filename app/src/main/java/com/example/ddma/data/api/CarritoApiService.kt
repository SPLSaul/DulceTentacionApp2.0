package com.example.ddma.data.api

import com.example.ddma.data.model.CarritoDto
import com.example.ddma.data.model.CarritoItemDto
import com.example.ddma.data.model.UpdateCartItemRequest
import com.example.ddma.data.model.payment.*
import retrofit2.Response
import retrofit2.http.*

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

    // Nuevos endpoints para Stripe
    @POST("api/payments/create-payment-intent")
    suspend fun createPaymentIntent(
        @Body request: PaymentIntentRequest
    ): Response<PaymentIntentResponse>

    @POST("api/payments/confirm-payment")
    suspend fun confirmPayment(
        @Body request: ConfirmPaymentRequest
    ): Response<PaymentConfirmationResponse>

    @GET("api/payments/payment-methods")
    suspend fun getPaymentMethods(
        @Query("userId") userId: Int
    ): Response<List<PaymentMethodResponse>>
}