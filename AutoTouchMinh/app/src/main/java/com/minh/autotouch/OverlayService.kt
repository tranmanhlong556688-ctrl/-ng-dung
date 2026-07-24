package com.minh.autotouch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class OverlayService : Service() {
    companion object {
        @Volatile private var instance: OverlayService? = null
        private val mainHandler = Handler(Looper.getMainLooper())

        fun postStatus(text: String) = mainHandler.post { instance?.statusView?.text = text }
        fun highlightStep(index: Int) = mainHandler.post { instance?.highlight(index) }
        fun setMarkersTouchable(touchable: Boolean) = mainHandler.post { instance?.updateMarkerTouchability(touchable) }
    }

    private lateinit var windowManager: WindowManager
    private val markerViews = mutableListOf<TextView>()
    private val markerParams = mutableListOf<WindowManager.LayoutParams>()
    private var controlView: LinearLayout? = null
    internal var statusView: TextView? = null
    private var minimized = false
    private var config = AppConfig()

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotification()
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Chưa cấp quyền hiển thị nổi", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }
        config = ConfigStore.load(this)
        createMarkers()
        createControlPanel()
        updateMarkerTouchability(!config.markersLocked)
        LogStore.append(this, "OVERLAY_STARTED")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "REFRESH") refreshAll()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification() {
        val channelId = "autotouch_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Auto Touch đang chạy", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("Auto Touch Minh")
                .setContentText("Bảng điều khiển nổi đang hoạt động")
                .setSmallIcon(com.minh.autotouch.R.drawable.ic_launcher_foreground)
                .setOngoing(true).build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Auto Touch Minh")
                .setContentText("Bảng điều khiển nổi đang hoạt động")
                .setSmallIcon(com.minh.autotouch.R.drawable.ic_launcher_foreground)
                .setOngoing(true).build()
        }
        startForeground(1001, notification)
    }

    private fun overlayType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
    }

    private fun createMarkers() {
        markerViews.clear(); markerParams.clear()
        config.steps.forEachIndexed { index, step ->
            if (!step.enabled) return@forEachIndexed
            val size = dp(config.markerSizeDp)
            val view = TextView(this).apply {
                text = "${index + 1}"
                textSize = 16f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.argb((255 * config.markerAlphaPercent / 100f).toInt(), 21, 101, 192))
                alpha = config.markerAlphaPercent / 100f
                setPadding(0, 0, 0, 0)
            }
            val params = WindowManager.LayoutParams(
                size, size, overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = step.x - size / 2
                y = step.y - size / 2
            }
            attachDrag(view, params, index)
            markerViews.add(view)
            markerParams.add(params)
            windowManager.addView(view, params)
        }
    }

    private fun attachDrag(view: TextView, params: WindowManager.LayoutParams, stepIndex: Int) {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x; startY = params.y
                    touchX = event.rawX; touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (event.rawX - touchX).toInt()
                    params.y = startY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val size = params.width
                    val fresh = ConfigStore.load(this)
                    fresh.steps[stepIndex].x = params.x + size / 2
                    fresh.steps[stepIndex].y = params.y + size / 2
                    ConfigStore.save(this, fresh)
                    config = fresh
                    postStatus("Đã lưu vị trí điểm ${stepIndex + 1}")
                    true
                }
                else -> false
            }
        }
    }

    private fun createControlPanel() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(6), dp(6), dp(6), dp(6))
            setBackgroundColor(Color.argb(225, 30, 30, 30))
        }
        statusView = TextView(this).apply {
            text = "Sẵn sàng"
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(dp(4), dp(2), dp(4), dp(4))
        }
        panel.addView(statusView, LinearLayout.LayoutParams(dp(230), LinearLayout.LayoutParams.WRAP_CONTENT))

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(makeButton("▶") { startOrResume() })
        row.addView(makeButton("Ⅱ") { pause() })
        row.addView(makeButton("■") { stop("Người dùng dừng") })
        row.addView(makeButton("🔒") { toggleLock() })
        row.addView(makeButton("—") { toggleMinimize() })
        panel.addView(row)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(12)
            y = dp(120)
        }
        attachControlDrag(panel, params)
        controlView = panel
        windowManager.addView(panel, params)
    }

    private fun makeButton(text: String, onClick: () -> Unit): Button = Button(this).apply {
        this.text = text
        textSize = 16f
        minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
        setPadding(dp(8), dp(2), dp(8), dp(2))
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(dp(44), dp(40)).apply { setMargins(dp(2), 0, dp(2), 0) }
    }

    private fun attachControlDrag(view: View, params: WindowManager.LayoutParams) {
        var startX = 0; var startY = 0; var touchX = 0f; var touchY = 0f
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x; startY = params.y; touchX = event.rawX; touchY = event.rawY; false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt(); val dy = (event.rawY - touchY).toInt()
                    if (kotlin.math.abs(dx) > 8 || kotlin.math.abs(dy) > 8) {
                        params.x = startX - dx
                        params.y = startY + dy
                        windowManager.updateViewLayout(view, params)
                        true
                    } else false
                }
                else -> false
            }
        }
    }

    private fun startOrResume() {
        val service = AutomationAccessibilityService.instance
        if (service == null) {
            Toast.makeText(this, "Hãy bật Dịch vụ trợ năng Auto Touch Minh", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        if (service.isPaused()) service.resumeAutomation() else if (!service.isRunning()) service.startAutomation(ConfigStore.load(this))
    }

    private fun pause() = AutomationAccessibilityService.instance?.pauseAutomation()
    private fun stop(reason: String) = AutomationAccessibilityService.instance?.stopAutomation(reason)

    private fun toggleLock() {
        config = ConfigStore.load(this)
        config.markersLocked = !config.markersLocked
        ConfigStore.save(this, config)
        updateMarkerTouchability(!config.markersLocked && AutomationAccessibilityService.instance?.isRunning() != true)
        postStatus(if (config.markersLocked) "Đã khóa điểm" else "Đã mở khóa điểm")
    }

    private fun toggleMinimize() {
        minimized = !minimized
        statusView?.visibility = if (minimized) View.GONE else View.VISIBLE
        val panel = controlView ?: return
        val row = panel.getChildAt(1) as? LinearLayout ?: return
        for (i in 0 until row.childCount - 1) row.getChildAt(i).visibility = if (minimized) View.GONE else View.VISIBLE
        (row.getChildAt(row.childCount - 1) as? Button)?.text = if (minimized) "+" else "—"
    }

    private fun updateMarkerTouchability(touchable: Boolean) {
        markerViews.forEachIndexed { index, view ->
            val params = markerParams[index]
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                (if (touchable) 0 else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            runCatching { windowManager.updateViewLayout(view, params) }
        }
    }

    private fun highlight(index: Int) {
        val enabledIndexes = config.steps.mapIndexedNotNull { i, s -> if (s.enabled) i else null }
        markerViews.forEachIndexed { markerIndex, view ->
            val actualStep = enabledIndexes.getOrNull(markerIndex)
            view.scaleX = if (actualStep == index) 1.25f else 1f
            view.scaleY = if (actualStep == index) 1.25f else 1f
        }
    }

    private fun refreshAll() {
        markerViews.forEach { runCatching { windowManager.removeView(it) } }
        markerViews.clear(); markerParams.clear()
        config = ConfigStore.load(this)
        createMarkers()
        updateMarkerTouchability(!config.markersLocked)
        postStatus("Đã nạp lại cấu hình")
    }

    override fun onDestroy() {
        AutomationAccessibilityService.instance?.stopAutomation("Tắt bảng điều khiển nổi")
        markerViews.forEach { runCatching { windowManager.removeView(it) } }
        controlView?.let { runCatching { windowManager.removeView(it) } }
        markerViews.clear(); controlView = null
        if (instance === this) instance = null
        LogStore.append(this, "OVERLAY_STOPPED")
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
