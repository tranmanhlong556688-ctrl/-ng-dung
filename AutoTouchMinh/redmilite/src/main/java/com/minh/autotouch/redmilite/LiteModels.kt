package com.minh.autotouch.redmilite

import android.content.Context

data class LitePoint(
    var enabled: Boolean = false,
    var x: Int = 540,
    var y: Int = 800
)

data class LiteConfig(
    var targetPackage: String = "",
    var targetLabel: String = "Chưa chọn ứng dụng",
    var loopCount: Int = 5,
    var initialDelayMs: Long = 3000L,
    var intervalMs: Long = 600L,
    var points: MutableList<LitePoint> = MutableList(3) { index ->
        LitePoint(enabled = index == 0, x = 540, y = 650 + index * 180)
    }
)

object LiteStore {
    private const val PREFS = "redmi_lite_config"

    fun load(context: Context): LiteConfig {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val config = LiteConfig(
            targetPackage = p.getString("targetPackage", "") ?: "",
            targetLabel = p.getString("targetLabel", "Chưa chọn ứng dụng") ?: "Chưa chọn ứng dụng",
            loopCount = p.getInt("loopCount", 5).coerceIn(1, 100),
            initialDelayMs = p.getLong("initialDelayMs", 3000L).coerceIn(1000L, 15000L),
            intervalMs = p.getLong("intervalMs", 600L).coerceIn(200L, 5000L)
        )
        config.points = MutableList(3) { index ->
            LitePoint(
                enabled = p.getBoolean("p${index}_enabled", index == 0),
                x = p.getInt("p${index}_x", 540).coerceAtLeast(0),
                y = p.getInt("p${index}_y", 650 + index * 180).coerceAtLeast(0)
            )
        }
        return config
    }

    fun save(context: Context, config: LiteConfig) {
        val e = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("targetPackage", config.targetPackage)
            .putString("targetLabel", config.targetLabel)
            .putInt("loopCount", config.loopCount.coerceIn(1, 100))
            .putLong("initialDelayMs", config.initialDelayMs.coerceIn(1000L, 15000L))
            .putLong("intervalMs", config.intervalMs.coerceIn(200L, 5000L))
        config.points.take(3).forEachIndexed { index, point ->
            e.putBoolean("p${index}_enabled", point.enabled)
                .putInt("p${index}_x", point.x.coerceAtLeast(0))
                .putInt("p${index}_y", point.y.coerceAtLeast(0))
        }
        e.apply()
    }
}
