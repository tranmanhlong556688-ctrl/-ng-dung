package com.minh.autotouch

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.KeyguardManager
import android.content.Context
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class AutomationAccessibilityService : AccessibilityService() {
    enum class RunState { IDLE, COUNTDOWN, RUNNING, PAUSED }

    companion object {
        @Volatile var instance: AutomationAccessibilityService? = null
            private set
    }

    private val handler = Handler(Looper.getMainLooper())
    private var config = AppConfig()
    private var loopIndex = 0
    private var stepIndex = 0
    private var repeatIndex = 0
    private var state = RunState.IDLE
    private var volumePresses = mutableListOf<Long>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        OverlayService.postStatus("Trợ năng đã sẵn sàng")
        LogStore.append(this, "ACCESSIBILITY_CONNECTED")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = stopAutomation("Dịch vụ trợ năng bị gián đoạn")

    override fun onDestroy() {
        if (instance === this) instance = null
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    fun isRunning(): Boolean = state == RunState.RUNNING || state == RunState.COUNTDOWN
    fun isPaused(): Boolean = state == RunState.PAUSED

    fun startAutomation(newConfig: AppConfig) {
        if (state != RunState.IDLE) stopAutomation("Khởi động lại")
        config = newConfig.deepCopy()
        if (config.steps.none { it.enabled }) {
            OverlayService.postStatus("Chưa bật điểm thao tác nào")
            return
        }
        loopIndex = 0
        stepIndex = 0
        repeatIndex = 0
        state = RunState.COUNTDOWN
        OverlayService.setMarkersTouchable(false)
        LogStore.append(this, "START", config.profileName)
        runCountdown(config.countdownSec)
    }

    private fun runCountdown(seconds: Int) {
        if (state != RunState.COUNTDOWN) return
        if (seconds <= 0) {
            state = RunState.RUNNING
            OverlayService.postStatus("Đang chạy vòng 1")
            runNextStep()
            return
        }
        OverlayService.postStatus("Bắt đầu sau $seconds giây")
        handler.postDelayed({ runCountdown(seconds - 1) }, 1000)
    }

    fun pauseAutomation() {
        if (state == RunState.RUNNING || state == RunState.COUNTDOWN) {
            state = RunState.PAUSED
            handler.removeCallbacksAndMessages(null)
            OverlayService.postStatus("Đã tạm dừng")
            LogStore.append(this, "PAUSE")
        }
    }

    fun resumeAutomation() {
        if (state == RunState.PAUSED) {
            state = RunState.RUNNING
            OverlayService.postStatus("Tiếp tục")
            LogStore.append(this, "RESUME")
            handler.postDelayed({ runNextStep() }, 150)
        }
    }

    fun stopAutomation(reason: String = "Người dùng dừng") {
        if (state == RunState.IDLE) return
        handler.removeCallbacksAndMessages(null)
        state = RunState.IDLE
        OverlayService.setMarkersTouchable(!ConfigStore.load(this).markersLocked)
        OverlayService.postStatus("Đã dừng: $reason")
        LogStore.append(this, "STOP", reason)
    }

    private fun runNextStep() {
        if (state != RunState.RUNNING) return
        val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (keyguard.isKeyguardLocked) {
            stopAutomation("Màn hình đang khóa")
            return
        }

        while (stepIndex < config.steps.size && !config.steps[stepIndex].enabled) stepIndex++
        if (stepIndex >= config.steps.size) {
            loopIndex++
            if (config.loopCount > 0 && loopIndex >= config.loopCount) {
                stopAutomation("Đã đủ ${config.loopCount} vòng")
                return
            }
            stepIndex = 0
            repeatIndex = 0
            OverlayService.postStatus("Đang chạy vòng ${loopIndex + 1}")
            handler.postDelayed({ runNextStep() }, config.loopDelayMs)
            return
        }

        val step = config.steps[stepIndex]
        repeatIndex = 0
        OverlayService.highlightStep(stepIndex)
        OverlayService.postStatus("Vòng ${loopIndex + 1} • Điểm ${stepIndex + 1}")
        handler.postDelayed({ executeRepeated(step) }, step.preDelayMs)
    }

    private fun executeRepeated(step: StepConfig) {
        if (state != RunState.RUNNING) return
        val targetRepeats = when (step.action) {
            ActionType.DOUBLE_TAP -> 2
            ActionType.MULTI_TAP, ActionType.TAP -> step.repeatCount.coerceAtLeast(1)
            else -> 1
        }
        executeAction(step) { success ->
            if (state != RunState.RUNNING) return@executeAction
            if (!success) {
                stopAutomation("Không thể thực hiện điểm ${stepIndex + 1}")
                return@executeAction
            }
            repeatIndex++
            if (repeatIndex < targetRepeats) {
                handler.postDelayed({ executeRepeated(step) }, step.intervalMs.coerceAtLeast(80))
            } else {
                LogStore.append(this, "STEP_OK", "point=${stepIndex + 1},action=${step.action.name}")
                stepIndex++
                repeatIndex = 0
                handler.postDelayed({ runNextStep() }, step.postDelayMs)
            }
        }
    }

    private fun executeAction(step: StepConfig, done: (Boolean) -> Unit) {
        when (step.action) {
            ActionType.TAP, ActionType.MULTI_TAP, ActionType.DOUBLE_TAP -> gestureTap(step.x, step.y, 60, done)
            ActionType.LONG_PRESS -> gestureTap(step.x, step.y, step.durationMs, done)
            ActionType.SWIPE_UP -> gestureSwipe(step.x, step.y, step.x, step.y - 400, step.durationMs, done)
            ActionType.SWIPE_DOWN -> gestureSwipe(step.x, step.y, step.x, step.y + 400, step.durationMs, done)
            ActionType.SWIPE_LEFT -> gestureSwipe(step.x, step.y, step.x - 400, step.y, step.durationMs, done)
            ActionType.SWIPE_RIGHT -> gestureSwipe(step.x, step.y, step.x + 400, step.y, step.durationMs, done)
            ActionType.SWIPE_CUSTOM -> gestureSwipe(step.x, step.y, step.endX, step.endY, step.durationMs, done)
            ActionType.WAIT -> handler.postDelayed({ done(true) }, step.durationMs)
            ActionType.BACK -> done(performGlobalAction(GLOBAL_ACTION_BACK))
            ActionType.HOME -> done(performGlobalAction(GLOBAL_ACTION_HOME))
            ActionType.RECENTS -> done(performGlobalAction(GLOBAL_ACTION_RECENTS))
            ActionType.NOTIFICATIONS -> done(performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS))
        }
    }

    private fun gestureTap(x: Int, y: Int, duration: Long, done: (Boolean) -> Unit) {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        dispatch(path, duration, done)
    }

    private fun gestureSwipe(x1: Int, y1: Int, x2: Int, y2: Int, duration: Long, done: (Boolean) -> Unit) {
        val path = Path().apply {
            moveTo(x1.toFloat(), y1.toFloat())
            lineTo(x2.toFloat(), y2.toFloat())
        }
        dispatch(path, duration.coerceAtLeast(100), done)
    }

    private fun dispatch(path: Path, duration: Long, done: (Boolean) -> Unit) {
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration.coerceAtLeast(1)))
            .build()
        val accepted = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) = done(true)
            override fun onCancelled(gestureDescription: GestureDescription?) = done(false)
        }, handler)
        if (!accepted) done(false)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val now = System.currentTimeMillis()
            volumePresses.add(now)
            volumePresses = volumePresses.filter { now - it <= 1500 }.toMutableList()
            if (volumePresses.size >= 3) {
                volumePresses.clear()
                stopAutomation("Dừng khẩn cấp bằng phím âm lượng")
            }
        }
        return false
    }
}
