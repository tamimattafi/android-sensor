package com.attafitamim.sensor.widgets.card

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.withTranslation
import com.attafitamim.sensor.core.base.Speed
import com.attafitamim.sensor.core.hardware.OrientationSensor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

class SensibleImageView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attributeSet, defStyleAttr) {

    private var sensor: OrientationSensor? = null

    private var initPitch: Double? = null
    private var initRoll: Double? = null

    private var translateX: Float = 0f
    private var translateY: Float = 0f

    private var shadowRadius: Float = MAX_BLUR_RADIUS
    private val shadowPadding get() = (shadowRadius * 2).roundToInt()

    private val shadowPaint = Paint()

    private var shadowBitmap: Bitmap? = null

    private val imageWidth get() = measuredWidth - paddingEnd - shadowPadding
    private val imageHeight get() = measuredHeight - paddingBottom - shadowPadding

    private var currentStabilizingLevel = 0

    init {
        context.withStyledAttributes(attributeSet, R.styleable.SensibleImageView) {
            val shadowRadiusAttribute = getDimension(
                R.styleable.SensibleImageView_shadowRadius,
                MAX_BLUR_RADIUS
            )

            shadowRadius = min(MAX_BLUR_RADIUS, shadowRadiusAttribute)

            shadowPaint.maskFilter = BlurMaskFilter(
                shadowRadius,
                BlurMaskFilter.Blur.NORMAL
            )
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        initSensor()
    }

    override fun onDetachedFromWindow() {
        destroySensor()
        super.onDetachedFromWindow()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        updateShadowBitmap()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        updateShadowBitmap()
    }

    override fun draw(canvas: Canvas) {
        val currentShadowBitmap = shadowBitmap

        if (currentShadowBitmap != null) {
            canvas.drawBitmap(currentShadowBitmap, shadowRadius, shadowRadius, shadowPaint)
        }

        canvas.withTranslation(translateX, translateY) {
            super.draw(this)
        }
    }

    private fun updateShadowBitmap() {
        val currentDrawable = drawable
        if (currentDrawable == null) {
            shadowBitmap = null
            return
        }

        val viewSize = imageWidth * imageHeight
        if (viewSize <= 0) return

        val currentShadowBitmap = shadowBitmap
        if (currentShadowBitmap != null) {
            val isWidthEqual = currentShadowBitmap.width == imageWidth
            val isHeightEqual = currentShadowBitmap.height == imageHeight

            if (isWidthEqual && isHeightEqual) return
        }

        shadowBitmap = currentDrawable.toBitmap(
            imageWidth,
            imageHeight
        ).getBlurBitmap(context, shadowRadius)
    }

    private fun initSensor() {
        if (sensor != null) return
        sensor = OrientationSensor(context, ::tryUpdateValues).apply {
            on(Speed.FASTEST)
        }
    }

    private fun destroySensor() {
        sensor?.apply {
            off()
            dispose()
        }

        sensor = null
    }

    private fun tryUpdateValues(azimuth: Double, pitch: Double, roll: Double) {
        if (currentStabilizingLevel < STABILIZING_LEVEL) {
            currentStabilizingLevel++
            return
        }

        val initPitch = this.initPitch ?: kotlin.run {
            this.initPitch = pitch
            pitch
        }

        val initRoll = this.initRoll ?: kotlin.run {
            this.initRoll = roll
            roll
        }

        val pitchDiff = initPitch - pitch
        val rollDiff = initRoll - roll

        val maxTranslate = shadowPadding.toFloat()
        val minTranslate = -shadowPadding.toFloat()

        val initialTranslateX = -(rollDiff * 1.5).toFixed(2).toFloat()
        val initialTranslateY = (pitchDiff * 1.5).toFixed(2).toFloat()

        this.translateX = when {
            initialTranslateX < minTranslate -> minTranslate
            initialTranslateX > maxTranslate -> maxTranslate
            else -> initialTranslateX
        }

        this.translateY = when {
            initialTranslateY < minTranslate -> minTranslate
            initialTranslateY > maxTranslate -> maxTranslate
            else -> initialTranslateY
        }

        invalidate()
    }

    private fun Double.toFixed(decimals: Int): Double {
        val base = 10.0.pow(decimals)
        return (this * base).roundToInt() / base
    }

    private companion object {
        private const val STABILIZING_LEVEL = 200
        private const val MAX_BLUR_RADIUS = 25f
    }
}
