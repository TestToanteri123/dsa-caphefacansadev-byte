package com.alarmy.lumirise.ui.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alarmy.lumirise.R
import com.alarmy.lumirise.databinding.ItemSleepRecordBinding
import com.alarmy.lumirise.model.SleepRecord
import com.alarmy.lumirise.ui.SleepHistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SleepHistoryAdapter(
    private val onRecordClick: (SleepRecord) -> Unit,
    private val onRecordLongClick: (SleepRecord) -> Unit
) : ListAdapter<SleepRecord, SleepHistoryAdapter.SleepRecordViewHolder>(SleepRecordDiffCallback()) {

    private var expandedRecordId: Long? = null

    fun setExpandedRecordId(id: Long?) {
        val oldExpanded = expandedRecordId
        expandedRecordId = id
        
        currentList.forEachIndexed { index, record ->
            if (record.id == oldExpanded || record.id == id) {
                notifyItemChanged(index, PAYLOAD_EXPAND_CHANGE)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SleepRecordViewHolder {
        val binding = ItemSleepRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SleepRecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SleepRecordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(
        holder: SleepRecordViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_EXPAND_CHANGE)) {
            holder.updateExpandState(getItem(position))
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class SleepRecordViewHolder(
        private val binding: ItemSleepRecordBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

        fun bind(record: SleepRecord) {
            val startDate = Date(record.startTime)
            val endDate = Date(record.endTime)

            binding.sleepDate.text = dateFormat.format(startDate)
            binding.sleepTime.text = "${timeFormat.format(startDate)} - ${timeFormat.format(endDate)}"

            binding.qualityScore.text = record.qualityScore.toString()
            binding.qualityScore.setTextColor(SleepHistoryViewModel.getQualityColor(record.qualityScore))
            binding.qualityLabel.text = SleepHistoryViewModel.getQualityLabel(record.qualityScore)

            binding.durationLabel.text = SleepHistoryViewModel.calculateDuration(record.startTime, record.endTime)

            binding.snoreCount.text = SleepHistoryViewModel.formatSnoreCount(record.totalSnoreCount)
            binding.snorePercentage.text = "${record.snorePercentage.toInt()}%"

            binding.qualityProgressBar.progress = record.qualityScore
            binding.qualityProgressBar.setIndicatorColor(SleepHistoryViewModel.getQualityColor(record.qualityScore))

            val qualityColor = SleepHistoryViewModel.getQualityColor(record.qualityScore)
            val background = binding.qualityScore.background as? GradientDrawable
            background?.setStroke(4, qualityColor)

            updateExpandState(record)

            binding.root.setOnClickListener {
                onRecordClick(record)
            }

            binding.root.setOnLongClickListener {
                onRecordLongClick(record)
                true
            }
        }

        fun updateExpandState(record: SleepRecord) {
            val isExpanded = expandedRecordId == record.id
            binding.expandedDetails.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.expandIcon.rotation = if (isExpanded) 180f else 0f
        }
    }

    private class SleepRecordDiffCallback : DiffUtil.ItemCallback<SleepRecord>() {
        override fun areItemsTheSame(oldItem: SleepRecord, newItem: SleepRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SleepRecord, newItem: SleepRecord): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val PAYLOAD_EXPAND_CHANGE = "expand_change"
    }
}
