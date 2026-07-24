package com.minh.autotouch

import org.json.JSONArray
import org.json.JSONObject

enum class ActionType(val label: String) {
    TAP("Nhấp 1 lần"),
    MULTI_TAP("Nhấp nhiều lần"),
    DOUBLE_TAP("Nhấp đúp"),
    LONG_PRESS("Nhấn giữ"),
    SWIPE_UP("Vuốt lên"),
    SWIPE_DOWN("Vuốt xuống"),
    SWIPE_LEFT("Vuốt trái"),
    SWIPE_RIGHT("Vuốt phải"),
    SWIPE_CUSTOM("Vuốt đến tọa độ đích"),
    WAIT("Chờ")
}

data class StepConfig(
    var enabled: Boolean = false,
    var name: String = "",
    var action: ActionType = ActionType.TAP,
    var x: Int = 300,
    var y: Int = 500,
    var endX: Int = 300,
    var endY: Int = 200,
    var repeatCount: Int = 1,
    var preDelayMs: Long = 300,
    var intervalMs: Long = 250,
    var durationMs: Long = 500,
    var postDelayMs: Long = 300
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("enabled", enabled)
        put("name", name)
        put("action", action.name)
        put("x", x)
        put("y", y)
        put("endX", endX)
        put("endY", endY)
        put("repeatCount", repeatCount)
        put("preDelayMs", preDelayMs)
        put("intervalMs", intervalMs)
        put("durationMs", durationMs)
        put("postDelayMs", postDelayMs)
    }

    companion object {
        fun fromJson(o: JSONObject): StepConfig = StepConfig(
            enabled = o.optBoolean("enabled", false),
            name = o.optString("name", ""),
            action = runCatching {
                ActionType.valueOf(o.optString("action", "TAP"))
            }.getOrDefault(ActionType.TAP),
            x = o.optInt("x", 300),
            y = o.optInt("y", 500),
            endX = o.optInt("endX", 300),
            endY = o.optInt("endY", 200),
            repeatCount = o.optInt("repeatCount", 1).coerceIn(1, 100),
            preDelayMs = o.optLong("preDelayMs", 300).coerceAtLeast(0),
            intervalMs = o.optLong("intervalMs", 250).coerceAtLeast(120),
            durationMs = o.optLong("durationMs", 500).coerceIn(50, 60_000),
            postDelayMs = o.optLong("postDelayMs", 300).coerceAtLeast(0)
        )
    }
}

data class AppConfig(
    var profileName: String = "Cấu hình mặc định",
    var allowedPackagesRaw: String = "",
    var loopCount: Int = 1,
    var loopDelayMs: Long = 500,
    var countdownSec: Int = 3,
    var markerSizeDp: Int = 46,
    var markerAlphaPercent: Int = 80,
    var markersLocked: Boolean = false,
    val steps: MutableList<StepConfig> = MutableList(10) { index ->
        StepConfig(
            enabled = index < 2,
            name = "Điểm ${index + 1}",
            x = 120 + (index % 5) * 110,
            y = 350 + (index / 5) * 150
        )
    }
) {
    fun allowedPackages(): Set<String> = allowedPackagesRaw
        .split(',', ';', '\n', ' ')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()

    fun toJson(): JSONObject = JSONObject().apply {
        put("version", 2)
        put("profileName", profileName)
        put("allowedPackagesRaw", allowedPackagesRaw)
        put("loopCount", loopCount)
        put("loopDelayMs", loopDelayMs)
        put("countdownSec", countdownSec)
        put("markerSizeDp", markerSizeDp)
        put("markerAlphaPercent", markerAlphaPercent)
        put("markersLocked", markersLocked)
        put("steps", JSONArray().apply { steps.forEach { put(it.toJson()) } })
    }

    fun deepCopy(): AppConfig = fromJson(toJson())

    companion object {
        fun fromJson(o: JSONObject): AppConfig {
            val importedLoopCount = o.optInt("loopCount", 1)
            val config = AppConfig(
                profileName = o.optString("profileName", "Cấu hình mặc định"),
                allowedPackagesRaw = o.optString("allowedPackagesRaw", ""),
                loopCount = if (importedLoopCount <= 0) 1 else importedLoopCount.coerceIn(1, 999),
                loopDelayMs = o.optLong("loopDelayMs", 500).coerceAtLeast(200),
                countdownSec = o.optInt("countdownSec", 3).coerceIn(1, 30),
                markerSizeDp = o.optInt("markerSizeDp", 46).coerceIn(28, 90),
                markerAlphaPercent = o.optInt("markerAlphaPercent", 80).coerceIn(20, 100),
                markersLocked = o.optBoolean("markersLocked", false),
                steps = mutableListOf()
            )
            val arr = o.optJSONArray("steps") ?: JSONArray()
            for (i in 0 until 10) {
                config.steps.add(
                    if (i < arr.length()) StepConfig.fromJson(arr.getJSONObject(i))
                    else StepConfig(name = "Điểm ${i + 1}")
                )
            }
            return config
        }
    }
}
