package com.minh.autotouch

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ConfigStore {
    private const val PREF = "auto_touch_config"
    private const val KEY_POINTS = "points"

    fun loadPoints(context: Context): MutableList<PointConfig> {
        val raw = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_POINTS, null)
        if (raw.isNullOrBlank()) return MutableList(10) { PointConfig(it + 1) }
        return runCatching {
            val arr = JSONArray(raw)
            MutableList(10) { index ->
                val o = arr.optJSONObject(index) ?: JSONObject()
                PointConfig(index + 1, o.optBoolean("enabled", index < 2), o.optInt("x", 120 + index * 45), o.optInt("y", 300 + index * 55), runCatching { ActionType.valueOf(o.optString("action", "TAP")) }.getOrDefault(ActionType.TAP), o.optInt("repeat", 1).coerceIn(1, 100), o.optLong("preDelayMs", 300).coerceAtLeast(0), o.optLong("postDelayMs", 300).coerceAtLeast(0), o.optLong("durationMs", 250).coerceAtLeast(50), o.optInt("distancePx", 300).coerceAtLeast(50))
            }
        }.getOrElse { MutableList(10) { PointConfig(it + 1) } }
    }

    fun savePoints(context: Context, points: List<PointConfig>) {
        val arr = JSONArray()
        points.forEach { p -> arr.put(JSONObject().apply { put("enabled", p.enabled); put("x", p.x); put("y", p.y); put("action", p.action.name); put("repeat", p.repeat); put("preDelayMs", p.preDelayMs); put("postDelayMs", p.postDelayMs); put("durationMs", p.durationMs); put("distancePx", p.distancePx) }) }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_POINTS, arr.toString()).apply()
    }

    fun getLoops(context: Context) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt("loops", 0)
    fun setLoops(context: Context, value: Int) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putInt("loops", value).apply()
    fun getBetweenCycles(context: Context) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong("between", 500L)
    fun setBetweenCycles(context: Context, value: Long) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putLong("between", value).apply()
}
