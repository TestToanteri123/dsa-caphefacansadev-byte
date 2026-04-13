package com.alarmy.lumirise.api

import com.alarmy.lumirise.api.model.SleepAnalysisRequest
import com.alarmy.lumirise.api.model.SleepAnalysisResponse
import com.alarmy.lumirise.api.model.SnoreDetectionRequest
import com.alarmy.lumirise.api.model.SnoreDetectionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface SleepApiService {
    
    @POST("sleep/analyze")
    suspend fun analyzeSleep(
        @Body request: SleepAnalysisRequest
    ): Response<SleepAnalysisResponse>
    
    @POST("snore/detect")
    suspend fun detectSnore(
        @Body request: SnoreDetectionRequest
    ): Response<SnoreDetectionResponse>
}
