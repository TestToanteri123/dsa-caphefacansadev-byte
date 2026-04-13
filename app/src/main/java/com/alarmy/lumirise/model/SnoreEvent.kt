package com.alarmy.lumirise.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "snore_events",
    foreignKeys = [
        ForeignKey(
            entity = SleepRecord::class,
            parentColumns = ["id"],
            childColumns = ["sleepRecordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sleepRecordId")]
)
data class SnoreEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sleepRecordId: Long,
    val timestamp: Long,
    val durationMs: Int = 0,
    val confidence: Float = 0f
)
