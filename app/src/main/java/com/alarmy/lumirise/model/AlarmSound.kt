package com.alarmy.lumirise.model

/**
 * Represents an alarm sound with its metadata.
 *
 * @property id Unique identifier for the sound
 * @property name Display name of the sound
 * @property uri URI string to the sound resource (empty for built-in sounds)
 * @property isBuiltIn Whether this is a built-in system sound
 */
data class AlarmSound(
    val id: String,
    val name: String,
    val uri: String,
    val isBuiltIn: Boolean = true
)
