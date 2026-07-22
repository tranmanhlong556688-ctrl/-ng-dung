package vn.minh.lgirremote

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import vn.minh.lgirremote.ir.LgAcProtocol

object TimerScheduler {
    const val KEY_ACTIVE = "timer_active"
    const val KEY_TIME = "timer_time"
    const val KEY_POWER = "timer_power"
    private const val REQUEST_CODE = 2108

    fun schedule(context: Context, triggerAt: Long, profile: LgAcProtocol.Profile, state: LgAcProtocol.State) {
        val prefs = context.getSharedPreferences("lg_ir_remote", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_ACTIVE, true)
            .putLong(KEY_TIME, triggerAt)
            .putBoolean(KEY_POWER, state.power)
            .putString("timer_profile_id", profile.id)
            .putInt("timer_temperature", state.temperature)
            .putString("timer_mode", state.mode.name)
            .putString("timer_fan", state.fan.name)
            .putBoolean("timer_swing", state.swing)
            .apply()

        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, TimerReceiver::class.java).setAction("vn.minh.lgirremote.TIMER"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } else {
            manager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, TimerReceiver::class.java).setAction("vn.minh.lgirremote.TIMER"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        manager.cancel(pending)
        context.getSharedPreferences("lg_ir_remote", Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ACTIVE, false)
            .remove(KEY_TIME)
            .apply()
    }

    fun restore(context: Context) {
        val prefs = context.getSharedPreferences("lg_ir_remote", Context.MODE_PRIVATE)
        val triggerAt = prefs.getLong(KEY_TIME, 0L)
        if (!prefs.getBoolean(KEY_ACTIVE, false) || triggerAt <= System.currentTimeMillis()) return
        val profile = LgAcProtocol.profileById(prefs.getString("timer_profile_id", null)) ?: return
        val state = LgAcProtocol.State(
            power = prefs.getBoolean(KEY_POWER, false),
            temperature = prefs.getInt("timer_temperature", 24),
            mode = runCatching { LgAcProtocol.Mode.valueOf(prefs.getString("timer_mode", "COOL")!!) }.getOrDefault(LgAcProtocol.Mode.COOL),
            fan = runCatching { LgAcProtocol.Fan.valueOf(prefs.getString("timer_fan", "AUTO")!!) }.getOrDefault(LgAcProtocol.Fan.AUTO),
            swing = prefs.getBoolean("timer_swing", false)
        )
        schedule(context, triggerAt, profile, state)
    }
}
