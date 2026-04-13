package com.alarmy.lumirise.mission

import com.alarmy.lumirise.model.Alarm

object MissionFactory {
    fun createMission(alarm: Alarm): Mission? {
        val missionType = MissionType.fromString(alarm.missionType)
        
        return when (missionType) {
            MissionType.NONE -> null
            MissionType.MATH -> createMathMission(alarm)
            MissionType.SHAKE -> createShakeMission(alarm)
            MissionType.PHOTO -> createPhotoMission(alarm)
        }
    }
    
    private fun createMathMission(alarm: Alarm): Mission {
        val difficulty = MissionDifficulty.fromString(alarm.missionDifficulty)
        return MathMissionFactory.create(alarm, difficulty)
    }
    
    private fun createShakeMission(alarm: Alarm): Mission {
        return ShakeMissionFactory.create(alarm, alarm.shakeCount)
    }
    
    private fun createPhotoMission(alarm: Alarm): Mission {
        return PhotoMissionFactory.create(alarm)
    }
}

private object MathMissionFactory {
    fun create(alarm: Alarm, difficulty: MissionDifficulty): Mission {
        return MathMission(alarm, difficulty)
    }
}

private object ShakeMissionFactory {
    fun create(alarm: Alarm, shakeCount: Int): Mission {
        return ShakeMission(alarm, shakeCount)
    }
}

private object PhotoMissionFactory {
    fun create(alarm: Alarm): Mission {
        return PhotoMission(alarm)
    }
}

private class MathMission(
    override val alarm: Alarm,
    private val difficulty: MissionDifficulty
) : Mission {
    override val missionType = MissionType.MATH
    private var completed = false
    private var problemsSolved = 0
    private val problemsRequired: Int

    init {
        problemsRequired = when (difficulty) {
            MissionDifficulty.EASY -> 3
            MissionDifficulty.MEDIUM -> 5
            MissionDifficulty.HARD -> 7
        }
    }

    override fun startMission() {}
    
    override fun isMissionComplete(): Boolean = completed
    
    override fun onMissionResult(): MissionResult {
        return MissionResult(
            success = completed,
            missionType = missionType,
            attempts = problemsSolved
        )
    }
    
    override fun cancelMission() {}
}

private class ShakeMission(
    override val alarm: Alarm,
    private val targetShakeCount: Int
) : Mission {
    override val missionType = MissionType.SHAKE
    private var currentShakeCount = 0
    private var completed = false

    override fun startMission() {}
    
    override fun isMissionComplete(): Boolean = completed
    
    override fun onMissionResult(): MissionResult {
        return MissionResult(
            success = completed,
            missionType = missionType,
            attempts = currentShakeCount
        )
    }
    
    override fun cancelMission() {}
}

private class PhotoMission(
    override val alarm: Alarm
) : Mission {
    override val missionType = MissionType.PHOTO
    private var completed = false

    override fun startMission() {}
    
    override fun isMissionComplete(): Boolean = completed
    
    override fun onMissionResult(): MissionResult {
        return MissionResult(
            success = completed,
            missionType = missionType
        )
    }
    
    override fun cancelMission() {}
}
