package com.minh.autotouch

import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class FloatingControlService : Service() {
    private lateinit var windowManager: WindowManager
    private val pointViews = mutableListOf<TextView>()
    private lateinit var controlPanel: LinearLayout
    private var points = mutableListOf<PointConfig>()
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var paused = false
    private var currentIndex = 0
    private var completedLoops = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        points = ConfigStore.loadPoints(this)
        showPoints()
        showControlPanel()
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun pointParams(point: PointConfig) = WindowManager.LayoutParams(
        POINT_SIZE,
        POINT_SIZE,
        overlayType(),
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = point.x
        y = point.y
    }

    private fun showPoints() {
        points.forEachIndexed { index, point ->
            val pointView = TextView(this).apply {
                text = point.id.toString()
                gravity = Gravity.CENTER
                textSize = 18f
                setTextColor(Color.WHITE)
                setBackgroundColor(pointColor(point.enabled))
                setOnClickListener { editPoint(index) }
            }
            val params = pointParams(point)
            var downX = 0f
            var downY = 0f
            var startX = 0
            var startY = 0
            var moved = false

            pointView.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        startX = params.x
                        startY = params.y
                        moved = false
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (abs(event.rawX - downX) > 6f || abs(event.rawY - downY) > 6f) {
                            moved = true
                        }
                        params.x = startX + (event.rawX - downX).toInt()
                        params.y = startY + (event.rawY - downY).toInt()
                        windowManager.updateViewLayout(pointView, params)
                        true
                    }

                    MotionEvent.ACTION_UP -> {
                        point.x = params.x
                        point.y = params.y
                        ConfigStore.savePoints(this, points)
                        if (!moved) pointView.performClick()
                        true
                    }

                    else -> false
                }
            }

            windowManager.addView(pointView, params)
            pointViews += pointView
        }
    }

    private fun showControlPanel() {
        controlPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(4, 4, 4, 4)
            setBackgroundColor(0xDD222222.toInt())
        }

        addControlButton("▶") { startRun() }
        addControlButton("Ⅱ") { paused = true }
        addControlButton("↻") {
            if (running && paused) {
                paused = false
                runNext()
            }
        }
        addControlButton("■") { stopRun() }
        addControlButton("!") {
            stopRun()
            stopSelf()
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 20
            y = 100
        }

        controlPanel.setOnTouchListener(PanelDragTouch(controlPanel, params))
        windowManager.addView(controlPanel, params)
    }

    private fun addControlButton(label: String, action: () -> Unit) {
        controlPanel.addView(
            Button(this).apply {
                text = label
                minWidth = 0
                setPadding(10, 0, 10, 0)
                setOnClickListener { action() }
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                52
            )
        )
    }

    private inner class PanelDragTouch(
        private val view: View,
        private val params: WindowManager.LayoutParams
    ) : View.OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var startX = 0
        private var startY = 0

        override fun onTouch(v: View?, event: MotionEvent): Boolean {
            return when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    params.x = startX - (event.rawX - downX).toInt()
                    params.y = startY + (event.rawY - downY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }

                else -> false
            }
        }
    }

    private fun editPoint(index: Int) {
        if (running) return
        val point = points[index]
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 0)
        }
        val enabled = CheckBox(this).apply {
            text = "Bật điểm ${point.id}"
            isChecked = point.enabled
        }
        val actionSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@FloatingControlService,
                android.R.layout.simple_spinner_dropdown_item,
                ActionType.entries.map { it.name }
            )
            setSelection(ActionType.entries.indexOf(point.action))
        }
        val repeatInput = numberInput("Số lần", point.repeat.toString())
        val preDelayInput = numberInput("Chờ trước (ms)", point.preDelayMs.toString())
        val postDelayInput = numberInput("Chờ sau (ms)", point.postDelayMs.toString())
        val durationInput = numberInput("Thời gian nhấn/vuốt (ms)", point.durationMs.toString())
        val distanceInput = numberInput("Khoảng vuốt (px)", point.distancePx.toString())

        listOf(
            enabled,
            actionSpinner,
            repeatInput,
            preDelayInput,
            postDelayInput,
            durationInput,
            distanceInput
        ).forEach(layout::addView)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Cài đặt điểm ${point.id}")
            .setView(layout)
            .setPositiveButton("Lưu") { _, _ ->
                point.enabled = enabled.isChecked
                point.action = ActionType.entries[actionSpinner.selectedItemPosition]
                point.repeat = repeatInput.text.toString().toIntOrNull()?.coerceIn(1, 100) ?: 1
                point.preDelayMs = preDelayInput.text.toString().toLongOrNull()?.coerceAtLeast(0L) ?: 0L
                point.postDelayMs = postDelayInput.text.toString().toLongOrNull()?.coerceAtLeast(0L) ?: 0L
                point.durationMs = durationInput.text.toString().toLongOrNull()?.coerceAtLeast(50L) ?: 250L
                point.distancePx = distanceInput.text.toString().toIntOrNull()?.coerceAtLeast(50) ?: 300
                pointViews[index].setBackgroundColor(pointColor(point.enabled))
                ConfigStore.savePoints(this, points)
            }
            .setNegativeButton("Hủy", null)
            .create()

        dialog.window?.setType(overlayType())
        dialog.show()
    }

    private fun numberInput(hintText: String, value: String) = EditText(this).apply {
        hint = hintText
        inputType = android.text.InputType.TYPE_CLASS_NUMBER
        setText(value)
    }

    private fun startRun() {
        if (AutoAccessibilityService.instance == null) {
            Toast.makeText(this, "Chưa bật dịch vụ trợ năng.", Toast.LENGTH_LONG).show()
            return
        }
        if (running) return
        running = true
        paused = false
        currentIndex = 0
        completedLoops = 0
        Toast.makeText(this, "Bắt đầu sau 3 giây", Toast.LENGTH_SHORT).show()
        handler.postDelayed({ runNext() }, 3_000L)
    }

    private fun stopRun() {
        running = false
        paused = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun runNext() {
        if (!running || paused) return
        val enabledPoints = points.filter { it.enabled }
        if (enabledPoints.isEmpty()) {
            stopRun()
            Toast.makeText(this, "Chưa có điểm nào được bật.", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentIndex >= enabledPoints.size) {
            completedLoops++
            val maximumLoops = ConfigStore.getLoops(this)
            if (maximumLoops > 0 && completedLoops >= maximumLoops) {
                stopRun()
                Toast.makeText(this, "Đã hoàn thành $completedLoops vòng", Toast.LENGTH_SHORT).show()
                return
            }
            currentIndex = 0
            handler.postDelayed({ runNext() }, ConfigStore.getBetweenCycles(this))
            return
        }

        val point = enabledPoints[currentIndex++]
        handler.postDelayed({ executePoint(point) }, point.preDelayMs)
    }

    private fun executePoint(point: PointConfig) {
        if (!running || paused) return
        val accessibility = AutoAccessibilityService.instance ?: run {
            stopRun()
            return
        }
        val centerX = point.x + POINT_SIZE / 2f
        val centerY = point.y + POINT_SIZE / 2f
        var remaining = if (point.action == ActionType.MULTI_TAP) point.repeat else 1

        fun executeOnce() {
            if (!running || paused) return
            when (point.action) {
                ActionType.TAP,
                ActionType.MULTI_TAP -> accessibility.tap(centerX, centerY, 80L) {
                    remaining--
                    if (remaining > 0) {
                        handler.postDelayed({ executeOnce() }, point.durationMs)
                    } else {
                        handler.postDelayed({ runNext() }, point.postDelayMs)
                    }
                }

                ActionType.LONG_PRESS -> accessibility.tap(centerX, centerY, point.durationMs) {
                    handler.postDelayed({ runNext() }, point.postDelayMs)
                }

                ActionType.SWIPE_UP -> accessibility.swipe(
                    centerX,
                    centerY,
                    centerX,
                    centerY - point.distancePx,
                    point.durationMs
                ) { handler.postDelayed({ runNext() }, point.postDelayMs) }

                ActionType.SWIPE_DOWN -> accessibility.swipe(
                    centerX,
                    centerY,
                    centerX,
                    centerY + point.distancePx,
                    point.durationMs
                ) { handler.postDelayed({ runNext() }, point.postDelayMs) }

                ActionType.SWIPE_LEFT -> accessibility.swipe(
                    centerX,
                    centerY,
                    centerX - point.distancePx,
                    centerY,
                    point.durationMs
                ) { handler.postDelayed({ runNext() }, point.postDelayMs) }

                ActionType.SWIPE_RIGHT -> accessibility.swipe(
                    centerX,
                    centerY,
                    centerX + point.distancePx,
                    centerY,
                    point.durationMs
                ) { handler.postDelayed({ runNext() }, point.postDelayMs) }

                ActionType.WAIT -> handler.postDelayed({ runNext() }, point.postDelayMs)
            }
        }

        executeOnce()
    }

    private fun pointColor(enabled: Boolean): Int =
        if (enabled) 0xCC6750A4.toInt() else 0x88777777.toInt()

    private fun buildNotification(): Notification {
        val channelId = "auto_touch_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Auto Touch",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_app)
            .setContentTitle("Auto Touch Minh đang hoạt động")
            .setContentText("Nhấn để mở ứng dụng")
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopRun()
        pointViews.forEach { runCatching { windowManager.removeView(it) } }
        if (::controlPanel.isInitialized) {
            runCatching { windowManager.removeView(controlPanel) }
        }
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 7
        private const val POINT_SIZE = 56
    }
}
