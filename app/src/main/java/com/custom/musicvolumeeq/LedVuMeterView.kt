package com.custom.musicvolumeeq

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class LedVuMeterView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var leftLevel: Int = 15
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    var rightLevel: Int = 15
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    private val numSegments = 20
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9E9E9E")
        textSize = 24f
        typeface = Typeface.DEFAULT_BOLD
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val startX = 42f
        val availableWidth = width - startX - 10f
        val spacing = 5f
        val segWidth = (availableWidth - (numSegments - 1) * spacing) / numSegments
        val barHeight = (height - 16f) / 2f

        canvas.drawText("L", 8f, barHeight - 4f, labelPaint)
        canvas.drawText("R", 8f, height - 6f, labelPaint)

        val activeLeft = (leftLevel / 100f * numSegments).toInt()
        val activeRight = (rightLevel / 100f * numSegments).toInt()

        for (i in 0 until numSegments) {
            val x = startX + i * (segWidth + spacing)
            val activeColor = when {
                i < 13 -> Color.parseColor("#00E676") // Yeşil (-20dB .. -3dB)
                i < 17 -> Color.parseColor("#FFEB3B") // Sarı (-3dB .. 0dB)
                else -> Color.parseColor("#FF1744")   // Kırmızı (+3dB Peak)
            }
            val offColor = Color.parseColor("#232830")

            // Sol Kanal (L)
            paint.color = if (i < activeLeft) activeColor else offColor
            canvas.drawRoundRect(x, 2f, x + segWidth, barHeight, 4f, 4f, paint)

            // Sağ Kanal (R)
            paint.color = if (i < activeRight) activeColor else offColor
            canvas.drawRoundRect(x, barHeight + 12f, x + segWidth, height - 2f, 4f, 4f, paint)
        }
    }
}
