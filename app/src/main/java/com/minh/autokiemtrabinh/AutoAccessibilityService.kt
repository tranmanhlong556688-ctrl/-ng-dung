package com.minh.autokiemtrabinh

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView

internal enum class RunState { IDLE, COUNTDOWN, RUNNING, PAUSED, COMPLETED, ERROR }
internal enum class AutomationStep { P1_FIRST, P1_SECOND, P2 }
internal enum class PointId { P1, P2 }

class AutoAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: AutoAccessibilityService? = null
            private set

        const val ACTION_SHOW = "com.minh.autokiemtrabinh.SHOW"
        const val ACTION_PAUSE_RESUME = "com.minh.autokiemtrabinh.PAUSE_RESUME"
        const val ACTION_STOP = "com.minh.autokiemtrabinh.STOP"
        internal const val CHANNEL_ID = "auto_kiem_tra_binh"
        internal const val NOTIFICATION_ID = 4201
    }

    internal lateinit var windowManager: WindowManager
    internal val handler = Handler(Looper.getMainLooper())

    internal var controllerView: View? = null
    internal var controllerParams: WindowManager.LayoutParams? = null
    internal var miniView: View? = null
    internal var miniParams: WindowManager.LayoutParams? = null
    internal var p1View: View? = null
    internal var p1Params: WindowManager.LayoutParams? = null
    internal var p2View: View? = null
    internal var p2Params: WindowManager.LayoutParams? = null

    internal var state = RunState.IDLE
    internal var currentStep = AutomationStep.P1_FIRST
    internal var completedRounds = 0
    internal var requestedRounds = 10
    internal var runInfinite = false
    internal var delayBetweenP1 = 150L
    internal var delayBeforeP2 = 300L
    internal var delayAfterP2 = 400L
    internal var gestureDuration = 60L
    internal var setupMode = false
    internal var targetPackage: String? = null
    internal var lastExternalPackage: String? = null
    internal var runToken = 0
    internal var scheduledRunnable: Runnable? = null
    internal var currentOrientation = Configuration.ORIENTATION_UNDEFINED

    internal var statusText: TextView? = null
    internal var counterText: TextView? = null
    internal var coordinateText: TextView? = null
    internal var roundsInput: EditText? = null
    internal var infiniteCheck: CheckBox? = null
    internal var betweenP1Input: EditText? = null
    internal var beforeP2Input: EditText? = null
    internal var afterP2Input: EditText? = null
    internal var tapDurationInput: EditText? = null
    internal var pauseResumeButton: Button? = null
    internal var setupButton: Button? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> pauseWithReason("Màn hình đã tắt")
                Intent.ACTION_USER_PRESENT -> updateUi("Đã mở khóa; bấm TIẾP TỤC để chạy tiếp")
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        currentOrientation = resources.configuration.orientation
        registerScreenReceiver()
        createNotificationChannel()
        ensureOverlays()
        showController(!Prefs.collapsed(this))
        updateUi("Đã dừng")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            lastExternalPackage = pkg
            val target = targetPackage
            if ((state == RunState.RUNNING || state == RunState.COUNTDOWN) && target != null && pkg != target) {
                pauseWithReason("Đã chuyển khỏi ứng dụng mục tiêu")
            }
        }
    }

    override fun onInterrupt() {
        pauseWithReason("Dịch vụ Trợ năng bị gián đoạn")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (currentOrientation != Configuration.ORIENTATION_UNDEFINED && currentOrientation != newConfig.orientation) {
            stopAutomation("Màn hình đã xoay; cần hiệu chỉnh lại P1/P2", showPoints = true)
        }
        currentOrientation = newConfig.orientation
    }

    override fun onUnbind(intent: Intent?): Boolean {
        shutdownServiceUi("Dịch vụ Trợ năng đã tắt")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        shutdownServiceUi("Dịch vụ đã dừng")
        super.onDestroy()
    }

    fun showControllerFromActivity() {
        ensureOverlays()
        showController(true)
    }

    fun handleExternalAction(action: String) {
        when (action) {
            ACTION_SHOW -> showController(true)
            ACTION_PAUSE_RESUME -> togglePauseResume()
            ACTION_STOP -> stopAutomation("Đã dừng khẩn cấp", showPoints = true)
        }
    }

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") registerReceiver(screenReceiver, filter)
    }

    private fun shutdownServiceUi(message: String) {
        stopAutomation(message, showPoints = false)
        removeAllOverlays()
        cancelNotification()
        runCatching { unregisterReceiver(screenReceiver) }
        instance = null
    }

    internal fun removeAllOverlays() {
        listOf(controllerView, miniView, p1View, p2View).forEach { view ->
            if (view != null) runCatching { windowManager.removeView(view) }
        }
        controllerView = null
        miniView = null
        p1View = null
        p2View = null
    }
}
