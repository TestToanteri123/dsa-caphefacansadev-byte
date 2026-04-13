package com.alarmy.lumirise.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.alarmy.lumirise.service.AlarmRingService

class PowerButtonReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "PowerButtonReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SHUTDOWN) {
            Log.d(TAG, "Shutdown attempt detected, keeping alarm service running")
            val alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1)
            if (alarmId > 0) {
                AlarmRingService.start(context, alarmId)
            }
        }
    }
}
