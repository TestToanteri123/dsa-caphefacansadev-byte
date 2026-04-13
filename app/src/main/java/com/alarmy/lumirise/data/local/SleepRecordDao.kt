package com.alarmy.lumirise.data.local

import androidx.room.*
import com.alarmy.lumirise.model.SleepRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepRecordDao {
    @Query("SELECT * FROM sleep_records WHERE isComplete = 1 ORDER BY startTime DESC")
    fun getAllCompleteSleepRecords(): Flow<List<SleepRecord>>
    
    @Query("SELECT * FROM sleep_records WHERE id = :id")
    suspend fun getSleepRecordById(id: Long): SleepRecord?
    
    @Query("SELECT * FROM sleep_records WHERE isComplete = 0 LIMIT 1")
    suspend fun getActiveSleepRecord(): SleepRecord?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepRecord(record: SleepRecord): Long
    
    @Update
    suspend fun updateSleepRecord(record: SleepRecord)
    
    @Delete
    suspend fun deleteSleepRecord(record: SleepRecord)
}
