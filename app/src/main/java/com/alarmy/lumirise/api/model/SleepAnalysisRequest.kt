package com.alarmy.lumirise.api.model

import com.google.gson.annotations.SerializedName

/**
 * Request model for sleep analysis AI endpoint.
 * Contains sleep session data to be analyzed by the AI worker.
 */
data class SleepAnalysisRequest(
    @SerializedName("user_id")
    val userId: String,
    
    @SerializedName("session_id")
    val sessionId: String,
    
    @SerializedName("sleep_start_time")
    val sleepStartTime: Long,
    
    @SerializedName("sleep_end_time")
    val sleepEndTime: Long,
    
    @SerializedName("total_snore_count")
    val totalSnoreCount: Int,
    
    @SerializedName("snore_percentage")
    val snorePercentage: Float,
    
    @SerializedName("audio_features")
    val audioFeatures: AudioFeatures? = null,
    
    @SerializedName("additional_notes")
    val additionalNotes: String? = null
)

data class AudioFeatures(
    @SerializedName("average_volume_db")
    val averageVolumeDb: Float,
    
    @SerializedName("snore_intensity_distribution")
    val snoreIntensityDistribution: Map<String, Int> = emptyMap(),
    
    @SerializedName("peak_snore_times")
    val peakSnoreTimes: List<Long> = emptyList()
)
