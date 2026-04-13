package com.alarmy.lumirise.api.model

import com.google.gson.annotations.SerializedName

/**
 * Response model from snore detection AI endpoint.
 * Contains detection results with confidence scores.
 */
data class SnoreDetectionResponse(
    @SerializedName("is_snore")
    val isSnore: Boolean,
    
    @SerializedName("confidence")
    val confidence: Float,
    
    @SerializedName("snore_type")
    val snoreType: String?,
    
    @SerializedName("intensity")
    val intensity: Float,
    
    @SerializedName("start_time_ms")
    val startTimeMs: Long,
    
    @SerializedName("end_time_ms")
    val endTimeMs: Long,
    
    @SerializedName("analysis_timestamp")
    val analysisTimestamp: Long
)
