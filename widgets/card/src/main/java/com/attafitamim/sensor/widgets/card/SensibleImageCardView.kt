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

    private var sensibleElement = SensibleElement.SHADOW

    private val blurProvider by lazy {
        BlurProvider(context)
    }

    init {
        context.withStyledAttributes(attributeSet, R.styleable.SensibleImageCardView) {
            shadowRadius = getInt(
                R.styleable.SensibleImageCardView_shadowRadius,
                DEFAULT_SHADOW_RADIUS
            )

            shadowPaint.alpha = getInt(
                R.styleable.SensibleImageCardView_shadowAlpha,
                DEFAULT_SHADOW_ALPHA
            )

            shadowBlurSampling = getInt(
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

            val shadowElementOrdinal = getInt(
                R.styleable.SensibleImageCardView_sensibleElement,
                SensibleElement.SHADOW.ordinal
            )

            sensibleElement = SensibleElement.values()[shadowElementOrdinal]

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
            if (shadowBitmap != null) {
                val isShadowSensible = sensibleElement == SensibleElement.ALL ||
                        sensibleElement == SensibleElement.SHADOW

                var shadowTranslationX = shadowPadding
                var shadowTranslationY = shadowPadding

                if (isShadowSensible) {
                    shadowTranslationX -= translateX
                    shadowTranslationY -= translateY
                }

                canvas.withTranslation(shadowTranslationX, shadowTranslationY) {
                    drawBitmap(
                        shadowBitmap,
                        0f,
                        0f,
                        shadowPaint
                    )
                }
            }

            val isSensibleImage = sensibleElement == SensibleElement.ALL ||
                    sensibleElement == SensibleElement.IMAGE

            var imageTranslationX = 0f
            var imageTranslationY = 0f

            if (isSensibleImage) {
                imageTranslationX += translateX
                imageTranslationY += translateY
            }

            canvas.withTranslation(imageTranslationX, imageTranslationY) {
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

        val processedRoll = when {
            roll >= MAX_ROLL_DEGREE -> roll - MAX_FLAT_DEGREE
            roll <= -MAX_ROLL_DEGREE -> roll + MAX_FLAT_DEGREE
            else -> roll
        }

        val initPitch = this.initPitch ?: kotlin.run {
            this.initPitch = pitch
            pitch
        }

        val initRoll = this.initRoll ?: kotlin.run {
            this.initRoll = processedRoll
            processedRoll
        }

        val pitchDiff: Double = initPitch - pitch
        val rollDiff: Double = initRoll - processedRoll

        val maxTranslateX = paddingStart.toFloat()
        val minTranslateX = -paddingEnd.toFloat()

        val maxTranslateY = paddingTop.toFloat()
        val minTranslateY = -paddingBottom.toFloat()

        val initialTranslateX = -(rollDiff * 1.5).toFixed(2).toFloat()
        val initialTranslateY = (pitchDiff * 1.5).toFixed(2).toFloat()

        val finalTranslateX = when {
            initialTranslateX < minTranslateX -> minTranslateX
            initialTranslateX > maxTranslateX -> maxTranslateX
            else -> initialTranslateX
        }

        val finalTranslateY = when {
            initialTranslateY < minTranslateY -> minTranslateY
            initialTranslateY > maxTranslateY -> maxTranslateY
            else -> initialTranslateY
        }

        if (this.translateX == finalTranslateX && this.translateY == finalTranslateY) return

        this.translateX = finalTranslateX
        this.translateY = finalTranslateY
        invalidate()
    }

    enum class SensibleElement {
        SHADOW,
        IMAGE,
        ALL,
        NONE
    }

    private fun Double.toFixed(decimals: Int): Double {
        val base = 10.0.pow(decimals)
        return (this * base).roundToInt() / base
    }

    private companion object {
        private const val STABILIZING_LEVEL = 100
        private const val DEFAULT_SHADOW_RADIUS = 25
        private const val DEFAULT_SHADOW_ALPHA = 255
        private const val DEFAULT_SHADOW_BLUR_SAMPLING = 1
        private const val DEFAULT_SHADOW_PADDING = 0f
        private const val DEFAULT_SHADOW_DARKEN_COLOR = 0xFF7F7F7F.toInt()

        private const val MAX_ROLL_DEGREE = 170f
        private const val MAX_FLAT_DEGREE = 180f
    }
}
