package vn.mltudonghoa.cuncung

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, DogOverlayService::class.java).apply {
            action = DogOverlayService.ACTION_SPEAK
            putExtra(DogOverlayService.EXTRA_TEXT, intent.getStringExtra("message") ?: "Đến giờ rồi!")
            intent.getStringExtra("audio")?.let { putExtra(DogOverlayService.EXTRA_AUDIO, it) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(serviceIntent)
        else context.startService(serviceIntent)
    }
}
