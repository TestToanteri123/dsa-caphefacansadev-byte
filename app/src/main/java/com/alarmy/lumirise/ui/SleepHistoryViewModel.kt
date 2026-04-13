package com.alarmy.lumirise.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alarmy.lumirise.R
import com.alarmy.lumirise.data.SleepRepository
import com.alarmy.lumirise.model.SleepRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SleepHistoryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = SleepRepository.getInstance(application)
    private val context = application.applicationContext
    
    // Date filter state
    private val _dateFilter = MutableStateFlow(DateFilter.WEEK)
    val dateFilter: StateFlow<DateFilter> = _dateFilter
    
    // Filtered sleep records based on date filter
    val filteredSleepRecords: StateFlow<List<SleepRecord>> = combine(
        repository.allCompleteSleepRecords,
        _dateFilter
    ) { records, filter ->
        filterRecords(records, filter)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // Summary statistics derived from filtered records
    val averageSleepDuration: StateFlow<String> = combine(
        filteredSleepRecords,
        _dateFilter
    ) { records, _ ->
        calculateAverageDuration(records)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "0h"
    )
    
    val averageQuality: StateFlow<Int> = combine(
        filteredSleepRecords,
        _dateFilter
    ) { records, _ ->
        if (records.isEmpty()) 0 else records.map { it.qualityScore }.average().toInt()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )
    
    val nightsTracked: StateFlow<Int> = filteredSleepRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        .let { flow ->
            MutableStateFlow(0).also { mutableFlow ->
                viewModelScope.launch {
                    flow.collect { records ->
                        mutableFlow.value = records.size
                    }
                }
            }
        }
    
    val sleepRecords: StateFlow<List<SleepRecord>> = repository.allCompleteSleepRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    
    private val _uiState = MutableStateFlow<SleepHistoryUiState>(SleepHistoryUiState.Idle)
    val uiState: StateFlow<SleepHistoryUiState> = _uiState
    
    private val _expandedRecordId = MutableStateFlow<Long?>(null)
    val expandedRecordId: StateFlow<Long?> = _expandedRecordId
    
    fun setDateFilter(filter: DateFilter) {
        _dateFilter.value = filter
    }
    
    private fun filterRecords(records: List<SleepRecord>, filter: DateFilter): List<SleepRecord> {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        return when (filter) {
            DateFilter.WEEK -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val weekAgo = calendar.timeInMillis
                records.filter { it.startTime >= weekAgo }
            }
            DateFilter.MONTH -> {
                calendar.add(Calendar.MONTH, -1)
                val monthAgo = calendar.timeInMillis
                records.filter { it.startTime >= monthAgo }
            }
            DateFilter.ALL -> records
        }.sortedByDescending { it.startTime }
    }
    
    private fun calculateAverageDuration(records: List<SleepRecord>): String {
        if (records.isEmpty()) return "0h"
        
        val totalMs = records.sumOf { it.endTime - it.startTime }
        val avgMs = totalMs / records.size
        
        val hours = TimeUnit.MILLISECONDS.toHours(avgMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(avgMs) % 60
        
        return if (hours > 0 && minutes > 0) {
            "${hours}h ${minutes}m"
        } else if (hours > 0) {
            "${hours}h"
        } else {
            "${minutes}m"
        }
    }
    
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            kotlinx.coroutines.delay(500)
            _isRefreshing.value = false
        }
    }
    
    fun toggleExpanded(recordId: Long) {
        _expandedRecordId.value = if (_expandedRecordId.value == recordId) null else recordId
    }
    
    fun deleteSleepRecord(record: SleepRecord) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteSleepRecord(record)
                _uiState.value = SleepHistoryUiState.Success(context.getString(R.string.sleep_record_deleted))
            } catch (e: Exception) {
                _uiState.value = SleepHistoryUiState.Error(e.message ?: "Failed to delete record")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun resetUiState() {
        _uiState.value = SleepHistoryUiState.Idle
    }
    
    companion object {
        fun calculateDuration(startTime: Long, endTime: Long): String {
            val durationMs = endTime - startTime
            val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
            
            return when {
                hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
                hours > 0 -> "${hours}h"
                else -> "${minutes}m"
            }
        }
        
        fun getQualityColor(qualityScore: Int): Int {
            return when {
                qualityScore >= 80 -> android.graphics.Color.parseColor("#10B981") // Good - Green
                qualityScore >= 60 -> android.graphics.Color.parseColor("#F59E0B") // Medium - Amber
                else -> android.graphics.Color.parseColor("#EF4444") // Poor - Red
            }
        }
        
        fun getQualityLabel(qualityScore: Int): String {
            return when {
                qualityScore >= 80 -> "Good"
                qualityScore >= 60 -> "Fair"
                qualityScore >= 40 -> "Moderate"
                else -> "Poor"
            }
        }
        
        fun formatSnoreCount(count: Int): String {
            return if (count == 1) "$count snoring event" else "$count snoring events"
        }
    }
}

enum class DateFilter {
    WEEK, MONTH, ALL
}

sealed class SleepHistoryUiState {
    data object Idle : SleepHistoryUiState()
    data object Loading : SleepHistoryUiState()
    data class Success(val message: String) : SleepHistoryUiState()
    data class Error(val message: String) : SleepHistoryUiState()
}
