package com.minh.autotouch

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class AutoAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile
        var instance: AutoAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    fun tap(x: Float, y: Float, duration: Long = 80L, done: (() -> Unit)? = null) {
        gesture(x, y, x, y, duration, done)
    }

    fun swipe(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        duration: Long,
        done: (() -> Unit)? = null
    ) {
        gesture(x1, y1, x2, y2, duration, done)
    }

    private fun gesture(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        duration: Long,
        done: (() -> Unit)?
    ) {
        val path = Path().apply {
            moveTo(x1, y1)
            if (x1 != x2 || y1 != y2) lineTo(x2, y2)
        }
        val description = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    path,
                    0,
                    duration.coerceAtLeast(50L)
                )
            )
            .build()

        dispatchGesture(description, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                done?.invoke()
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                done?.invoke()
            }
        }, null)
    }
}
