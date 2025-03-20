package com.example.view360.utilityClasses

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.CUPCAKE)
class MovementDetector(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var velocityY = 0f
    private var velocityZ = 0f
    private var gravityY = 0f
    private var gravityZ = 0f
    private var lastTimestamp: Long = 0
    private var velocities: Pair<Float, Float> = Pair(0f, 0f)

    fun startListening() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val alpha = 0.8f // High-pass filter factor

            // Raw acceleration values
            val rawAccelY = it.values[1] // Y-axis
            val rawAccelZ = it.values[2] // Z-axis

            // Remove gravity (low-pass filter)
            gravityY = alpha * gravityY + (1 - alpha) * rawAccelY
            gravityZ = alpha * gravityZ + (1 - alpha) * rawAccelZ

            // Apply high-pass filter to get real movement
            val accelY = rawAccelY - gravityY
            val accelZ = rawAccelZ - gravityZ

            val currentTime = it.timestamp
            if (lastTimestamp != 0L) {
                val deltaTime = (currentTime - lastTimestamp) / 1_000_000_000f // Convert to seconds
                velocityY += accelY * deltaTime
                velocityZ += accelZ * deltaTime
                velocities = Pair(velocityY, velocityZ)
            }
            lastTimestamp = currentTime
        }
    }


    fun getVelocities(): Pair<Float, Float> = velocities

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

