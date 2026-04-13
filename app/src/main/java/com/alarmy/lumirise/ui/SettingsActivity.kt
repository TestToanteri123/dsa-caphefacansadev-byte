package com.alarmy.lumirise.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.alarmy.lumirise.BuildConfig
import com.alarmy.lumirise.R
import com.alarmy.lumirise.databinding.ActivitySettingsBinding
import com.alarmy.lumirise.ui.AlarmMainActivity
import com.alarmy.lumirise.util.ThemeManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: android.content.SharedPreferences

    companion object {
        private const val PREFS_NAME = "lumi_rise_settings"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_SNOOZE_DURATION = "snooze_duration"
        private const val KEY_AUTO_DELETE = "auto_delete_old_alarms"
        private const val KEY_ALARM_SOUND = "alarm_sound"
        private const val KEY_VOLUME_LEVEL = "volume_level"
        private const val KEY_GRADUAL_VOLUME = "gradual_volume"
        private const val KEY_AUTO_TRACKING = "auto_tracking"
        private const val KEY_SNORING_SENSITIVITY = "snoring_sensitivity"
        private const val KEY_KEEP_ALARM_HISTORY = "keep_alarm_history"
        private const val KEY_SMART_ALARM = "smart_alarm"
        private const val KEY_VIBRATION_PATTERN = "vibration_pattern"

        const val RESULT_THEME_CHANGED = 100

        private val snoozeValues = intArrayOf(1, 3, 5, 10, 15)
        private val alarmSounds = arrayOf("Solar Flare", "Ocean Waves", "Forest Dawn", "Gentle Breeze", "Digital Beep", "Morning Mist")
        private val sensitivityLevels = arrayOf("Low", "Medium", "High")
        private val vibrationPatterns = arrayOf("Default", "Heartbeat", "SOS", "Rhythm", "Pulse")

        fun getSnoozeDuration(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val index = prefs.getInt(KEY_SNOOZE_DURATION, 2)
            return snoozeValues[index.coerceIn(0, snoozeValues.size - 1)]
        }

        fun shouldAutoDelete(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_AUTO_DELETE, false)
        }

        fun getAlarmSound(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val index = prefs.getInt(KEY_ALARM_SOUND, 0)
            return alarmSounds[index.coerceIn(0, alarmSounds.size - 1)]
        }

        fun getVolumeLevel(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(KEY_VOLUME_LEVEL, 70)
        }

        fun shouldGradualVolume(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_GRADUAL_VOLUME, true)
        }

        fun shouldAutoTracking(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_AUTO_TRACKING, false)
        }

        fun getSnoringSensitivity(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val index = prefs.getInt(KEY_SNORING_SENSITIVITY, 1)
            return sensitivityLevels[index.coerceIn(0, sensitivityLevels.size - 1)]
        }

        fun isDarkMode(context: Context): Boolean {
            return ThemeManager.isDarkMode()
        }
    }

    private val snoozeDurations = arrayOf("1 min", "3 min", "5 min", "10 min", "15 min")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        setupToolbar()
        loadSettings()
        setupListeners()
        setupBottomNavigation()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadSettings() {
        binding.darkModeSwitch.isChecked = ThemeManager.isDarkMode()

        // Snooze Duration
        val snoozeIndex = prefs.getInt(KEY_SNOOZE_DURATION, 2)
        binding.snoozeDurationValue.text = snoozeDurations[snoozeIndex.coerceIn(0, snoozeDurations.size - 1)]

        // Auto-delete
        binding.autoDeleteSwitch.isChecked = prefs.getBoolean(KEY_AUTO_DELETE, false)

        // Keep Alarm History
        binding.keepAlarmHistorySwitch.isChecked = prefs.getBoolean(KEY_KEEP_ALARM_HISTORY, true)

        // Alarm Sound
        val soundIndex = prefs.getInt(KEY_ALARM_SOUND, 5) // Default to "Morning Mist"
        binding.alarmSoundValue.text = alarmSounds[soundIndex.coerceIn(0, alarmSounds.size - 1)]

        // Volume Level
        val volume = prefs.getInt(KEY_VOLUME_LEVEL, 85)
        binding.volumeSlider.value = volume.toFloat()
        binding.volumeValue.text = "$volume%"

        // Gradual Volume
        binding.gradualVolumeSwitch.isChecked = prefs.getBoolean(KEY_GRADUAL_VOLUME, true)

        // Vibration Pattern
        val vibrationIndex = prefs.getInt(KEY_VIBRATION_PATTERN, 1) // Default to "Heartbeat"
        binding.vibrationPatternValue.text = vibrationPatterns[vibrationIndex.coerceIn(0, vibrationPatterns.size - 1)]

        // Auto Tracking
        binding.autoTrackingSwitch.isChecked = prefs.getBoolean(KEY_AUTO_TRACKING, false)

        // Snoring Sensitivity
        val sensitivityIndex = prefs.getInt(KEY_SNORING_SENSITIVITY, 1)
        binding.snoringSensitivityValue.text = sensitivityLevels[sensitivityIndex.coerceIn(0, sensitivityLevels.size - 1)]

        // Smart Alarm
        binding.smartAlarmSwitch.isChecked = prefs.getBoolean(KEY_SMART_ALARM, true)

        // App Version
        binding.appVersionValue.text = BuildConfig.VERSION_NAME
    }

    private fun setupListeners() {
        binding.darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            ThemeManager.setDarkMode(isChecked)
            setResult(RESULT_THEME_CHANGED)
        }

        // Snooze Duration Card
        binding.snoozeDurationCard.setOnClickListener {
            showSnoozeDurationPicker()
        }

        // Auto-delete Switch
        binding.autoDeleteSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_AUTO_DELETE, isChecked).apply()
        }

        // Keep Alarm History Switch
        binding.keepAlarmHistorySwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_KEEP_ALARM_HISTORY, isChecked).apply()
        }

        // Alarm Sound Card
        binding.alarmSoundCard.setOnClickListener {
            showAlarmSoundPicker()
        }

        // Volume Slider
        binding.volumeSlider.addOnChangeListener { _, value, _ ->
            val volume = value.toInt()
            binding.volumeValue.text = "$volume%"
            prefs.edit().putInt(KEY_VOLUME_LEVEL, volume).apply()
        }

        // Gradual Volume Switch
        binding.gradualVolumeSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_GRADUAL_VOLUME, isChecked).apply()
        }

        // Vibration Pattern Card
        binding.vibrationPatternCard.setOnClickListener {
            showVibrationPatternPicker()
        }

        // Auto Tracking Switch
        binding.autoTrackingSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_AUTO_TRACKING, isChecked).apply()
        }

        // Snoring Sensitivity Card
        binding.snoringSensitivityCard.setOnClickListener {
            showSnoringSensitivityPicker()
        }

        // Smart Alarm Switch
        binding.smartAlarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_SMART_ALARM, isChecked).apply()
        }

        // Rate App Card
        binding.rateAppCard.setOnClickListener {
            openAppStore()
        }

        // Privacy Policy Card
        binding.privacyPolicyCard.setOnClickListener {
            openPrivacyPolicy()
        }

        // Terms of Service Card
        binding.termsOfServiceCard.setOnClickListener {
            openTermsOfService()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_alarms -> {
                    startActivity(Intent(this, AlarmMainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_missions -> {
                    // Already on settings, do nothing or handle differently
                    true
                }
                R.id.nav_records -> {
                    startActivity(Intent(this, SleepHistoryActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    // Already on settings
                    true
                }
                else -> false
            }
        }
        binding.bottomNavigation.selectedItemId = R.id.nav_settings
    }

    private fun showSnoozeDurationPicker() {
        val currentIndex = prefs.getInt(KEY_SNOOZE_DURATION, 2)

        MaterialAlertDialogBuilder(this)
            .setTitle("Snooze Duration")
            .setSingleChoiceItems(snoozeDurations, currentIndex) { dialog, which ->
                binding.snoozeDurationValue.text = snoozeDurations[which]
                prefs.edit().putInt(KEY_SNOOZE_DURATION, which).apply()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAlarmSoundPicker() {
        val currentIndex = prefs.getInt(KEY_ALARM_SOUND, 5)

        MaterialAlertDialogBuilder(this)
            .setTitle("Default Alarm Sound")
            .setSingleChoiceItems(alarmSounds, currentIndex) { dialog, which ->
                binding.alarmSoundValue.text = alarmSounds[which]
                prefs.edit().putInt(KEY_ALARM_SOUND, which).apply()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSnoringSensitivityPicker() {
        val currentIndex = prefs.getInt(KEY_SNORING_SENSITIVITY, 1)

        MaterialAlertDialogBuilder(this)
            .setTitle("Snoring Sensitivity")
            .setSingleChoiceItems(sensitivityLevels, currentIndex) { dialog, which ->
                binding.snoringSensitivityValue.text = sensitivityLevels[which]
                prefs.edit().putInt(KEY_SNORING_SENSITIVITY, which).apply()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showVibrationPatternPicker() {
        val currentIndex = prefs.getInt(KEY_VIBRATION_PATTERN, 1)

        MaterialAlertDialogBuilder(this)
            .setTitle("Vibration Pattern")
            .setSingleChoiceItems(vibrationPatterns, currentIndex) { dialog, which ->
                binding.vibrationPatternValue.text = vibrationPatterns[which]
                prefs.edit().putInt(KEY_VIBRATION_PATTERN, which).apply()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openAppStore() {
        try {
            val appPackage = packageName
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackage"))
            startActivity(intent)
        } catch (e: Exception) {
            val appPackage = packageName
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackage"))
            startActivity(intent)
        }
    }

    private fun openPrivacyPolicy() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://lumirise.app/privacy"))
        startActivity(intent)
    }

    private fun openTermsOfService() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://lumirise.app/terms"))
        startActivity(intent)
    }
}
