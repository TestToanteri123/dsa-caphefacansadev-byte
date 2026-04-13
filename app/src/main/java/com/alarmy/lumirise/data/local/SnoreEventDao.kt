package com.alarmy.lumirise.data.local

import androidx.room.*
import com.alarmy.lumirise.model.SnoreEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface SnoreEventDao {
    @Query("SELECT * FROM snore_events WHERE sleepRecordId = :sleepRecordId ORDER BY timestamp")
    fun getSnoreEventsForRecord(sleepRecordId: Long): Flow<List<SnoreEvent>>
    
    @Query("SELECT * FROM snore_events WHERE sleepRecordId = :sleepRecordId ORDER BY timestamp")
    suspend fun getSnoreEventsForRecordSync(sleepRecordId: Long): List<SnoreEvent>
    
    @Query("SELECT COUNT(*) FROM snore_events WHERE sleepRecordId = :sleepRecordId")
    suspend fun getSnoreCountForRecord(sleepRecordId: Long): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnoreEvent(event: SnoreEvent): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnoreEvents(events: List<SnoreEvent>)
    
    @Query("DELETE FROM snore_events WHERE sleepRecordId = :sleepRecordId")
    suspend fun deleteSnoreEventsForRecord(sleepRecordId: Long)
}
