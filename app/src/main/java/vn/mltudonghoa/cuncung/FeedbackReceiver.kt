package vn.mltudonghoa.cuncung

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

object FeedbackScheduler {
    private const val ACTION_FEEDBACK = "vn.mltudonghoa.cuncung.FEEDBACK_REMINDER"
    private const val DAY = 24L * 60L * 60L * 1000L

    fun ensureInitial(context: Context) {
        if (!Prefs.feedbackEnabled(context) || Prefs.feedbackStage(context) != 0) return
        schedule(context, 1, System.currentTimeMillis() + DAY)
    }

    fun schedule(context: Context, stage: Int, triggerAt: Long) {
        Prefs.setFeedbackSchedule(context, stage, triggerAt)
        val intent = Intent(context, FeedbackReceiver::class.java).apply {
            action = ACTION_FEEDBACK
            putExtra("stage", stage)
        }
        val pending = PendingIntent.getBroadcast(
            context, 8800 + stage, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarm = context.getSystemService(AlarmManager::class.java)
        runCatching { alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending) }
            .onFailure { alarm.set(AlarmManager.RTC_WAKEUP, triggerAt, pending) }
    }

    fun cancel(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        (1..2).forEach { stage ->
            val pending = PendingIntent.getBroadcast(
                context, 8800 + stage,
                Intent(context, FeedbackReceiver::class.java).setAction(ACTION_FEEDBACK),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pending != null) alarm.cancel(pending)
        }
    }
}

class FeedbackReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!Prefs.feedbackEnabled(context)) return
        val stage = intent?.getIntExtra("stage", Prefs.feedbackStage(context)) ?: 1
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
            FeedbackScheduler.schedule(context, stage, System.currentTimeMillis() + 3L * 60L * 60L * 1000L)
            return
        }

        val channelId = "feedback_suggestions"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(channelId, "Góp ý ML Cún Cưng", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_feedback", true)
            putExtra("feedback_stage", stage)
        }
        val pending = PendingIntent.getActivity(
            context, 8900 + stage, open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = if (stage == 1) "Cún muốn nghe góp ý sau một ngày" else "Đã một tháng rồi, cún muốn được nâng cấp"
        val text = "Chạm để xem mẫu góp ý đã điền sẵn. Thư chỉ gửi khi bạn bấm Gửi."
        val notification = Notification.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(9000 + stage, notification)

        if (stage == 1) {
            FeedbackScheduler.schedule(context, 2, System.currentTimeMillis() + 30L * 24L * 60L * 60L * 1000L)
        } else {
            Prefs.setFeedbackSchedule(context, 3, 0L)
        }
    }
}
