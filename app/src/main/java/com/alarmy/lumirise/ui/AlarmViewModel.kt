package com.alarmy.lumirise.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alarmy.lumirise.data.AlarmRepository
import com.alarmy.lumirise.model.Alarm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = AlarmRepository.getInstance(application)
    
    val alarms: StateFlow<List<Alarm>> = repository.allAlarms
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    private val _uiState = MutableStateFlow<AlarmUiState>(AlarmUiState.Idle)
    val uiState: StateFlow<AlarmUiState> = _uiState
    
    fun addAlarm(
        hour: Int,
        minute: Int,
        label: String = "",
        repeatDays: String = "",
        missionType: String = "NONE",
        missionDifficulty: String = "EASY",
        shakeCount: Int = 30,
        soundUri: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = AlarmUiState.Loading
            try {
                val alarm = Alarm(
                    hour = hour,
                    minute = minute,
                    label = label,
                    repeatDays = repeatDays,
                    missionType = missionType,
                    missionDifficulty = missionDifficulty,
                    shakeCount = shakeCount,
                    soundUri = soundUri,
                    isEnabled = true
                )
                repository.insertAlarm(alarm)
                _uiState.value = AlarmUiState.Success("Alarm added")
            } catch (e: Exception) {
                _uiState.value = AlarmUiState.Error(e.message ?: "Failed to add alarm")
            }
        }
    }
    
    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            _uiState.value = AlarmUiState.Loading
            try {
                repository.updateAlarm(alarm)
                _uiState.value = AlarmUiState.Success("Alarm updated")
            } catch (e: Exception) {
                _uiState.value = AlarmUiState.Error(e.message ?: "Failed to update alarm")
            }
        }
    }
    
    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            _uiState.value = AlarmUiState.Loading
            try {
                repository.deleteAlarm(alarm)
                _uiState.value = AlarmUiState.Success("Alarm deleted")
            } catch (e: Exception) {
                _uiState.value = AlarmUiState.Error(e.message ?: "Failed to delete alarm")
            }
        }
    }
    
    fun toggleAlarmEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleAlarmEnabled(id, enabled)
            } catch (e: Exception) {
                _uiState.value = AlarmUiState.Error(e.message ?: "Failed to toggle alarm")
            }
        }
    }
    
    fun resetUiState() {
        _uiState.value = AlarmUiState.Idle
    }
}

sealed class AlarmUiState {
    data object Idle : AlarmUiState()
    data object Loading : AlarmUiState()
    data class Success(val message: String) : AlarmUiState()
    data class Error(val message: String) : AlarmUiState()
}
