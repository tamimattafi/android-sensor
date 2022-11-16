package com.attafitamim.sensor.core.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.attafitamim.sensor.core.base.Speed
import java.util.Observable

class GyroscopeSensor(context: Context) : Observable(), SensorEventListener,
    com.attafitamim.sensor.core.base.Sensor {

    private val sensorManager: SensorManager
    private val gyroscopeSensor: Sensor?

    var event: SensorEvent? = null
        private set

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    override val isSupport: Boolean
        get() = gyroscopeSensor != null

    override fun on(speed: Speed) {
        when (speed) {
            Speed.NORMAL -> sensorManager.registerListener(
                this,
                gyroscopeSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )

            Speed.UI -> sensorManager.registerListener(
                this,
                gyroscopeSensor,
                SensorManager.SENSOR_DELAY_UI
            )

            Speed.GAME -> sensorManager.registerListener(
                this,
                gyroscopeSensor,
                SensorManager.SENSOR_DELAY_GAME
            )

            Speed.FASTEST -> sensorManager.registerListener(
                this,
                gyroscopeSensor,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }
    }

    override fun off() {
        sensorManager.unregisterListener(this, gyroscopeSensor)
    }

    override val maximumRange: Float
        get() = gyroscopeSensor!!.maximumRange

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        event = sensorEvent
        setChanged()
        notifyObservers()
    }

    override fun onAccuracyChanged(sensor: Sensor, i: Int) {
    }
}
