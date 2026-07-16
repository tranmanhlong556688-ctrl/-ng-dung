package vn.mltudonghoa.cuncung

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.sin

class DogView(context: Context) : View(context) {
    enum class Mood { HAPPY, RUNNING, SITTING, SLEEPING, TALKING }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        color = Color.rgb(70, 48, 35)
    }
    private var phase = 0f
    private var mood = Mood.HAPPY
    private var bubble: String? = null
    private var touchHandler: ((MotionEvent) -> Boolean)? = null

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        ValueAnimator.ofFloat(0f, (Math.PI * 2).toFloat()).apply {
            duration = 900
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                phase = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun setMood(newMood: Mood) {
        mood = newMood
        invalidate()
    }

    fun speak(text: String) {
        bubble = text.take(70)
        mood = Mood.TALKING
        invalidate()
        postDelayed({
            bubble = null
            mood = Mood.HAPPY
            invalidate()
        }, 6500)
    }

    fun setOverlayTouchHandler(handler: (MotionEvent) -> Boolean) {
        touchHandler = handler
    }

    override fun onTouchEvent(event: MotionEvent): Boolean = touchHandler?.invoke(event) ?: true

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bounce = when (mood) {
            Mood.RUNNING -> sin(phase * 2f) * 10f
            Mood.HAPPY, Mood.TALKING -> sin(phase) * 5f
            else -> 0f
        }
        canvas.save()
        canvas.translate(0f, bounce)
        drawShadow(canvas)
        drawTail(canvas)
        drawBody(canvas)
        drawHead(canvas)
        drawFace(canvas)
        drawLegs(canvas)
        if (mood == Mood.SLEEPING) drawSleepMarks(canvas)
        canvas.restore()
        bubble?.let { drawBubble(canvas, it) }
    }

    private fun drawShadow(canvas: Canvas) {
        paint.color = Color.argb(55, 20, 20, 20)
        canvas.drawOval(RectF(40f, 190f, 210f, 225f), paint)
    }

    private fun drawTail(canvas: Canvas) {
        stroke.color = Color.rgb(163, 103, 55)
        stroke.strokeWidth = 22f
        val wag = if (mood == Mood.HAPPY || mood == Mood.TALKING) sin(phase * 2f) * 20f else 0f
        val path = Path().apply {
            moveTo(184f, 142f)
            cubicTo(225f, 125f + wag, 220f, 75f + wag, 196f, 70f)
        }
        canvas.drawPath(path, stroke)
    }

    private fun drawBody(canvas: Canvas) {
        paint.color = Color.rgb(202, 137, 77)
        canvas.drawOval(RectF(52f, 105f, 196f, 198f), paint)
        paint.color = Color.rgb(245, 209, 154)
        canvas.drawOval(RectF(86f, 125f, 172f, 190f), paint)
    }

    private fun drawHead(canvas: Canvas) {
        paint.color = Color.rgb(202, 137, 77)
        canvas.drawOval(RectF(40f, 24f, 188f, 150f), paint)
        paint.color = Color.rgb(106, 66, 43)
        val leftEar = Path().apply {
            moveTo(58f, 43f)
            lineTo(22f, 8f)
            quadTo(15f, 77f, 60f, 91f)
            close()
        }
        val rightEar = Path().apply {
            moveTo(169f, 43f)
            lineTo(205f, 8f)
            quadTo(212f, 77f, 168f, 91f)
            close()
        }
        canvas.drawPath(leftEar, paint)
        canvas.drawPath(rightEar, paint)
        paint.color = Color.rgb(247, 213, 168)
        canvas.drawOval(RectF(70f, 72f, 162f, 146f), paint)
    }

    private fun drawFace(canvas: Canvas) {
        paint.color = Color.rgb(45, 35, 30)
        if (mood == Mood.SLEEPING) {
            stroke.color = paint.color
            stroke.strokeWidth = 5f
            canvas.drawLine(72f, 74f, 94f, 74f, stroke)
            canvas.drawLine(135f, 74f, 157f, 74f, stroke)
        } else {
            canvas.drawCircle(84f, 70f, 9f, paint)
            canvas.drawCircle(146f, 70f, 9f, paint)
            paint.color = Color.WHITE
            canvas.drawCircle(81f, 67f, 3f, paint)
            canvas.drawCircle(143f, 67f, 3f, paint)
        }

        paint.color = Color.rgb(50, 37, 31)
        canvas.drawOval(RectF(103f, 93f, 129f, 112f), paint)
        stroke.color = Color.rgb(80, 45, 35)
        stroke.strokeWidth = 4f
        val mouth = Path().apply {
            moveTo(116f, 111f)
            quadTo(106f, 124f, 94f, 116f)
            moveTo(116f, 111f)
            quadTo(126f, 124f, 138f, 116f)
        }
        canvas.drawPath(mouth, stroke)
        if (mood == Mood.HAPPY || mood == Mood.TALKING) {
            paint.color = Color.rgb(239, 97, 112)
            canvas.drawOval(RectF(107f, 117f, 125f, 139f), paint)
        }
    }

    private fun drawLegs(canvas: Canvas) {
        paint.color = Color.rgb(202, 137, 77)
        val lift = if (mood == Mood.RUNNING) sin(phase * 2f) * 10f else 0f
        canvas.drawRoundRect(RectF(61f, 164f + lift, 91f, 215f + lift), 13f, 13f, paint)
        canvas.drawRoundRect(RectF(154f, 164f - lift, 184f, 215f - lift), 13f, 13f, paint)
        paint.color = Color.rgb(247, 213, 168)
        canvas.drawOval(RectF(56f, 199f + lift, 97f, 220f + lift), paint)
        canvas.drawOval(RectF(149f, 199f - lift, 190f, 220f - lift), paint)
    }

    private fun drawSleepMarks(canvas: Canvas) {
        paint.color = Color.rgb(40, 100, 210)
        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas.drawText("Z", 180f, 42f, paint)
        paint.textSize = 20f
        canvas.drawText("z", 204f, 20f, paint)
    }

    private fun drawBubble(canvas: Canvas, text: String) {
        paint.textSize = 25f
        paint.isFakeBoldText = false
        val padding = 18f
        val maxWidth = 430f
        val shown = if (paint.measureText(text) > maxWidth) text.take(42) + "…" else text
        val width = (paint.measureText(shown) + padding * 2).coerceAtMost(500f)
        val left = 5f
        val top = 0f
        paint.color = Color.WHITE
        paint.setShadowLayer(10f, 0f, 3f, Color.argb(80, 0, 0, 0))
        canvas.drawRoundRect(RectF(left, top, left + width, 54f), 22f, 22f, paint)
        paint.clearShadowLayer()
        paint.color = Color.rgb(25, 45, 80)
        canvas.drawText(shown, left + padding, 36f, paint)
    }
}
