package com.alarmy.lumirise.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.alarmy.lumirise.R
import com.alarmy.lumirise.ui.AlarmRingActivity

class WakeUpCheckService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private var alarmId: Long = -1
    private var checkStartTime: Long = 0
    private var isCheckActive = false

    companion object {
        const val CHANNEL_ID = "wake_check_channel"
        const val NOTIFICATION_ID = 2
        const val ACTION_CHECK_RESPONSE = "com.alarmy.lumirise.ACTION_WAKE_CHECK_RESPONSE"
        const val EXTRA_ALARM_ID = "alarm_id"

        private const val CHECK_INTERVAL_MS = 5 * 60 * 1000L
        private const val MAX_CHECK_DURATION_MS = 15 * 60 * 1000L

        private val WAKE_QUESTIONS = listOf(
            "Are you awake?",
            "Still awake?",
            "Wake up!",
            "Are you up?",
            "Still sleeping?"
        )

        fun start(context: Context, alarmId: Long) {
            val intent = Intent(context, WakeUpCheckService::class.java).apply {
                putExtra(EXTRA_ALARM_ID, alarmId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WakeUpCheckService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CHECK_RESPONSE) {
            handleUserResponse()
            return START_STICKY
        }

        alarmId = intent?.getLongExtra(EXTRA_ALARM_ID, -1) ?: -1
        checkStartTime = System.currentTimeMillis()
        isCheckActive = true

        val notification = createNotification(getNextQuestion())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        scheduleNextCheck()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopPeriodicChecks()
        releaseWakeLock()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Wake Check",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Wake-up verification checks"
                setBypassDnd(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(question: String): Notification {
        val dialogIntent = Intent(this, AlarmRingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(AlarmRingActivity.EXTRA_ALARM_ID, alarmId)
            putExtra("wake_check_dialog", true)
        }
        
        val dialogPendingIntent = PendingIntent.getActivity(
            this,
            0,
            dialogIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val responseIntent = Intent(this, WakeUpCheckService::class.java).apply {
            action = ACTION_CHECK_RESPONSE
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val responsePendingIntent = PendingIntent.getService(
            this,
            1,
            responseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val elapsedTime = System.currentTimeMillis() - checkStartTime
        val remainingMinutes = ((MAX_CHECK_DURATION_MS - elapsedTime) / 60000).toInt()
        val contentText = if (remainingMinutes > 0) {
            "$question Tap to confirm. Checking for $remainingMinutes more min."
        } else {
            question
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.wake_check_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(dialogPendingIntent, true)
            .addAction(R.drawable.ic_alarm, getString(R.string.im_awake), responsePendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }

    private fun getNextQuestion(): String {
        val checkNumber = ((System.currentTimeMillis() - checkStartTime) / CHECK_INTERVAL_MS).toInt()
        return WAKE_QUESTIONS[checkNumber % WAKE_QUESTIONS.size]
    }

    private fun scheduleNextCheck() {
        handler.removeCallbacksAndMessages(null)
        
        handler.postDelayed({
            if (!isCheckActive) return@postDelayed
            
            val elapsed = System.currentTimeMillis() - checkStartTime
            
            if (elapsed >= MAX_CHECK_DURATION_MS) {
                ringAlarmAgain()
                stopSelf()
                return@postDelayed
            }
            
            val notification = createNotification(getNextQuestion())
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, notification)
            
            scheduleNextCheck()
            
        }, CHECK_INTERVAL_MS)
    }

    private fun stopPeriodicChecks() {
        isCheckActive = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun handleUserResponse() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        
        val successNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.wake_check_confirmed))
            .setContentText(getString(R.string.wake_check_confirmed_message))
            .setSmallIcon(R.drawable.ic_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, successNotification)
        
        handler.postDelayed({
            stopSelf()
        }, 2000)
    }

    private fun ringAlarmAgain() {
        AlarmRingService.start(this, alarmId)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "LumiRise:WakeCheckWakeLock"
        ).apply {
            acquire(MAX_CHECK_DURATION_MS + 60000)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }
}
