package com.alarmy.lumirise.mission

/**
 * Callback interface for mission lifecycle events.
 * Implemented by activities/fragments that host missions.
 */
interface MissionCallback {
    /**
     * Called when a mission starts
     * @param missionType The type of mission that started
     */
    fun onMissionStarted(missionType: MissionType)
    
    /**
     * Called when mission progress updates
     * @param progress Progress percentage (0-100)
     * @param message Optional status message
     */
    fun onMissionProgress(progress: Int, message: String)
    
    /**
     * Called when mission completes successfully
     * @param result The mission result containing completion details
     */
    fun onMissionComplete(result: MissionResult)
    
    /**
     * Called when mission is cancelled
     */
    fun onMissionCancelled()
    
    /**
     * Called when user requests a hint
     * @return Hint text to display
     */
    fun onMissionHintRequested(): String
}
