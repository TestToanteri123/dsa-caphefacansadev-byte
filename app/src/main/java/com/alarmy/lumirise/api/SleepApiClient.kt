package com.alarmy.lumirise.api

import com.alarmy.lumirise.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
}

object SleepApiClient {
    
    private const val TIMEOUT_SECONDS = 30L
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.AI_WORKER_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val service: SleepApiService = retrofit.create(SleepApiService::class.java)
    
    suspend fun analyzeSleep(request: com.alarmy.lumirise.api.model.SleepAnalysisRequest): ApiResult<com.alarmy.lumirise.api.model.SleepAnalysisResponse> {
        return try {
            val response = service.analyzeSleep(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    ApiResult.Success(it)
                } ?: ApiResult.Error("Empty response body", response.code())
            } else {
                ApiResult.Error(response.message(), response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown network error")
        }
    }
    
    suspend fun detectSnore(request: com.alarmy.lumirise.api.model.SnoreDetectionRequest): ApiResult<com.alarmy.lumirise.api.model.SnoreDetectionResponse> {
        return try {
            val response = service.detectSnore(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    ApiResult.Success(it)
                } ?: ApiResult.Error("Empty response body", response.code())
            } else {
                ApiResult.Error(response.message(), response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown network error")
        }
    }
}
