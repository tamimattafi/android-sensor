package com.attafitamim.sensor.core.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.attafitamim.sensor.core.base.Speed
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.util.Observable

class AccelerometerSensor(context: Context) : Observable(), SensorEventListener,
    com.attafitamim.sensor.core.base.Sensor {

    private val sensorManager: SensorManager
    private val accelerometerSensor: Sensor?

    var event: SensorEvent? = null
        private set

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override val isSupport: Boolean
        get() = accelerometerSensor != null

    override fun on(speed: Speed) {
        when (speed) {
            Speed.NORMAL -> sensorManager.registerListener(
                this,
                accelerometerSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )

            Speed.UI -> sensorManager.registerListener(
                this,
                accelerometerSensor,
                SensorManager.SENSOR_DELAY_UI
            )

            Speed.GAME -> sensorManager.registerListener(
                this,
                accelerometerSensor,
                SensorManager.SENSOR_DELAY_GAME
            )

            Speed.FASTEST -> sensorManager.registerListener(
                this,
                accelerometerSensor,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }
    }

    override fun off() {
        sensorManager.unregisterListener(this, accelerometerSensor)
    }

    override val maximumRange: Float
        get() = accelerometerSensor!!.maximumRange

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        event = sensorEvent
        setChanged()
        notifyObservers()
    }

    override fun onAccuracyChanged(sensor: Sensor, i: Int) {}
}
