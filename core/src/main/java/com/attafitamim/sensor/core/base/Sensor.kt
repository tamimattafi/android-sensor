package com.attafitamim.sensor.core.base

interface Sensor {
    val isSupport: Boolean
    fun on(speed: Speed)
    fun off()
    val maximumRange: Float
}
