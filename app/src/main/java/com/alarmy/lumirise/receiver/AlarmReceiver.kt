package com.alarmy.lumirise.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alarmy.lumirise.service.AlarmRingService

class AlarmReceiver : BroadcastReceiver() {
    
    companion object {
        const val ACTION_ALARM = "com.alarmy.lumirise.ALARM_TRIGGER"
        const val EXTRA_ALARM_ID = "alarm_id"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_ALARM) {
            val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
            if (alarmId > 0) {
                AlarmRingService.start(context, alarmId)
            }
        }
    }
}
