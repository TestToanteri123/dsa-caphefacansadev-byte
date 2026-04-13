package com.alarmy.lumirise.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val repeatDays: String = "", // Comma-separated: "MON,TUE,WED" or empty for once
    val missionType: String = "NONE", // NONE, MATH, SHAKE, PHOTO
    val missionDifficulty: String = "EASY", // For MATH: EASY, MEDIUM, HARD
    val shakeCount: Int = 30, // For SHAKE mission
    val soundUri: String = "", // Empty = default sound
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val snoozeCount: Int = 0 // Tracks snooze count for anti-snooze feature
)
