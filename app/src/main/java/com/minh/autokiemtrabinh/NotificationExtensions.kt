package com.minh.autokiemtrabinh

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

internal fun AutoAccessibilityService.createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= 26) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                AutoAccessibilityService.CHANNEL_ID,
                "Điều khiển Auto Kiểm Tra Bình",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Dừng hoặc tạm dừng thao tác tự động." }
        )
    }
}

internal fun AutoAccessibilityService.notifyState() {
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val showIntent = PendingIntent.getBroadcast(
        this, 1,
        Intent(this, ControlActionReceiver::class.java).setAction(AutoAccessibilityService.ACTION_SHOW),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val pauseIntent = PendingIntent.getBroadcast(
        this, 2,
        Intent(this, ControlActionReceiver::class.java).setAction(AutoAccessibilityService.ACTION_PAUSE_RESUME),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val stopIntent = PendingIntent.getBroadcast(
        this, 3,
        Intent(this, ControlActionReceiver::class.java).setAction(AutoAccessibilityService.ACTION_STOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val stateLabel = when (state) {
        RunState.IDLE -> "Đã dừng"
        RunState.COUNTDOWN -> "Đang đếm ngược"
        RunState.RUNNING -> "Đang chạy – $completedRounds vòng"
        RunState.PAUSED -> "Đang tạm dừng"
        RunState.COMPLETED -> "Đã hoàn thành"
        RunState.ERROR -> "Có lỗi"
    }
    val notification = NotificationCompat.Builder(this, AutoAccessibilityService.CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_app_icon)
        .setContentTitle("Auto Kiểm Tra Bình – Minh")
        .setContentText(stateLabel)
        .setOngoing(state == RunState.RUNNING || state == RunState.COUNTDOWN || state == RunState.PAUSED)
        .setOnlyAlertOnce(true)
        .setContentIntent(showIntent)
        .addAction(0, if (state == RunState.PAUSED) "Tiếp tục" else "Tạm dừng", pauseIntent)
        .addAction(0, "Dừng khẩn cấp", stopIntent)
        .build()
    runCatching { manager.notify(AutoAccessibilityService.NOTIFICATION_ID, notification) }
}

internal fun AutoAccessibilityService.cancelNotification() {
    (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        .cancel(AutoAccessibilityService.NOTIFICATION_ID)
}
