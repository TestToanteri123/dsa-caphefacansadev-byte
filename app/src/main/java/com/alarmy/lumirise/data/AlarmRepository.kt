package com.alarmy.lumirise.data

import android.content.Context
import com.alarmy.lumirise.data.local.AlarmDao
import com.alarmy.lumirise.data.local.AppDatabase
import com.alarmy.lumirise.model.Alarm
import com.alarmy.lumirise.util.AlarmScheduler
import kotlinx.coroutines.flow.Flow

class AlarmRepository private constructor(context: Context) {
    
    private val alarmDao: AlarmDao = AppDatabase.getInstance(context).alarmDao()
    private val appContext = context.applicationContext
    
    val allAlarms: Flow<List<Alarm>> = alarmDao.getAllAlarms()
    
    suspend fun getAlarmById(id: Long): Alarm? {
        return alarmDao.getAlarmById(id)
    }
    
    suspend fun insertAlarm(alarm: Alarm): Long {
        val id = alarmDao.insertAlarm(alarm)
        if (alarm.isEnabled) {
            val savedAlarm = alarm.copy(id = id)
            AlarmScheduler.schedule(appContext, savedAlarm)
        }
        return id
    }
    
    suspend fun updateAlarm(alarm: Alarm) {
        alarmDao.updateAlarm(alarm)
        if (alarm.isEnabled) {
            AlarmScheduler.schedule(appContext, alarm)
        }
    }
    
    suspend fun deleteAlarm(alarm: Alarm) {
        AlarmScheduler.cancel(appContext, alarm)
        alarmDao.deleteAlarm(alarm)
    }
    
    suspend fun toggleAlarmEnabled(id: Long, enabled: Boolean) {
        val alarm = alarmDao.getAlarmById(id) ?: return
        
        alarmDao.setAlarmEnabled(id, enabled)
        
        if (enabled) {
            AlarmScheduler.schedule(appContext, alarm.copy(isEnabled = true))
        }
    }
    
    suspend fun rescheduleAllEnabledAlarms() {
        val enabledAlarms = alarmDao.getEnabledAlarms()
        enabledAlarms.forEach { alarm ->
            AlarmScheduler.schedule(appContext, alarm)
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: AlarmRepository? = null
        
        fun getInstance(context: Context): AlarmRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = AlarmRepository(context)
                INSTANCE = instance
                instance
            }
        }
    }
}
