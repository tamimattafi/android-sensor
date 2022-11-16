package com.attafitamim.sensor.core.providers

import com.attafitamim.sensor.core.hardware.OrientationSensor
import kotlin.math.abs

class OrientationResponseProvider(orientationDelegate: OrientationSensor.Delegate) {

    private val sensorValueLog = ArrayList<Double>()
    private val tolerance = ArrayList<Double>()
    private val observer: OrientationSensor.Delegate

    init {
        sensorValueLog.add(0, 0.0)
        sensorValueLog.add(1, 0.0)
        sensorValueLog.add(2, 0.0)
        tolerance.add(0, 0.0)
        tolerance.add(1, 0.0)
        tolerance.add(2, 0.0)
        observer = orientationDelegate
    }

    fun init(azimuthTol: Double, pitchTol: Double, rollTol: Double) {
        tolerance.add(0, azimuthTol)
        tolerance.add(1, pitchTol)
        tolerance.add(2, rollTol)
    }

    fun dispatcher(gyroOrientation: FloatArray) {
        var azimuth = gyroOrientation[0] * 180 / Math.PI
        if (azimuth < 0) azimuth += 360.0
        val pitch = gyroOrientation[1] * 180 / Math.PI
        val roll = gyroOrientation[2] * 180 / Math.PI
        if (abs(sensorValueLog[0] - azimuth) > tolerance[0] || abs(
                sensorValueLog[1] - pitch
            ) > tolerance[1] || abs(sensorValueLog[2] - roll) > tolerance[2]
        ) {
            sensorValueLog[0] = azimuth
            sensorValueLog[1] = pitch
            sensorValueLog[2] = roll
            observer.onOrientation(sensorValueLog[0], sensorValueLog[1], sensorValueLog[2])
        }
    }
}
