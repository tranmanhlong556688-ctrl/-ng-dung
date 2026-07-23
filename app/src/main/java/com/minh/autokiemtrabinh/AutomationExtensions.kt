package com.minh.autokiemtrabinh

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.View
import android.widget.EditText
import android.widget.Toast

internal fun AutoAccessibilityService.startAutomation() {
    if (state == RunState.RUNNING || state == RunState.COUNTDOWN) return
    if (!Prefs.isP1Set(this) || !Prefs.isP2Set(this)) {
        setSetupMode(true)
        Toast.makeText(this, "Hãy kéo P1 và P2 đến đúng nút trước.", Toast.LENGTH_LONG).show()
        updateUi("Chưa có đủ tọa độ P1/P2")
        return
    }
    readAndSaveInputs()
    val rootPackage = runCatching { rootInActiveWindow?.packageName?.toString() }.getOrNull()
    val candidate = rootPackage?.takeIf(::isAllowedTargetPackage)
        ?: lastExternalPackage?.takeIf(::isAllowedTargetPackage)
    if (candidate == null) {
        Toast.makeText(this, "Hãy mở ứng dụng kiểm tra bình rồi bấm BẮT ĐẦU.", Toast.LENGTH_LONG).show()
        updateUi("Chưa xác định được ứng dụng mục tiêu")
        return
    }
    targetPackage = candidate
    completedRounds = 0
    currentStep = AutomationStep.P1_FIRST
    setSetupMode(false)
    runToken++
    val token = runToken
    state = RunState.COUNTDOWN
    updateCounter()
    startCountdown(3, token)
}

private fun AutoAccessibilityService.isAllowedTargetPackage(pkg: String): Boolean =
    pkg != packageName && pkg != "com.android.systemui" && !pkg.startsWith("com.android.settings")

private fun AutoAccessibilityService.readAndSaveInputs() {
    fun readLong(editText: EditText?, default: Long, min: Long, max: Long): Long =
        editText?.text?.toString()?.trim()?.toLongOrNull()?.coerceIn(min, max) ?: default

    requestedRounds = roundsInput?.text?.toString()?.trim()?.toIntOrNull()?.coerceIn(1, 999999) ?: 10
    runInfinite = infiniteCheck?.isChecked == true
    delayBetweenP1 = readLong(betweenP1Input, 150L, 50L, 60000L)
    delayBeforeP2 = readLong(beforeP2Input, 300L, 50L, 60000L)
    delayAfterP2 = readLong(afterP2Input, 400L, 50L, 60000L)
    gestureDuration = readLong(tapDurationInput, 60L, 20L, 500L)

    roundsInput?.setText(requestedRounds.toString())
    betweenP1Input?.setText(delayBetweenP1.toString())
    beforeP2Input?.setText(delayBeforeP2.toString())
    afterP2Input?.setText(delayAfterP2.toString())
    tapDurationInput?.setText(gestureDuration.toString())
    Prefs.saveTiming(this, requestedRounds, runInfinite, delayBetweenP1, delayBeforeP2, delayAfterP2, gestureDuration)
}

private fun AutoAccessibilityService.startCountdown(seconds: Int, token: Int) {
    if (token != runToken || state != RunState.COUNTDOWN) return
    if (seconds <= 0) {
        state = RunState.RUNNING
        updateUi("Đang chạy")
        executeStep(token)
        return
    }
    updateUi("Bắt đầu sau $seconds giây")
    schedule(1000L, token) { startCountdown(seconds - 1, token) }
}

private fun AutoAccessibilityService.executeStep(token: Int) {
    if (token != runToken || state != RunState.RUNNING) return
    val target = targetPackage
    val active = lastExternalPackage
    if (target == null || (active != null && active != target && active != packageName)) {
        pauseWithReason("Ứng dụng mục tiêu không còn ở phía trước")
        return
    }
    when (currentStep) {
        AutomationStep.P1_FIRST -> dispatchConfiguredTap(PointId.P1, token) {
            currentStep = AutomationStep.P1_SECOND
            schedule(delayBetweenP1, token) { executeStep(token) }
        }
        AutomationStep.P1_SECOND -> dispatchConfiguredTap(PointId.P1, token) {
            currentStep = AutomationStep.P2
            schedule(delayBeforeP2, token) { executeStep(token) }
        }
        AutomationStep.P2 -> dispatchConfiguredTap(PointId.P2, token) {
            completedRounds++
            updateCounter()
            if (!runInfinite && completedRounds >= requestedRounds) {
                state = RunState.COMPLETED
                targetPackage = null
                setSetupMode(true)
                updateUi("Đã hoàn thành $completedRounds vòng")
            } else {
                currentStep = AutomationStep.P1_FIRST
                schedule(delayAfterP2, token) { executeStep(token) }
            }
        }
    }
}

