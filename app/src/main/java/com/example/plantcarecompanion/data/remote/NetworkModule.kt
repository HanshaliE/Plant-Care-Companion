package com.example.plantcarecompanion.data.remote

import com.example.plantcarecompanion.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

class ApiKeyInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val url = original.url.newBuilder().addQueryParameter("key", apiKey).build()
        return chain.proceed(original.newBuilder().url(url).build())
    }
}

object NetworkModule {

    private const val BASE_URL = "https://perenual.com/api/v2/"

    val apiKey: String = BuildConfig.PERENUAL_API_KEY
    val hasApiKey: Boolean get() = apiKey.isNotBlank()

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(ApiKeyInterceptor(apiKey))
            .build()
    }

    val perenualApi: PerenualApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(PerenualApi::class.java)
    }
}