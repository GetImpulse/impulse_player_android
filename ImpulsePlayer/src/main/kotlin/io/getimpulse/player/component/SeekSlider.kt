package io.getimpulse.player.component

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import com.google.android.material.slider.Slider
import io.getimpulse.player.R
import io.getimpulse.player.extension.asColor
import io.getimpulse.player.extension.asInt
import io.getimpulse.player.extension.dpToPx

class SeekSlider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : Slider(context, attrs, defStyleAttr) {

    private var buffer: Float = 0f
    private val bufferPaint by lazy {
        Paint().apply {
            color = R.color.seek_buffer.asColor(context)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
    }

    private val seekTrackHeight by lazy { R.dimen.seek_track_height.asInt(context) }
    private val trackCornerRadius = 10f

    fun setBuffer(buffer: Float) {
        this.buffer = buffer
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        drawBufferProgress(canvas)
        super.onDraw(canvas)
    }

    private val bufferRect = RectF(0f, 0f, 0f, 0f)

    private fun drawBufferProgress(canvas: Canvas) {
        val startX = paddingStart.toFloat() + 16.dpToPx()
        val centerY = height / 2f
        val bufferTrackWidth = (width - 32.dpToPx()) * (buffer / valueTo)
        bufferRect.set(
            startX,
            centerY - seekTrackHeight / 2 - 0.5f,
            bufferTrackWidth + startX,
            centerY + seekTrackHeight / 2 + 0.5f,
        )
        canvas.drawRoundRect(bufferRect, trackCornerRadius, trackCornerRadius, bufferPaint)
    }

    override fun setValue(value: Float) {
        if (value <= valueTo) {
            super.setValue(value)
        } else {
            super.setValue(valueTo)
        }
    }
}
