package com.alarmy.lumirise.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.alarmy.lumirise.R

object NotificationHelper {
    
    const val CHANNEL_ALARM = "alarm_channel"
    const val CHANNEL_SLEEP = "sleep_tracking_channel"
    const val CHANNEL_INFO = "info_channel"
    
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            
            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM,
                "Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm notifications for wake-up"
                enableVibration(true)
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            
            val sleepChannel = NotificationChannel(
                CHANNEL_SLEEP,
                "Sleep Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Sleep tracking notifications"
                setShowBadge(false)
            }
            
            val infoChannel = NotificationChannel(
                CHANNEL_INFO,
                "Information",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General information notifications"
            }
            
            notificationManager.createNotificationChannels(
                listOf(alarmChannel, sleepChannel, infoChannel)
            )
        }
    }
    
    fun showNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        content: String,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        ongoing: Boolean = false
    ) {
        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_alarm)
            .setPriority(priority)
            .setOngoing(ongoing)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
        }
        
        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
    
    fun cancelNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}
