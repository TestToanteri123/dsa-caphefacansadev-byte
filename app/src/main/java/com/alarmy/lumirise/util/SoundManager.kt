package com.alarmy.lumirise.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import com.alarmy.lumirise.model.AlarmSound

object SoundManager {
    
    private var mediaPlayer: MediaPlayer? = null
    
    val builtInSounds: List<AlarmSound> = listOf(
        AlarmSound("default", "Default", "", true),
        AlarmSound("gentle", "Gentle Wake", "", true),
        AlarmSound("powerful", "Powerful", "", true),
        AlarmSound("beeps", "Morning Beeps", "", true),
        AlarmSound("melody", "Sweet Melody", "", true)
    )
    
    fun playAlarmSound(context: Context, soundUri: String = "") {
        stopSound()
        
        mediaPlayer = MediaPlayer().apply {
            try {
                val uri = if (soundUri.isNotEmpty()) {
                    Uri.parse(soundUri)
                } else {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
                
                setDataSource(context, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun stopSound() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }
    
    fun pauseSound() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
    }
    
    fun resumeSound() {
        if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
            mediaPlayer?.start()
        }
    }
    
    fun setVolume(volume: Float) {
        mediaPlayer?.setVolume(volume, volume)
    }
    
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true
    
    fun getAllSounds(context: Context): List<AlarmSound> {
        val sounds = builtInSounds.toMutableList()
        
        try {
            val ringtoneManager = RingtoneManager(context)
            ringtoneManager.setType(RingtoneManager.TYPE_ALARM)
            
            val cursor = ringtoneManager.cursor
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val uri = ringtoneManager.getRingtoneUri(cursor.position)
                    val ringtone = ringtoneManager.getRingtone(cursor.position)
                    val name = ringtone?.getTitle(context)
                    if (uri != null && name != null) {
                        sounds.add(AlarmSound(
                            id = uri.toString(),
                            name = name,
                            uri = uri.toString(),
                            isBuiltIn = false
                        ))
                    }
                } while (cursor.moveToNext())
            }
            cursor?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return sounds
    }
}
