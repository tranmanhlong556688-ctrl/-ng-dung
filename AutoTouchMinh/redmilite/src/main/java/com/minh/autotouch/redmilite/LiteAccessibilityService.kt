package com.minh.autotouch.redmilite

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class LiteAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var currentPackage = ""
    private var runConfig: LiteConfig? = null
    private var armed = false
    private var running = false
    private var loopIndex = 0
    private var pointIndex = 0

    companion object {
        @Volatile
        private var instance: LiteAccessibilityService? = null

        @Volatile
        private var pendingConfig: LiteConfig? = null

        fun prepare(config: LiteConfig) {
            val safeCopy = config.copy(
                points = config.points.map { it.copy() }.toMutableList()
            )
            pendingConfig = safeCopy
            instance?.arm(safeCopy)
        }

        fun stopNow() {
            pendingConfig = null
            instance?.stopRun("Đã dừng thao tác")
        }

        fun isConnected(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        pendingConfig?.let { arm(it) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString()?.trim().orEmpty()
        if (pkg.isBlank()) return
        if (pkg == "com.android.systemui") return

        currentPackage = pkg
        val config = runConfig ?: return

        if (armed && !running && pkg == config.targetPackage) {
            armed = false
            running = true
            loopIndex = 0
            pointIndex = 0
            handler.removeCallbacksAndMessages(null)
            toast("Đã vào ứng dụng mục tiêu. Bắt đầu sau ${config.initialDelayMs / 1000} giây")
            handler.postDelayed({ runNext() }, config.initialDelayMs)
            return
        }

        if (running && pkg != config.targetPackage) {
            stopRun("Đã chuyển khỏi ứng dụng mục tiêu")
        }
    }

    override fun onInterrupt() {
        stopRun("Dịch vụ Trợ năng bị gián đoạn")
    }

    override fun onDestroy() {
        stopRun(null)
        if (instance === this) instance = null
        super.onDestroy()
    }

    private fun arm(config: LiteConfig) {
        stopRun(null)
        runConfig = config
        pendingConfig = config
        armed = true
        running = false
        loopIndex = 0
        pointIndex = 0
        handler.postDelayed({
            if (armed && !running) stopRun("Không mở được ứng dụng mục tiêu")
        }, 25_000L)
    }

    private fun runNext() {
        val config = runConfig ?: return stopRun(null)
        if (!running) return

        val power = getSystemService(POWER_SERVICE) as PowerManager
        if (!power.isInteractive) return stopRun("Màn hình đã khóa")
        if (currentPackage != config.targetPackage) return stopRun("Ứng dụng mục tiêu không còn ở phía trước")

        val activePoints = config.points.filter { it.enabled }
        if (activePoints.isEmpty()) return stopRun("Chưa bật điểm thao tác")
        if (loopIndex >= config.loopCount) return stopRun("Đã hoàn thành ${config.loopCount} vòng")

        if (pointIndex >= activePoints.size) {
            pointIndex = 0
            loopIndex++
            if (loopIndex >= config.loopCount) return stopRun("Đã hoàn thành ${config.loopCount} vòng")
        }

        val point = activePoints[pointIndex]
        val metrics = resources.displayMetrics
        val x = point.x.coerceIn(0, (metrics.widthPixels - 1).coerceAtLeast(0))
        val y = point.y.coerceIn(0, (metrics.heightPixels - 1).coerceAtLeast(0))

        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 60L))
            .build()

        val accepted = dispatchGesture(
            gesture,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    pointIndex++
                    handler.postDelayed({ runNext() }, config.intervalMs)
                }

                override fun onCancelled(gestureDescription: GestureDescription) {
                    stopRun("Android đã hủy thao tác chạm")
                }
            },
            handler
        )
        if (!accepted) stopRun("Không thể gửi thao tác chạm")
    }

    private fun stopRun(message: String?) {
        handler.removeCallbacksAndMessages(null)
        armed = false
        running = false
        runConfig = null
        pendingConfig = null
        loopIndex = 0
        pointIndex = 0
        if (!message.isNullOrBlank()) toast(message)
    }

    private fun toast(message: String) {
        handler.post { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
    }
}
