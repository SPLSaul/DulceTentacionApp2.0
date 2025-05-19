package com.example.ddma.di

import android.content.Context
import com.example.ddma.data.api.CarritoApiService
import com.example.ddma.data.model.ApiService
import com.example.ddma.data.api.CarritoService
import com.example.ddma.data.model.PastelesRepository
import com.example.ddma.data.model.payment.ConfirmPaymentRequest
import com.example.ddma.data.model.payment.PaymentConfirmationResponse
import com.example.ddma.data.model.payment.PaymentIntentRequest
import com.example.ddma.data.model.payment.PaymentIntentResponse
import com.example.ddma.data.model.payment.PaymentMethodResponse
import com.example.ddma.data.repositories.PaymentRepository
import com.stripe.android.PaymentConfiguration
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import okhttp3.JavaNetCookieJar
import okhttp3.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.net.CookieManager
import java.net.CookiePolicy

object DependencyProvider {
    private var appContext: Context? = null
    private const val STRIPE_PUBLISHABLE_KEY = "pk_test_51QyfjmJrA7WETaGHhtzTuabVrpFdxenPYTktuYO5ByYpKf99BmjOmooflPePuqODG7Rvf0NuIe0EahaSq0oaJcIM00PYF5tDXU" // Reemplaza con tu clave de Stripe

    fun initialize(context: Context) {
        appContext = context.applicationContext
        // Configuración inicial de Stripe
        PaymentConfiguration.init(
            context,
            STRIPE_PUBLISHABLE_KEY
        )
    }

    private val retrofit by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val cookieManager = CookieManager().apply {
            setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        }

        val client = OkHttpClient.Builder()
            .cookieJar(JavaNetCookieJar(cookieManager))
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://ddkmaapi-ewcndnazfmfnhkfv.centralus-01.azurewebsites.net/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Servicios existentes
    fun provideCarritoService(): CarritoService {
        return retrofit.create(CarritoService::class.java)
    }

    fun providePastelesRepository(): PastelesRepository {
        return PastelesRepository(retrofit.create(ApiService::class.java))
    }

    val carritoService: CarritoService by lazy {
        retrofit.create(CarritoService::class.java)
    }

    val carritoApiService: CarritoApiService by lazy {
        retrofit.create(CarritoApiService::class.java)
    }

    // Nuevos servicios para Stripe
    val paymentApiService: PaymentApiService by lazy {
        retrofit.create(PaymentApiService::class.java)
    }

    val paymentRepository: PaymentRepository by lazy {
        PaymentRepository(paymentApiService)
    }

    // Interfaz para el servicio de pagos
    interface PaymentApiService {
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
}