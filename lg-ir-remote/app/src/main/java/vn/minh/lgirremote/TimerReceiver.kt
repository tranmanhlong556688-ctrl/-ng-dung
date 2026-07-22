package vn.minh.lgirremote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import vn.minh.lgirremote.ir.IrTransmitter
import vn.minh.lgirremote.ir.LgAcProtocol

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = context.getSharedPreferences("lg_ir_remote", Context.MODE_PRIVATE)
        val profile = LgAcProtocol.profileById(prefs.getString("timer_profile_id", null))
        if (profile == null) {
            notify(context, "Không thể thực hiện lịch hẹn", "Không tìm thấy cấu hình IR đã lưu.")
            prefs.edit().putBoolean(TimerScheduler.KEY_ACTIVE, false).apply()
            return
        }
        val state = LgAcProtocol.State(
            power = prefs.getBoolean(TimerScheduler.KEY_POWER, false),
            temperature = prefs.getInt("timer_temperature", 24),
            mode = runCatching { LgAcProtocol.Mode.valueOf(prefs.getString("timer_mode", "COOL")!!) }.getOrDefault(LgAcProtocol.Mode.COOL),
            fan = runCatching { LgAcProtocol.Fan.valueOf(prefs.getString("timer_fan", "AUTO")!!) }.getOrDefault(LgAcProtocol.Fan.AUTO),
            swing = prefs.getBoolean("timer_swing", false)
        )
        val raw = LgAcProtocol.encodeState(state)
        val result = IrTransmitter(context).send(profile, raw)
        prefs.edit()
            .putBoolean(TimerScheduler.KEY_ACTIVE, false)
            .putBoolean("power", state.power)
            .putInt("temperature", state.temperature)
            .putString("mode", state.mode.name)
            .putString("fan", state.fan.name)
            .putBoolean("swing", state.swing)
            .apply()
        result.fold(
            onSuccess = { notify(context, "Đã thực hiện lịch hẹn", "Điều hòa đã được ${if (state.power) "bật" else "tắt"} bằng hồng ngoại.") },
            onFailure = { notify(context, "Lịch hẹn không thành công", "Không phát được IR: ${it.message ?: "lỗi không xác định"}") }
        )
    }

    private fun notify(context: Context, title: String, text: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "lg_ir_timer"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(channelId, "Hẹn giờ điều hòa", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val openApp = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(context)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .build()
        }
        manager.notify(2108, notification)
    }
}