private fun AutoAccessibilityService.dispatchConfiguredTap(point: PointId, token: Int, onSuccess: () -> Unit) {
    val x = if (point == PointId.P1) Prefs.p1X(this) else Prefs.p2X(this)
    val y = if (point == PointId.P1) Prefs.p1Y(this) else Prefs.p2Y(this)
    dispatchTap(x, y, object : AccessibilityService.GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription?) {
            if (token == runToken && state == RunState.RUNNING) onSuccess()
        }
        override fun onCancelled(gestureDescription: GestureDescription?) {
            if (token == runToken) {
                state = RunState.ERROR
                updateUi("Lỗi: thao tác chạm bị hủy")
                setSetupMode(true)
            }
        }
    })
}

internal fun AutoAccessibilityService.dispatchTap(
    x: Float,
    y: Float,
    callback: AccessibilityService.GestureResultCallback
): Boolean {
    val path = Path().apply { moveTo(x, y) }
    val gesture = GestureDescription.Builder()
        .addStroke(GestureDescription.StrokeDescription(path, 0L, gestureDuration))
        .build()
    val accepted = dispatchGesture(gesture, callback, handler)
    if (!accepted) {
        state = RunState.ERROR
        updateUi("Lỗi: Android từ chối thao tác chạm")
        setSetupMode(true)
    }
    return accepted
}

internal fun AutoAccessibilityService.testPoint(point: PointId) {
    if ((point == PointId.P1 && !Prefs.isP1Set(this)) || (point == PointId.P2 && !Prefs.isP2Set(this))) {
        Toast.makeText(this, "Điểm này chưa được đặt.", Toast.LENGTH_SHORT).show(); return
    }
    if (state == RunState.RUNNING || state == RunState.COUNTDOWN) {
        Toast.makeText(this, "Hãy dừng chương trình trước khi thử điểm.", Toast.LENGTH_SHORT).show(); return
    }
    val restore = setupMode
    p1View?.visibility = View.GONE
    p2View?.visibility = View.GONE
    handler.postDelayed({
        val x = if (point == PointId.P1) Prefs.p1X(this) else Prefs.p2X(this)
        val y = if (point == PointId.P1) Prefs.p1Y(this) else Prefs.p2Y(this)
        dispatchTap(x, y, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                if (restore) setSetupMode(true)
                Toast.makeText(this@testPoint, "Đã thử ${point.name}.", Toast.LENGTH_SHORT).show()
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                if (restore) setSetupMode(true)
                Toast.makeText(this@testPoint, "Không thể thử ${point.name}.", Toast.LENGTH_SHORT).show()
            }
        })
    }, 120L)
}

internal fun AutoAccessibilityService.togglePauseResume() {
    when (state) {
        RunState.RUNNING, RunState.COUNTDOWN -> pauseWithReason("Đã tạm dừng")
        RunState.PAUSED -> resumeAutomation()
        else -> Toast.makeText(this, "Chương trình chưa chạy.", Toast.LENGTH_SHORT).show()
    }
}

internal fun AutoAccessibilityService.pauseWithReason(reason: String) {
    if (state != RunState.RUNNING && state != RunState.COUNTDOWN) return
    runToken++
    cancelScheduled()
    state = RunState.PAUSED
    updateUi(reason)
    setSetupMode(false)
}

private fun AutoAccessibilityService.resumeAutomation() {
    if (state != RunState.PAUSED) return
    val activeRoot = runCatching { rootInActiveWindow?.packageName?.toString() }.getOrNull()
    val target = targetPackage
    if (target == null || (activeRoot != null && activeRoot != target)) {
        Toast.makeText(this, "Hãy mở lại ứng dụng mục tiêu trước.", Toast.LENGTH_LONG).show()
        updateUi("Đang tạm dừng: chưa trở lại ứng dụng mục tiêu")
        return
    }
    runToken++
    val token = runToken
    state = RunState.COUNTDOWN
    setSetupMode(false)
    startCountdown(2, token)
}

internal fun AutoAccessibilityService.stopAutomation(message: String, showPoints: Boolean) {
    runToken++
    cancelScheduled()
    state = RunState.IDLE
    currentStep = AutomationStep.P1_FIRST
    targetPackage = null
    setSetupMode(showPoints)
    updateUi(message)
}

private fun AutoAccessibilityService.schedule(delay: Long, token: Int, block: () -> Unit) {
    cancelScheduled()
    val runnable = Runnable { if (token == runToken) block() }
    scheduledRunnable = runnable
    handler.postDelayed(runnable, delay)
}

private fun AutoAccessibilityService.cancelScheduled() {
    scheduledRunnable?.let(handler::removeCallbacks)
    scheduledRunnable = null
}
