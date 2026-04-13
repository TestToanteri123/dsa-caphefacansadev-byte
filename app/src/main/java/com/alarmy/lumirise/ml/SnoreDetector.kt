package com.alarmy.lumirise.ml

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.abs
import kotlin.math.max

/**
 * SnoreDetector uses YAMNet TensorFlow Lite model for audio-based snoring detection.
 * 
 * YAMNet specifications:
 * - Sample rate: 16kHz
 * - Input: 15600 samples (0.96 seconds)
 * - Output: 521 AudioSet classes
 * - Snoring class index: 489
 * 
 * Detection runs on a background thread and emits events via SharedFlow.
 */
class SnoreDetector(private val context: Context) {
    
    companion object {
        private const val TAG = "SnoreDetector"
        
        // YAMNet expects 16kHz mono PCM 16-bit audio
        const val SAMPLE_RATE = 16000
        
        // YAMNet frame size: 0.96 seconds at 16kHz = 15600 samples
        const val FRAME_SIZE = 15600
        
        // Snoring detection interval: 1 second
        const val DETECTION_INTERVAL_MS = 1000L
        
        // Snoring class index in AudioSet (YAMNet class 489)
        const val SNORING_CLASS_INDEX = 489
        
        // Confidence threshold for snoring detection
        const val SNORING_CONFIDENCE_THRESHOLD = 0.35f
        
        // Model filename in assets
        private const val MODEL_FILENAME = "yamnet.tflite"
        
        // Minimum audio amplitude to consider (noise gate)
        private const val MIN_AMPLITUDE_THRESHOLD = 100f
    }
    
    // TFLite interpreter for inference
    private var interpreter: Interpreter? = null
    
    // Audio recording state
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    
    // Background thread for processing
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    
    // Circular buffer for audio samples
    private val audioBuffer = ShortArray(FRAME_SIZE)
    private var bufferIndex = 0
    private var samplesInBuffer = 0
    
    // Detection callback flow
    private val _snoreEvents = MutableSharedFlow<SnoreEvent>(
        replay = 0,
        extraBufferCapacity = 10
    )
    val snoreEvents: SharedFlow<SnoreEvent> = _snoreEvents.asSharedFlow()
    
    // State callbacks
    private val _detectionState = MutableSharedFlow<DetectionState>(
        replay = 1,
        extraBufferCapacity = 1
    )
    val detectionState: SharedFlow<DetectionState> = _detectionState.asSharedFlow()
    
    // Consecutive snoring frames for confirmation
    private var consecutiveSnoreFrames = 0
    private val requiredConsecutiveFrames = 2
    
    // Model loaded flag
    @Volatile
    private var isModelLoaded = false
    
    /**
     * SnoreEvent emitted when snoring is detected
     */
    data class SnoreEvent(
        val confidence: Float,
        val timestamp: Long = System.currentTimeMillis(),
        val audioAmplitude: Float
    )
    
    /**
     * Detection state for UI feedback
     */
    sealed class DetectionState {
        data object Loading : DetectionState()
        data object Ready : DetectionState()
        data class Listening(val amplitude: Float) : DetectionState()
        data class SnoringDetected(val confidence: Float, val amplitude: Float) : DetectionState()
        data class Error(val message: String) : DetectionState()
    }
    
    /**
     * Load YAMNet model from assets asynchronously
     */
    suspend fun loadModel() {
        _detectionState.emit(DetectionState.Loading)
        
        try {
            val modelBuffer = loadModelFile()
            if (modelBuffer == null) {
                _detectionState.emit(DetectionState.Error("Failed to load model from assets"))
                return
            }
            
            val options = Interpreter.Options().apply {
                numThreads = 2
                useNNAPI = false
            }
            
            interpreter = Interpreter(modelBuffer, options)
            isModelLoaded = true
            _detectionState.emit(DetectionState.Ready)
            
            Log.d(TAG, "YAMNet model loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model", e)
            _detectionState.emit(DetectionState.Error("Model loading failed: ${e.message}"))
        }
    }
    
    /**
     * Load model file from assets folder
     */
    private fun loadModelFile(): MappedByteBuffer? {
        return try {
            val assetFileDescriptor: AssetFileDescriptor = context.assets.openFd(MODEL_FILENAME)
            val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model file: ${e.message}")
            null
        }
    }
    
    /**
     * Start listening for audio and detecting snoring
     */
    @Suppress("MissingPermission")
    fun startListening() {
        if (!isModelLoaded) {
            Log.e(TAG, "Model not loaded, cannot start listening")
            return
        }
        
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return
        }
        
        // Start background thread
        handlerThread = HandlerThread("SnoreDetectorThread").apply {
            start()
            handler = Handler(looper)
        }
        
