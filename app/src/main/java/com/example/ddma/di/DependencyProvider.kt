package com.example.ddma.di

import android.content.Context
import com.example.ddma.data.api.*
import com.example.ddma.data.model.*
import com.example.ddma.data.model.payment.ConfirmPaymentRequest
import com.example.ddma.data.model.payment.PaymentConfirmationResponse
import com.example.ddma.data.model.payment.PaymentIntentRequest
import com.example.ddma.data.model.payment.PaymentIntentResponse
import com.example.ddma.data.model.payment.PaymentMethodResponse
import com.example.ddma.data.repositories.*
import com.stripe.android.BuildConfig
import com.stripe.android.PaymentConfiguration
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit

object DependencyProvider {
    private lateinit var applicationContext: Context

    // Configuración de Stripe
    private const val STRIPE_PUBLISHABLE_KEY = "pk_test_51QyfjmJrA7WETaGHhtzTuabVrpFdxenPYTktuYO5ByYpKf99BmjOmooflPePuqODG7Rvf0NuIe0EahaSq0oaJcIM00PYF5tDXU"

    // Configuración de Retrofit
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://ddkmaapi-ewcndnazfmfnhkfv.centralus-01.azurewebsites.net/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder().apply {
            cookieJar(JavaNetCookieJar(cookieManager))
            addInterceptor(httpLoggingInterceptor)
            addInterceptor(authInterceptor)
            connectTimeout(30, TimeUnit.SECONDS)
            readTimeout(30, TimeUnit.SECONDS)
            writeTimeout(30, TimeUnit.SECONDS)
        }.build()
    }

    private val cookieManager: CookieManager by lazy {
        CookieManager().apply {
            setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        }
    }

    private val httpLoggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    private val authInterceptor: Interceptor by lazy {
        Interceptor { chain ->
            val request = chain.request().newBuilder().apply {
                addHeader("Accept", "application/json")
                addHeader("Content-Type", "application/json")
                // Agregar headers de autenticación si es necesario
            }.build()
            chain.proceed(request)
        }
    }

    // Inicialización
    fun init(context: Context) {
        applicationContext = context.applicationContext
        PaymentConfiguration.init(context, STRIPE_PUBLISHABLE_KEY)
    }

    // Servicios API
    val carritoApiService: CarritoApiService by lazy {
        retrofit.create(CarritoApiService::class.java)
    }
    val carritoService: CarritoService by lazy {
        retrofit.create(CarritoService::class.java)
    }

    val paymentApiService: PaymentApiService by lazy {
        retrofit.create(PaymentApiService::class.java)
    }

    // Repositorios
    val paymentRepository: PaymentRepository by lazy {
        PaymentRepository(paymentApiService)
    }
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    val pastelesRepository: PastelesRepository by lazy {
        PastelesRepository(apiService)
    }


    // Interfaz para el servicio de pagos
    interface PaymentApiService {
        @POST("api/payments/create-payment-intent")
        suspend fun createPaymentIntent(
            @Body request: PaymentIntentRequest
        ): retrofit2.Response<PaymentIntentResponse>

        @POST("api/payments/confirm-payment")
        suspend fun confirmPayment(
            @Body request: ConfirmPaymentRequest
        ): retrofit2.Response<PaymentConfirmationResponse>

        @GET("api/payments/payment-methods")
        suspend fun getPaymentMethods(
            @Query("userId") userId: Int
        ): retrofit2.Response<List<PaymentMethodResponse>>
    }
}