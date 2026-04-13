package com.alarmy.lumirise.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alarmy.lumirise.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val repository = AlarmRepository.getInstance(context)
                repository.rescheduleAllEnabledAlarms()
            }
        }
    }
}
