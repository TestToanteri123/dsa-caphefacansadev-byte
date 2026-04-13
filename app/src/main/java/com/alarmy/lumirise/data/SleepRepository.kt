package com.alarmy.lumirise.data

import android.content.Context
import com.alarmy.lumirise.data.local.AppDatabase
import com.alarmy.lumirise.model.SleepRecord
import kotlinx.coroutines.flow.Flow

class SleepRepository private constructor(context: Context) {
    
    private val sleepRecordDao = AppDatabase.getInstance(context).sleepRecordDao()
    
    val allCompleteSleepRecords: Flow<List<SleepRecord>> = sleepRecordDao.getAllCompleteSleepRecords()
    
    suspend fun getSleepRecordById(id: Long): SleepRecord? {
        return sleepRecordDao.getSleepRecordById(id)
    }
    
    suspend fun getActiveSleepRecord(): SleepRecord? {
        return sleepRecordDao.getActiveSleepRecord()
    }
    
    suspend fun insertSleepRecord(record: SleepRecord): Long {
        return sleepRecordDao.insertSleepRecord(record)
    }
    
    suspend fun updateSleepRecord(record: SleepRecord) {
        sleepRecordDao.updateSleepRecord(record)
    }
    
    suspend fun deleteSleepRecord(record: SleepRecord) {
        sleepRecordDao.deleteSleepRecord(record)
    }
    
    companion object {
        @Volatile
        private var INSTANCE: SleepRepository? = null
        
        fun getInstance(context: Context): SleepRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = SleepRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
