@file:Suppress("DEPRECATION")

package com.attafitamim.sensor.widgets.card

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LightingColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import androidx.core.graphics.applyCanvas
import com.attafitamim.sensor.widgets.card.blur.BlurAlgorithm
import com.attafitamim.sensor.widgets.card.blur.RenderScriptBlur
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.math.min
import kotlin.math.roundToInt

private const val PAINT_FLAGS = Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG
private val DEFAULT_PAINT = Paint(PAINT_FLAGS)

private const val MAX_BLUR_RADIUS = 25f
private const val MIN_BLUR_RADIUS = 1f

val ByteBuffer.byteArray: ByteArray
    get() {
        this.rewind()
        val bytes = ByteArray(this.remaining())
        this.get(bytes)
        return bytes
    }

fun Bitmap.getBytes(): ByteArray {
    val byteBuffer = ByteBuffer.allocate(allocationByteCount)
    copyPixelsToBuffer(byteBuffer)
    return byteBuffer.byteArray
}

/**
 * Code copied from glide library: TransformationUtils.fitCenter()
 */
fun Bitmap.getFitCenterBitmap(width: Int, height: Int): Bitmap {
    val widthPercentage = width / this.width.toFloat()
    val heightPercentage = height / this.height.toFloat()
    val minPercentage = min(widthPercentage, heightPercentage)

    var targetWidth = (minPercentage * this.width).roundToInt()
    var targetHeight = (minPercentage * this.height).roundToInt()

    if (this.width == targetWidth && this.height == targetHeight) return this

    targetWidth = (minPercentage * this.width).toInt()
    targetHeight = (minPercentage * this.height).toInt()
    val config = this.config ?: Bitmap.Config.ARGB_8888

    val targetBitmap = Bitmap.createBitmap(targetWidth, targetHeight, config)

    val matrix = Matrix().apply {
        setScale(minPercentage, minPercentage)
    }

    return this.applyMatrixToBitmap(targetBitmap, matrix)
}

fun Bitmap.getBlurBitmap(
    context: Context,
    blurRadius: Float
): Bitmap {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        alpha = 242
        colorFilter = LightingColorFilter(0xFF7F7F7F.toInt(), 0x00000000)
    }

    val blurAlgorithm = getBlurAlgorithm(context)

    val minimizedWidth = (width * 0.95).roundToInt()
    val minimizedHeight = (height * 0.95).roundToInt()
    val minimizedBitmap = this.getFitCenterBitmap(minimizedWidth, minimizedHeight)
        .asMutableBitmap()

    val increasedWidth = (width * 1.05).roundToInt()
    val increasedHeight = (height * 1.05).roundToInt()

    val paddingStartWidth = (increasedWidth - minimizedWidth) / 2f
    val paddingStartHeight = (increasedHeight - minimizedHeight) / 2f

    return Bitmap.createBitmap(
        increasedWidth,
        increasedHeight,
        Bitmap.Config.ARGB_8888
    ).apply {
        applyCanvas {
            drawBitmap(minimizedBitmap, paddingStartWidth, paddingStartHeight, paint)
        }

        blurAlgorithm.blur(
            this,
            blurRadius.coerceIn(MIN_BLUR_RADIUS, MAX_BLUR_RADIUS)
        )
    }
}

fun Bitmap.compressToBytes(
    compressFormat: Bitmap.CompressFormat,
    qualityPercentage: Int
): ByteArray {
    val imageOutputStream = ByteArrayOutputStream()
    compress(compressFormat, qualityPercentage, imageOutputStream)
    imageOutputStream.flush()

    return imageOutputStream.use(ByteArrayOutputStream::toByteArray)
}

fun Bitmap.asMutableBitmap(): Bitmap {
    val mutableBitmap = getMutableBitmapOrCopy(this)
    if (mutableBitmap != null) {
        return mutableBitmap
    }

    return BitmapFactory.decodeByteArray(
        this.getBytes(),
        0,
        this.byteCount
    ).let { decodedBitmap ->
        getMutableBitmapOrCopy(decodedBitmap)!!
    }
}

private fun Bitmap.applyMatrixToBitmap(targetBitmap: Bitmap, matrix: Matrix): Bitmap {
    val canvas = Canvas(targetBitmap.asMutableBitmap())

    canvas.drawBitmap(this, matrix, DEFAULT_PAINT)
    canvas.setBitmap(null)

    return targetBitmap
}

private fun getMutableBitmapOrCopy(bitmap: Bitmap): Bitmap? {
    return if (bitmap.isMutable) bitmap
    else bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
}

// TODO may be in the future use new api
private fun getBlurAlgorithm(context: Context): BlurAlgorithm = RenderScriptBlur(context)

private fun ByteArray.toBitmap(): Bitmap =
    BitmapFactory.decodeByteArray(this, 0, this.size)
