package com.example.view360.utilityClasses

import android.content.Context
import android.hardware.*
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import kotlin.math.abs

class DeviceRotationMngr(
    private val context: Context,
    private val onSensorUpdate: (FloatArray) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val accelerometerValues = FloatArray(3)
    private val magnetometerValues = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val referenceAngles = FloatArray(3)
    private var currentAngles = FloatArray(3)

    // Low-pass filter constant (adjust as needed for smoothing)
    private val alpha = 0.1f

    fun setReferenceAngles() {
        for (i in 0 until 3) {
            referenceAngles[i] = currentAngles[i]
        }
    }

    fun calculateDifference() : FloatArray{
        val difference = FloatArray(3)
        for (i in 0 until 3) {
            difference[i] = referenceAngles[i] - currentAngles[i]
        }
        return difference
    }

    fun startListening() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> applyLowPassFilter(event.values, accelerometerValues)
            Sensor.TYPE_MAGNETIC_FIELD -> applyLowPassFilter(event.values, magnetometerValues)
        }

        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerValues, magnetometerValues)) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()  // X-axis rotation
            val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat() // Y-axis rotation
            var yaw = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()   // Z-axis rotation (Azimuth)



            // Adjust yaw based on device rotation
            yaw = adjustYawForScreenRotation(yaw)
            currentAngles = floatArrayOf(abs(roll), abs(pitch),abs(yaw))
            onSensorUpdate(calculateDifference())

        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // Apply a low-pass filter to smooth sensor values
    private fun applyLowPassFilter(input: FloatArray, output: FloatArray) {
        if (output[0] == 0f && output[1] == 0f && output[2] == 0f) {
            // Initialize with first reading
            input.copyInto(output)
        } else {
            for (i in input.indices) {
                output[i] = output[i] + alpha * (input[i] - output[i])
            }
        }
    }

    // Adjust yaw (azimuth) based on screen rotation
    private fun adjustYawForScreenRotation(yaw: Float): Float {
        val rotation = windowManager.defaultDisplay.rotation
        return when (rotation) {
            Surface.ROTATION_90 -> (yaw + 90) % 360
            Surface.ROTATION_180 -> (yaw + 180) % 360
            Surface.ROTATION_270 -> (yaw + 270) % 360
            else -> yaw
        }
    }
}
