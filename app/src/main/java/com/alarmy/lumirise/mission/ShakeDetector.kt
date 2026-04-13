package com.alarmy.lumirise.mission

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context
import kotlin.math.sqrt

class ShakeDetector(
    context: Context,
    private val onShakeDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastShakeTime: Long = 0
    private var isListening = false

    companion object {
        const val SHAKE_THRESHOLD_G = 2.7f
        const val SHAKE_COOLDOWN_MS = 500L
    }

    fun start() {
        if (isListening) return
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            isListening = true
        }
    }

    fun stop() {
        if (!isListening) return
        sensorManager.unregisterListener(this)
        isListening = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val acceleration = kotlin.math.sqrt(x * x + y * y + z * z)
        val accelerationG = acceleration / SensorManager.GRAVITY_EARTH

        val currentTime = System.currentTimeMillis()

        if (accelerationG > SHAKE_THRESHOLD_G &&
            currentTime - lastShakeTime > SHAKE_COOLDOWN_MS
        ) {
            lastShakeTime = currentTime
            onShakeDetected()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun reset() {
        lastShakeTime = 0
    }
}
