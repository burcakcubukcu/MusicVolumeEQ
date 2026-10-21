package com.custom.musicvolumeeq

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class RotaryKnobView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var progress: Int = 0
        set(value) {
            field = value.coerceIn(0, max)
            invalidate()
        }
    var max: Int = 100
    var label: String = "KNOB"
    var activeColor: Int = Color.parseColor("#00E676")
    var isCentered: Boolean = false // Balans için orta nokta modu
    var onProgressChanged: ((Int) -> Unit)? = null

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f
        color = Color.parseColor("#2A2F38")
        strokeCap = Paint.Cap.ROUND
    }

    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
    }

    private val knobBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#252A34")
    }

    private val knobBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#3E4656")
    }

    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 26f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9E9E9E")
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }

    private val arcRect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f - 12f
        val radius = min(width, height) / 2f - 28f

        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius)

        canvas.drawArc(arcRect, 135f, 270f, false, trackPaint)

        activePaint.color = activeColor
        val sweepAngle = (progress.toFloat() / max) * 270f

        if (isCentered) {
            // Balans modu: Merkezden (135° + 135° = 270°'nin ortası) sağa veya sola yay çizer
            val centerAngle = 135f + 135f
            val currentAngle = 135f + sweepAngle
            val startDraw = min(centerAngle, currentAngle)
            val sweepDraw = abs(currentAngle - centerAngle)
            canvas.drawArc(arcRect, startDraw, sweepDraw, false, activePaint)
        } else {
            canvas.drawArc(arcRect, 135f, sweepAngle, false, activePaint)
        }

        val innerRadius = radius - 16f
        canvas.drawCircle(cx, cy, innerRadius, knobBodyPaint)
        canvas.drawCircle(cx, cy, innerRadius, knobBorderPaint)

        indicatorPaint.color = activeColor
        val angleDeg = 135f + sweepAngle
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val startX = cx + (innerRadius * 0.35f) * cos(angleRad).toFloat()
        val startY = cy + (innerRadius * 0.35f) * sin(angleRad).toFloat()
        val stopX = cx + (innerRadius * 0.82f) * cos(angleRad).toFloat()
        val stopY = cy + (innerRadius * 0.82f) * sin(angleRad).toFloat()
        canvas.drawLine(startX, startY, stopX, stopY, indicatorPaint)

        val displayText = if (isCentered) {
            val balanceVal = progress - (max / 2)
            when {
                balanceVal < 0 -> "L ${abs(balanceVal)}%"
                balanceVal > 0 -> "R $balanceVal%"
                else -> "MERKEZ"
            }
        } else {
            "%${((progress.toFloat() / max) * 100).toInt()}"
        }

        canvas.drawText(displayText, cx, cy + 8f, textPaint)
        canvas.drawText(label, cx, height - 6f, subTextPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent.requestDisallowInterceptTouchEvent(true)
                val cx = width / 2f
                val cy = height / 2f - 12f
                val dx = event.x - cx
                val dy = event.y - cy
                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                if (angle < 0) angle += 360f

                var relative = angle - 135f
                if (relative < 0) relative += 360f

                if (relative <= 270f) {
                    progress = ((relative / 270f) * max).toInt()
                    onProgressChanged?.invoke(progress)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
