package com.minh.autokiemtrabinh

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ControlActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        AutoAccessibilityService.instance?.handleExternalAction(action)
    }
}
