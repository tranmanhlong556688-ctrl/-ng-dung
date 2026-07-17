package vn.mltudonghoa.cuncung

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class DogView(context: Context) : View(context) {
    enum class Mood { HAPPY, RUNNING, SITTING, SLEEPING, TALKING, JUMPING, PLAYING, SNIFFING }
    enum class Direction { LEFT, RIGHT, UP, DOWN, UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val ticker = Handler(Looper.getMainLooper())
    private var phase = 0f
    private var mood = Mood.HAPPY
    private var direction = Direction.RIGHT
    private var bubble: String? = null
    private var touchHandler: ((MotionEvent) -> Boolean)? = null
    private var moving = false
    private var screenActive = true
    private var lowBattery = false
    private var quality = Prefs.QUALITY_BALANCED
    private var depthScale = 1f
    private var twinProgress: Float? = null

    private val frameTicker = object : Runnable {
        override fun run() {
            if (!screenActive) return
            val fps = effectiveFps()
            phase += if (moving) 0.34f else 0.16f
            if (phase > (PI * 200).toFloat()) phase = 0f
            invalidate()
            ticker.postDelayed(this, 1000L / fps.coerceAtLeast(1))
        }
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        ticker.post(frameTicker)
    }

    fun release() = ticker.removeCallbacksAndMessages(null)

    fun setMood(newMood: Mood) {
        mood = newMood
        invalidate()
    }

    fun setDirection(newDirection: Direction) {
        direction = newDirection
        invalidate()
    }

    fun setMoving(value: Boolean) {
        moving = value
        if (value) mood = Mood.RUNNING
        invalidate()
    }

    fun setDepthScale(value: Float) {
        depthScale = value.coerceIn(0.72f, 1.18f)
        invalidate()
    }

    fun setQuality(value: String) {
        quality = value
        restartTicker()
    }

    fun setLowBattery(value: Boolean) {
        lowBattery = value
        restartTicker()
    }

    fun setScreenActive(value: Boolean) {
        screenActive = value
        if (value) restartTicker() else ticker.removeCallbacks(frameTicker)
    }

    fun setTwinProgress(value: Float?) {
        twinProgress = value?.coerceIn(0f, 1f)
        invalidate()
    }

    fun speak(text: String) {
        bubble = text.take(72)
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
        val cx = width / 2f
        val cy = height * 0.63f
        val twin = twinProgress
        if (twin != null) {
            val split = splitAmount(twin)
            val chase = sin(twin * PI.toFloat() * 5f) * 15f
            drawDog(canvas, cx - split, cy + chase, 0.94f, 0.92f)
            drawDog(canvas, cx + split, cy - chase, 0.94f, 0.92f)
        } else {
            drawDog(canvas, cx, cy, 1f, 1f)
        }
        bubble?.let { drawBubble(canvas, it) }
    }

    private fun splitAmount(progress: Float): Float {
        val envelope = sin(progress * PI.toFloat()).coerceAtLeast(0f)
        return 68f * envelope
    }

    private fun drawDog(canvas: Canvas, centerX: Float, centerY: Float, localScale: Float, alpha: Float) {
        val upBias = when (direction) {
            Direction.UP, Direction.UP_LEFT, Direction.UP_RIGHT -> -0.06f
            Direction.DOWN, Direction.DOWN_LEFT, Direction.DOWN_RIGHT -> 0.05f
            else -> 0f
        }
        val bounce = when (mood) {
            Mood.RUNNING -> abs(sin(phase * 1.7f)) * 8f
            Mood.JUMPING -> abs(sin(phase)) * 24f
            Mood.HAPPY, Mood.TALKING, Mood.PLAYING -> abs(sin(phase * 0.8f)) * 4f
            else -> 0f
        }
        val facingLeft = direction == Direction.LEFT || direction == Direction.UP_LEFT || direction == Direction.DOWN_LEFT
        val verticalSquash = 1f + upBias
        val finalScale = depthScale * localScale

        canvas.save()
        canvas.translate(centerX, centerY - bounce)
        canvas.scale(if (facingLeft) -finalScale else finalScale, finalScale * verticalSquash)
        drawShadow(canvas, bounce, alpha)
        drawTail(canvas, alpha)
        drawRearLegs(canvas, alpha)
        drawBody(canvas, alpha)
        drawFrontLegs(canvas, alpha)
        drawHead(canvas, alpha)
        drawFace(canvas, alpha)
        if (mood == Mood.SLEEPING) drawSleepMarks(canvas, alpha)
        canvas.restore()
    }

    private fun drawShadow(canvas: Canvas, jumpHeight: Float, alpha: Float) {
        val shrink = (1f - jumpHeight / 80f).coerceIn(0.65f, 1f)
        paint.shader = RadialGradient(
            0f, 44f, 78f * shrink,
            intArrayOf(Color.argb((70 * alpha).toInt(), 20, 20, 25), Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawOval(RectF(-82f * shrink, 24f, 82f * shrink, 64f), paint)
        paint.shader = null
    }

    private fun drawTail(canvas: Canvas, alpha: Float) {
        val wag = if (mood == Mood.HAPPY || mood == Mood.TALKING || mood == Mood.PLAYING) sin(phase * 2.4f) * 18f else 0f
        stroke.color = Color.argb((255 * alpha).toInt(), 117, 70, 38)
        stroke.strokeWidth = 20f
        val path = Path().apply {
            moveTo(66f, -4f)
            cubicTo(105f, -18f + wag, 104f, -64f + wag, 75f, -72f)
        }
        canvas.drawPath(path, stroke)
        stroke.color = Color.argb((180 * alpha).toInt(), 242, 181, 101)
        stroke.strokeWidth = 6f
        canvas.drawPath(path, stroke)
    }

    private fun drawBody(canvas: Canvas, alpha: Float) {
        paint.shader = LinearGradient(
            -70f, -54f, 70f, 34f,
            intArrayOf(
                Color.argb((255 * alpha).toInt(), 244, 174, 91),
                Color.argb((255 * alpha).toInt(), 194, 113, 55),
                Color.argb((255 * alpha).toInt(), 126, 70, 37)
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawOval(RectF(-75f, -56f, 75f, 38f), paint)
        paint.shader = null
        paint.color = Color.argb((200 * alpha).toInt(), 255, 226, 177)
        canvas.drawOval(RectF(-36f, -34f, 48f, 28f), paint)
        paint.color = Color.argb((115 * alpha).toInt(), 255, 255, 255)
        canvas.drawOval(RectF(-48f, -47f, 23f, -26f), paint)
    }

    private fun drawRearLegs(canvas: Canvas, alpha: Float) {
        val gait = if (moving) sin(phase * 1.8f) * 11f else 0f
        drawLeg(canvas, -50f, 8f + gait, alpha, false)
        drawLeg(canvas, 43f, 8f - gait, alpha, false)
    }

    private fun drawFrontLegs(canvas: Canvas, alpha: Float) {
        val gait = if (moving) sin(phase * 1.8f + PI.toFloat()) * 12f else 0f
        drawLeg(canvas, -24f, 9f + gait, alpha, true)
        drawLeg(canvas, 63f, 7f - gait, alpha, true)
    }

    private fun drawLeg(canvas: Canvas, x: Float, lift: Float, alpha: Float, front: Boolean) {
        paint.shader = LinearGradient(
            x, 0f, x + 26f, 52f,
            Color.argb((255 * alpha).toInt(), if (front) 228 else 205, 135, 66),
            Color.argb((255 * alpha).toInt(), 132, 72, 39),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(RectF(x, -2f - lift, x + 25f, 49f - lift), 11f, 11f, paint)
        paint.shader = null
        paint.color = Color.argb((255 * alpha).toInt(), 255, 222, 164)
        canvas.drawOval(RectF(x - 5f, 36f - lift, x + 31f, 55f - lift), paint)
    }

    private fun drawHead(canvas: Canvas, alpha: Float) {
        val nod = if (mood == Mood.SNIFFING) abs(sin(phase)) * 12f else 0f
        canvas.save()
        canvas.translate(-28f, -88f + nod)
        paint.shader = RadialGradient(
            -18f, -22f, 90f,
            intArrayOf(
                Color.argb((255 * alpha).toInt(), 255, 196, 111),
                Color.argb((255 * alpha).toInt(), 206, 119, 56),
                Color.argb((255 * alpha).toInt(), 112, 61, 34)
            ),
            floatArrayOf(0f, 0.68f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawOval(RectF(-65f, -58f, 70f, 62f), paint)
        paint.shader = null

        paint.color = Color.argb((255 * alpha).toInt(), 100, 55, 34)
        val earWag = sin(phase * 1.2f) * 5f
        val leftEar = Path().apply {
            moveTo(-45f, -38f)
            lineTo(-82f, -70f + earWag)
            quadTo(-88f, -4f, -48f, 16f)
            close()
        }
        val rightEar = Path().apply {
            moveTo(47f, -38f)
            lineTo(82f, -69f - earWag)
            quadTo(87f, -4f, 48f, 16f)
            close()
        }
        canvas.drawPath(leftEar, paint)
        canvas.drawPath(rightEar, paint)
        paint.color = Color.argb((255 * alpha).toInt(), 255, 224, 174)
        canvas.drawOval(RectF(-34f, -2f, 51f, 57f), paint)
        paint.color = Color.argb((100 * alpha).toInt(), 255, 255, 255)
        canvas.drawOval(RectF(-43f, -47f, 18f, -27f), paint)
        canvas.restore()
    }

    private fun drawFace(canvas: Canvas, alpha: Float) {
        val ox = -28f
        val oy = -88f + if (mood == Mood.SNIFFING) abs(sin(phase)) * 12f else 0f
        paint.color = Color.argb((255 * alpha).toInt(), 42, 30, 27)
        if (mood == Mood.SLEEPING) {
            stroke.color = paint.color
            stroke.strokeWidth = 4f
            canvas.drawLine(ox - 35f, oy - 7f, ox - 15f, oy - 7f, stroke)
            canvas.drawLine(ox + 18f, oy - 7f, ox + 38f, oy - 7f, stroke)
        } else {
            canvas.drawCircle(ox - 25f, oy - 8f, 8f, paint)
            canvas.drawCircle(ox + 28f, oy - 8f, 8f, paint)
            paint.color = Color.argb((255 * alpha).toInt(), 255, 255, 255)
            canvas.drawCircle(ox - 28f, oy - 11f, 2.7f, paint)
            canvas.drawCircle(ox + 25f, oy - 11f, 2.7f, paint)
        }
        paint.color = Color.argb((255 * alpha).toInt(), 47, 34, 30)
        canvas.drawOval(RectF(ox - 9f, oy + 14f, ox + 17f, oy + 33f), paint)
        stroke.color = Color.argb((255 * alpha).toInt(), 76, 40, 35)
        stroke.strokeWidth = 3.5f
        val mouthOpen = mood == Mood.HAPPY || mood == Mood.TALKING || mood == Mood.PLAYING || moving
        val mouth = Path().apply {
            moveTo(ox + 4f, oy + 32f)
            quadTo(ox - 7f, oy + 44f, ox - 20f, oy + 36f)
            moveTo(ox + 4f, oy + 32f)
            quadTo(ox + 15f, oy + 44f, ox + 28f, oy + 36f)
        }
        canvas.drawPath(mouth, stroke)
        if (mouthOpen) {
            paint.color = Color.argb((255 * alpha).toInt(), 238, 95, 112)
            canvas.drawOval(RectF(ox - 4f, oy + 37f, ox + 13f, oy + 57f), paint)
        }
    }

    private fun drawSleepMarks(canvas: Canvas, alpha: Float) {
        paint.shader = null
        paint.color = Color.argb((255 * alpha).toInt(), 48, 112, 224)
        paint.textSize = 25f
        paint.isFakeBoldText = true
        canvas.drawText("Z", 48f, -115f, paint)
        paint.textSize = 18f
        canvas.drawText("z", 70f, -137f, paint)
        paint.isFakeBoldText = false
    }

    private fun drawBubble(canvas: Canvas, text: String) {
        paint.shader = null
        paint.textSize = 23f
        paint.isFakeBoldText = false
        val shown = if (paint.measureText(text) > width - 42f) text.take(40) + "…" else text
        val bubbleWidth = (paint.measureText(shown) + 34f).coerceAtMost(width - 16f)
        val left = ((width - bubbleWidth) / 2f).coerceAtLeast(8f)
        paint.color = Color.WHITE
        paint.setShadowLayer(9f, 0f, 3f, Color.argb(80, 0, 0, 0))
        canvas.drawRoundRect(RectF(left, 8f, left + bubbleWidth, 58f), 20f, 20f, paint)
        paint.clearShadowLayer()
        paint.color = Color.rgb(25, 45, 80)
        canvas.drawText(shown, left + 17f, 40f, paint)
    }

    private fun effectiveFps(): Int {
        if (lowBattery) return 8
        return when (quality) {
            Prefs.QUALITY_SAVER -> if (moving) 15 else 10
            Prefs.QUALITY_BEST -> if (moving || mood == Mood.TALKING || twinProgress != null) 30 else 15
            else -> if (moving || mood == Mood.TALKING || twinProgress != null) 30 else 12
        }
    }

    private fun restartTicker() {
        ticker.removeCallbacks(frameTicker)
        if (screenActive) ticker.post(frameTicker)
    }
}
