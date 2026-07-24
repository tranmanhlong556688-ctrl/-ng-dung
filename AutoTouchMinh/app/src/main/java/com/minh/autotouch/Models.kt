package com.minh.autotouch

enum class ActionType { TAP, MULTI_TAP, LONG_PRESS, SWIPE_UP, SWIPE_DOWN, SWIPE_LEFT, SWIPE_RIGHT, WAIT }

data class PointConfig(
    val id: Int,
    var enabled: Boolean = id <= 2,
    var x: Int = 120 + (id - 1) * 45,
    var y: Int = 300 + (id - 1) * 55,
    var action: ActionType = ActionType.TAP,
    var repeat: Int = 1,
    var preDelayMs: Long = 300,
    var postDelayMs: Long = 300,
    var durationMs: Long = 250,
    var distancePx: Int = 300
)
