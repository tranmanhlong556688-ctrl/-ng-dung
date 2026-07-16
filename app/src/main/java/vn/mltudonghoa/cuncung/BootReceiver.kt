package vn.mltudonghoa.cuncung

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED &&
            Prefs.autoStart(context) && Settings.canDrawOverlays(context)
        ) {
            val serviceIntent = Intent(context, DogOverlayService::class.java).setAction(DogOverlayService.ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(serviceIntent)
            else context.startService(serviceIntent)
        }
    }
}
