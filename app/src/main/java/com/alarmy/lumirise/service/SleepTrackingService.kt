package com.alarmy.lumirise.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.alarmy.lumirise.R
import com.alarmy.lumirise.data.local.AppDatabase
import com.alarmy.lumirise.model.SleepRecord
import com.alarmy.lumirise.model.SnoreEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Foreground service for overnight sleep tracking with microphone recording.
 * Records audio at 16kHz mono PCM, passes chunks to SnoreDetector for analysis,
 * and saves detected snore events to the database.
 */
class SleepTrackingService : Service() {

    companion object {
        const val CHANNEL_ID = "sleep_tracking_channel"
        const val NOTIFICATION_ID = 2
        
        const val EXTRA_SLEEP_RECORD_ID = "sleep_record_id"
        
        // Audio recording parameters - 16kHz for battery efficiency
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val BUFFER_SIZE_FACTOR = 2
        
        // Chunk duration: 1 second of audio for snore detection
        const val CHUNK_DURATION_MS = 1000
        val CHUNK_SIZE_SAMPLES: Int get() = SAMPLE_RATE * CHUNK_DURATION_MS / 1000
        
        // Tracking state
        private val _isTracking = MutableStateFlow(false)
        val isTracking: StateFlow<Boolean> = _isTracking
        
        private val _currentSleepRecordId = MutableStateFlow<Long?>(null)
        val currentSleepRecordId: StateFlow<Long?> = _currentSleepRecordId
        
        private val _snoreCount = MutableStateFlow(0)
        val snoreCount: StateFlow<Int> = _snoreCount
        
        fun start(context: Context, sleepRecordId: Long) {
            val intent = Intent(context, SleepTrackingService::class.java).apply {
                putExtra(EXTRA_SLEEP_RECORD_ID, sleepRecordId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            context.stopService(Intent(context, SleepTrackingService::class.java))
        }
    }
    
    private var audioRecord: AudioRecord? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var recordingJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private lateinit var database: AppDatabase
    
    // Circular buffer for continuous audio recording
    private val audioBuffer = ConcurrentLinkedQueue<Short>()
    private val bufferLock = Any()
    
    // Current tracking state
    private var currentSleepRecordId: Long = -1
    
    // Snore detector interface (implementation provided by T17)
    var snoreDetector: SnoreDetector? = null
    
    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(applicationContext)
        createNotificationChannel()
        acquireWakeLock()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        currentSleepRecordId = intent?.getLongExtra(EXTRA_SLEEP_RECORD_ID, -1) ?: -1
        
        if (currentSleepRecordId == -1L) {
            // Create a new sleep record if not provided
            serviceScope.launch {
                val record = SleepRecord(startTime = System.currentTimeMillis())
                currentSleepRecordId = database.sleepRecordDao().insertSleepRecord(record)
                _currentSleepRecordId.value = currentSleepRecordId
            }
        } else {
            _currentSleepRecordId.value = currentSleepRecordId
        }
        
        // Start foreground with appropriate service type for Android 14+
        val notification = createNotification("Sleep tracking started...", 0)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID, 
                notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, 
                notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        // Reset snore count for this session
        _snoreCount.value = 0
        
        // Start audio recording
        startRecording()
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        stopRecording()
        serviceScope.launch {
            saveSleepEndTime()
        }
        releaseWakeLock()
        serviceScope.cancel()
        _isTracking.value = false
        _currentSleepRecordId.value = null
        super.onDestroy()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sleep Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows sleep tracking status during the night"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(status: String, snoreCount: Int): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, SleepTrackingService::class.java).let { intent ->
            PendingIntent.getService(
                this,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sleep Tracking Active")
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_alarm) // Use existing alarm icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setContentIntent(contentIntent)
        
        if (snoreCount > 0) {
            builder.setSubText("$snoreCount snore events detected")
        }
        
        // Add stop action for user convenience
        builder.addAction(
            R.drawable.ic_alarm,
            "Stop Tracking",
            stopIntent
        )
        
        return builder.build()
    }
    
    private fun updateNotification(status: String, snoreCount: Int) {
        val notification = createNotification(status, snoreCount)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "LumiRise:SleepTrackingWakeLock"
        ).apply {
            // Indefinite wake lock for overnight tracking
            // Will be released when service is destroyed
            acquire(12 * 60 * 60 * 1000L) // 12 hours max
        }
    }
    
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }
    
    private fun startRecording() {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )
        
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            stopSelf()
            return
        }
        
        val bufferSize = minBufferSize * BUFFER_SIZE_FACTOR
        
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                stopSelf()
                return
            }
            
            audioRecord?.startRecording()
            _isTracking.value = true
            
            recordingJob = serviceScope.launch {
                // Buffer for 1 second chunks (16kHz * 1s = 16000 samples)
                val buffer = ShortArray(CHUNK_SIZE_SAMPLES)
                var chunkCount = 0
                
                while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    
                    if (bytesRead > 0) {
                        // Add to circular buffer for potential future use
                        synchronized(bufferLock) {
                            for (i in 0 until bytesRead) {
                                audioBuffer.offer(buffer[i])
                                // Keep buffer size manageable (last 30 seconds)
                                while (audioBuffer.size > SAMPLE_RATE * 30) {
                                    audioBuffer.poll()
                                }
                            }
                        }
                        
                        // Process chunk for snore detection
                        chunkCount++
                        processAudioChunk(buffer.copyOf(bytesRead), chunkCount)
                    }
                    
                    // Small delay to prevent CPU overuse while maintaining near real-time processing
                    delay(10)
                }
            }
            
        } catch (e: SecurityException) {
            e.printStackTrace()
            stopSelf()
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }
    
    private fun processAudioChunk(audioSamples: ShortArray, chunkNumber: Int) {
        val detector = snoreDetector ?: return
        
        serviceScope.launch {
            try {
                // Convert ShortArray to ByteArray for the detector
                val audioBytes = ByteArray(audioSamples.size * 2)
                for (i in audioSamples.indices) {
                    audioBytes[i * 2] = (audioSamples[i].toInt() and 0xFF).toByte()
                    audioBytes[i * 2 + 1] = (audioSamples[i].toInt() shr 8 and 0xFF).toByte()
                }
                
                val result = detector.analyzeChunk(audioBytes)
                
                if (result.isSnore) {
                    // Save snore event to database
                    saveSnoreEvent(result)
                    
                    // Update UI state
                    _snoreCount.value++
                    
                    // Update notification periodically (every 10 chunks to reduce overhead)
                    if (chunkNumber % 10 == 0) {
                        val status = formatTrackingStatus(chunkNumber)
                        withContext(Dispatchers.Main) {
                            updateNotification(status, _snoreCount.value)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun formatTrackingStatus(chunkNumber: Int): String {
        val durationMinutes = (chunkNumber * CHUNK_DURATION_MS) / 60000
        return when {
            durationMinutes < 60 -> "Tracking: ${durationMinutes}min"
            else -> {
                val hours = durationMinutes / 60
                val mins = durationMinutes % 60
                "Tracking: ${hours}h ${mins}m"
            }
        }
    }
    
    private suspend fun saveSnoreEvent(result: SnoreDetectorResult) {
        if (currentSleepRecordId == -1L) return
        
        val event = SnoreEvent(
            sleepRecordId = currentSleepRecordId,
            timestamp = System.currentTimeMillis(),
            durationMs = result.durationMs,
            confidence = result.confidence
        )
        
        database.snoreEventDao().insertSnoreEvent(event)
    }
    
    private suspend fun saveSleepEndTime() {
        if (currentSleepRecordId == -1L) return
        
        try {
            val record = database.sleepRecordDao().getSleepRecordById(currentSleepRecordId)
            record?.let {
                val updatedRecord = it.copy(
                    endTime = System.currentTimeMillis(),
                    totalSnoreCount = _snoreCount.value,
                    isComplete = true
                )
                database.sleepRecordDao().updateSleepRecord(updatedRecord)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun stopRecording() {
        _isTracking.value = false
        recordingJob?.cancel()
        recordingJob = null
        
        audioRecord?.let {
            if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                it.stop()
            }
            it.release()
        }
        audioRecord = null
        
        audioBuffer.clear()
    }
    
    /**
     * Get the current audio buffer for external processing.
     * Returns a copy of the buffer to avoid concurrency issues.
     */
    fun getRecentAudioBuffer(): ShortArray {
        synchronized(bufferLock) {
            return audioBuffer.toShortArray()
        }
    }
    
    /**
     * Get the RMS amplitude of the current audio buffer.
     * Useful for visualizing audio levels.
     */
    fun getCurrentAmplitude(): Float {
        synchronized(bufferLock) {
            if (audioBuffer.isEmpty()) return 0f
            
            var sum = 0L
            for (sample in audioBuffer) {
                sum += sample * sample
            }
            val rms = kotlin.math.sqrt(sum.toDouble() / audioBuffer.size)
            return (rms / Short.MAX_VALUE).toFloat()
        }
    }
    
    /**
     * Interface for snore detection.
     * Implementation will be provided by T17.
     */
    interface SnoreDetector {
        suspend fun analyzeChunk(audioData: ByteArray): SnoreDetectorResult
    }
    
    /**
     * Result from snore detection analysis.
     */
    data class SnoreDetectorResult(
        val isSnore: Boolean,
        val confidence: Float,
        val durationMs: Int
    )
}
