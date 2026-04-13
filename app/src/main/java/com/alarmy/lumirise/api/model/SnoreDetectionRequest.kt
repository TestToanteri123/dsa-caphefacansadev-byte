package com.alarmy.lumirise.api.model

import com.google.gson.annotations.SerializedName

/**
 * Request model for real-time snore detection AI endpoint.
 * Contains audio data to be analyzed for snoring patterns.
 */
data class SnoreDetectionRequest(
    @SerializedName("audio_data_base64")
    val audioDataBase64: String,
    
    @SerializedName("sample_rate")
    val sampleRate: Int,
    
    @SerializedName("duration_ms")
    val durationMs: Int,
    
    @SerializedName("timestamp")
    val timestamp: Long,
    
    @SerializedName("session_id")
    val sessionId: String,
    
    @SerializedName("confidence_threshold")
    val confidenceThreshold: Float = 0.7f
)