        // Configure audio recording
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        // Use larger buffer for smoother recording
        val audioBufferSize = max(bufferSize, FRAME_SIZE * 2)
        
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            audioBufferSize
        )
        
        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord initialization failed")
            _detectionState.tryEmit(DetectionState.Error("Microphone initialization failed"))
            return
        }
        
        isRecording = true
        audioRecord?.startRecording()
        
        // Start processing loop
        handler?.post { processAudioLoop() }
        
        Log.d(TAG, "Started listening for audio")
    }
    
    /**
     * Stop listening for audio
     */
    fun stopListening() {
        isRecording = false
        
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        }
        
        handlerThread?.quitSafely()
        handlerThread = null
        handler = null
        
        // Reset buffer
        bufferIndex = 0
        samplesInBuffer = 0
        consecutiveSnoreFrames = 0
        
        Log.d(TAG, "Stopped listening")
    }
    
    /**
     * Main audio processing loop
     */
    private fun processAudioLoop() {
        val buffer = ShortArray(FRAME_SIZE)
        
        while (isRecording && audioRecord != null) {
            try {
                val readCount = audioRecord?.read(buffer, 0, FRAME_SIZE) ?: 0
                
                if (readCount > 0) {
                    // Calculate audio amplitude
                    val amplitude = calculateAmplitude(buffer, readCount)
                    _detectionState.tryEmit(DetectionState.Listening(amplitude))
                    
                    // Add samples to circular buffer
                    for (i in 0 until readCount) {
                        audioBuffer[bufferIndex] = buffer[i]
                        bufferIndex = (bufferIndex + 1) % FRAME_SIZE
                        samplesInBuffer = minOf(samplesInBuffer + 1, FRAME_SIZE)
                    }
                    
                    // Run inference when we have enough samples
                    if (samplesInBuffer >= FRAME_SIZE) {
                        runInference()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in audio processing loop", e)
            }
        }
    }
    
    /**
     * Calculate RMS amplitude of audio buffer
     */
    private fun calculateAmplitude(buffer: ShortArray, length: Int): Float {
        var sum = 0L
        for (i in 0 until length) {
            sum += buffer[i].toInt() * buffer[i].toInt()
        }
        val rms = kotlin.math.sqrt(sum.toDouble() / length)
        return rms.toFloat()
    }
    
    /**
     * Run YAMNet inference on current audio buffer
     */
    private fun runInference() {
        val interpreter = this.interpreter ?: return
        
        // Prepare input buffer (float32, normalized from int16)
        val byteBuffer = ByteBuffer.allocateDirect(FRAME_SIZE * 4).apply {
            order(ByteOrder.nativeOrder())
        }
        val inputBuffer = byteBuffer.asFloatBuffer()
        
        // Copy audio samples from circular buffer to input
        val startIndex = (bufferIndex - FRAME_SIZE + FRAME_SIZE) % FRAME_SIZE
        for (i in 0 until FRAME_SIZE) {
            val idx = (startIndex + i) % FRAME_SIZE
            // Normalize to [-1.0, 1.0] range
            val normalizedSample = audioBuffer[idx] / 32768f
            inputBuffer.put(normalizedSample)
        }
        inputBuffer.rewind()
        
        // Prepare output buffer: [1, 521] (YAMNet has 521 classes)
        val outputBuffer = Array(1) { FloatArray(521) }
        
        try {
            // Run inference
            interpreter.run(inputBuffer, outputBuffer)
            
            // Get snoring score
            val snoringScore = outputBuffer[0][SNORING_CLASS_INDEX]
            
            // Calculate current amplitude
            val amplitude = calculateAmplitude(audioBuffer, FRAME_SIZE)
            
            // Emit state update
            _detectionState.tryEmit(
                DetectionState.SnoringDetected(snoringScore, amplitude)
            )
            
            // Check if snoring detected with sufficient confidence
            if (snoringScore > SNORING_CONFIDENCE_THRESHOLD && amplitude > MIN_AMPLITUDE_THRESHOLD) {
                consecutiveSnoreFrames++
                
                // Require consecutive frames to confirm snoring (reduces false positives)
                if (consecutiveSnoreFrames >= requiredConsecutiveFrames) {
                    emitSnoreEvent(snoringScore, amplitude)
                    consecutiveSnoreFrames = 0 // Reset after emitting
                }
            } else {
                consecutiveSnoreFrames = max(0, consecutiveSnoreFrames - 1)
            }
            
            Log.d(TAG, "Inference result: snoring=$snoringScore, amplitude=$amplitude")
            
        } catch (e: Exception) {
            Log.e(TAG, "Inference error", e)
        }
    }
    
    /**
     * Emit snore event to subscribers
     */
    private fun emitSnoreEvent(confidence: Float, amplitude: Float) {
        val event = SnoreEvent(
            confidence = confidence,
            timestamp = System.currentTimeMillis(),
            audioAmplitude = amplitude
        )
        
        _snoreEvents.tryEmit(event)
        Log.i(TAG, "Snoring detected! Confidence: $confidence, Amplitude: $amplitude")
    }
    
    /**
     * Release all resources
     */
    fun release() {
        stopListening()
        
        try {
            interpreter?.close()
            interpreter = null
            isModelLoaded = false
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing resources", e)
        }
        
        Log.d(TAG, "Resources released")
    }
    
    /**
     * Check if model is loaded
     */
    fun isReady(): Boolean = isModelLoaded && interpreter != null
    
    /**
     * Check if currently recording
     */
    fun isListening(): Boolean = isRecording
}
