package com.minh.autokiemtrabinh

import android.content.Context

object Prefs {
    private const val FILE = "auto_kiem_tra_binh"

    private fun p(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun p1X(context: Context) = p(context).getFloat("p1_x", 0f)
    fun p1Y(context: Context) = p(context).getFloat("p1_y", 0f)
    fun p2X(context: Context) = p(context).getFloat("p2_x", 0f)
    fun p2Y(context: Context) = p(context).getFloat("p2_y", 0f)
    fun isP1Set(context: Context) = p(context).getBoolean("p1_set", false)
    fun isP2Set(context: Context) = p(context).getBoolean("p2_set", false)

    fun saveP1(context: Context, x: Float, y: Float) {
        p(context).edit().putFloat("p1_x", x).putFloat("p1_y", y).putBoolean("p1_set", true).apply()
    }

    fun saveP2(context: Context, x: Float, y: Float) {
        p(context).edit().putFloat("p2_x", x).putFloat("p2_y", y).putBoolean("p2_set", true).apply()
    }

    fun clearPoints(context: Context) {
        p(context).edit().remove("p1_x").remove("p1_y").remove("p2_x").remove("p2_y")
            .putBoolean("p1_set", false).putBoolean("p2_set", false).apply()
    }

    fun rounds(context: Context) = p(context).getInt("rounds", 10)
    fun infinite(context: Context) = p(context).getBoolean("infinite", false)
    fun betweenP1(context: Context) = p(context).getLong("between_p1", 150L)
    fun beforeP2(context: Context) = p(context).getLong("before_p2", 300L)
    fun afterP2(context: Context) = p(context).getLong("after_p2", 400L)
    fun tapDuration(context: Context) = p(context).getLong("tap_duration", 60L)

    fun saveTiming(
        context: Context,
        rounds: Int,
        infinite: Boolean,
        betweenP1: Long,
        beforeP2: Long,
        afterP2: Long,
        tapDuration: Long
    ) {
        p(context).edit()
            .putInt("rounds", rounds)
            .putBoolean("infinite", infinite)
            .putLong("between_p1", betweenP1)
            .putLong("before_p2", beforeP2)
            .putLong("after_p2", afterP2)
            .putLong("tap_duration", tapDuration)
            .apply()
    }

    fun controllerX(context: Context) = p(context).getInt("controller_x", 8)
    fun controllerY(context: Context) = p(context).getInt("controller_y", 100)
    fun miniX(context: Context) = p(context).getInt("mini_x", 8)
    fun miniY(context: Context) = p(context).getInt("mini_y", 180)
    fun collapsed(context: Context) = p(context).getBoolean("collapsed", false)

    fun saveControllerPosition(context: Context, x: Int, y: Int) {
        p(context).edit().putInt("controller_x", x).putInt("controller_y", y).apply()
    }

    fun saveMiniPosition(context: Context, x: Int, y: Int) {
        p(context).edit().putInt("mini_x", x).putInt("mini_y", y).apply()
    }

    fun saveCollapsed(context: Context, value: Boolean) {
        p(context).edit().putBoolean("collapsed", value).apply()
    }
}
