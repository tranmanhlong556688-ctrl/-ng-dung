package vn.mltudonghoa.cuncung

import android.app.Activity
import android.graphics.*
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*

class IconMarkerActivity : Activity() {
    private lateinit var markerView: MarkerView
    private lateinit var countText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.argb(180, 0, 0, 0)

        val root = FrameLayout(this)
        markerView = MarkerView().apply { points.addAll(Prefs.markers(this@IconMarkerActivity)) }
        root.addView(markerView, FrameLayout.LayoutParams(-1, -1))

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(Color.argb(220, 15, 31, 58))
        }
        panel.addView(TextView(this).apply {
            text = "Chạm giữa từng icon để đánh dấu. Cún sẽ chạy vòng quanh các điểm này."
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
        })
        countText = TextView(this).apply {
            setTextColor(Color.rgb(154, 220, 255))
            textSize = 14f
            gravity = Gravity.CENTER
        }
        panel.addView(countText)
        val buttons = LinearLayout(this).apply { gravity = Gravity.CENTER }
        buttons.addView(Button(this).apply { text = "Hoàn tác"; setOnClickListener { markerView.undo(); updateCount() } })
        buttons.addView(Button(this).apply { text = "Xóa hết"; setOnClickListener { markerView.clear(); updateCount() } })
        buttons.addView(Button(this).apply {
            text = "Lưu"
            setOnClickListener {
                Prefs.setMarkers(this@IconMarkerActivity, markerView.points)
                Toast.makeText(this@IconMarkerActivity, "Đã lưu ${markerView.points.size} vị trí icon", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
        panel.addView(buttons)
        root.addView(panel, FrameLayout.LayoutParams(-1, -2, Gravity.TOP))
        setContentView(root)
        updateCount()
    }

    private fun updateCount() { countText.text = "Đã đánh dấu: ${markerView.points.size} icon" }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    inner class MarkerView : View(this@IconMarkerActivity) {
        val points = mutableListOf<Prefs.MarkerPoint>()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(3).toFloat()
            color = Color.rgb(90, 210, 255)
        }

        override fun onDraw(canvas: Canvas) {
            canvas.drawColor(Color.argb(75, 0, 0, 0))
            points.forEachIndexed { index, point ->
                val x = point.x * width
                val y = point.y * height
                paint.color = Color.argb(95, 40, 180, 255)
                canvas.drawCircle(x, y, dp(34).toFloat(), paint)
                canvas.drawCircle(x, y, dp(34).toFloat(), stroke)
                paint.color = Color.WHITE
                paint.textSize = dp(16).toFloat()
                paint.textAlign = Paint.Align.CENTER
                paint.isFakeBoldText = true
                canvas.drawText("${index + 1}", x, y + dp(6), paint)
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.y > dp(105)) {
                    points.add(Prefs.MarkerPoint(event.x / width, event.y / height))
                    updateCount()
                    invalidate()
                }
                return true
            }
            return true
        }

        fun undo() { if (points.isNotEmpty()) { points.removeAt(points.lastIndex); invalidate() } }
        fun clear() { points.clear(); invalidate() }
    }
}
