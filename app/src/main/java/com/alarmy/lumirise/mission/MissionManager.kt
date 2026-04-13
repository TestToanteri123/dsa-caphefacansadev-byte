package com.alarmy.lumirise.mission

import com.alarmy.lumirise.model.Alarm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MissionManager private constructor() {
    
    private var activeMission: Mission? = null
    private var callback: MissionCallback? = null
    private var startTime: Long = 0
    
    private val _missionState = MutableStateFlow<MissionState>(MissionState.Idle)
    val missionState: StateFlow<MissionState> = _missionState.asStateFlow()
    
    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()
    
    private val _hint = MutableStateFlow("")
    val hint: StateFlow<String> = _hint.asStateFlow()
    
    fun attachCallback(callback: MissionCallback) {
        this.callback = callback
    }
    
    fun detachCallback() {
        this.callback = null
    }
    
    fun startMission(alarm: Alarm) {
        val mission = MissionFactory.createMission(alarm) ?: run {
            _missionState.value = MissionState.NoMission
            return
        }
        
        activeMission = mission
        startTime = System.currentTimeMillis()
        _missionState.value = MissionState.InProgress(mission.missionType)
        _progress.value = 0
        callback?.onMissionStarted(mission.missionType)
        mission.startMission()
    }
    
    fun updateProgress(progressValue: Int, message: String = "") {
        _progress.value = progressValue
        if (message.isNotEmpty()) {
            callback?.onMissionProgress(progressValue, message)
        }
    }
    
    fun setHint(hintText: String) {
        _hint.value = hintText
        callback?.onMissionHintRequested()
    }
    
    fun completeMission() {
        val mission = activeMission ?: return
        val result = mission.onMissionResult().copy(
            success = true,
            durationMs = System.currentTimeMillis() - startTime
        )
        _missionState.value = MissionState.Completed(result)
        callback?.onMissionComplete(result)
        cleanup()
    }
    
    fun failMission(attempts: Int = 0) {
        val mission = activeMission ?: return
        val result = MissionResult(
            success = false,
            missionType = mission.missionType,
            attempts = attempts,
            durationMs = System.currentTimeMillis() - startTime
        )
        _missionState.value = MissionState.Failed(result)
        cleanup()
    }
    
    fun cancelMission() {
        activeMission?.cancelMission()
        _missionState.value = MissionState.Cancelled
        callback?.onMissionCancelled()
        cleanup()
    }
    
    fun getCurrentMission(): Mission? = activeMission
    
    fun isMissionActive(): Boolean {
        return activeMission != null && _missionState.value is MissionState.InProgress
    }
    
    private fun cleanup() {
        activeMission = null
        callback = null
    }
    
    companion object {
        @Volatile
        private var instance: MissionManager? = null
        
        fun getInstance(): MissionManager {
            return instance ?: synchronized(this) {
                instance ?: MissionManager().also { instance = it }
            }
        }
    }
}

sealed class MissionState {
    data object Idle : MissionState()
    data object NoMission : MissionState()
    data class InProgress(val missionType: MissionType) : MissionState()
    data class Completed(val result: MissionResult) : MissionState()
    data class Failed(val result: MissionResult) : MissionState()
    data object Cancelled : MissionState()
}
