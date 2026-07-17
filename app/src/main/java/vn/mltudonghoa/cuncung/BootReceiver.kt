package vn.mltudonghoa.cuncung

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        if (Prefs.feedbackEnabled(context)) {
            val stage = Prefs.feedbackStage(context)
            val due = Prefs.feedbackDue(context)
            if (stage in 1..2 && due > 0L) {
                FeedbackScheduler.schedule(context, stage, due.coerceAtLeast(System.currentTimeMillis() + 60_000L))
            } else if (stage == 0) {
                FeedbackScheduler.ensureInitial(context)
            }
        }

        if (Prefs.autoStart(context) && Settings.canDrawOverlays(context)) {
            val serviceIntent = Intent(context, DogOverlayService::class.java).setAction(DogOverlayService.ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(serviceIntent)
            else context.startService(serviceIntent)
        }
    }
}
