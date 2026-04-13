package com.alarmy.lumirise.api.model

import com.google.gson.annotations.SerializedName

/**
 * Response model from sleep analysis AI endpoint.
 * Contains AI-generated sleep quality insights and recommendations.
 */
data class SleepAnalysisResponse(
    @SerializedName("session_id")
    val sessionId: String,
    
    @SerializedName("quality_score")
    val qualityScore: Int,
    
    @SerializedName("quality_label")
    val qualityLabel: String,
    
    @SerializedName("insights")
    val insights: List<SleepInsight>,
    
    @SerializedName("recommendations")
    val recommendations: List<String>,
    
    @SerializedName("sleep_stages")
    val sleepStages: SleepStages? = null,
    
    @SerializedName("analysis_timestamp")
    val analysisTimestamp: Long
)

data class SleepInsight(
    @SerializedName("type")
    val type: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("severity")
    val severity: String
)

data class SleepStages(
    @SerializedName("light_sleep_minutes")
    val lightSleepMinutes: Int,
    
    @SerializedName("deep_sleep_minutes")
    val deepSleepMinutes: Int,
    
    @SerializedName("rem_minutes")
    val remMinutes: Int,
    
    @SerializedName("awake_minutes")
    val awakeMinutes: Int
)
