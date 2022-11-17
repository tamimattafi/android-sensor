package com.attafitamim.sensor.widgets.card

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.withTranslation
import com.attafitamim.sensor.core.base.Speed
import com.attafitamim.sensor.core.hardware.OrientationSensor
import com.attafitamim.sensor.widgets.card.blur.BlurFactor
import com.attafitamim.sensor.widgets.card.blur.BlurProvider
import kotlin.math.pow
import kotlin.math.roundToInt

class SensibleImageCardView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attributeSet, defStyleAttr) {

    private var sensor: OrientationSensor? = null

    private var initPitch: Double? = null
    private var initRoll: Double? = null

    private var translateX: Float = 0f
    private var translateY: Float = 0f

    private var shadowRadius = DEFAULT_SHADOW_RADIUS
    private var shadowBlurSampling = DEFAULT_SHADOW_BLUR_SAMPLING
    private var shadowPadding = DEFAULT_SHADOW_PADDING

    private val shadowPaint = Paint()
    private var shadowBitmap: Bitmap? = null
    private var currentStabilizingLevel = 0

    private val shadowHeight get() = measuredHeight - shadowPadding * 2
    private val shadowWidth get() = measuredWidth - shadowPadding * 2

    private val blurProvider by lazy {
        BlurProvider(context)
    }

    init {
        context.withStyledAttributes(attributeSet, R.styleable.SensibleImageCardView) {
            shadowRadius = getInteger(
                R.styleable.SensibleImageCardView_shadowRadius,
                DEFAULT_SHADOW_RADIUS
            )

            shadowPaint.alpha = getInteger(
                R.styleable.SensibleImageCardView_shadowAlpha,
                DEFAULT_SHADOW_ALPHA
            )

            shadowBlurSampling = getInteger(
                R.styleable.SensibleImageCardView_shadowBlurSampling,
                DEFAULT_SHADOW_BLUR_SAMPLING
            )

            shadowPadding = getDimension(
                R.styleable.SensibleImageCardView_shadowPadding,
                DEFAULT_SHADOW_PADDING
            )

            val colorFilter = getColor(
                R.styleable.SensibleImageCardView_shadowColorFilter,
                DEFAULT_SHADOW_DARKEN_COLOR
            )

            shadowPaint.colorFilter = LightingColorFilter(colorFilter, Color.TRANSPARENT)
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
        fun drawImages(shadowBitmap: Bitmap?) {
            if (shadowBitmap != null) canvas.withTranslation(shadowPadding, shadowPadding) {
                drawBitmap(
                    shadowBitmap,
                    0f,
                    0f,
                    shadowPaint
                )
            }

            canvas.withTranslation(translateX, translateY) {
                super.draw(this)
            }
        }

        val currentDrawable = drawable
        if (currentDrawable == null) {
            shadowBitmap = null
            return
        }

        val viewSize = shadowWidth * shadowHeight
        if (viewSize <= 0) return

        val currentShadowBitmap = shadowBitmap
        if (currentShadowBitmap != null) {
            drawImages(currentShadowBitmap)
            return
        }

        Bitmap.createBitmap(
            measuredWidth,
            measuredHeight,
            Bitmap.Config.ARGB_8888
        ).applyCanvas {
            super.draw(this)
        }.also { newBitmap ->
            val factor = BlurFactor(
                newBitmap.width,
                newBitmap.height,
                shadowRadius,
                shadowPadding.roundToInt(),
                shadowBlurSampling
            )

            shadowBitmap = blurProvider.blur(newBitmap, factor)
            drawImages(shadowBitmap)
        }
    }

    private fun updateShadowBitmap() {

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

        val maxTranslate = shadowPadding + shadowRadius
        val minTranslate = -maxTranslate

        val initialTranslateX = -(rollDiff * 1.25).toFixed(2).toFloat()
        val initialTranslateY = (pitchDiff * 1.25).toFixed(2).toFloat()

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
        private const val STABILIZING_LEVEL = 50
        private const val DEFAULT_SHADOW_RADIUS = 25
        private const val DEFAULT_SHADOW_ALPHA = 242
        private const val DEFAULT_SHADOW_BLUR_SAMPLING = 1
        private const val DEFAULT_SHADOW_PADDING = 0f
        private const val DEFAULT_SHADOW_DARKEN_COLOR = 0xFF7F7F7F.toInt()
    }
}
