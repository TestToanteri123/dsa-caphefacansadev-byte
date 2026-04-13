package com.alarmy.lumirise.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alarmy.lumirise.R
import com.alarmy.lumirise.databinding.ItemAlarmBinding
import com.alarmy.lumirise.model.Alarm
import java.text.SimpleDateFormat
import java.util.*

class AlarmAdapter(
    private val onAlarmClick: (Alarm) -> Unit,
    private val onAlarmToggle: (Alarm, Boolean) -> Unit,
    private val onAlarmDelete: (Alarm) -> Unit
) : ListAdapter<Alarm, AlarmAdapter.AlarmViewHolder>(AlarmDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val binding = ItemAlarmBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AlarmViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class AlarmViewHolder(
        private val binding: ItemAlarmBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(alarm: Alarm) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
            }
            val timeFormat = SimpleDateFormat("hh:mm", Locale.getDefault())
            val amPmFormat = SimpleDateFormat("aa", Locale.getDefault())
            
            binding.alarmTime.text = timeFormat.format(calendar.time)
            binding.alarmAmPm.text = amPmFormat.format(calendar.time)
            
            binding.alarmLabel.text = alarm.label.ifEmpty { "Alarm" }
            binding.alarmLabel.visibility = if (alarm.label.isNotEmpty()) View.VISIBLE else View.INVISIBLE
            
            val daysList = alarm.repeatDays.split(",").map { it.trim() }
            binding.apply {
                dayMon.visibility = if (daysList.contains("MON")) View.VISIBLE else View.GONE
                dayTue.visibility = if (daysList.contains("TUE")) View.VISIBLE else View.GONE
                dayWed.visibility = if (daysList.contains("WED")) View.VISIBLE else View.GONE
                dayThu.visibility = if (daysList.contains("THU")) View.VISIBLE else View.GONE
                dayFri.visibility = if (daysList.contains("FRI")) View.VISIBLE else View.GONE
                daySat.visibility = if (daysList.contains("SAT")) View.VISIBLE else View.GONE
                daySun.visibility = if (daysList.contains("SUN")) View.VISIBLE else View.GONE
                repeatDaysContainer.visibility = if (alarm.repeatDays.isNotEmpty()) View.VISIBLE else View.GONE
            }
            
            if (alarm.missionType != "NONE") {
                binding.missionIcon.visibility = View.VISIBLE
                binding.missionText.visibility = View.VISIBLE
                binding.missionText.text = formatMission(alarm)
            } else {
                binding.missionIcon.visibility = View.GONE
                binding.missionText.visibility = View.GONE
            }
            
            binding.alarmSwitch.isChecked = alarm.isEnabled
            
            binding.root.setOnClickListener { onAlarmClick(alarm) }
            binding.alarmSwitch.setOnCheckedChangeListener { _, isChecked ->
                onAlarmToggle(alarm, isChecked)
            }
            
            binding.root.setOnLongClickListener {
                onAlarmDelete(alarm)
                true
            }
        }
        
        private fun formatMission(alarm: Alarm): String {
            return when (alarm.missionType) {
                "MATH" -> "Math: ${alarm.missionDifficulty}"
                "SHAKE" -> "Shake: ${alarm.shakeCount}x"
                "PHOTO" -> "Photo"
                else -> alarm.missionType
            }
        }
    }
    
    class AlarmDiffCallback : DiffUtil.ItemCallback<Alarm>() {
        override fun areItemsTheSame(oldItem: Alarm, newItem: Alarm): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Alarm, newItem: Alarm): Boolean {
            return oldItem == newItem
        }
    }
}
