package com.alarmy.lumirise.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.alarmy.lumirise.model.Alarm
import com.alarmy.lumirise.model.SleepRecord
import com.alarmy.lumirise.model.SnoreEvent

@Database(
    entities = [Alarm::class, SleepRecord::class, SnoreEvent::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun sleepRecordDao(): SleepRecordDao
    abstract fun snoreEventDao(): SnoreEventDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lumi_rise_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
