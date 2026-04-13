package com.alarmy.lumirise.mission

import com.alarmy.lumirise.model.Alarm

interface Mission {
    val missionType: MissionType
    val alarm: Alarm
    
    fun startMission()
    
    fun isMissionComplete(): Boolean
    
    fun onMissionResult(): MissionResult
    
    fun cancelMission()
}

data class MissionResult(
    val success: Boolean,
    val missionType: MissionType,
    val completedAt: Long = System.currentTimeMillis(),
    val attempts: Int = 0,
    val durationMs: Long = 0
)
