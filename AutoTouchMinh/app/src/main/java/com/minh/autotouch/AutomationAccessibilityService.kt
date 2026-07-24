package com.minh.autotouch

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.KeyguardManager
import android.content.Context
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class AutomationAccessibilityService : AccessibilityService() {
    enum class RunState { IDLE, COUNTDOWN, RUNNING, PAUSED }

    companion object {
        @Volatile
        var instance: AutomationAccessibilityService? = null
            private set
    }

    private val handler = Handler(Looper.getMainLooper())
    private var config = AppConfig()
    private var loopIndex = 0
    private var stepIndex = 0
    private var repeatIndex = 0
    private var state = RunState.IDLE
    private var currentPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        OverlayService.postStatus("Trợ năng đã sẵn sàng")
        LogStore.append(this, "ACCESSIBILITY_CONNECTED")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return
        currentPackage = packageName
        if (state == RunState.RUNNING || state == RunState.COUNTDOWN) {
            if (!isCurrentPackageAllowed()) {
                stopAutomation("Đã chuyển khỏi ứng dụng được phép")
            }
        }
    }

    override fun onInterrupt() = stopAutomation("Dịch vụ trợ năng bị gián đoạn")

    override fun onDestroy() {
        if (instance === this) instance = null
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    fun isRunning(): Boolean = state == RunState.RUNNING || state == RunState.COUNTDOWN
    fun isPaused(): Boolean = state == RunState.PAUSED

    fun startAutomation(newConfig: AppConfig) {
        if (!ConsentStore.isAccepted(this)) {
            OverlayService.postStatus("Chưa đồng ý thông báo sử dụng Trợ năng")
            return
        }
        if (state != RunState.IDLE) stopAutomation("Khởi động lại")
        config = newConfig.deepCopy()
        if (config.allowedPackages().isEmpty()) {
            OverlayService.postStatus("Chưa chọn ứng dụng được phép")
            return
        }
        if (!isCurrentPackageAllowed()) {
            OverlayService.postStatus("Hãy mở đúng ứng dụng được phép rồi nhấn chạy")
            return
        }
        if (config.steps.none { it.enabled }) {
            OverlayService.postStatus("Chưa bật điểm thao tác nào")
            return
        }
        loopIndex = 0
        stepIndex = 0
        repeatIndex = 0
        state = RunState.COUNTDOWN
        OverlayService.setMarkersTouchable(false)
        LogStore.append(this, "START", "profile=${config.profileName},package=$currentPackage")
        runCountdown(config.countdownSec.coerceAtLeast(1))
    }

    private fun runCountdown(seconds: Int) {
        if (state != RunState.COUNTDOWN) return
        if (!isCurrentPackageAllowed()) {
            stopAutomation("Ứng dụng hiện tại không được phép")
            return
        }
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
            if (!isCurrentPackageAllowed()) {
                OverlayService.postStatus("Hãy mở lại ứng dụng được phép")
                return
            }
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

    private fun isCurrentPackageAllowed(): Boolean {
        val activePackage = currentPackage ?: return false
        return activePackage in config.allowedPackages()
    }

    private fun runNextStep() {
        if (state != RunState.RUNNING) return
        val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (keyguard.isKeyguardLocked) {
            stopAutomation("Màn hình đang khóa")
            return
        }
        if (!isCurrentPackageAllowed()) {
            stopAutomation("Đã chuyển khỏi ứng dụng được phép")
            return
        }

        while (stepIndex < config.steps.size && !config.steps[stepIndex].enabled) stepIndex++
        if (stepIndex >= config.steps.size) {
            loopIndex++
            if (loopIndex >= config.loopCount.coerceIn(1, 999)) {
                stopAutomation("Đã đủ ${config.loopCount.coerceIn(1, 999)} vòng")
                return
            }
            stepIndex = 0
            repeatIndex = 0
            OverlayService.postStatus("Đang chạy vòng ${loopIndex + 1}")
            handler.postDelayed({ runNextStep() }, config.loopDelayMs.coerceAtLeast(200))
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
        if (!isCurrentPackageAllowed()) {
            stopAutomation("Đã chuyển khỏi ứng dụng được phép")
            return
        }
        val targetRepeats = when (step.action) {
            ActionType.DOUBLE_TAP -> 2
            ActionType.MULTI_TAP, ActionType.TAP -> step.repeatCount.coerceIn(1, 100)
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
                handler.postDelayed({ executeRepeated(step) }, step.intervalMs.coerceAtLeast(120))
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
        if (!isCurrentPackageAllowed()) {
            done(false)
            return
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration.coerceAtLeast(1)))
            .build()
        val accepted = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) = done(true)
            override fun onCancelled(gestureDescription: GestureDescription?) = done(false)
        }, handler)
        if (!accepted) done(false)
    }
}
