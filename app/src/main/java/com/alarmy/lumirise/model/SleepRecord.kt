package com.alarmy.lumirise.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_records")
data class SleepRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long = 0,
    val qualityScore: Int = 0,
    val snorePercentage: Float = 0f,
    val totalSnoreCount: Int = 0,
    val isComplete: Boolean = false
)
