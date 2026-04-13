package com.alarmy.lumirise.mission

/**
 * Enum representing the different types of wake-up missions available in LumiRise.
 * Each mission requires the user to complete a task before the alarm can be dismissed.
 */
enum class MissionType {
    /** No mission - user can dismiss alarm directly */
    NONE,
    
    /** Math mission - solve arithmetic problems */
    MATH,
    
    /** Shake mission - shake device to dismiss */
    SHAKE,
    
    /** Photo mission - take a photo matching a target */
    PHOTO;

    companion object {
        /**
         * Parse mission type from string (e.g., from database)
         * @param value String value from alarm.missionType
         * @return MissionType matching the string, or NONE if invalid
         */
        fun fromString(value: String): MissionType {
            return entries.find { 
                it.name.equals(value, ignoreCase = true) 
            } ?: NONE
        }
    }
}

/**
 * Difficulty levels for math missions.
 */
enum class MissionDifficulty {
    EASY,
    MEDIUM,
    HARD;

    companion object {
        fun fromString(value: String): MissionDifficulty {
            return entries.find { 
                it.name.equals(value, ignoreCase = true) 
            } ?: EASY
        }
    }
}

/**
 * Photo mission mode.
 */
enum class PhotoMissionMode {
    /** User is setting up their target photo */
    SETUP,
    
    /** User needs to match the target photo */
    VERIFY
}
