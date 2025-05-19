package com.example.ddma.di

import android.content.Context
import com.example.ddma.data.api.CarritoApiService
import com.example.ddma.data.model.ApiService
import com.example.ddma.data.api.CarritoService
import com.example.ddma.data.model.PastelesRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import okhttp3.JavaNetCookieJar
import java.net.CookieManager
import java.net.CookiePolicy

object DependencyProvider {
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
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
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl("https://ddkmaapi-ewcndnazfmfnhkfv.centralus-01.azurewebsites.net/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

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
}