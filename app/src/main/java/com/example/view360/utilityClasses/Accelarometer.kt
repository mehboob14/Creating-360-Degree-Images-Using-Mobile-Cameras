package com.example.view360.utilityClasses

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.PI
import kotlin.math.atan2

class Accelarometer(private val context: Context, private val onSensorUpdate: (FloatArray) -> Unit) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    var referenceX = 0f
    var referenceY = 0f
    var referenceZ = 9.81f

    private fun toDegrees(radians: Float): Double {
        return radians * (180.0 / PI)
    }

    private fun calculateTiltAngle(y: Float,x: Float): Double {
        return toDegrees(atan2(y,x))
    }

    fun setReferenceValues(x: Float = 0f, y: Float = 0f, z: Float = 9.81f) {
        referenceX = x
        referenceY = y
        referenceZ = z
    }

    fun calculateTiltAngles(x: Float, y: Float, z: Float): DoubleArray{
        val angleX = calculateTiltAngle(y, z)
        val angleY = calculateTiltAngle(y, z)
        val angleZ = calculateTiltAngle(y, z)

        return doubleArrayOf(angleX,angleY,angleZ)
    }


    fun startSensor(): Boolean {
        if (accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI)
            return true
        }
        return false
    }

    fun stopSensor() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            onSensorUpdate(it.values)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
