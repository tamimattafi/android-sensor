package com.attafitamim.sensor.widgets.card.blur

import android.graphics.Color

internal data class BlurFactor(
    val width: Int,
    val height: Int,
    val radius: Int = MAX_RADIUS,
    val padding: Int = DEFAULT_PADDING,
    val sampling: Int = DEFAULT_SAMPLING,
    val color: Int = Color.TRANSPARENT
) {
    companion object {
        const val MAX_RADIUS = 25
        const val DEFAULT_SAMPLING = 1
        const val DEFAULT_PADDING = 0
    }
}