package com.minh.autotouch.redmilite

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class CalibrationActivity : Activity() {
    private var screenX = 0
    private var screenY = 0
    private lateinit var coordinateText: TextView
    private lateinit var grid: CoordinateGridView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        screenX = intent.getIntExtra("x", 540)
        screenY = intent.getIntExtra("y", 800)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }
        root.addView(TextView(this).apply {
            text = "CHỌN VỊ TRÍ CHẠM\nChạm vào vị trí tương ứng trên màn hình, sau đó bấm LƯU."
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(13, 71, 161))
            setPadding(24, 24, 24, 18)
        }, LinearLayout.LayoutParams(-1, -2))

        coordinateText = TextView(this).apply {
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
        }
        updateCoordinateText()
        root.addView(coordinateText, LinearLayout.LayoutParams(-1, -2))

        grid = CoordinateGridView().apply {
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                    screenX = event.rawX.toInt().coerceAtLeast(0)
                    screenY = event.rawY.toInt().coerceAtLeast(0)
                    crossX = event.x
                    crossY = event.y
                    invalidate()
                    updateCoordinateText()
                    true
                } else false
            }
        }
        root.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(16, 16, 16, 24)
        }
        buttons.addView(Button(this).apply {
            text = "HỦY"
            setOnClickListener { setResult(RESULT_CANCELED); finish() }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        buttons.addView(Button(this).apply {
            text = "LƯU VỊ TRÍ"
            setOnClickListener {
                setResult(RESULT_OK, Intent().putExtra("x", screenX).putExtra("y", screenY))
                finish()
            }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(buttons, LinearLayout.LayoutParams(-1, -2))

        setContentView(root)
    }

    private fun updateCoordinateText() {
        coordinateText.text = "Tọa độ màn hình: X = $screenX, Y = $screenY"
    }

    private inner class CoordinateGridView : View(this@CalibrationActivity) {
        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(210, 220, 230)
            strokeWidth = 1f
        }
        private val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.RED
            strokeWidth = 4f
        }
        var crossX = -1f
        var crossY = -1f

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawColor(Color.rgb(248, 250, 252))
            var x = 0
            while (x < width) {
                canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), linePaint)
                x += 100
            }
            var y = 0
            while (y < height) {
                canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), linePaint)
                y += 100
            }
            if (crossX >= 0 && crossY >= 0) {
                canvas.drawLine(crossX - 35, crossY, crossX + 35, crossY, crossPaint)
                canvas.drawLine(crossX, crossY - 35, crossX, crossY + 35, crossPaint)
                canvas.drawCircle(crossX, crossY, 15f, crossPaint)
            }
        }
    }
}
