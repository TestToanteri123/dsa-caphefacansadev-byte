package com.alarmy.lumirise.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.alarmy.lumirise.R
import com.alarmy.lumirise.databinding.ActivityAlarmEditorBinding
import com.alarmy.lumirise.model.Alarm
import kotlinx.coroutines.launch

class AlarmEditorActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAlarmEditorBinding
    private val viewModel: AlarmViewModel by viewModels()
    
    private var alarmId: Long = -1
    private var existingAlarm: Alarm? = null
    
    private var selectedHour: Int = 7
    private var selectedMinute: Int = 0
    private var isAm: Boolean = true
    
    private val selectedDays = mutableSetOf<String>()
    
    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
        
        setupToolbar()
        setupTimeDisplay()
        setupTimeButtons()
        setupDayChips()
        setupMissionSelection()
        setupShakeSlider()
        setupSaveButton()
        
        if (alarmId > 0) {
            loadExistingAlarm()
        } else {
            updateTimeDisplay()
        }
    }
    
    private fun setupToolbar() {
        binding.toolbar.title = if (alarmId > 0) "EDIT ALARM" else "NEW ALARM"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
    
    private fun setupTimeDisplay() {
        updateTimeDisplay()
        
        binding.amButton.setOnClickListener {
            isAm = true
            updateAmPmButtons()
        }
        
        binding.pmButton.setOnClickListener {
            isAm = false
            updateAmPmButtons()
        }
    }
    
    private fun setupTimeButtons() {
        binding.decreaseTimeButton.setOnClickListener {
            decreaseTime()
        }
        
        binding.increaseTimeButton.setOnClickListener {
            increaseTime()
        }
    }
    
    private fun decreaseTime() {
        if (selectedMinute >= 5) {
            selectedMinute -= 5
        } else {
            selectedMinute = 55
            if (selectedHour == 12) {
                selectedHour = 1
            } else if (selectedHour == 0) {
                selectedHour = 23
            } else {
                selectedHour -= 1
            }
        }
        updateTimeDisplay()
    }
    
    private fun increaseTime() {
        if (selectedMinute < 55) {
            selectedMinute += 5
        } else {
            selectedMinute = 0
            if (selectedHour == 11) {
                selectedHour = 12
            } else if (selectedHour == 12) {
                selectedHour = 1
            } else {
                selectedHour += 1
            }
        }
        updateTimeDisplay()
    }
    
    private fun updateTimeDisplay() {
        val displayHour = when {
            selectedHour == 0 -> 12
            selectedHour > 12 -> selectedHour - 12
            else -> selectedHour
        }
        
        binding.hourText.text = String.format("%02d", displayHour)
        binding.minuteText.text = String.format("%02d", selectedMinute)
        updateAmPmButtons()
    }
    
    private fun updateAmPmButtons() {
        binding.amButton.setBackgroundColor(
            if (isAm) getColor(R.color.primary) else getColor(R.color.surface_container_highest)
        )
        binding.amButton.setTextColor(
            if (isAm) getColor(R.color.on_primary) else getColor(R.color.on_surface_variant)
        )
        
        binding.pmButton.setBackgroundColor(
            if (!isAm) getColor(R.color.primary) else getColor(R.color.surface_container_highest)
        )
        binding.pmButton.setTextColor(
            if (!isAm) getColor(R.color.on_primary) else getColor(R.color.on_surface_variant)
        )
    }
    
    private fun setupDayChips() {
        val dayChipClickListener = View.OnClickListener { view ->
            val day = when (view.id) {
                R.id.chipMon -> "MON"
                R.id.chipTue -> "TUE"
                R.id.chipWed -> "WED"
                R.id.chipThu -> "THU"
                R.id.chipFri -> "FRI"
                R.id.chipSat -> "SAT"
                R.id.chipSun -> "SUN"
                else -> return@OnClickListener
            }
            
            if (selectedDays.contains(day)) {
                selectedDays.remove(day)
                view.setBackgroundColor(getColor(R.color.surface_container_highest))
            } else {
                selectedDays.add(day)
                view.setBackgroundColor(getColor(R.color.secondary_container))
            }
        }
        
        binding.chipMon.setOnClickListener(dayChipClickListener)
        binding.chipTue.setOnClickListener(dayChipClickListener)
        binding.chipWed.setOnClickListener(dayChipClickListener)
        binding.chipThu.setOnClickListener(dayChipClickListener)
        binding.chipFri.setOnClickListener(dayChipClickListener)
        binding.chipSat.setOnClickListener(dayChipClickListener)
        binding.chipSun.setOnClickListener(dayChipClickListener)
    }
    
    private fun setupMissionSelection() {
        binding.missionSelectorCard.setOnClickListener {
            showMissionBottomSheet()
        }
        
        binding.missionRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val missionType = when (checkedId) {
                R.id.radioMath -> "MATH"
                R.id.radioShake -> "SHAKE"
                R.id.radioPhoto -> "PHOTO"
                else -> "NONE"
            }
            
            updateMissionDisplay(missionType)
            updateMissionSettingsVisibility(missionType)
        }
    }
    
    private fun showMissionBottomSheet() {
        binding.missionRadioGroup.visibility = View.VISIBLE
        binding.missionRadioGroup.check(
            when {
                binding.radioMath.isChecked -> R.id.radioMath
                binding.radioShake.isChecked -> R.id.radioShake
                binding.radioPhoto.isChecked -> R.id.radioPhoto
                else -> R.id.radioNone
            }
        )
        
        val bottomSheet = MissionBottomSheetFragment.newInstance()
        bottomSheet.show(supportFragmentManager, "mission_bottom_sheet")
    }
    
    private fun updateMissionDisplay(missionType: String) {
        binding.missionTitleText.text = when (missionType) {
            "MATH" -> "Math Problem"
            "SHAKE" -> "Shake Phone"
            "PHOTO" -> "Take Photo"
            else -> "None Selected"
        }
        
        binding.mathChip.visibility = if (missionType == "MATH") View.VISIBLE else View.GONE
        binding.shakeChip.visibility = if (missionType == "SHAKE") View.VISIBLE else View.GONE
        binding.photoChip.visibility = if (missionType == "PHOTO") View.VISIBLE else View.GONE
    }
    
    private fun updateMissionSettingsVisibility(missionType: String) {
        val isMissionSelected = missionType != "NONE"
        binding.missionSettingsCard.visibility = if (isMissionSelected) View.VISIBLE else View.GONE
        
        binding.mathDifficultyLayout.visibility = if (missionType == "MATH") View.VISIBLE else View.GONE
        binding.shakeCountLayout.visibility = if (missionType == "SHAKE") View.VISIBLE else View.GONE
    }
    
    private fun setupShakeSlider() {
        binding.shakeCountSlider.addOnChangeListener { _, value, _ ->
            binding.shakeCountText.text = value.toInt().toString()
        }
    }
    
    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            saveAlarm()
        }
    }
    
    private fun loadExistingAlarm() {
        lifecycleScope.launch {
            val alarm = viewModel.alarms.value.find { it.id == alarmId }
            alarm?.let {
                existingAlarm = it
                populateFields(it)
            }
        }
    }
    
    private fun populateFields(alarm: Alarm) {
        selectedHour = alarm.hour
        selectedMinute = alarm.minute
        binding.labelInput.setText(alarm.label)
        
        isAm = alarm.hour < 12
        updateTimeDisplay()
        
        selectedDays.clear()
        val dayViews = mapOf(
            "MON" to binding.chipMon,
            "TUE" to binding.chipTue,
            "WED" to binding.chipWed,
            "THU" to binding.chipThu,
            "FRI" to binding.chipFri,
            "SAT" to binding.chipSat,
            "SUN" to binding.chipSun
        )
        
        alarm.repeatDays.split(",").forEach { day ->
            val trimmedDay = day.trim()
            if (trimmedDay.isNotEmpty()) {
                selectedDays.add(trimmedDay)
                dayViews[trimmedDay]?.setBackgroundColor(getColor(R.color.secondary_container))
            }
        }
        
        when (alarm.missionType) {
            "MATH" -> {
                binding.radioMath.isChecked = true
                updateMissionDisplay("MATH")
                updateMissionSettingsVisibility("MATH")
                when (alarm.missionDifficulty) {
                    "EASY" -> binding.chipEasy.isChecked = true
                    "MEDIUM" -> binding.chipMedium.isChecked = true
                    "HARD" -> binding.chipHard.isChecked = true
                }
            }
            "SHAKE" -> {
                binding.radioShake.isChecked = true
                updateMissionDisplay("SHAKE")
                updateMissionSettingsVisibility("SHAKE")
                binding.shakeCountSlider.value = alarm.shakeCount.toFloat()
                binding.shakeCountText.text = alarm.shakeCount.toString()
            }
            "PHOTO" -> {
                binding.radioPhoto.isChecked = true
                updateMissionDisplay("PHOTO")
                updateMissionSettingsVisibility("PHOTO")
            }
            else -> {
                binding.radioNone.isChecked = true
                updateMissionDisplay("NONE")
                updateMissionSettingsVisibility("NONE")
            }
        }
        
        binding.deleteButton.visibility = View.VISIBLE
        binding.deleteButton.setOnClickListener {
            existingAlarm?.let { alarm ->
                viewModel.deleteAlarm(alarm)
                Toast.makeText(this, "Alarm deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    private fun saveAlarm() {
        val hour = if (isAm) {
            when (selectedHour) {
                12 -> 0
                else -> selectedHour
            }
        } else {
            when (selectedHour) {
                12 -> 12
                else -> selectedHour + 12
            }
        }
        
        val label = binding.labelInput.text?.toString() ?: ""
        val repeatDays = selectedDays.toList().joinToString(",")
        
        val missionType = when (binding.missionRadioGroup.checkedRadioButtonId) {
            R.id.radioMath -> "MATH"
            R.id.radioShake -> "SHAKE"
            R.id.radioPhoto -> "PHOTO"
            else -> "NONE"
        }
        
        val missionDifficulty = when {
            binding.chipEasy.isChecked -> "EASY"
            binding.chipMedium.isChecked -> "MEDIUM"
            binding.chipHard.isChecked -> "HARD"
            else -> "EASY"
        }
        
        val shakeCount = binding.shakeCountSlider.value.toInt()
        
        if (existingAlarm != null) {
            val updatedAlarm = existingAlarm!!.copy(
                hour = hour,
                minute = selectedMinute,
                label = label,
                repeatDays = repeatDays,
                missionType = missionType,
                missionDifficulty = missionDifficulty,
                shakeCount = shakeCount
            )
            viewModel.updateAlarm(updatedAlarm)
        } else {
            viewModel.addAlarm(
                hour = hour,
                minute = selectedMinute,
                label = label,
                repeatDays = repeatDays,
                missionType = missionType,
                missionDifficulty = missionDifficulty,
                shakeCount = shakeCount
            )
        }
        
        Toast.makeText(this, "Alarm saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
