package com.example.ddma.data.repositories

import com.example.ddma.data.model.payment.*
import com.example.ddma.di.DependencyProvider
import javax.inject.Inject

class PaymentRepository @Inject constructor(
    private val apiService: DependencyProvider.PaymentApiService
) {
    suspend fun createPaymentIntent(
        userId: Int,
        cartId: Int,
        amount: Double
    ): Result<PaymentIntentResponse> {
        return try {
            val amountInCents = (amount * 100).toLong()
            val response = apiService.createPaymentIntent(
                PaymentIntentRequest(
                    userId = userId,
                    amount = amountInCents,
                    cartId = cartId
                )
            )

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun confirmPayment(
        paymentIntentId: String,
        paymentMethodId: String,
        userId: Int,
        cartId: Int
    ): Result<PaymentConfirmationResponse> {
        return try {
            val response = apiService.confirmPayment(
                ConfirmPaymentRequest(
                    paymentIntentId = paymentIntentId,
                    paymentMethodId = paymentMethodId,
                    userId = userId,
                    cartId = cartId
                )
            )

            if (response.isSuccessful) {
                response.body()?.let {
                    if (it.success) {
                        Result.success(it)
                    } else {
                        Result.failure(Exception("Payment confirmation failed"))
                    }
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPaymentMethods(userId: Int): Result<List<PaymentMethodResponse>> {
        return try {
            val response = apiService.getPaymentMethods(userId)

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}