package com.alarmy.lumirise.ui

import android.app.KeyguardManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.alarmy.lumirise.R
import com.alarmy.lumirise.data.AlarmRepository
import com.alarmy.lumirise.databinding.ActivityAlarmRingBinding
import com.alarmy.lumirise.mission.MathMissionFragment
import com.alarmy.lumirise.mission.MissionCallback
import com.alarmy.lumirise.mission.MissionManager
import com.alarmy.lumirise.mission.MissionResult
import com.alarmy.lumirise.mission.MissionType
import com.alarmy.lumirise.mission.PhotoMissionFragment
import com.alarmy.lumirise.mission.ShakeMissionFragment
import com.alarmy.lumirise.model.Alarm
import com.alarmy.lumirise.service.AlarmRingService
import com.alarmy.lumirise.service.WakeUpCheckService
import com.alarmy.lumirise.util.AlarmScheduler
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AlarmRingActivity : AppCompatActivity(), MissionCallback {
    
    private lateinit var binding: ActivityAlarmRingBinding
    private var alarmId: Long = -1
    private var currentAlarm: Alarm? = null
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var snoozeCount = 0
    private val missionManager = MissionManager.getInstance()
    
    private val handler = Handler(Looper.getMainLooper())
    private var dismissedOverlay: View? = null
    private var snoozeCountdownRunnable: Runnable? = null
    private var snoozedTimeMs: Long = 0
    
    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        private val SNOOZE_DURATIONS = listOf(9, 6, 3)
        private const val MAX_SNOOZES = 3
        private const val AUTO_DISMISS_DELAY_MS = 3000L
        private const val BASE_VOLUME = 0.5f
        private const val VOLUME_INCREMENT = 0.15f
        private const val MAX_VOLUME = 1.0f
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupWindowFlags()
        
        binding = ActivityAlarmRingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
        
        setupUI()
        loadAlarmData()
        startAlarmSound()
        startVibration()
    }
    
    private fun setupWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    
    private fun setupUI() {
        updateTimeDisplay()
        
        binding.dismissButton.setOnClickListener {
            dismissAlarm()
        }
        
        binding.snoozeButton.setOnClickListener {
            snoozeAlarm()
        }
    }
    
    private fun updateTimeDisplay() {
        val calendar = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("hh:mm", Locale.getDefault())
        val amPmFormat = SimpleDateFormat("aa", Locale.getDefault())
        
        binding.timeText.text = timeFormat.format(calendar.time)
        binding.amPmText.text = amPmFormat.format(calendar.time)
    }
    
    private fun loadAlarmData() {
        lifecycleScope.launch {
            val repository = AlarmRepository.getInstance(applicationContext)
            currentAlarm = repository.getAlarmById(alarmId)
            
            currentAlarm?.let { alarm ->
                if (alarm.label.isNotEmpty()) {
                    binding.alarmLabel.text = alarm.label
                    binding.alarmLabel.visibility = View.VISIBLE
                } else {
                    binding.alarmLabel.visibility = View.GONE
                }
                
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, alarm.hour)
                    set(Calendar.MINUTE, alarm.minute)
                }
                val timeFormat = SimpleDateFormat("hh:mm", Locale.getDefault())
                val amPmFormat = SimpleDateFormat("aa", Locale.getDefault())
                binding.timeText.text = timeFormat.format(calendar.time)
                binding.amPmText.text = amPmFormat.format(calendar.time)
                
                snoozeCount = alarm.snoozeCount
                updateSnoozeButton()
                startAlarmSound(alarm.snoozeCount)
                
                setupMission(alarm)
            } ?: run {
                finish()
            }
        }
    }
    
    private fun setupMission(alarm: Alarm) {
        val missionType = MissionType.fromString(alarm.missionType)
        
        if (missionType == MissionType.NONE) {
            binding.missionContentContainer.visibility = View.GONE
            return
        }
        
        binding.missionContentContainer.visibility = View.VISIBLE
        
        missionManager.attachCallback(this)
        missionManager.startMission(alarm)
        
        val fragment = when (missionType) {
            MissionType.MATH -> MathMissionFragment()
            MissionType.SHAKE -> ShakeMissionFragment()
            MissionType.PHOTO -> PhotoMissionFragment()
            MissionType.NONE -> return
        }
        
        supportFragmentManager.commit {
            replace(R.id.missionContentContainer, fragment)
        }
    }
    
    override fun onMissionStarted(missionType: MissionType) {
        binding.missionContentContainer.visibility = View.VISIBLE
    }
    
    override fun onMissionProgress(progress: Int, message: String) {
    }
    
    override fun onMissionComplete(result: MissionResult) {
        missionManager.detachCallback()
        handleMissionDismissed()
    }
    
    override fun onMissionCancelled() {
        missionManager.detachCallback()
    }
    
    override fun onMissionHintRequested(): String {
        return ""
    }
    
    private fun handleMissionDismissed() {
        stopAlarmSound()
        stopVibration()
        stopSnoozeCountdown()
        AlarmRingService.stop(this)
        
        currentAlarm?.let { alarm ->
            if (alarm.repeatDays.isNotEmpty()) {
                val resetAlarm = alarm.copy(snoozeCount = 0)
                lifecycleScope.launch {
                    val repository = AlarmRepository.getInstance(applicationContext)
                    repository.updateAlarm(resetAlarm)
                }
                scheduleNextRepeat(resetAlarm)
            } else {
                val resetAlarm = alarm.copy(snoozeCount = 0, isEnabled = false)
                lifecycleScope.launch {
                    val repository = AlarmRepository.getInstance(applicationContext)
                    repository.updateAlarm(resetAlarm)
                }
            }
        }
        
        showDismissedOverlay()
        
        WakeUpCheckService.start(this, alarmId)
        
        handler.postDelayed({
            finish()
        }, AUTO_DISMISS_DELAY_MS)
    }
    
    private fun dismissAlarm() {
        val missionType = MissionType.fromString(currentAlarm?.missionType ?: "NONE")
        
        if (missionType != MissionType.NONE) {
            if (missionManager.isMissionActive()) {
                return
            }
        }
        
        handleMissionDismissed()
    }
    
    private fun snoozeAlarm() {
        if (snoozeCount >= MAX_SNOOZES) {
            binding.snoozeButton.isEnabled = false
            binding.snoozeButton.text = getString(R.string.no_more_snoozes)
            return
        }
        
        snoozeCount++
        
        missionManager.cancelMission()
        missionManager.detachCallback()
        
        stopAlarmSound()
        stopVibration()
        AlarmRingService.stop(this)
        
        currentAlarm?.let { alarm ->
            scheduleSnooze(alarm)
        }
        
        finish()
    }
    
    private fun getSnoozeDurationMinutes(): Int {
        val index = (snoozeCount - 1).coerceIn(0, SNOOZE_DURATIONS.size - 1)
        return SNOOZE_DURATIONS.getOrElse(index) { 3 }
    }
    
    private fun updateSnoozeButton() {
        if (snoozeCount >= MAX_SNOOZES) {
            binding.snoozeButton.isEnabled = false
            binding.snoozeButton.text = getString(R.string.no_more_snoozes)
            binding.snoozeInfoText.visibility = View.GONE
        } else {
            val remaining = MAX_SNOOZES - snoozeCount
            binding.snoozeButton.text = getString(R.string.snooze_count, remaining)
            binding.snoozeInfoText.text = getString(R.string.snoozes_remaining, remaining)
            binding.snoozeInfoText.visibility = View.VISIBLE
        }
    }
    
    private fun scheduleSnooze(alarm: Alarm) {
        val snoozeMinutes = getSnoozeDurationMinutes()
        snoozedTimeMs = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        
        val calendar = Calendar.getInstance().apply {
            timeInMillis = snoozedTimeMs
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val snoozedAlarm = alarm.copy(
            hour = calendar.get(Calendar.HOUR_OF_DAY),
            minute = calendar.get(Calendar.MINUTE),
            snoozeCount = snoozeCount
        )
        
        lifecycleScope.launch {
            val repository = AlarmRepository.getInstance(applicationContext)
            repository.updateAlarm(snoozedAlarm)
        }
        
        AlarmScheduler.schedule(this, snoozedAlarm)
    }
    
    private fun scheduleNextRepeat(alarm: Alarm) {
        lifecycleScope.launch {
            AlarmScheduler.schedule(this@AlarmRingActivity, alarm)
        }
    }
    
    private fun showDismissedOverlay() {
        dismissedOverlay = layoutInflater.inflate(R.layout.layout_alarm_dismissed, binding.root as android.view.ViewGroup, false)
        (binding.root as android.view.ViewGroup).addView(dismissedOverlay)
        
        binding.bottomBar.visibility = View.GONE
        binding.missionContentContainer.visibility = View.GONE
    }
    
    private fun startAlarmSound(snoozeCount: Int = 0) {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            
            val volume = (BASE_VOLUME + (snoozeCount * VOLUME_INCREMENT)).coerceAtMost(MAX_VOLUME)
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmRingActivity, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                setVolume(volume, volume)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun stopAlarmSound() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }
    
    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        val pattern = longArrayOf(0, 500, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }
    
    private fun stopVibration() {
        vibrator?.cancel()
        vibrator = null
    }
    
    private fun stopSnoozeCountdown() {
        snoozeCountdownRunnable?.let { handler.removeCallbacks(it) }
        snoozeCountdownRunnable = null
    }
    
    override fun onDestroy() {
        stopAlarmSound()
        stopVibration()
        missionManager.detachCallback()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_POWER) {
            Toast.makeText(
                this,
                R.string.power_button_blocked_message,
                Toast.LENGTH_SHORT
            ).show()
            true
        } else {
            super.onKeyDown(keyCode, event)
        }
    }
}
