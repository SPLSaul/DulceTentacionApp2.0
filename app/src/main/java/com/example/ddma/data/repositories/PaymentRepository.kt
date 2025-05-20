package com.example.ddma.data.repositories

import com.example.ddma.data.model.payment.*
import com.example.ddma.di.DependencyProvider
import retrofit2.Response
import java.lang.Exception

class PaymentRepository(
    private val apiService: DependencyProvider.PaymentApiService
) {
    suspend fun createPaymentIntent(request: PaymentIntentRequest): Result<PaymentIntentResponse> {
        return try {
            val response = apiService.createPaymentIntent(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de red: ${e.message}"))
        }
    }

    suspend fun confirmPayment(request: ConfirmPaymentRequest): Result<PaymentConfirmationResponse> {
        return try {
            val response = apiService.confirmPayment(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    if (it.success) {
                        Result.success(it)
                    } else {
                        Result.failure(Exception(it.nextAction ?: "Confirmación fallida"))
                    }
                } ?: Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de red: ${e.message}"))
        }
    }

    suspend fun getPaymentMethods(userId: Int): Result<List<PaymentMethodResponse>> {
        return try {
            val response = apiService.getPaymentMethods(userId)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de red: ${e.message}"))
        }
    }
}